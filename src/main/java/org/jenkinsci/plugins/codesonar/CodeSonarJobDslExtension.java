package org.jenkinsci.plugins.codesonar;

import org.jenkinsci.plugins.codesonar.services.IAnalysisService;

import hudson.Extension;
import javaposse.jobdsl.dsl.RequiresPlugin;
import javaposse.jobdsl.dsl.helpers.publisher.PublisherContext;
import javaposse.jobdsl.plugin.ContextExtensionPoint;
import javaposse.jobdsl.plugin.DslExtensionMethod;

/** 
Implement extension point for job-dsl plugin.
<p>
The job-dsl plugin allows one to generate new Jenkins projects from a script.
This extension point implementation allows one to declare the use of the CodeSonar plugin in their job-dsl script.
<p>
This is the general structure:
<pre>
{@code
job {
    publishers {
        codesonar(String protocol, String hubAddress, String projectName, String credentialId, String visibilityFilter) {
            socketTimeoutMS(int value)
            sslCertificateCredentialId(String value)
            projectFile(String value)
            newWarningsFilter(String value)
            comparisonAnalysis(String value)
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
}
</pre>

For example:

<pre>
{@code
job('myProject_GEN') {
    publishers {
        codesonar('https','codesonarhub.com:7340','MyProjectName','codesonar_hub_credential_id','active') {
            socketTimeoutMS(0)
            sslCertificateCredentialId('codesonar_hub_server_certificate_id')
            projectFile('MyProjectFile')
            newWarningsFilter('new')
            comparisonAnalysis('MyBaseAnalysisId')
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
}
</pre>
 */
@Extension(optional = true)
public class CodeSonarJobDslExtension extends ContextExtensionPoint {
    /*
     * See https://github.com/jenkinsci/job-dsl-plugin/blob/master/CONTRIBUTING.md#dsl-design
     * @RequiresPlugin version should be updated whenever the `codesonar` method signature is changed here.
     * @DslExtensionMethod declares that this method will implement a publisher declaration for a job-dsl script.
     */
    //TODO: @RequiresPlugin appears to be unnecessary here
    @RequiresPlugin(id = "codesonar", minimumVersion = "3.3.0")
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
        publisher.setComparisonAnalysis(context.getComparisonAnalysis());
        return publisher;
    }

    /* TODO: Is it possible to use this method?  Can it be removed? */
    public Object codesonar(String protocol, String hubAddress, String projectName, Runnable closure) {
        return codesonar(protocol, hubAddress, projectName, null, IAnalysisService.VISIBILITY_FILTER_ALL_WARNINGS_DEFAULT, closure);
    }
}
