package org.jenkinsci.plugins.codesonar.unit.services;

import hudson.AbortException;
import java.io.IOException;
import org.jenkinsci.plugins.codesonar.models.metrics.Metrics;
import org.jenkinsci.plugins.codesonar.services.HttpService;
import org.jenkinsci.plugins.codesonar.services.MetricsService;
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
public class MetricsServiceTest {

    private XmlSerializationService mockedXmlSerializationService;
    private HttpService mockedHttpService;
    private MetricsService metricsService;

    @Before
    public void setUp() {
        mockedXmlSerializationService = mock(XmlSerializationService.class);
        mockedHttpService = mock(HttpService.class);
        metricsService = new MetricsService(mockedHttpService, mockedXmlSerializationService);
    }

    @Test
    public void providedHubAddressAndAnalysisId_shouldReturnAMetricsUrl() {
        final String HUB_ADDRESS = "10.10.10.10";
        final String ANALYSIS_ID = "10";

        final String EXPECTED_RESULT = String.format("http://%s/metrics/%s.xml", HUB_ADDRESS, ANALYSIS_ID);

        String result = metricsService.getMetricsUrlForAnAnalysisId(HUB_ADDRESS, ANALYSIS_ID);

        Assert.assertEquals(EXPECTED_RESULT, result);
    }

    @Test(expected = AbortException.class)
    public void providedInvalidMetricsUrl_shouldThrowAnAbortException() throws IOException {
        final String INVALID_URL = "http://10.10.10.10/invalid";

        when(mockedHttpService.getContentFromUrlAsString(INVALID_URL)).thenCallRealMethod();

        metricsService.getMetricsFromUrl(INVALID_URL);
    }

    @Test
    public void providedValidMetricsUrl_shouldReturnMetrics() throws IOException {
        final String VALID_METRICS_URL = "validUrl";
        final String RESPONSE_XML_CONTENT = "valid xml content";
        final Metrics EXPECTED_RESULT = new Metrics();

        when(mockedHttpService.getContentFromUrlAsString(VALID_METRICS_URL)).thenReturn(RESPONSE_XML_CONTENT);
        when(mockedXmlSerializationService.deserialize(any(String.class), isA(Class.class))).thenReturn(EXPECTED_RESULT);

        Metrics result = metricsService.getMetricsFromUrl(VALID_METRICS_URL);

        Assert.assertEquals(EXPECTED_RESULT, result);
    }
}
