package org.jenkinsci.plugins.codesonar.unit.services;

import hudson.AbortException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jenkinsci.plugins.codesonar.models.Analysis;
import org.jenkinsci.plugins.codesonar.models.Projects;
import org.jenkinsci.plugins.codesonar.services.AnalysisService;
import org.jenkinsci.plugins.codesonar.services.HttpService;
import org.jenkinsci.plugins.codesonar.services.XmlSerializationService;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;

/**
 *
 * @author Andrius
 */
public class AnalysisServiceTest {

    private XmlSerializationService mockedXmlSerializationService;
    private HttpService mockedHttpService;
    private AnalysisService analysisService;

    @Before
    public void setUp() {
        mockedXmlSerializationService = mock(XmlSerializationService.class);
        mockedHttpService = mock(HttpService.class);
        analysisService = new AnalysisService(mockedXmlSerializationService, mockedHttpService);
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
    public void providedLogFileWithNoAnalysisUrlPresent_shouldReturNull() {
        final List<String> LOG_FILE_WITHOUT_URL = new ArrayList<>();
        LOG_FILE_WITHOUT_URL.add("codesonar: Files parsed successfully.  Logs are visible at:");
        LOG_FILE_WITHOUT_URL.add("Use 'codesonar analyze' to start the analysis");

        String result = analysisService.getAnalysisUrlFromLogFile(LOG_FILE_WITHOUT_URL);

        assertNull(result);
    }

    @Test(expected = AbortException.class)
    public void providedInvalidHubAddress_shouldThrowAnAbortException() throws IOException {
        final String INVALID_HUB_ADDRESS = "99.99.99.99";
        final String PROJECT_NAME = "pojectName";

        when(mockedHttpService.getContentFromUrlAsString(any(String.class))).thenCallRealMethod();

        analysisService.getLatestAnalysisUrlForAProject(INVALID_HUB_ADDRESS, PROJECT_NAME);
    }

    @Test(expected = AbortException.class)
    public void projectWithProvidedProjectNameIsNotFoundOnTheHub_shouldThrowAnAbortException() throws Exception {
        final String VALID_HUB_ADDRESS = "99.99.99.99";
        final String VALID_PROJECT_NAME = "pojectName";

        final String RESPONSE_XML_CONTENT = "valid xml";

        Projects projects = new Projects();
        projects.setProjects(Collections.EMPTY_LIST);

        when(mockedHttpService.getContentFromUrlAsString(notNull(String.class))).thenReturn(RESPONSE_XML_CONTENT);
        when(mockedXmlSerializationService.deserialize(notNull(String.class), isA(Class.class))).thenReturn(projects);

        analysisService.getLatestAnalysisUrlForAProject(VALID_HUB_ADDRESS, VALID_PROJECT_NAME);
    }

    @Test(expected = AbortException.class)
    public void providedInvalidAnalysisUrl_shouldThrowAnAbortException() throws IOException {
        final String INVALID_ANALYSIS_URL = "10.10.10.10";

        when(mockedHttpService.getContentFromUrlAsString(any(String.class))).thenCallRealMethod();

        analysisService.getAnalysisFromUrl(INVALID_ANALYSIS_URL);
    }

    @Test
    public void providedValidAnalysisUrl_shouldReturnAnAnalysis() throws Exception {
        final String VALID_ANALYSIS_URL = "10.10.10.10";
        final String RESPONSE_XML_CONTENT = "valid xml content";
        final Analysis Analysis = new Analysis();

        when(mockedHttpService.getContentFromUrlAsString(any(String.class))).thenReturn(RESPONSE_XML_CONTENT);
        when(mockedXmlSerializationService.deserialize(any(String.class), isA(Class.class))).thenReturn(Analysis);

        Analysis analysis = analysisService.getAnalysisFromUrl(VALID_ANALYSIS_URL);

        assertNotNull(analysis);
    }

}
