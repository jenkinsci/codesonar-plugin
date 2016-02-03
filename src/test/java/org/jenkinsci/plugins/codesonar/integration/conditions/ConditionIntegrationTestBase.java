package org.jenkinsci.plugins.codesonar.integration.conditions;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.jenkinsci.plugins.codesonar.Utils;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.jenkinsci.plugins.codesonar.models.analysis.Warning;
import org.jenkinsci.plugins.codesonar.models.metrics.Metrics;
import org.jenkinsci.plugins.codesonar.models.procedures.Procedures;
import org.jenkinsci.plugins.codesonar.services.AnalysisService;
import org.jenkinsci.plugins.codesonar.services.AuthenticationService;
import org.jenkinsci.plugins.codesonar.services.MetricsService;
import org.jenkinsci.plugins.codesonar.services.ProceduresService;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author Andrius
 */
public abstract class ConditionIntegrationTestBase {
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();
    
    protected AnalysisService mockedAnalysisService;
    protected MetricsService mockedMetricsService;
    protected ProceduresService mockedProceduresService;
    protected AuthenticationService mockedAuthenticationService;
    
    protected final String VALID_HUB_ADDRESS = "10.10.10.10";
    protected final String VALID_PROJECT_NAME = "projectName";

    public void setUp() throws Exception {
        mockedAnalysisService = mock(AnalysisService.class);
        mockedMetricsService = mock(MetricsService.class);
        mockedProceduresService = mock(ProceduresService.class);
        mockedAuthenticationService = mock(AuthenticationService.class);
        
        final String VALID_ANALYSIS_URL = "VALID_ANALYSIS_URL";
        final Analysis VALID_ANALYSIS_ACTIVE_WARNINGS = new Analysis();
        VALID_ANALYSIS_ACTIVE_WARNINGS.setAnalysisId("10");

        final List<Warning> ACTIVE_WARNINGS = new ArrayList<Warning>();
        Warning warning = new Warning();
        warning.setRank(25);
        ACTIVE_WARNINGS.add(warning);
        ACTIVE_WARNINGS.add(warning);
        ACTIVE_WARNINGS.add(warning);
        warning = new Warning();
        warning.setRank(35);
        ACTIVE_WARNINGS.add(warning);
        ACTIVE_WARNINGS.add(warning);
        VALID_ANALYSIS_ACTIVE_WARNINGS.setWarnings(ACTIVE_WARNINGS);

        final String VALID_METRICS_URL = "VALID_METRICS_URL";
        final Metrics VALID_METRICS = new Metrics();

        final String VALID_PROCEDURES_URL = "VALID_PROCEDURES_URL";
        final Procedures VALID_PROCEDURES = new Procedures();

        final Analysis VALID_ANALYSIS_NEW_WARNINGS = new Analysis();

        final List<Warning> NEW_WARNINGS = new ArrayList<Warning>();
        warning = new Warning();
        warning.setRank(25);
        NEW_WARNINGS.add(warning);
        NEW_WARNINGS.add(warning);
        NEW_WARNINGS.add(warning);
        VALID_ANALYSIS_NEW_WARNINGS.setWarnings(NEW_WARNINGS);
        
        when(mockedAnalysisService.getAnalysisUrlFromLogFile(any(List.class)))
                .thenReturn(VALID_ANALYSIS_URL);
        when(mockedAnalysisService.getAnalysisFromUrl(VALID_ANALYSIS_URL, Utils.UrlFilters.ACTIVE))
                .thenReturn(VALID_ANALYSIS_ACTIVE_WARNINGS);

        when(mockedMetricsService.getMetricsUrlFromAnAnalysisId(VALID_HUB_ADDRESS, VALID_ANALYSIS_ACTIVE_WARNINGS.getAnalysisId()))
                .thenReturn(VALID_METRICS_URL);
        when(mockedMetricsService.getMetricsFromUrl(VALID_METRICS_URL)).thenReturn(VALID_METRICS);

        when(mockedProceduresService.getProceduresUrlFromAnAnalysisId(VALID_HUB_ADDRESS, VALID_ANALYSIS_ACTIVE_WARNINGS.getAnalysisId()))
                .thenReturn(VALID_PROCEDURES_URL);
        when(mockedProceduresService.getProceduresFromUrl(VALID_PROCEDURES_URL)).thenReturn(VALID_PROCEDURES);

        when(mockedAnalysisService.getAnalysisFromUrl(VALID_ANALYSIS_URL, Utils.UrlFilters.NEW))
                .thenReturn(VALID_ANALYSIS_NEW_WARNINGS);
    }
}
