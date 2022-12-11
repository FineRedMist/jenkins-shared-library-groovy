package org.sample.jenkins

import org.jenkinsci.plugins.workflow.cps.CpsScript

class Configuration {

    private String version = null
    private String nugetVersion = null
    private String scmTrigger = 'H * * * *'
    private String nugetSource = null
    private String slackChannel = null
    private boolean sendSlack = true
    private boolean sendSlackStartNotification = true
    private String gitHubStatusName = null
    private String gitHubStatusCredentialsId = null
    private String nugetKeyCredentialsId = null

    private Configuration() {
    }

    static Configuration read(CpsScript script, String file) {
        def config = new Configuration()
        config.version = "1.0.0.${script.env.BUILD_NUMBER}"
        config.nugetVersion = version

        if(!script.fileExists(file)) {
            return config
        }
        def data = script.readJSON(file: file)
        
        config.processVersion(script, data)

        config.scmTrigger = getValue(data, 'ScmTrigger', config.scmTrigger)
        config.nugetSource = getValue(data, 'NugetSource', config.nugetSource)
        config.sendSlack = getValue(data, 'SendSlack', config.sendSlack)
        config.sendSlackStartNotification = getValue(data, 'SendSlackStartNotification', config.sendSlackStartNotification)
        config.gitHubStatusName = getValue(data, 'GitHubStatusName', config.gitHubStatusName)
        config.gitHubStatusCredentialsId = getValue(data, 'GitHubStatusCredentialsId', config.gitHubStatusCredentialsId)
        config.slackChannel = getValue(data, 'SlackChannel', config.slackChannel)
        config.nugetKeyCredentialsId = getValue(data, 'NugetKeyCredentialsId', config.nugetKeyCredentialsId)

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
}