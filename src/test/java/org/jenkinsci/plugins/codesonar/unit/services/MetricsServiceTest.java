package org.jenkinsci.plugins.codesonar.unit.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.commons.io.IOUtils;
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
        final String HUB_ADDRESS = "http://10.10.10.10";
        final String ANALYSIS_ID = "10";
        final String EXPECTED_RESULT = String.format("%s/metrics/%s.xml", HUB_ADDRESS, ANALYSIS_ID);
        URI result = metricsService.getMetricsUriFromAnAnalysisId(URI.create(HUB_ADDRESS), ANALYSIS_ID);
        Assert.assertEquals(EXPECTED_RESULT, result.toString());
    }

    @Test
    public void providedValidMetricsUrl_shouldReturnMetrics() throws IOException {
        final URI VALID_METRICS_URI = URI.create("http://10.10.10.10/validUrl");
        final String RESPONSE_XML_CONTENT = "valid xml content";
        final Metrics EXPECTED_RESULT = new Metrics();
        final InputStream RESPONSE_IS = IOUtils.toInputStream(RESPONSE_XML_CONTENT, "UTF-8");
        when(mockedHttpService.getContentFromUrlAsString(VALID_METRICS_URI)).thenReturn(RESPONSE_XML_CONTENT);
        when(mockedHttpService.getContentFromUrlAsInputStream(VALID_METRICS_URI)).thenReturn(RESPONSE_IS);
        when(mockedXmlSerializationService.deserialize(any(InputStream.class), isA(Class.class))).thenReturn(EXPECTED_RESULT);
        Metrics result = metricsService.getMetricsFromUri(VALID_METRICS_URI);
        Assert.assertEquals(EXPECTED_RESULT, result);
    }
}
