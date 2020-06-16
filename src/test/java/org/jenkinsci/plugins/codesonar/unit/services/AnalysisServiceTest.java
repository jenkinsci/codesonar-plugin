package org.jenkinsci.plugins.codesonar.unit.services;

import hudson.AbortException;
import java.io.ByteArrayInputStream;

import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.codesonar.models.SearchResults;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.jenkinsci.plugins.codesonar.models.projects.Project;
import org.jenkinsci.plugins.codesonar.services.AnalysisService;
import org.jenkinsci.plugins.codesonar.services.HttpService;
import org.jenkinsci.plugins.codesonar.services.IAnalysisService;
import org.jenkinsci.plugins.codesonar.services.XmlSerializationService;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AnalysisServiceTest {

    private XmlSerializationService mockedXmlSerializationService;
    private HttpService mockedHttpService;
    private IAnalysisService analysisService;
    private final String ACTIVE_WARNING_FILTER = "2";

    @Before
    public void setUp() {
        mockedXmlSerializationService = mock(XmlSerializationService.class);
        mockedHttpService = mock(HttpService.class);
        analysisService = new AnalysisService(mockedHttpService, mockedXmlSerializationService, this.ACTIVE_WARNING_FILTER);
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

        when(mockedHttpService.getContentFromUrlAsInputStream(notNull(URI.class))).thenReturn(RESPONSE_XML_CONTENT);
        when(mockedHttpService.getContentFromUrlAsInputStream(VALID_HUB_ADDRESS)).thenReturn(RESPONSE_XML_CONTENT);
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

    @Test(expected = AbortException.class)
    public void providedInvalidHubAddress_shouldThrowAnAbortException() throws IOException, URISyntaxException {
        final String INVALID_HUB_ADDRESS = "99.99.99.99";
        final String PROJECT_NAME = "pojectName";

        when(mockedHttpService.getContentFromUrlAsInputStream(notNull(URI.class))).thenCallRealMethod();
        when(mockedHttpService.getContentFromUrlAsInputStream(any(String.class))).thenCallRealMethod();


        analysisService.getLatestAnalysisUrlForAProject(new URI(INVALID_HUB_ADDRESS), PROJECT_NAME);
    }

    @Test(expected = AbortException.class)
    public void projectWithProvidedProjectNameIsNotFoundOnTheHub_shouldThrowAnAbortException() throws Exception {
        final String VALID_HUB_ADDRESS = "http://10.10.1.131";
        final String VALID_PROJECT_NAME = "pojectName";

        final String RESPONSE_XML_CONTENT = "valid xml";

        SearchResults searchResults = new SearchResults();
        when(mockedHttpService.getContentFromUrlAsString(notNull(URI.class))).thenCallRealMethod();
        when(mockedHttpService.getContentFromUrlAsString(notNull(String.class))).thenReturn(RESPONSE_XML_CONTENT);
        when(mockedHttpService.getContentFromUrlAsInputStream(notNull(URI.class))).thenCallRealMethod();
        when(mockedHttpService.getContentFromUrlAsInputStream(notNull(String.class))).thenReturn(new ByteArrayInputStream(RESPONSE_XML_CONTENT.getBytes()));
        when(mockedXmlSerializationService.deserialize(notNull(InputStream.class), isA(Class.class))).thenReturn(searchResults);

        analysisService.getLatestAnalysisUrlForAProject(new URI(VALID_HUB_ADDRESS), VALID_PROJECT_NAME);
    }

    @Test(expected = AbortException.class)
    public void providedInvalidAnalysisUrl_shouldThrowAnAbortException() throws IOException {
        final String INVALID_ANALYSIS_URL = "10.10.10.10";
        when(mockedHttpService.getContentFromUrlAsInputStream(any(String.class))).thenCallRealMethod();
        analysisService.getAnalysisFromUrl(INVALID_ANALYSIS_URL);
    }

    @Test
    public void providedValidAnalysisUrl_shouldReturnAnAnalysis() throws Exception {
        final String VALID_ANALYSIS_URL = "10.10.10.10";
        final String RESPONSE_XML_CONTENT = "valid xml content";
        final InputStream RESPONSE_IS = IOUtils.toInputStream(RESPONSE_XML_CONTENT, "UTF-8");
        final Analysis ANALYSIS = new Analysis();
        when(mockedHttpService.getContentFromUrlAsString(any(String.class))).thenReturn(RESPONSE_XML_CONTENT);
        when(mockedHttpService.getContentFromUrlAsInputStream(any(String.class))).thenReturn(RESPONSE_IS);               
        when(mockedXmlSerializationService.deserialize(any(InputStream.class), isA(Class.class))).thenReturn(ANALYSIS);
        Analysis analysis = analysisService.getAnalysisFromUrl(VALID_ANALYSIS_URL);
        assertNotNull(analysis);
    }
    
    @Test
    public void providedValidAnalysisUrlAndUrlFilterNEW_shouldReturnAnAnalysisUrlForNewWarnings() throws IOException {
        final String VALID_ANALYSIS_URL = "10.10.10.10";
        final String RESPONSE_XML_CONTENT = "valid xml content";
        final InputStream RESPONSE_IS = IOUtils.toInputStream(RESPONSE_XML_CONTENT, "UTF-8");
        final Analysis ANALYSIS = new Analysis();

        when(mockedHttpService.getContentFromUrlAsString(any(String.class))).thenReturn(RESPONSE_XML_CONTENT);
        when(mockedHttpService.getContentFromUrlAsInputStream(any(String.class))).thenReturn(RESPONSE_IS);
        when(mockedXmlSerializationService.deserialize(any(InputStream.class), isA(Class.class))).thenReturn(ANALYSIS);

        Analysis analysis = analysisService.getAnalysisFromUrlWithNewWarnings(VALID_ANALYSIS_URL);

        assertNotNull(analysis);
    }
}
