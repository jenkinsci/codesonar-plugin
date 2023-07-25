package org.jenkinsci.plugins.codesonar.integration.conditions;

import java.util.ArrayList;
import java.util.List;

import org.jenkinsci.plugins.codesonar.CodeSonarPublisher;
import org.jenkinsci.plugins.codesonar.conditions.Condition;
import org.jenkinsci.plugins.codesonar.conditions.WarningCountIncreaseSpecifiedScoreAndHigherCondition;
import org.jenkinsci.plugins.codesonar.services.IAnalysisService;
import org.junit.Assert;
import org.junit.Test;

import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;

/**
 *
 * @author Andrius
 */
public class WarningCountIncreaseSpecifiedScoreAndHigherConditionIT extends ConditionIntegrationTestBase {

    @Test
    public void percentageOfWarningsBellowTheDesignatedScoreIsAboveTheThreshold_BuildIsSetToWarrantedResult() throws Exception {
        // arrange
        final Result EXPECTED_RESULT = Result.FAILURE;
        final String WARRANTED_RESULT = Result.FAILURE.toString();

        final int RANK_OF_WARNINGS = 30;
        final float WARNING_PERCENTAGE = 50.0f;

        WarningCountIncreaseSpecifiedScoreAndHigherCondition condition
                = new WarningCountIncreaseSpecifiedScoreAndHigherCondition(RANK_OF_WARNINGS, Float.toString(WARNING_PERCENTAGE));
        condition.setWarrantedResult(WARRANTED_RESULT);

        List<Condition> conditions = new ArrayList<>();
        conditions.add(condition);

        CodeSonarPublisher codeSonarPublisher = new CodeSonarPublisher(conditions, "http", VALID_HUB_ADDRESS.toString(), VALID_PROJECT_NAME, "", IAnalysisService.VISIBILITY_FILTER_ALL_WARNINGS_DEFAULT);
        codeSonarPublisher.setNewWarningsFilter(IAnalysisService.VISIBILITY_FILTER_NEW_WARNINGS_DEFAULT);
        codeSonarPublisher.setProjectFile(VALID_CODESONAR_PROJECT_FILE);
        codeSonarPublisher.setHttpService(mockedHttpService);

        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        project.getPublishersList().add(codeSonarPublisher);

        // act
        QueueTaskFuture<FreeStyleBuild> queueTaskFuture = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = queueTaskFuture.get();

        // assert
        Assert.assertEquals(EXPECTED_RESULT, build.getResult());
    }

    @Test
    public void percentageOfWarningsBelowTheDesignatedRankIsBellowTheThreshold_BuildIsSuccessful() throws Exception {
        // arrange
        final Result EXPECTED_RESULT = Result.SUCCESS;
        final String WARRANTED_RESULT = Result.FAILURE.toString();

        final int RANK_OF_WARNINGS = 30;
        final float WARNING_PERCENTAGE = 70.0f;

        WarningCountIncreaseSpecifiedScoreAndHigherCondition condition
                = new WarningCountIncreaseSpecifiedScoreAndHigherCondition(RANK_OF_WARNINGS, Float.toString(WARNING_PERCENTAGE));
        condition.setWarrantedResult(WARRANTED_RESULT);

        List<Condition> conditions = new ArrayList<>();
        conditions.add(condition);

        CodeSonarPublisher codeSonarPublisher = new CodeSonarPublisher(conditions, "http", VALID_HUB_ADDRESS.toString(), VALID_PROJECT_NAME, "", IAnalysisService.VISIBILITY_FILTER_ALL_WARNINGS_DEFAULT);
        codeSonarPublisher.setNewWarningsFilter(IAnalysisService.VISIBILITY_FILTER_NEW_WARNINGS_DEFAULT);
        codeSonarPublisher.setProjectFile(VALID_CODESONAR_PROJECT_FILE);
        codeSonarPublisher.setAuthenticationService(mockedAuthenticationService);
        codeSonarPublisher.setHttpService(mockedHttpService);
        
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        project.getPublishersList().add(codeSonarPublisher);

        // act
        QueueTaskFuture<FreeStyleBuild> queueTaskFuture = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = queueTaskFuture.get();

        // assert
        Assert.assertEquals(EXPECTED_RESULT, build.getResult());
    }
}
