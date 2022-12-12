package org.sample.jenkins

import org.sample.Jenkins.*
import org.jenkinsci.plugins.workflow.cps.CpsScript

class SlackBuilder {
    private CpsScript script
    private Configuration config
    private def env
    private List threadedMessages = []

    SlackBuilder(CpsScript script, Configuration config) {
        this.script = script
        this.env = script.env
        this.config = config
    }

    void addThreadedMessage(String message) {
        threadedMessages << message
    }

    void send(BuildNotifyStatus status) {
        if(!config 
            || !config.getSendSlack() 
            || (status == BuildNotifyStatus.Pending && !config.getSendSlackStartNotification())) {
            return
        }

        def sent = script.slackSend(channel: config.getSlackChannel(), color: status.slackColour, message: "Build ${status.notifyText}: <${env.BUILD_URL}|${env.JOB_NAME} #${env.BUILD_NUMBER}>")
        threadedMessages.each { message ->
            if(message.length() > 0) {
                script.slackSend(channel: sent.threadId, color: status.slackColour, message: message)
            }
        }
    }
}