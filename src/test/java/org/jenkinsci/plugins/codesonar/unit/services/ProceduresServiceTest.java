package org.jenkinsci.plugins.codesonar.unit.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.codesonar.models.procedures.Procedures;
import org.jenkinsci.plugins.codesonar.services.HttpService;
import org.jenkinsci.plugins.codesonar.services.HttpServiceResponse;
import org.jenkinsci.plugins.codesonar.services.ProceduresService;
import org.jenkinsci.plugins.codesonar.services.XmlSerializationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Andrius
 */
class ProceduresServiceTest {

    private XmlSerializationService mockedXmlSerializationService;
    private HttpService mockedHttpService;
    private HttpServiceResponse mockedHttpServiceResponse;
    private ProceduresService proceduresService;

    @BeforeEach
    void setUp() {
        mockedXmlSerializationService = mock(XmlSerializationService.class);
        mockedHttpService = mock(HttpService.class);
        mockedHttpServiceResponse = mock(HttpServiceResponse.class);
        proceduresService = new ProceduresService(mockedHttpService, mockedXmlSerializationService, true);
    }

    @Test
    void providedHubAddressAndAnalysisId_shouldReturnAProceduresUrl() {
        final String HUB_ADDRESS = "http://10.10.1.131";
        final String ANALYSIS_ID = "10";

        final String EXPECTED_RESULT = String.format("%s/analysis/%s-procedures.xml", HUB_ADDRESS, ANALYSIS_ID);

        URI result = proceduresService.getProceduresUriFromAnAnalysisId(URI.create(HUB_ADDRESS), ANALYSIS_ID);

        assertEquals(EXPECTED_RESULT, result.toString());
    }

    @Test
    void providedValidMetricsUrl_shouldReturnMetrics() throws IOException {
        final URI VALID_METRICS_URI = URI.create("http://10.10.10.10/valid");
        final String RESPONSE_XML_CONTENT = "valid xml content";
        final Procedures EXPECTED_RESULT = new Procedures();
        final InputStream RESPONSE_IS = IOUtils.toInputStream(RESPONSE_XML_CONTENT, "UTF-8");

        when(mockedHttpServiceResponse.getContentInputStream()).thenReturn(RESPONSE_IS);
        when(mockedHttpServiceResponse.readContent()).thenReturn(RESPONSE_XML_CONTENT);
        when(mockedHttpServiceResponse.getStatusCode()).thenReturn(200);
        when(mockedHttpServiceResponse.getReasonPhrase()).thenReturn("OK");
        when(mockedHttpService.getResponseFromUrl(VALID_METRICS_URI)).thenReturn(mockedHttpServiceResponse);
        when(mockedXmlSerializationService.deserialize(any(InputStream.class), isA(Class.class))).thenReturn(EXPECTED_RESULT);

        Procedures result = proceduresService.getProceduresFromUri(VALID_METRICS_URI);

        assertEquals(EXPECTED_RESULT, result);
    }
}
