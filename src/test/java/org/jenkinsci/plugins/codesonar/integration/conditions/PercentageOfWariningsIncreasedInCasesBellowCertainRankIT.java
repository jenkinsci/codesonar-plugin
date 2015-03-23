/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.codesonar.integration.conditions;

import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.util.RunList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jenkinsci.plugins.codesonar.CodeSonarPublisher;
import org.jenkinsci.plugins.codesonar.conditions.Condition;
import org.jenkinsci.plugins.codesonar.conditions.PercentageOfWariningsIncreasedInCasesBellowCertainRank;
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
public class PercentageOfWariningsIncreasedInCasesBellowCertainRankIT {

    /*@Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }*/

    /*@Test
    public void buildDoesNoContainBuildAction_ReturnsResultSuccess() throws Exception {
        // arrange
        final Result EXPECTED_RESULT = Result.SUCCESS;
        
        final int RANK_OF_WARNINGS = 30;
        final float WARNING_PERCENTAGE = 50.0f;
        PercentageOfWariningsIncreasedInCasesBellowCertainRank condition = new PercentageOfWariningsIncreasedInCasesBellowCertainRank(RANK_OF_WARNINGS, WARNING_PERCENTAGE);
        
        List<Condition> conditions = new ArrayList<>();
        conditions.add(condition);
        
        final String VALID_HUB_ADDRESS = "10.10.10.10";
        final String VALID_PROJECT_NAME = "projectName";

        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        project.getPublishersList().add(new CodeSonarPublisher(conditions, VALID_HUB_ADDRESS, VALID_PROJECT_NAME));
        
        project.scheduleBuild(new Cause.UserIdCause());
        
        RunList<FreeStyleBuild> builds = project.getBuilds();
        
        for(FreeStyleBuild b : builds) {
             String console = jenkinsRule.createWebClient().getPage(b, "console").asText();
             System.out.println(console);
        }
        
        // act
        // assert
        fail("not implemented");
    }

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
