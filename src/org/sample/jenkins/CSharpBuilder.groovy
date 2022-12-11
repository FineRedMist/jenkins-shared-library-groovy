package org.sample.jenkins

import org.sample.Jenkins.*

import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.jenkinsci.plugins.pipeline.modeldefinition.Utils

import groovy.xml.*

class CSharpBuilder {
    List testResults = []
    List analyses = []
    CpsScript script
    def env = {}
    Configuration config = null

    CSharpBuilder(CpsScript script) {
        this.script = script
        this.env = script.env
    }

    void run(nodeLabel = null) {
        script.node(label: nodeLabel) {
            try {
                wrappedRun()
            } catch (e) {
                notifyBuildStatus(BuildNotifyStatus.Failure)
                throw e
            } finally {
                try {
                    def analysisIssues = script.scanForIssues(tool: script.msBuild())
                    analyses << analysisIssues
                    def analysisText = getAnaylsisResultsText(analysisIssues)
                    if(analysisText.length() > 0) {
                        testResults << "Build warnings and errors:\n" + analysisText
                    } else {
                        testResults << "No build warnings or errors."
                    }
                    // Rescan. If we collect and then aggregate, warnings become errors
                    script.recordIssues(aggregatingResults: true, skipPublishingChecks: true, tool: script.msBuild())

                    def currentResult = script.currentBuild.result ?: 'SUCCESS'
                    if (currentResult == 'UNSTABLE') {
                        notifyBuildStatus(BuildNotifyStatus.Unstable)
                    } else if (currentResult == 'SUCCESS') {
                        notifyBuildStatus(BuildNotifyStatus.Success)
                    } else {
                        script.echo("Unexpected build status! ${currentResult}")
                    }
                } catch (e) {
                    notifyBuildStatus(BuildNotifyStatus.Failure)
                } finally {
                    script.cleanWs()
                }
            }
        }        
    }

    private boolean isMainBranch() {
        return env.BRANCH_NAME.toLowerCase() in ['main', 'master']
    }

