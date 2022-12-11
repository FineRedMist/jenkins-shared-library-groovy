package org.sample.jenkins

enum GitHubStatus {
    Pending('PENDING'),
    Failure('FAILURE'),
    Success('SUCCESS')

    String githubState

    GitHubStatus(String state) {
        githubState = state
    }
}
