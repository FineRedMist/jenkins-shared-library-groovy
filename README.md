# JenkinsLibrarySample
This is a sample library that factors out common functionality for building C# projects for [jenkins-project-sample-dotnet](https://github.com/FineRedMist/jenkins-project-sample-dotnet)

The script is driven by a Configuration.json that is in the root of the repository. The expected values are:
* Version: Typically a x.y.z version (trailing zeroes optional).
* ScmTrigger: [optional] The frequency to poll source control for changes.
* RunTests: [optional, default true] Whether to build the TestBuildConfiguration and run tests & code coverage against it.
* TestBuildConfiguration: [optional, default debug] The build configuration to use for building and running tests, code coverage, static analysis, etc.
* AdditionalBuildArtifacts: [optional, default not set] Additional files to store as build artifacts for debugging.
* Nuget Configuration Options
    * NugetSource: [optional] The nuget repository to push nuget packages to. If not set, it will not be queried or pushed to.
    * NugetKeyCredentialsId: [optional] The credentials for pushing to a nuget repository. If not set, nuget will not be queried or pushed to.
    * NugetBuildConfiguration: [optional, default: release] The build configuration to use for building the nuget package.
* Slack Configuration Options
    * SlackChannel: [optional] The Slack channel to post build status messages to. If not set, it will use the default channel as part of the general Slack configuration.
    * SendSlack: [optional, default true] Whether to send Slack messages.
    * SendSlackStartNotification: [optional, default true] Whether to send the build start notification to Slack.
* GitHub Configuration Options
    * GitHubStatusName: [optional] The status name for custom GitHub status names. If not set, they will not be sent (and rely on Jenkins built in notifications).
    * GitHubStatusCredentialsId: [optional] The credentials to use for updating the GitHub status. If not set, they will not be sent (and rely on Jenkins built in notifications).
* Coverage Thresholds [optional]
    * This field is a map representing thresholds passed to [publishCoverage](https://www.jenkins.io/doc/pipeline/steps/code-coverage-api/)
    * The default value of 'CoverageThresholds' is:

```json
[
    {
        "thresholdTarget": "Group",
        "unhealthyThreshold": 100.0
    },
    {
        "thresholdTarget": "Package", 
        "unhealthyThreshold": 100.0
    },
    {
        "thresholdTarget": "File", 
        "unhealthyThreshold": 50.0, 
        "unstableThreshold": 85.0
    },
    {
        "thresholdTarget": "Class", 
        "unhealthyThreshold": 50.0, 
        "unstableThreshold": 85.0
    },
    {
        "thresholdTarget": "Method", 
        "unhealthyThreshold": 50.0, 
        "unstableThreshold": 85.0
    },
    {
        "thresholdTarget": "Instruction", 
        "unhealthyThreshold": 0.0, 
        "unstableThreshold": 0.0
    },
    {
        "thresholdTarget": "Line", 
        "unhealthyThreshold": 50.0, 
        "unstableThreshold": 85.0
    },
    {
        "thresholdTarget": "Conditional", 
        "unhealthyThreshold": 0.0, 
        "unstableThreshold": 0.0
    }
]
```

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