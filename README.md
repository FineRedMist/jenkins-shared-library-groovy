# JenkinsLibrarySample
This is a sample library that factors out common functionality for building C# projects for [JenkinsSample](https://github.com/FineRedMist/JenkinsSample)

The script is driven by a Configuration.json that is in the root of the repository. The expected values are:
* Version: Typically a x.y.z version (trailing zeroes optional).
* ScmTrigger: [optional] The frequency to poll source control for changes.
* Nuget Configuration Options
    * NugetSource: [optional] The nuget repository to push nuget packages to. If not set, it will not be queried or pushed to.
    * NugetKeyCredentialsId: [optional] The credentials for pushing to a nuget repository. If not set, nuget will not be queried or pushed to.
* Slack Configuration Options
    * SlackChannel: [optional] The Slack channel to post build status messages to. If not set, it will use the default channel as part of the general Slack configuration.
    * SendSlack: [optional, default true] Whether to send Slack messages.
    * SendSlackStartNotification: [optional, default true] Whether to send the build start notification to Slack.
* GitHub Configuration Options
    * GitHubStatusName: [optional] The status name for custom GitHub status names. If not set, they will not be sent (and rely on Jenkins built in notifications).
    * GitHubStatusCredentialsId: [optional] The credentials to use for updating the GitHub status. If not set, they will not be sent (and rely on Jenkins built in notifications).

Extra configuration:
 * There is an expectation that a 'NuGet-2022' tool defined that points to the nuget executable.

Plugin Dependencies:
 * Cobertura Plugin
 * Code Coverage API Plugin
 * Credentials Binding Plugin
 * Custom Tools Plugin
 * Forensics API Plugin
 * Git Forensics Plugin
 * MSBuild Plugin
 * MSTest Plugin
 * Slack Notification Plugin
 * VSTest Runner Plugin
 * Warnings Next Generation Plugin
 * Workspace Cleanup Plugin