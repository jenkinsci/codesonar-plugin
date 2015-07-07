package org.jenkinsci.plugins.codesonar.unit.services;

import hudson.AbortException;
import java.io.IOException;
import org.jenkinsci.plugins.codesonar.models.metrics.Metrics;
import org.jenkinsci.plugins.codesonar.models.procedures.Procedures;
import org.jenkinsci.plugins.codesonar.services.HttpService;
import org.jenkinsci.plugins.codesonar.services.MetricsService;
import org.jenkinsci.plugins.codesonar.services.ProceduresService;
import org.jenkinsci.plugins.codesonar.services.XmlSerializationService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author Andrius
 */
public class ProceduresServiceTest {
    private XmlSerializationService mockedXmlSerializationService;
    private HttpService mockedHttpService;
    private ProceduresService proceduresService;

    @Before
    public void setUp() {
        mockedXmlSerializationService = mock(XmlSerializationService.class);
        mockedHttpService = mock(HttpService.class);
        proceduresService = new ProceduresService(mockedHttpService, mockedXmlSerializationService);
    }

    @Test
    public void providedHubAddressAndAnalysisId_shouldReturnAProceduresUrl() {
        final String HUB_ADDRESS = "10.10.10.10";
        final String ANALYSIS_ID = "10";

        final String EXPECTED_RESULT = String.format("http://%s/analysis/%s-procedures.xml", HUB_ADDRESS, ANALYSIS_ID);

        String result = proceduresService.getProceduresUrlFromAnAnalysisId(HUB_ADDRESS, ANALYSIS_ID);

        Assert.assertEquals(EXPECTED_RESULT, result);
    }

    @Test(expected = AbortException.class)
    public void providedInvalidMetricsUrl_shouldThrowAnAbortException() throws IOException {
        final String INVALID_URL = "http://10.10.10.10/invalid";

        when(mockedHttpService.getContentFromUrlAsString(INVALID_URL)).thenCallRealMethod();

        proceduresService.getProceduresFromUrl(INVALID_URL);
    }

    @Test
    public void providedValidMetricsUrl_shouldReturnMetrics() throws IOException {
        final String VALID_METRICS_URL = "validUrl";
        final String RESPONSE_XML_CONTENT = "valid xml content";
        final Procedures EXPECTED_RESULT = new Procedures();

        when(mockedHttpService.getContentFromUrlAsString(VALID_METRICS_URL)).thenReturn(RESPONSE_XML_CONTENT);
        when(mockedXmlSerializationService.deserialize(any(String.class), isA(Class.class))).thenReturn(EXPECTED_RESULT);

        Procedures result = proceduresService.getProceduresFromUrl(VALID_METRICS_URL);

        Assert.assertEquals(EXPECTED_RESULT, result);
    }
}
