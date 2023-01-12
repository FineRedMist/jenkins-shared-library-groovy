package org.sample.jenkins

enum BuildNotifyStatus {
    Pending("started", null, GitHubStatus.Pending, null),
    Unstable("unstable", "warning", GitHubStatus.Failure, 'UNSTABLE'),
    Failure("failed", "danger", GitHubStatus.Failure, 'FAILURE'),
    Success("successful", "good", GitHubStatus.Success, 'SUCCESS')

    String notifyText
    String slackColour
    GitHubStatus githubStatus
    String jenkinsStatus

    BuildNotifyStatus(String text, String colour, GitHubStatus github, String jenkins) {
        notifyText = text
        slackColour = colour
        githubStatus = github
        jenkinsStatus = jenkins
    }

    static BuildNotifyStatus fromText(String status) {
        if(status == null) {
            return Success
        }
        for(BuildNotifyStatus en in [Failure, Unstable, Success]) {
            if(en.jenkinsStatus == status) {
                return en
            }
        }
        throw new Exception("Unexpected build status: ${status}")
    }
}
