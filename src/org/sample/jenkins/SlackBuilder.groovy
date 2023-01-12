package org.sample.jenkins

import org.sample.Jenkins.*
import org.jenkinsci.plugins.workflow.cps.CpsScript

class SlackBuilder {
    private CpsScript script
    private Configuration config
    private def env
    // Messages added to the thread
    private List threadedMessages = []
    // Messages added in the main message
    private List inlineMessages = []

    SlackBuilder(Configuration config) {
        this.config = config
        this.script = config.script
        this.env = config.env
    }

    boolean isEnabled() {
        return config && config.getSendSlack() 
    }

    boolean isEnabledForStart() {
        return isEnabled() && config.getSendSlackStartNotification()
    }

    void addThreadedMessage(String message) {
        threadedMessages << message
    }

    void addInlineMessage(String message) {
        inlineMessages << message
    }

    String getInlineMessages() {
        if(inlineMessages.size()) {
            return "\n\n" + inlineMessages.join("\n\n")
        }
        return ""
    }

    void send(BuildNotifyStatus status) {
        if(!isEnabled()
            || (status == BuildNotifyStatus.Pending && !isEnabledForStart())) {
            return
        }

        def sent = script.slackSend(channel: config.getSlackChannel(), color: status.slackColour, message: "Build ${status.notifyText}: <${env.BUILD_URL}|${env.JOB_NAME} #${env.BUILD_NUMBER}>${getInlineMessages()}")
        threadedMessages.each { message ->
            if(message.length() > 0) {
                script.slackSend(channel: sent.threadId, color: status.slackColour, message: message)
            }
        }
    }
}