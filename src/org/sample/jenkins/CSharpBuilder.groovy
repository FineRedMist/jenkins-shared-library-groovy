package org.sample.jenkins

import org.jenkinsci.plugins.workflow.cps.CpsScript
import groovy.xml.*

class CSharpBuilder {
    List testResults = []
    String version
    String nugetVersion
    List analyses = []
    CpsScript script
    def env = {}
    def config = {}

    CSharpBuilder(CpsScript script) {
        this.script = script
        this.env = script.env
        this.version = "1.0.0.${env.BUILD_NUMBER}"
        this.nugetVersion = version
    }

    void getScmTrigger() {
        if(config.containsKey('scmTrigger')) {
            return config['scmTrigger']
        }
        return 'H * * * *'
    }

    void getNodeLabel() {
        if(config.containsKey('nodeLabel')) {
            return config['nodeLabel']
        }
        return 'any'
    }

    void run() {
        if(script.fileExists('Configuration.json')) {
            config = script.readJSON(file: 'Configuration.json')
        }
        List properties = []
        List triggers = []
        triggers.add(script.pollSCM(getScmTrigger()))
        properties.add(script.disableConcurrentBuilds())
        properties.add(script.disableResume())
        properties.add(script.pipelineTriggers(triggers))
        script.properties(properties)

        script.node(label: getNodeLabel()) {
            script.stage('Test') {
                script.echo 'Test'
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
        def sent = slackSend(channel: '#build-notifications', color: status.slackColour, message: "Build ${status.notifyText}: <${env.BUILD_URL}|${env.JOB_NAME} #${env.BUILD_NUMBER}>")
        testResults.each { message ->
            if(message.length() > 0) {
                slackSend(channel: sent.threadId, color: status.slackColour, message: message)
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
        githubNotify(credentialsId: 'GitHub-Status-Notify', repo: gitRepo, account: gitOwner, sha: gitSha, context: 'Status', description: message, status: state.githubState, targetUrl: env.BUIlD_URL)
    }

}