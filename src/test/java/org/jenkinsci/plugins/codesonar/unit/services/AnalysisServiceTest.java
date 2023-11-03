package org.jenkinsci.plugins.codesonar.unit.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.fluent.Executor;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.jenkinsci.plugins.codesonar.CodeSonarPluginException;
import org.jenkinsci.plugins.codesonar.models.SearchResults;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.jenkinsci.plugins.codesonar.models.projects.Project;
import org.jenkinsci.plugins.codesonar.services.AnalysisService;
import org.jenkinsci.plugins.codesonar.services.HttpService;
import org.jenkinsci.plugins.codesonar.services.HttpServiceRequest;
import org.jenkinsci.plugins.codesonar.services.HttpServiceResponse;
import org.jenkinsci.plugins.codesonar.services.IAnalysisService;
import org.jenkinsci.plugins.codesonar.services.XmlSerializationService;
import org.junit.Before;
import org.junit.Test;

import hudson.AbortException;

public class AnalysisServiceTest {

    private XmlSerializationService mockedXmlSerializationService;
    private HttpService mockedHttpService;
    private HttpService httpService;
    private HttpServiceResponse mockedHttpServiceResponse;
    private IAnalysisService analysisService;

    @Before
    public void setUp() throws CodeSonarPluginException {
        mockedXmlSerializationService = mock(XmlSerializationService.class);
        mockedHttpServiceResponse = mock(HttpServiceResponse.class);
        mockedHttpService = mock(HttpService.class);
        analysisService = new AnalysisService(mockedHttpService, mockedXmlSerializationService, IAnalysisService.VISIBILITY_FILTER_ALL_WARNINGS_DEFAULT, IAnalysisService.VISIBILITY_FILTER_NEW_WARNINGS_DEFAULT, true);
    }

    @Test
    public void providedValidHubURIAndProjectExistsInAProjectTree_shouldReturnLatestAnalysisUrl() throws IOException, URISyntaxException {
        final String VALID_HUB_ADDRESS = "http://10.10.1.131";
        final String VALID_PROJECT_NAME = "pojectName";
        // "projects with trees xml"
        final InputStream RESPONSE_XML_CONTENT = new ByteArrayInputStream(("\"projects with trees xml\"").getBytes());
        final String PROJECT_URL = "validProjectURL";
        final String EXPECTED_RESULT = new URI(VALID_HUB_ADDRESS).resolve(PROJECT_URL).toString();

        Project proj = new Project();
        proj.setName(VALID_PROJECT_NAME);
        proj.setUrl(EXPECTED_RESULT);

        SearchResults searchResults = new SearchResults();
        searchResults.getProjects().add(proj);

        when(mockedHttpServiceResponse.getContentInputStream()).thenReturn(RESPONSE_XML_CONTENT);
        when(mockedHttpServiceResponse.getStatusCode()).thenReturn(200);
        when(mockedHttpServiceResponse.getReasonPhrase()).thenReturn("OK");
        when(mockedHttpService.getResponseFromUrl(notNull(URI.class))).thenReturn(mockedHttpServiceResponse);
        when(mockedHttpService.getResponseFromUrl(VALID_HUB_ADDRESS)).thenReturn(mockedHttpServiceResponse);
        when(mockedXmlSerializationService.deserialize(RESPONSE_XML_CONTENT, SearchResults.class)).thenReturn(searchResults);

        String latestAnalysisUrl = analysisService.getLatestAnalysisUrlForAProject(new URI(VALID_HUB_ADDRESS), VALID_PROJECT_NAME);

        assertEquals(EXPECTED_RESULT, latestAnalysisUrl);
    }

    @Test
    public void providedLogFileWithAnAnalysisUrlPresent_shouldReturnAnAnalysisUrl() {
        final List<String> LOG_FILE_WITH_URL = new ArrayList<>();
        LOG_FILE_WITH_URL.add("codesonar: Files parsed successfully.  Logs are visible at:");
        LOG_FILE_WITH_URL.add("codesonar: http://10.10.1.125:8080/analysis/98.html");
        LOG_FILE_WITH_URL.add("Use 'codesonar analyze' to start the analysis");

        final String EXPECTED_RESULT = "http://10.10.1.125:8080/analysis/98.xml";

        String result = analysisService.getAnalysisUrlFromLogFile(LOG_FILE_WITH_URL);

        assertEquals(EXPECTED_RESULT, result);
    }

    @Test
    public void providedLogFileWithNoAnalysisUrlPresent_shouldReturnNull() {
        final List<String> LOG_FILE_WITHOUT_URL = new ArrayList<>();
        LOG_FILE_WITHOUT_URL.add("codesonar: Files parsed successfully.  Logs are visible at:");
        LOG_FILE_WITHOUT_URL.add("Use 'codesonar analyze' to start the analysis");

        String result = analysisService.getAnalysisUrlFromLogFile(LOG_FILE_WITHOUT_URL);

        assertNull(result);
    }

