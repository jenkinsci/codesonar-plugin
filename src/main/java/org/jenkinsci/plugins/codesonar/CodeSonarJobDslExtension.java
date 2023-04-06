package org.jenkinsci.plugins.codesonar;

import org.jenkinsci.plugins.codesonar.services.IAnalysisService;

import hudson.Extension;
import javaposse.jobdsl.dsl.RequiresPlugin;
import javaposse.jobdsl.dsl.helpers.publisher.PublisherContext;
import javaposse.jobdsl.plugin.ContextExtensionPoint;
import javaposse.jobdsl.plugin.DslExtensionMethod;

/*
 ```
job {
    publishers {
        codesonar(String protocol, String hubAddress, String projectName, String credentialId, String visibilityFilter) {
            socketTimeoutMS(int value)
            sslCertificateCredentialId(String value)
            projectFile(String value)
            newWarningsFilter(String value)
            cyclomaticComplexity(int max, boolean fail)
            redAlert(int max, boolean fail)
            yellowAlert(int max, boolean fail)
            newWarningCountIncrease(float percentage, boolean fail)
            overallWarningCountIncrease(float percentage, boolean fail)
            rankedWarningCountIncrease(int rank, float percentage, boolean fail)
            absoluteWarningCount(int rank, int count, boolean fail)
        }
    }
}
 ```
 For example:
 ```
job('myProject_GEN') {
    publishers {
        codesonar('https','codesonarhub.com:7340','MyProjectName','codesonar_hub_credential_id','active') {
            socketTimeoutMS(0)
            sslCertificateCredentialId('codesonar_hub_server_certificate_id')
            projectFile('MyProjectFile')
            newWarningsFilter('new')
            cyclomaticComplexity(20, false)
            redAlert(3, true)
            yellowAlert(10, false)
            newWarningCountIncrease(5.0, true)
            overallWarningCountIncrease(5.0, false)
            rankedWarningCountIncrease(30, 5.0, true)
            absoluteWarningCount(20,10,false)
        }
    }
}
 ```
 */
@Extension(optional = true)
public class CodeSonarJobDslExtension extends ContextExtensionPoint {

    @RequiresPlugin(id = "codesonar", minimumVersion = "2.0.0")
    @DslExtensionMethod(context = PublisherContext.class)
    public Object codesonar(
            String protocol, String hubAddress, String projectName, String credentialId, String visibilityFilter,
            Runnable closure
    ) {
        CodeSonarJobDslContext context = new CodeSonarJobDslContext();
        executeInContext(closure, context);
        

        CodeSonarPublisher publisher = new CodeSonarPublisher(context.getConditions(), protocol, hubAddress, projectName, credentialId, visibilityFilter);
        publisher.setSocketTimeoutMS(context.getSocketTimeoutMS());
        publisher.setServerCertificateCredentialId(context.getSslCertificateCredentialId());
        publisher.setProjectFile(context.getProjectFile());
        publisher.setNewWarningsFilter(context.getNewWarningsFilter());
        return publisher;
    }

    public Object codesonar(String protocol, String hubAddress, String projectName, Runnable closure) {
        return codesonar(protocol, hubAddress, projectName, null, IAnalysisService.VISIBILITY_FILTER_ALL_WARNINGS_DEFAULT, closure);
    }
}
