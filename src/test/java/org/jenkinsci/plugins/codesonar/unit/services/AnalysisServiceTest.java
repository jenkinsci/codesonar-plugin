/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.codesonar.unit.services;

import hudson.AbortException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jenkinsci.plugins.codesonar.services.AnalysisService;
import org.jenkinsci.plugins.codesonar.services.HttpService;
import org.jenkinsci.plugins.codesonar.services.XmlSerializationService;
import org.junit.After;
import org.junit.Assert;
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
    
    @After
    public void tearDown() {
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
        
        when(mockedHttpService.GetContentFromURLAsString(any(String.class))).thenCallRealMethod();
        
        analysisService.getLatestAnalysisUrlForAProject(INVALID_HUB_ADDRESS, PROJECT_NAME);
    }
    
    @Test
    public void projectWithProvidedProjectNameIsNotFoundOnTheHub_shouldThrowAnAbortException() {
        //arrange 
        //act
        //assert
        Assert.fail("not implemented");
    }
    
    @Test
    public void providedValidHubAddressAndPorojectName_shouldReturnAnAnalysisUrl() {
        //arrange
        //act
        //assert
        Assert.fail("not implemented");
    }
    
    @Test
    public void providedInvalidAnalysisUrl_shouldThrowAnAbortException() {
        //arrange
        //act
        //assert
        Assert.fail("not implemented");
    }
    
    @Test
    public void providedValidAnalysisUrl_shouldReturnAnAnalysis() {
        //arrange
        //act
        //assert
        Assert.fail("not implemented");
    }
            
            
            
    

}
