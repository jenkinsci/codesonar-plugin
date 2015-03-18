package org.jenkinsci.plugins.codesonar.unit.services;

import hudson.AbortException;
import org.jenkinsci.plugins.codesonar.services.HttpService;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Andrius
 */
public class HttpServiceTest {
    private HttpService httpService;

    @Before
    public void setUp() {
        httpService = new HttpService();
    }

    @Test(expected = AbortException.class)
    public void testGetContentFromURLAsString() throws Exception {
        final String INVALID_URL = "invalidurl";
       
        httpService.getContentFromUrlAsString(INVALID_URL);
    }
}
