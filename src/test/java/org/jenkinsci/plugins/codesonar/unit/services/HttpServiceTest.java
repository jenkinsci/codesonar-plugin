package org.jenkinsci.plugins.codesonar.unit.services;

import java.util.logging.Logger;

import org.jenkinsci.plugins.codesonar.CodeSonarPluginException;
import org.jenkinsci.plugins.codesonar.services.HttpService;
import org.junit.Before;
import org.junit.Test;

import hudson.AbortException;

/**
 *
 * @author Andrius
 */
public class HttpServiceTest {
    private HttpService httpService;
    private static final Logger log = Logger.getLogger(HttpServiceTest.class.toString());

    @Before
    public void setUp() throws CodeSonarPluginException {
        httpService = new HttpService(null, null, null, -1);
    }

    @Test(expected = CodeSonarPluginException.class)
    public void testgetResponseFromUrl() throws Exception {
        final String INVALID_URL = "invalidurl";
       
        httpService.getResponseFromUrl(INVALID_URL).readContent();
    }
}
