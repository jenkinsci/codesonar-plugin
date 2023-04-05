package org.jenkinsci.plugins.codesonar.integration.conditions;

import java.util.ArrayList;
import java.util.List;

import org.jenkinsci.plugins.codesonar.CodeSonarPublisher;
import org.jenkinsci.plugins.codesonar.conditions.Condition;
import org.jenkinsci.plugins.codesonar.conditions.WarningCountIncreaseSpecifiedScoreAndHigherCondition;
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
        final String VISIBILITY_FILTER = "active";
        final String VISIBILITY_FILTER_NEW_WARNINGS = "all";

        final int RANK_OF_WARNINGS = 30;
        final float WARNING_PERCENTAGE = 50.0f;

        WarningCountIncreaseSpecifiedScoreAndHigherCondition condition
                = new WarningCountIncreaseSpecifiedScoreAndHigherCondition(RANK_OF_WARNINGS, Float.toString(WARNING_PERCENTAGE));
        condition.setWarrantedResult(WARRANTED_RESULT);

        List<Condition> conditions = new ArrayList<>();
        conditions.add(condition);

        CodeSonarPublisher codeSonarPublisher = new CodeSonarPublisher(conditions, "http", VALID_HUB_ADDRESS.toString(), VALID_PROJECT_NAME, "", VISIBILITY_FILTER);
        codeSonarPublisher.setNewWarningsFilter(VISIBILITY_FILTER_NEW_WARNINGS);
        codeSonarPublisher.setProjectFile(VALID_CODESONAR_PROJECT_FILE);
        codeSonarPublisher.setAnalysisService(mockedAnalysisService);
        codeSonarPublisher.setMetricsService(mockedMetricsService);
        codeSonarPublisher.setProceduresService(mockedProceduresService);
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
        final String VISIBILITY_FILTER = "active";
        final String VISIBILITY_FILTER_NEW_WARNINGS = "all";

        final int RANK_OF_WARNINGS = 30;
        final float WARNING_PERCENTAGE = 70.0f;

        WarningCountIncreaseSpecifiedScoreAndHigherCondition condition
                = new WarningCountIncreaseSpecifiedScoreAndHigherCondition(RANK_OF_WARNINGS, Float.toString(WARNING_PERCENTAGE));
        condition.setWarrantedResult(WARRANTED_RESULT);

        List<Condition> conditions = new ArrayList<>();
        conditions.add(condition);

        CodeSonarPublisher codeSonarPublisher = new CodeSonarPublisher(conditions, "http", VALID_HUB_ADDRESS.toString(), VALID_PROJECT_NAME, "", VISIBILITY_FILTER);
        codeSonarPublisher.setNewWarningsFilter(VISIBILITY_FILTER_NEW_WARNINGS);
        codeSonarPublisher.setProjectFile(VALID_CODESONAR_PROJECT_FILE);
        codeSonarPublisher.setAnalysisService(mockedAnalysisService);
        codeSonarPublisher.setMetricsService(mockedMetricsService);
        codeSonarPublisher.setProceduresService(mockedProceduresService);
        codeSonarPublisher.setAuthenticationService(mockedAuthenticationService);
        codeSonarPublisher.setAnalysisServiceFactory(mockedAnalysisServiceFactory);
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
