package org.jenkinsci.plugins.codesonar.integration;

import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.FileUtils;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.codesonar.CodeSonarPublisher;
import org.jenkinsci.plugins.codesonar.conditions.Condition;
import org.jenkinsci.plugins.codesonar.conditions.WarningCountIncreaseSpecifiedScoreAndHigherCondition;
import org.jenkinsci.plugins.codesonar.services.HttpService;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author Andrius
 */
public class CodeSonarPublisherIT {
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();
    
    @Test
    public void providedHubAddressIsEmpty_BuildFails() throws Exception {
        // arrange
        final Result EXPECTED_RESULT = Result.FAILURE;
        
        final int RANK_OF_WARNINGS = 30;
        final float WARNING_PERCENTAGE = 50.0f;
        WarningCountIncreaseSpecifiedScoreAndHigherCondition condition = 
                new WarningCountIncreaseSpecifiedScoreAndHigherCondition(RANK_OF_WARNINGS, Float.toString(WARNING_PERCENTAGE));
        
        final String EMPTY_HUB_ADDRESS = "";
        final String VALID_PROJECT_NAME = "projectName";

        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        
        List<Condition> conditions = new ArrayList<>();
        CodeSonarPublisher codeSonarPublisher = new CodeSonarPublisher(conditions, "http", EMPTY_HUB_ADDRESS, VALID_PROJECT_NAME, "");
        project.getPublishersList().add(codeSonarPublisher);
        
        // act
        QueueTaskFuture<FreeStyleBuild> queueTaskFuture = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = queueTaskFuture.get();
      
        // assert
        assertEquals(EXPECTED_RESULT, build.getResult());
    }
    
    @Test
    public void providedProjectNameIsEmpty_BuildFails() throws Exception {
        // arrange
        final Result EXPECTED_RESULT = Result.FAILURE;
        
        final int RANK_OF_WARNINGS = 30;
        final float WARNING_PERCENTAGE = 50.0f;
        WarningCountIncreaseSpecifiedScoreAndHigherCondition condition = 
                new WarningCountIncreaseSpecifiedScoreAndHigherCondition(RANK_OF_WARNINGS, Float.toString(WARNING_PERCENTAGE));
        
        final String VALID_HUB_ADDRESS = "10.10.10.10";
        final String EMPTY_PROJECT_NAME = "";

        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        
        List<Condition> conditions = new ArrayList<>();
        project.getPublishersList().add(new CodeSonarPublisher(conditions, "http", VALID_HUB_ADDRESS, EMPTY_PROJECT_NAME,""));
        
        // act
        QueueTaskFuture<FreeStyleBuild> queueTaskFuture = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = queueTaskFuture.get();
      
        // assert
        assertEquals(EXPECTED_RESULT, build.getResult());
    }
    
    @Test
    public void pipelineIntegration_validation() throws Exception {
        WorkflowJob foo = jenkinsRule.jenkins.createProject(WorkflowJob.class, "foo");
        CpsFlowDefinition flowDef = new CpsFlowDefinition(StringUtils.join(Arrays.asList(
                "node {",
                "  codesonar conditions: [cyclomaticComplexity(maxCyclomaticComplexity: 30), redAlerts(alertLimit: 1), warningCountIncreaseNewOnly(percentage: '5.0'), warningCountIncreaseOverall('5.0'), warningCountIncreaseSpecifiedScoreAndHigher(rankOfWarnings: 30, warningPercentage: '5.0'), yellowAlerts(alertLimit: 1)], credentialId: '', hubAddress: '10', projectName: '${JOB_NAME}', protocol: 'http'",

                "}"), "\n"), true);
        foo.setDefinition(flowDef);

        WorkflowRun b = foo.scheduleBuild2(0).get();

        boolean valid = false;
        List<String> log = b.getLog(500);
        for (String line : log) {
            if (line.equals("ERROR: [CodeSonar] Error on url: http://10/index.xml")) {
                valid = true;
                break;
            }
        }

        Assert.assertThat(valid, is(true));
    }
}
