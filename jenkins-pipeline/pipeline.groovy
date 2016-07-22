REPOSITORY_URL = 'https://github.com/Praqma/codesonar-plugin.git'
MAIN_BRANCH = 'master'
REMOTE_NAME = 'origin'
NUM_OF_BUILDS_TO_KEEP = 100
GITHUB_PRAQMA_CREDENTIALS = '100247a2-70f4-4a4e-a9f6-266d139da9db'

JENKINS_SLAVE_LABELS = 'jenkinsubuntu'

PRETESTED_INTEGRATION_JOB_NAME = '1_pretested-integration_codesonar'
UNIT_TESTS_JOB_NAME = '2_unit-tests_codesonar'
INTEGRATION_TESTS_JOB_NAME = '3_integration-tests_codesonar'
ANALYSIS_JOB_NAME = '4_analysis_codesonar'
PUSH_TO_JENKINSCI_JOB_NAME = '5_push_to_jenkinsci_codesonar'
RELEASE_JOB_NAME = '6_release_codesonar'
SYNC_JOB_NAME = '7_sync_jenkinsci_codesonar'

job(PRETESTED_INTEGRATION_JOB_NAME) {
    logRotator {
        numToKeep(NUM_OF_BUILDS_TO_KEEP)
    }

    label(JENKINS_SLAVE_LABELS)

    properties {
        ownership {
            primaryOwnerId('and')
            coOwnerIds('man')
        }
    }

    authorization {
        permission('hudson.model.Item.Read', 'anonymous')
    }

    scm {
        git {
            remote {
                name(REMOTE_NAME)
                url(REPOSITORY_URL)
                credentials(GITHUB_PRAQMA_CREDENTIALS)
            }
            branch("$REMOTE_NAME/ready/**")

            extensions {
                wipeOutWorkspace()
            }
        }
    }

    triggers {
        githubPush()
    }

    steps {
        shell('mvn clean test')
    }

    wrappers {
        buildName('${BUILD_NUMBER}#${GIT_REVISION,length=8}(${GIT_BRANCH})')
        pretestedIntegration("SQUASHED", MAIN_BRANCH, REMOTE_NAME)
    }

    publishers {
        pretestedIntegration()
        downstream(UNIT_TESTS_JOB_NAME, 'SUCCESS')
        mailer('and@praqma.net', false, false)
    }
}

job(UNIT_TESTS_JOB_NAME) {

    logRotator {
        numToKeep(NUM_OF_BUILDS_TO_KEEP)
    }

    label(JENKINS_SLAVE_LABELS)

    properties {
        ownership {
            primaryOwnerId('and')
            coOwnerIds('man')
        }
    }

    scm {
        git {
            remote {
                name(REMOTE_NAME)
                url(REPOSITORY_URL)
            }
            branch(MAIN_BRANCH)
            extensions {}
        }
    }

    authorization {
        permission('hudson.model.Item.Read', 'anonymous')
    }

    steps {
        shell('mvn clean compile test-compile cobertura:cobertura')
    }

    wrappers {
        buildName('${BUILD_NUMBER}#${GIT_REVISION,length=8}(${GIT_BRANCH})')
    }

    publishers {
        cobertura('**/target/site/cobertura/coverage.xml') {
            failNoReports(true)
            sourceEncoding('ASCII')

            // the following targets are added by default to check the method, line and conditional level coverage
            methodTarget(80, 0, 0)
            lineTarget(80, 0, 0)
            conditionalTarget(70, 0, 0)
        }

        archiveJunit('target/surefire-reports/*.xml')

        downstream(INTEGRATION_TESTS_JOB_NAME, 'SUCCESS')
        mailer('and@praqma.net', false, false)
    }
}

job(INTEGRATION_TESTS_JOB_NAME) {

    logRotator {
        numToKeep(NUM_OF_BUILDS_TO_KEEP)
    }

    label(JENKINS_SLAVE_LABELS)

    properties {
        ownership {
            primaryOwnerId('and')
            coOwnerIds('man')
        }
    }

    scm {
        git {
            remote {
                name(REMOTE_NAME)
                url(REPOSITORY_URL)
            }
            branch(MAIN_BRANCH)
            extensions {}
        }
    }

    authorization {
        permission('hudson.model.Item.Read', 'anonymous')
    }

    steps {
        shell('mvn clean integration-test')
    }

    wrappers {
        buildName('${BUILD_NUMBER}#${GIT_REVISION,length=8}(${GIT_BRANCH})')
    }

    publishers {
        archiveJunit('target/failsafe-reports/*.xml')

        downstream(ANALYSIS_JOB_NAME, 'SUCCESS')
        mailer('and@praqma.net', false, false)
    }
}