    @Test(expected = CodeSonarPluginException.class)
    public void providedInvalidHubAddress_shouldThrowAnAbortException() throws IOException, URISyntaxException {
        final String INVALID_HUB_ADDRESS = "99.99.99.99";
        final String PROJECT_NAME = "pojectName";
        BasicCookieStore httpCookieStore = new BasicCookieStore();
        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        CloseableHttpClient httpClient = httpClientBuilder.evictExpiredConnections().build();
        Executor executor = Executor.newInstance(httpClient).use(httpCookieStore);
        

        when(mockedHttpService.getExecutor()).thenReturn(executor);
        when(mockedHttpService.getResponse(notNull(HttpServiceRequest.class))).thenCallRealMethod();
        when(mockedHttpService.getResponseFromUrl(notNull(URI.class))).thenCallRealMethod();
        when(mockedHttpService.getResponseFromUrl(any(String.class))).thenCallRealMethod();


        analysisService.getLatestAnalysisUrlForAProject(new URI(INVALID_HUB_ADDRESS), PROJECT_NAME);
    }

    @Test(expected = CodeSonarPluginException.class)
    public void projectWithProvidedProjectNameIsNotFoundOnTheHub_shouldThrowAnAbortException() throws Exception {
        final String VALID_HUB_ADDRESS = "http://10.10.1.131";
        final String VALID_PROJECT_NAME = "pojectName";
        final String RESPONSE_XML_CONTENT = "valid xml";
        
        SearchResults searchResults = new SearchResults();
        when(mockedHttpServiceResponse.getContentInputStream()).thenReturn(new ByteArrayInputStream(RESPONSE_XML_CONTENT.getBytes()));
        when(mockedHttpServiceResponse.readContent()).thenReturn(RESPONSE_XML_CONTENT);
        when(mockedHttpServiceResponse.getStatusCode()).thenReturn(200);
        when(mockedHttpServiceResponse.getReasonPhrase()).thenReturn("OK");
        when(mockedHttpService.getResponseFromUrl(notNull(String.class))).thenReturn(mockedHttpServiceResponse);
        when(mockedHttpService.getResponseFromUrl(notNull(URI.class))).thenReturn(mockedHttpServiceResponse);
        when(mockedXmlSerializationService.deserialize(notNull(InputStream.class), isA(Class.class))).thenReturn(searchResults);

        analysisService.getLatestAnalysisUrlForAProject(new URI(VALID_HUB_ADDRESS), VALID_PROJECT_NAME);
        
    }

    @Test(expected = CodeSonarPluginException.class)
    public void providedInvalidAnalysisUrl_shouldThrowAnAbortException() throws IOException {
        final String INVALID_ANALYSIS_URL = "10.10.10.10";
        BasicCookieStore httpCookieStore = new BasicCookieStore();
        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        CloseableHttpClient httpClient = httpClientBuilder.evictExpiredConnections().build();
        Executor executor = Executor.newInstance(httpClient).use(httpCookieStore);
        
        when(mockedHttpService.getExecutor()).thenReturn(executor);
        when(mockedHttpService.getResponse(any(HttpServiceRequest.class))).thenCallRealMethod();
        when(mockedHttpService.getResponseFromUrl(any(String.class))).thenCallRealMethod();
        analysisService.getAnalysisFromUrl(INVALID_ANALYSIS_URL);
    }

    @Test
    public void providedValidAnalysisUrl_shouldReturnAnAnalysis() throws Exception {
        final String VALID_ANALYSIS_URL = "10.10.10.10";
        final String RESPONSE_XML_CONTENT = "valid xml content";
        final InputStream RESPONSE_IS = IOUtils.toInputStream(RESPONSE_XML_CONTENT, "UTF-8");
        final Analysis ANALYSIS = new Analysis();
        when(mockedHttpServiceResponse.getContentInputStream()).thenReturn(RESPONSE_IS);
        when(mockedHttpServiceResponse.readContent()).thenReturn(RESPONSE_XML_CONTENT);
        when(mockedHttpServiceResponse.getStatusCode()).thenReturn(200);
        when(mockedHttpServiceResponse.getReasonPhrase()).thenReturn("OK");
        when(mockedHttpService.getResponseFromUrl(any(String.class))).thenReturn(mockedHttpServiceResponse);
        when(mockedXmlSerializationService.deserialize(any(InputStream.class), isA(Class.class))).thenReturn(ANALYSIS);
        Analysis analysis = analysisService.getAnalysisFromUrl(VALID_ANALYSIS_URL);
        assertNotNull(analysis);
    }
    
    @Test
    public void providedValidAnalysisUrlAndUrlFilterNEW_shouldReturnAnAnalysisUrlForNewWarnings() throws IOException {
        final URI BASE_HUB_URI = URI.create("10.10.10.10");
        final long ANALYSIS_ID = 15;
        final String RESPONSE_XML_CONTENT = "valid xml content";
        final InputStream RESPONSE_IS = IOUtils.toInputStream(RESPONSE_XML_CONTENT, "UTF-8");
        final Analysis ANALYSIS = new Analysis();

        when(mockedHttpServiceResponse.getContentInputStream()).thenReturn(RESPONSE_IS);
        when(mockedHttpServiceResponse.readContent()).thenReturn(RESPONSE_XML_CONTENT);
        when(mockedHttpServiceResponse.getStatusCode()).thenReturn(200);
        when(mockedHttpServiceResponse.getReasonPhrase()).thenReturn("OK");
        when(mockedHttpService.getResponseFromUrl(any(String.class))).thenReturn(mockedHttpServiceResponse);
        when(mockedXmlSerializationService.deserialize(any(InputStream.class), isA(Class.class))).thenReturn(ANALYSIS);

        Analysis analysis = analysisService.getAnalysisFromUrlWithNewWarnings(BASE_HUB_URI, ANALYSIS_ID);

        assertNotNull(analysis);
    }
}
