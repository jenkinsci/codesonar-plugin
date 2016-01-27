package org.jenkinsci.plugins.codesonar.integration;

import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.apache.commons.io.FileUtils;
import static org.junit.Assert.*;
import org.jenkinsci.plugins.codesonar.CodeSonarPublisher;
import org.jenkinsci.plugins.codesonar.conditions.Condition;
import org.jenkinsci.plugins.codesonar.conditions.PercentageOfWariningsIncreasedInCasesBellowCertainRankCondition;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

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
        PercentageOfWariningsIncreasedInCasesBellowCertainRankCondition condition = 
                new PercentageOfWariningsIncreasedInCasesBellowCertainRankCondition(RANK_OF_WARNINGS, WARNING_PERCENTAGE);
        
        final String EMPTY_HUB_ADDRESS = "";
        final String VALID_PROJECT_NAME = "projectName";

        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        
        List<Condition> conditions = new ArrayList<Condition>();
        project.getPublishersList().add(new CodeSonarPublisher(conditions, "http", EMPTY_HUB_ADDRESS, "8000", VALID_PROJECT_NAME, ""));
        
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
        PercentageOfWariningsIncreasedInCasesBellowCertainRankCondition condition = 
                new PercentageOfWariningsIncreasedInCasesBellowCertainRankCondition(RANK_OF_WARNINGS, WARNING_PERCENTAGE);
        
        final String VALID_HUB_ADDRESS = "10.10.10.10";
        final String EMPTY_PROJECT_NAME = "";

        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        
        List<Condition> conditions = new ArrayList<Condition>();
        project.getPublishersList().add(new CodeSonarPublisher(conditions, "http", VALID_HUB_ADDRESS, "8000", EMPTY_PROJECT_NAME,""));
        
        // act
        QueueTaskFuture<FreeStyleBuild> queueTaskFuture = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = queueTaskFuture.get();
      
        // assert
        assertEquals(EXPECTED_RESULT, build.getResult());
    }
    
    @Test
    public void testLogging() throws Exception {
        final Result EXPECTED_RESULT = Result.FAILURE;
        
        final int RANK_OF_WARNINGS = 30;
        final float WARNING_PERCENTAGE = 50.0f;
        PercentageOfWariningsIncreasedInCasesBellowCertainRankCondition condition = 
                new PercentageOfWariningsIncreasedInCasesBellowCertainRankCondition(RANK_OF_WARNINGS, WARNING_PERCENTAGE);
        
        final String VALID_HUB_ADDRESS = "10.10.10.10";
        final String RANDOM_NAME = "projectNotThere";

        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        
        List<Condition> conditions = new ArrayList<Condition>();
        project.getPublishersList().add(new CodeSonarPublisher(conditions, "http", VALID_HUB_ADDRESS, "8000", RANDOM_NAME, ""));
        
        // act
        QueueTaskFuture<FreeStyleBuild> queueTaskFuture = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = queueTaskFuture.get();
      
        // assert
        assertEquals(EXPECTED_RESULT, build.getResult());        
        
        // assert that we have a message in the console log
        String log = FileUtils.readFileToString(build.getLogFile());
        assertTrue(log.contains("[CodeSonar] Error on url: http://10.10.10.10/index.xml"));
        assertTrue(log.contains("[CodeSonar] Message is: "));
    }
}