job(ANALYSIS_JOB_NAME) {

    logRotator {
        numToKeep(NUM_OF_BUILDS_TO_KEEP)
    }

    label(JENKINS_SLAVE_LABELS)

    properties {
        ownership {
            primaryOwnerId('and')
            coOwnerIds('man')
        }
    }

    scm {
        git {
            remote {
                name(REMOTE_NAME)
                url(REPOSITORY_URL)
            }
            branch(MAIN_BRANCH)
            extensions {}
        }
    }

    authorization {
        permission('hudson.model.Item.Read', 'anonymous')
    }

    steps {
        shell('mvnclean package findbugs:findbugs checkstyle:checkstyle pmd:pmd pmd:cpd javancss:check javadoc:javadoc')
    }

    wrappers {
        buildName('${BUILD_NUMBER}#${GIT_REVISION,length=8}(${GIT_BRANCH})')
    }
    publishers {
        checkstyle('target/checkstyle-result.xml') {
            healthLimits(3, 20)
            thresholdLimit('high')
            defaultEncoding('UTF-8')
            canRunOnFailed(true)
            useStableBuildAsReference(true)
            useDeltaValues(true)
            computeNew(true)
            shouldDetectModules(true)
            thresholds(
                    unstableTotal: [all: 1, high: 2, normal: 3, low: 4],
                    failedTotal: [all: 5, high: 6, normal: 7, low: 8],
                    unstableNew: [all: 9, high: 10, normal: 11, low: 12],
                    failedNew: [all: 13, high: 14, normal: 15, low: 16]
            )
        }
        findbugs('target/findbugsXml.xml', false) {
            healthLimits(3, 20)
            thresholdLimit('high')
            defaultEncoding('UTF-8')
            canRunOnFailed(true)
            useStableBuildAsReference(true)
            useDeltaValues(true)
            computeNew(true)
            shouldDetectModules(true)
            thresholds(
                    unstableTotal: [all: 1, high: 2, normal: 3, low: 4],
                    failedTotal: [all: 5, high: 6, normal: 7, low: 8],
                    unstableNew: [all: 9, high: 10, normal: 11, low: 12],
                    failedNew: [all: 13, high: 14, normal: 15, low: 16]
            )
        }

        pmd('target/pmd.xml') {
            healthLimits(3, 20)
            thresholdLimit('high')
            defaultEncoding('UTF-8')
            canRunOnFailed(true)
            useStableBuildAsReference(true)
            useDeltaValues(true)
            computeNew(true)
            shouldDetectModules(true)
            thresholds(
                    unstableTotal: [all: 1, high: 2, normal: 3, low: 4],
                    failedTotal: [all: 5, high: 6, normal: 7, low: 8],
                    unstableNew: [all: 9, high: 10, normal: 11, low: 12],
                    failedNew: [all: 13, high: 14, normal: 15, low: 16]
            )
        }

        downstream(PUSH_TO_JENKINSCI_JOB_NAME, 'SUCCESS')
        mailer('and@praqma.net', false, false)
    }
}

job(PUSH_TO_JENKINSCI_JOB_NAME) {

    logRotator {
        numToKeep(NUM_OF_BUILDS_TO_KEEP)
    }

    label(JENKINS_SLAVE_LABELS)

    properties {
        ownership {
            primaryOwnerId('and')
            coOwnerIds('man')
        }
    }

    scm {
        git {
            remote {
                name(REMOTE_NAME)
                url(REPOSITORY_URL)
            }
            branch(MAIN_BRANCH)
            extensions {}
        }
    }

    authorization {
        permission('hudson.model.Item.Read', 'anonymous')
    }

    steps {
        shell('''git checkout master
                |git fetch --tags git@github.com:Praqma/codesonar-plugin.git

                |# push for JenkinsCI github repo:

                |git push git@github.com:jenkinsci/codesonar-plugin.git ${BRANCH}
                |git push git@github.com:jenkinsci/codesonar-plugin.git --tags'''.stripMargin())
    }

    wrappers {
        buildName('${BUILD_NUMBER}#${GIT_REVISION,length=8}(${GIT_BRANCH})')
    }

    publishers {
        buildPipelineTrigger(RELEASE_JOB_NAME) {
            parameters {
                gitRevision()
            }
        }
        mailer('and@praqma.net', false, false)
    }
}

job(RELEASE_JOB_NAME) {

    logRotator {
        numToKeep(NUM_OF_BUILDS_TO_KEEP)
    }

    label(JENKINS_SLAVE_LABELS)

    properties {
        ownership {
            primaryOwnerId('and')
            coOwnerIds('man')
        }
    }

    scm {
        git {
            remote {
                name(REMOTE_NAME)
                url(REPOSITORY_URL)
            }
            branch(MAIN_BRANCH)
            extensions {}
        }
    }

    authorization {
        permission('hudson.model.Item.Read', 'anonymous')
    }

    steps {
        shell('mvn release:clean release:prepare release:perform -B')
    }

    wrappers {
        buildName('${BUILD_NUMBER}#${GIT_REVISION,length=8}(${GIT_BRANCH})')
    }

    publishers {
        downstream(SYNC_JOB_NAME, 'SUCCESS')
        mailer('and@praqma.net', false, false)
    }
}

job(SYNC_JOB_NAME) {

    logRotator {
        numToKeep(NUM_OF_BUILDS_TO_KEEP)
    }

    label(JENKINS_SLAVE_LABELS)

    properties {
        ownership {
            primaryOwnerId('and')
            coOwnerIds('man')
        }
    }

    scm {
        git {
            remote {
                name(REMOTE_NAME)
                url(REPOSITORY_URL)
            }
            branch(MAIN_BRANCH)
            extensions {}
        }
    }

    authorization {
        permission('hudson.model.Item.Read', 'anonymous')
    }

    steps {
        shell('''git checkout master
                |git fetch --tags git@github.com:Praqma/codesonar-plugin.git

                |# push for JenkinsCI github repo:

                |git push git@github.com:jenkinsci/codesonar-plugin.git ${BRANCH}
                |git push git@github.com:jenkinsci/codesonar-plugin.git --tags'''.stripMargin())
    }

    wrappers {
        buildName('${BUILD_NUMBER}#${GIT_REVISION,length=8}(${GIT_BRANCH})')
    }

    publishers {
        mailer('and@praqma.net', false, false)
    }
}






