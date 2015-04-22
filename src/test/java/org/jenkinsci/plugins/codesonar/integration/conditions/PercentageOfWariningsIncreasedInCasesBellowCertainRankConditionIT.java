package org.jenkinsci.plugins.codesonar.integration.conditions;

import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import java.util.ArrayList;
import java.util.List;
import org.jenkinsci.plugins.codesonar.CodeSonarPublisher;
import org.jenkinsci.plugins.codesonar.Utils.UrlFilters;
import org.jenkinsci.plugins.codesonar.conditions.Condition;
import org.jenkinsci.plugins.codesonar.conditions.PercentageOfWariningsIncreasedInCasesBellowCertainRankCondition;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.jenkinsci.plugins.codesonar.models.analysis.Warning;
import org.jenkinsci.plugins.codesonar.models.metrics.Metrics;
import org.jenkinsci.plugins.codesonar.models.procedures.Procedures;
import org.jenkinsci.plugins.codesonar.services.AnalysisService;
import org.jenkinsci.plugins.codesonar.services.MetricsService;
import org.jenkinsci.plugins.codesonar.services.ProceduresService;
import org.junit.Assert;
import org.junit.Before;
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
public class PercentageOfWariningsIncreasedInCasesBellowCertainRankConditionIT {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();
    private AnalysisService mockedAnalysisService;
    private MetricsService mockedMetricsService;
    private ProceduresService mockedProceduresService;

    private final String VALID_HUB_ADDRESS = "10.10.10.10";
    private final String VALID_PROJECT_NAME = "projectName";

    @Before
    public void setUp() throws Exception {
        mockedAnalysisService = mock(AnalysisService.class);
        mockedMetricsService = mock(MetricsService.class);
        mockedProceduresService = mock(ProceduresService.class);

        final String VALID_ANALYSIS_URL = "VALID_ANALYSIS_URL";
        final Analysis VALID_ANALYSIS_ACTIVE_WARNINGS = new Analysis();
        VALID_ANALYSIS_ACTIVE_WARNINGS.setAnalysisId("10");

        final List<Warning> WARNINGS = new ArrayList<Warning>();
        Warning warning = new Warning();
        warning.setRank(25);
        WARNINGS.add(warning);
        WARNINGS.add(warning);
        WARNINGS.add(warning);
        warning = new Warning();
        warning.setRank(35);
        WARNINGS.add(warning);
        WARNINGS.add(warning);
        VALID_ANALYSIS_ACTIVE_WARNINGS.setWarnings(WARNINGS);

        final String VALID_METRICS_URL = "VALID_METRICS_URL";
        final Metrics VALID_METRICS = new Metrics();

        final String VALID_PROCEDURES_URL = "VALID_PROCEDURES_URL";
        final Procedures VALID_PROCEDURES = new Procedures();

        final Analysis VALID_ANALYSIS_NEW_WARNINGS = new Analysis();

        when(mockedAnalysisService.getAnalysisUrlFromLogFile(any(List.class)))
                .thenReturn(VALID_ANALYSIS_URL);
        when(mockedAnalysisService.getAnalysisFromUrl(VALID_ANALYSIS_URL, UrlFilters.ACTIVE))
                .thenReturn(VALID_ANALYSIS_ACTIVE_WARNINGS);

        when(mockedMetricsService.getMetricsUrlFromAnAnalysisId(VALID_HUB_ADDRESS, VALID_ANALYSIS_ACTIVE_WARNINGS.getAnalysisId()))
                .thenReturn(VALID_METRICS_URL);
        when(mockedMetricsService.getMetricsFromUrl(VALID_METRICS_URL)).thenReturn(VALID_METRICS);

        when(mockedProceduresService.getProceduresUrlFromAnAnalysisId(VALID_HUB_ADDRESS, VALID_ANALYSIS_ACTIVE_WARNINGS.getAnalysisId()))
                .thenReturn(VALID_PROCEDURES_URL);
        when(mockedProceduresService.getProceduresFromUrl(VALID_PROCEDURES_URL)).thenReturn(VALID_PROCEDURES);

        when(mockedAnalysisService.getAnalysisFromUrl(VALID_ANALYSIS_URL, UrlFilters.NEW))
                .thenReturn(VALID_ANALYSIS_NEW_WARNINGS);
    }

    @Test
    public void percentageOfWariningsBellowTheDesignatedRankIsAboveTheThreshold_BuildIsSetToWarrantedResult() throws Exception {
        // arrange
        final Result EXPECTED_RESULT = Result.FAILURE;
        final String WARRANTED_RESULT = Result.FAILURE.toString();

        final int RANK_OF_WARNINGS = 30;
        final float WARNING_PERCENTAGE = 50.0f;

        PercentageOfWariningsIncreasedInCasesBellowCertainRankCondition condition
                = new PercentageOfWariningsIncreasedInCasesBellowCertainRankCondition(RANK_OF_WARNINGS, WARNING_PERCENTAGE);
        condition.setWarrantedResult(WARRANTED_RESULT);

        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(condition);

        CodeSonarPublisher codeSonarPublisher = new CodeSonarPublisher(conditions, VALID_HUB_ADDRESS, VALID_PROJECT_NAME);
        codeSonarPublisher.setAnalysisService(mockedAnalysisService);
        codeSonarPublisher.setMetricsService(mockedMetricsService);
        codeSonarPublisher.setProceduresService(mockedProceduresService);

        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        project.getPublishersList().add(codeSonarPublisher);

        // act
        QueueTaskFuture<FreeStyleBuild> queueTaskFuture = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = queueTaskFuture.get();

        // assert
        Assert.assertEquals(EXPECTED_RESULT, build.getResult());
    }

    @Test
    public void percentageOfWariningsBellowTheDesignatedRankIsBellowTheThreshold_BuildIsSuccessful() throws Exception {
        // arrange
        final Result EXPECTED_RESULT = Result.SUCCESS;
        final String WARRANTED_RESULT = Result.FAILURE.toString();

        final int RANK_OF_WARNINGS = 30;
        final float WARNING_PERCENTAGE = 70.0f;

        PercentageOfWariningsIncreasedInCasesBellowCertainRankCondition condition
                = new PercentageOfWariningsIncreasedInCasesBellowCertainRankCondition(RANK_OF_WARNINGS, WARNING_PERCENTAGE);
        condition.setWarrantedResult(WARRANTED_RESULT);

        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(condition);

        CodeSonarPublisher codeSonarPublisher = new CodeSonarPublisher(conditions, VALID_HUB_ADDRESS, VALID_PROJECT_NAME);
        codeSonarPublisher.setAnalysisService(mockedAnalysisService);
        codeSonarPublisher.setMetricsService(mockedMetricsService);
        codeSonarPublisher.setProceduresService(mockedProceduresService);

        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        project.getPublishersList().add(codeSonarPublisher);

        // act
        QueueTaskFuture<FreeStyleBuild> queueTaskFuture = project.scheduleBuild2(0, new Cause.UserIdCause());
        FreeStyleBuild build = queueTaskFuture.get();

        // assert
        Assert.assertEquals(EXPECTED_RESULT, build.getResult());
    }
}