    private void wrappedRun()
    {
        // Populate the workspace
        script.checkout(script.scm)

        // Can't access files until we have a node and workspace.
        config = Configuration.read(script, 'Configuration.json')

        // Configure properties and triggers.
        List properties = []
        List triggers = []
        triggers.add(script.pollSCM(config.getScmTrigger()))
        properties.add(script.disableConcurrentBuilds())
        properties.add(script.disableResume())
        properties.add(script.pipelineTriggers(triggers))
        script.properties(properties)

        script.stage('Send Start Notification') {
            notifyBuildStatus(BuildNotifyStatus.Pending)
        }
        script.stage('Setup for forensics') {
            script.discoverGitReferenceBuild()
        }
        script.stage('Restore NuGet For Solution') {
            //  '--no-cache' to avoid a shared cache--if multiple projects are running NuGet restore, they can collide.
            script.bat("dotnet restore --nologo --no-cache")
        }
        script.stage('Build Solution - Debug') {
            script.bat("dotnet build --nologo -c Debug -p:PackageVersion=${config.getNugetVersion()} -p:Version=${config.getVersion()} --no-restore")
        }
        script.stage('Run Tests') {
            // MSTest projects automatically include coverlet that can generate cobertura formatted coverage information.
            script.bat("""
                dotnet test --nologo -c Debug --results-directory TestResults --logger trx --collect:"XPlat code coverage" --no-restore --no-build
                """)
        }
        script.stage('Publish Test Output') {
            def tests = gatherTestResults('TestResults/**/*.trx')
            def coverage = gatherCoverageResults('TestResults/**/In/**/*.cobertura.xml')
            testResults << "\n${tests}\n${coverage}"
            script.mstest(testResultsFile:"TestResults/**/*.trx", failOnError: true, keepLongStdio: true)
        }
        script.stage('Publish Code Coverage') {
            script.publishCoverage(adapters: [
                script.coberturaAdapter(path: "TestResults/**/In/**/*.cobertura.xml", thresholds: [
                [thresholdTarget: 'Group', unhealthyThreshold: 100.0],
                [thresholdTarget: 'Package', unhealthyThreshold: 100.0],
                [thresholdTarget: 'File', unhealthyThreshold: 50.0, unstableThreshold: 85.0],
                [thresholdTarget: 'Class', unhealthyThreshold: 50.0, unstableThreshold: 85.0],
                [thresholdTarget: 'Method', unhealthyThreshold: 50.0, unstableThreshold: 85.0],
                [thresholdTarget: 'Instruction', unhealthyThreshold: 0.0, unstableThreshold: 0.0],
                [thresholdTarget: 'Line', unhealthyThreshold: 50.0, unstableThreshold: 85.0],
                [thresholdTarget: 'Conditional', unhealthyThreshold: 0.0, unstableThreshold: 0.0],
                ])
            ], failNoReports: true, failUnhealthy: true, calculateDiffForChangeRequests: true)
        }
        script.stage('Clean') {
            script.bat("dotnet clean --nologo")
        }
        script.stage('Build Solution - Release') {
            script.bat("dotnet build --nologo -c Release -p:PackageVersion=${config.getNugetVersion()} -p:Version=${config.getVersion()} --no-restore")
        }
        script.stage('Run Security Scan') {
            script.bat("dotnet new tool-manifest")
            script.bat("dotnet tool install --local security-scan --no-cache")

            def slnFile = ""
            // Search the repository for a file ending in .sln.
            script.findFiles(glob: '**').each {
                def path = it.toString();
                if(path.toLowerCase().endsWith('.sln')) {
                    slnFile = path;
                }
            }
            if(slnFile.length() == 0) {
                throw new Exception('No solution files were found to build in the root of the git repository.')
            }
            script.bat("""
                dotnet security-scan ${slnFile} --excl-proj=**/*Test*/** -n --cwe --export=sast-report.sarif
                """)

            def analysisIssues = script.scanForIssues(tool: script.sarif(pattern: 'sast-report.sarif'))
            analyses << analysisIssues
            def analysisText = getAnaylsisResultsText(analysisIssues)
            if(analysisText.length() > 0) {
                testResults << "Static analysis results:\n" + analysisText
            } else {
                testResults << "No static analysis issues to report."
            }
            // Rescan. If we collect and then aggregate, warnings become errors
            script.recordIssues(aggregatingResults: true, enabledForFailure: true, failOnError: true, skipPublishingChecks: true, tool: script.sarif(pattern: 'sast-report.sarif'))
        }
        script.stage('Preexisting NuGet Package Check') {
            // Find all the nuget packages to publish.
            def nugetSource = config.getNugetSource()
            if(!nugetSource) {
                script.echo "Both 'NugetSource' and 'NugetKeyCredentialsId' are required to for nuget operations."
                Utils.markStageSkippedForConditional('Preexisting NuGet Package Check')
            } else {
                def tool = script.tool('NuGet-2022')
                def packageText = script.bat(returnStdout: true, script: "\"${tool}\" list -NonInteractive -Source \"${nugetSource}\"")
                packageText = packageText.replaceAll("\r", "")
                def packages = new ArrayList(packageText.split("\n").toList())
                packages.removeAll { line -> line.toLowerCase().startsWith("warning: ") }
                packages = packages.collect { pkg -> pkg.replaceAll(' ', '.') }

                def nupkgFiles = "**/*.nupkg"
                script.findFiles(glob: nupkgFiles).each { nugetPkg ->
                    def pkgName = nugetPkg.getName()
                    pkgName = pkgName.substring(0, pkgName.length() - 6) // Remove extension
                    if(packages.contains(pkgName)) {
                        script.error "The package ${pkgName} is already in the NuGet repository."
                    } else {
                        script.echo "The package ${nugetPkg} is not in the NuGet repository."
                    }
                }
            }
        }
        script.stage('NuGet Publish') {
            def nugetSource = config.getNugetSource()
            // We are only going to publish to NuGet when the branch is main or master.
            // This way other branches will test without interfering with releases.
            if(nugetSource && isMainBranch()) {
                script.withCredentials([script.string(credentialsId: config.getNugetKeyCredentialsId(), variable: 'APIKey')]) { 
                    // Find all the nuget packages to publish.
                    def nupkgFiles = "**/*.nupkg"
                    script.findFiles(glob: nupkgFiles).each { nugetPkg ->
                        script.bat("""
                            dotnet nuget push \"${nugetPkg}\" --api-key "%APIKey%" --source "${nugetSource}"
                            """)
                    }
                }
            } else {
                Utils.markStageSkippedForConditional('NuGet Publish')
            }
        }
    }

