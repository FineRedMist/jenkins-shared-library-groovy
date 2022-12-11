package org.sample.jenkins

enum BuildNotifyStatus {
    Pending("started", null, GitHubStatus.Pending),
    Unstable("unstable", "warning", GitHubStatus.Failure),
    Failure("failed", "danger", GitHubStatus.Failure),
    Success("successful", "good", GitHubStatus.Success)

    String notifyText
    String slackColour
    GitHubStatus githubStatus

    BuildNotifyStatus(String text, String colour, GitHubStatus github) {
        notifyText = text
        slackColour = colour
        githubStatus = github
    }
}
