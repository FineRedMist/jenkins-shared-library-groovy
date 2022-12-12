package org.sample.jenkins

import org.sample.Jenkins.*

enum GitHubStatus {
    Pending('PENDING'),
    Failure('FAILURE'),
    Success('SUCCESS')

    String githubState

    GitHubStatus(String state) {
        githubState = state
    }

    private void setStatus(Configuration config, String message) {
        if(!config || !config.getSendGitHubStatus()) {
            return
        }
        
        String gitRepo = ""
        String gitOwner = ""
        String gitSha = ""

        def script = config.script
        def env = config.env

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
            script.echo "Could not identify the GitHub repository, owner, or SHA digest of the commit."
            return
        }

        script.echo "Setting build status for owner ${gitOwner} and repository ${gitRepo} to: (${githubState}) ${message}"
        script.githubNotify(credentialsId: config.getGitHubStatusCredentialsId(), repo: gitRepo, account: gitOwner, sha: gitSha, context: config.getGitHubStatusName(), description: message, status: githubState, targetUrl: env.BUIlD_URL)
    }

}