    private String readTextFile(String filePath) {
        def bin64 = script.readFile(file: filePath, encoding: 'Base64')
        def binDat = bin64.decodeBase64()

        if(binDat.size() >= 3 
            && binDat[0] == -17
            && binDat[1] == -69
            && binDat[2] == -65) {
            return new String(binDat, 3, binDat.size() - 3, "UTF-8")
        } else {
            return new String(binDat)
        }
    }

    private void notifyBuildStatus(BuildNotifyStatus status, List<String> testResults = []) {
        if(config && config.getSendSlack() && (status != BuildNotifyStatus.Pending || config.getSendSlackStartNotification())) {
            def sent = script.slackSend(channel: config.getSlackChannel(), color: status.slackColour, message: "Build ${status.notifyText}: <${env.BUILD_URL}|${env.JOB_NAME} #${env.BUILD_NUMBER}>")
            testResults.each { message ->
                if(message.length() > 0) {
                    script.slackSend(channel: sent.threadId, color: status.slackColour, message: message)
                }
            }
        }
        setBuildStatus("Build ${status.notifyText}", status.githubStatus)
    }

    private String gatherTestResults(String searchPath) {
        def total = 0
        def passed = 0
        def failed = 0

        script.findFiles(glob: searchPath).each { f ->
            String fullName = f

            def data = readTextFile(fullName)

            def trx = new XmlParser(false, true, true).parseText(data)

            def counters = trx['ResultSummary']['Counters']

            // echo 'Getting counter values...'
            total += counters['@total'][0].toInteger()
            passed += counters['@passed'][0].toInteger()
            failed += counters['@failed'][0].toInteger()
        }

        if(total == 0) {
            return "No test results found."
        } else if(failed == 0) {
            if(passed == 1) {
                return "The only test passed!"
            } else {
                return "All ${total} tests passed!"
            }
        } else {
            return "${failed} of ${total} tests failed!"
        }
    }

    private String gatherCoverageResults(String searchPath) {
        def linesCovered = 0
        def linesValid = 0
        def files = 0

        script.findFiles(glob: searchPath).each { f ->
            String fullName = f

            def data = readTextFile(fullName)

            def cover = new XmlParser(false, true, true).parseText(data)

            linesCovered += cover['@lines-covered'].toInteger()
            linesValid += cover['@lines-valid'].toInteger()
            files += 1
        }

        if(files == 0) {
            return "No code coverage results were found to report."
        } else if(linesValid == 0) {
            return "No code lines were found to collect test coverage for."
        } else {
            def pct = linesCovered.toDouble() * 100 / linesValid.toDouble()
            return "${linesCovered} of ${linesValid} lines were covered by testing (${pct.round(1)}%)."
        }
    }

    private String getAnaylsisResultsText(def analysisResults) {
        String issues = ""
        analysisResults.getIssues().each { issue ->
            issues = issues + "* ${issue}\n"
        }
        return issues
    }

    private void setBuildStatus(String message, GitHubStatus state) {
        if(!config || !config.getSendGitHubStatus()) {
            return
        }
        String gitRepo = ""
        String gitOwner = ""
        String gitSha = ""

        gitSha = env.GIT_COMMIT
        if(env.GIT_URL && env.GIT_URL.toLowerCase().endsWith('.git')) {
            def matcher = env.GIT_URL =~ /.*[:\/](?<owner>[^:\/]*)\/(?<repo>.*)\.git/
            if(matcher.find()) {
                gitRepo = matcher.group("repo")
                gitOwner = matcher.group("owner")
            }
        }

        if(gitRepo.length() == 0
            || gitOwner.length() == 0
            || gitSha.length() == 0) {
            return
        }

        script.echo "Setting build status for owner ${gitOwner} and repository ${gitRepo} to: (${state}) ${message}"
        githubNotify(credentialsId: config.getGitHubStatusCredentialsId(), repo: gitRepo, account: gitOwner, sha: gitSha, context: config.getGitHubStatusName(), description: message, status: state.githubState, targetUrl: env.BUIlD_URL)
    }

}