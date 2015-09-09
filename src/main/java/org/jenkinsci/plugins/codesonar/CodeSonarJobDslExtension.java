package org.jenkinsci.plugins.codesonar;

import hudson.Extension;
import javaposse.jobdsl.dsl.RequiresPlugin;
import javaposse.jobdsl.dsl.helpers.publisher.PublisherContext;
import javaposse.jobdsl.plugin.ContextExtensionPoint;
import javaposse.jobdsl.plugin.DslExtensionMethod;

/*
```
job{
    publishers{
        codeSonar(String hubAddress, String projectName){
            cyclomaticComplexity(int maxComplexity, boolean markAsFailed)
            redAlert(int maxAlerts, boolean markAsFailed)
            yellowAlert(int maxAlerts, boolean markAsFailed)
            newWarningCountIncrease(float percentage, boolean markAsFailed)
            overallWarningCountIncrease(float percentage, boolean markAsFailed)
            rankedWarningCountIncrease(int minRank, float percentage, boolean markAsFailed)
        }
    }
}
```
For example:
```
job('myProject_GEN'){
    publishers{
        codeSonar('hub','proj'){
            cyclomaticComplexity(20, false)
            redAlert(3, true)
            yellowAlert(10, false)
            newWarningCountIncrease(5, true)
            overallWarningCountIncrease(5, false)
            rankedWarningCountIncrease(30, 5, true)
        }
    }
}
```
*/

@Extension(optional = true)
public class CodeSonarJobDslExtension extends ContextExtensionPoint {
    @RequiresPlugin(id = "codesonar", minimumVersion = "1.0.0")
    @DslExtensionMethod(context = PublisherContext.class)
    public Object codeSonar(String hubAddress, String projectName, Runnable closure){
        CodeSonarJobDslContext context = new CodeSonarJobDslContext();
        executeInContext(closure, context);

        return new CodeSonarPublisher(context.conditions, hubAddress, projectName);
    }
}
