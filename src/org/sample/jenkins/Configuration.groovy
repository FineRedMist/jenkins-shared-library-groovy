package org.sample.jenkins

import org.jenkinsci.plugins.workflow.cps.CpsScript

class Configuration {

    // Cached here so it is easily sharable when passing the configuration around.
    CpsScript script
    // The environment of the executing script.
    def env

    private String version = null
    private String nugetVersion = null
    private String scmTrigger = 'H * * * *'
    private boolean runTests = true
    private String nugetSource = null
    private String slackChannel = null
    private boolean sendSlack = true
    private boolean sendSlackStartNotification = true
    private String gitHubStatusName = null
    private String gitHubStatusCredentialsId = null
    private String nugetKeyCredentialsId = null
    private String testBuildConfiguration = 'Debug'
    private String nugetBuildConfiguration = 'Release'
    private List coverageThresholds = [
                    [thresholdTarget: 'Group', unhealthyThreshold: 100.0],
                    [thresholdTarget: 'Package', unhealthyThreshold: 100.0],
                    [thresholdTarget: 'File', unhealthyThreshold: 50.0, unstableThreshold: 85.0],
                    [thresholdTarget: 'Class', unhealthyThreshold: 50.0, unstableThreshold: 85.0],
                    [thresholdTarget: 'Method', unhealthyThreshold: 50.0, unstableThreshold: 85.0],
                    [thresholdTarget: 'Instruction', unhealthyThreshold: 0.0, unstableThreshold: 0.0],
                    [thresholdTarget: 'Line', unhealthyThreshold: 50.0, unstableThreshold: 85.0],
                    [thresholdTarget: 'Conditional', unhealthyThreshold: 0.0, unstableThreshold: 0.0],
                ]

    private Configuration() {
    }

    static Configuration read(CpsScript script, String file) {
        def config = new Configuration()
        config.script = script
        config.env = script.env

        config.version = "1.0.0.${script.env.BUILD_NUMBER}"
        config.nugetVersion = config.version

        if(!script.fileExists(file)) {
            return config
        }
        def data = script.readJSON(file: file)
        
        config.processVersion(script, data)

        config.scmTrigger = getValue(data, 'ScmTrigger', config.scmTrigger)
        config.runTests = getValue(data, 'RunTests', config.runTests)
        config.nugetSource = getValue(data, 'NugetSource', config.nugetSource)
        config.sendSlack = getValue(data, 'SendSlack', config.sendSlack)
        config.sendSlackStartNotification = getValue(data, 'SendSlackStartNotification', config.sendSlackStartNotification)
        config.gitHubStatusName = getValue(data, 'GitHubStatusName', config.gitHubStatusName)
        config.gitHubStatusCredentialsId = getValue(data, 'GitHubStatusCredentialsId', config.gitHubStatusCredentialsId)
        config.slackChannel = getValue(data, 'SlackChannel', config.slackChannel)
        config.nugetKeyCredentialsId = getValue(data, 'NugetKeyCredentialsId', config.nugetKeyCredentialsId)
        config.coverageThresholds = getValue(data, 'CoverageThresholds', config.coverageThresholds)
        config.testBuildConfiguration = getValue(data, 'TestBuildConfiguration', config.testBuildConfiguration)
        config.nugetBuildConfiguration = getValue(data, 'NugetBuildConfiguration', config.nugetBuildConfiguration)

        return config
    }

    private void processVersion(CpsScript script, Map data) {
        if(data.containsKey('Version')) {
            def buildVersion = data['Version']
            // Count the parts, and add any missing zeroes to get up to 3, then add the build version.
            def parts = new ArrayList(buildVersion.split('\\.').toList())
            while(parts.size() < 3) {
                parts << "0"
            }
            // The nuget version does not include the build number.
            nugetVersion = parts.join('.')
            if(parts.size() < 4) {
                parts << script.env.BUILD_NUMBER
            }
            // This version is for the file and assembly versions.
            version = parts.join('.')
        }
    }

    private static def getValue(Map data, String key, def defaultValue) {
        if(data.containsKey(key)) {
            return data[key]
        }
        return defaultValue
    }

    String getVersion() {
        return version
    }

    String getNugetVersion() {
        return nugetVersion
    }

    String getScmTrigger() {
        return scmTrigger
    }

    String getRunTests() {
        return runTests
    }

    String getNugetSource() {
        return nugetSource
    }

    boolean getSendSlack() {
        return sendSlack
    }

    boolean getSendSlackStartNotification() {
        return sendSlackStartNotification
    }

    boolean getSendGitHubStatus() {
        return gitHubStatusName && gitHubStatusCredentialsId
    }

    String getGitHubStatusName() {
        return gitHubStatusName
    }

    String getSlackChannel() {
        return slackChannel
    }

    String getNugetKeyCredentialsId() {
        return nugetKeyCredentialsId
    }

    String getGitHubStatusCredentialsId() {
        return gitHubStatusCredentialsId
    }

    Map getCoverageThresholds() {
        return coverageThresholds
    }

    String getTestBuildConfiguration() {
        return testBuildConfiguration
    }
    String getNugetBuildConfiguration() {
        return nugetBuildConfiguration
    }
}