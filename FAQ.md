# Frequently Asked Questions

Below, we have described some

## How do I use the plugin together with a pipeline?

If you already have a Pipeline file that works, you can use the snippet generator
to output the necessary code. This is some rather basic [Pipeline DSL](https://www.jenkins.io/doc/book/pipeline/syntax/) 
that just serves as an example.

```groovy
pipeline {
    agent any
    parameters {
        string (
            defaultValue: 'some-name',
            description: 'The name of the project in CodeSonar',
            name : 'JOB_NAME'
        )
        string (
            defaultValue: '127.0.0.1:7340',
            description: 'The default address of the CodeSonar hub',
            name : 'HUB'
        )
    }
    stages {
        stage('build and analyze') {
            steps {
                // run the codesonar analyze commands ...
                script {
                    codesonar conditions:
                        [warningCountIncreaseSpecifiedScoreAndHigher(rankOfWarnings: 55, warningPercentage: '3')],
                            credentialId: '....', hubAddress: '${HUB}', projectName: '${JOB_NAME}', protocol: 'http'
                }
            }
        }
    }
}
```

The credential id can be found in Manage Jenkins -> Credentials. It's a UUID-ish string.

## Jenkins marks the job as successful, although CodeSonar has warnings that match my conditions.

We currently use the active visibility filter. You may need to update your job (or pipeline) to
use a different visibility filter. The visibilty filter number can be found from the URL of
analysis, as `filter=<ID>`.

## I get a timeout during the codesonar step

If you received a network timeout during your codesonar steps it may be beneficial to set a higher timeout using the
`socketTimetoutMS` parameter. The timeout is set in milliseconds. The default value may vary based on the system and java setup. 

## "PKIX path building failed" - the certificate of the CodeSonar Hub is not accepted.

If the CodeSonar Hub has a self-signed Server Certificate that is not included in Jenkins' Trust Stores, 
then it may be necessary to provide the accepted certificate as a parameter (ServerCertificateCredentialId) to the 
codesonar step. 

The certificate must be of type X509, and must be store in Jenkins File Credential - found under Manage Jenkins -> Credentials.

## I don't get any results from CodeSonar.

It's necessary to configure a launch daemon from the CodeSonar instance. The command is given in the
output, when you finish configuring the CodeSonar instance.
This needs to be configured as a [cronjob](https://en.wikipedia.org/wiki/Cron), which is also advised 
in the page on CodeSonar, and the command is given when you configure the hub, using the command-line tool.

```shell script
/opt/codesonar/codesonar/bin/cslaunchd http://<code sonar url>:7340 -hubuser Anonymous -launchd-group / -launchd-key ""
```
