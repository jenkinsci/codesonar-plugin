package org.jenkinsci.plugins.codesonar.integration.conditions;

import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import hudson.util.RunList;
import org.jenkinsci.plugins.codesonar.conditions.PercentageOfWariningsIncreasedInCasesBellowCertainRankCondition;
import org.junit.After;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 *
 * @author Andrius
 */
public class PercentageOfWariningsIncreasedInCasesBellowCertainRankConditionIT {

   /* @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void buildDoesNoContainBuildAction_ReturnsResultSuccess() throws Exception {
        // arrange
        final Result EXPECTED_RESULT = Result.SUCCESS;
        
        final int RANK_OF_WARNINGS = 30;
        final float WARNING_PERCENTAGE = 50.0f;
        PercentageOfWariningsIncreasedInCasesBellowCertainRankCondition condition = 
                new PercentageOfWariningsIncreasedInCasesBellowCertainRankCondition(RANK_OF_WARNINGS, WARNING_PERCENTAGE);
        
//        List<> l = new Arraylist
//        conditions.add(condition);
        
        final String VALID_HUB_ADDRESS = "10.10.10.10";
        final String VALID_PROJECT_NAME = "projectName";

        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        
//        project.getPublishersList().add(new CodeSonarPublisher(conditions, VALID_HUB_ADDRESS, VALID_PROJECT_NAME));
       
        QueueTaskFuture<FreeStyleBuild> queueTaskFuture = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = queueTaskFuture.get();
        
        // act
        // assert
        fail("not implemented");
    }*/
/*
    @Test
    public void buildActionDoesNotContainAnalysis_ReturnsResultSuccess() {
        // arrange
        // act
        // assert
        fail("not implemented");
    }

    @Test
    public void calculatedWarningPercentageAboveTheAllowedLimit_ReturnsWarrantedResult() {
        // arrange
        // act
        // assert
        fail("not implemented");
    }

    @Test
    public void calculatedWarningPercentageBellowTheAllowedLimit_ReturnsResultSuccess() {
        // arrange
        // act
        // assert
        fail("not implemented");
    }

    @Test
    public void calculatedWarningPercentageAboveTheAllowedLimitRunningOnSlave_ReturnsWarrantedResult() {
        // arrange
        // act
        // assert
        fail("not implemented");
    }

    @Test
    public void calculatedWarningPercentageBellowTheAllowedLimitRunningOnSlave_ReturnsResultSuccess() {
        // arrange
        // act
        // assert
        fail("not implemented");
    }*/
}
