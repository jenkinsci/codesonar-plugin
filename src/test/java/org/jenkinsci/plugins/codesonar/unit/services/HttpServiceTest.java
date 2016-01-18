package org.jenkinsci.plugins.codesonar.unit.services;

import hudson.AbortException;
import java.util.logging.Logger;
import org.jenkinsci.plugins.codesonar.services.HttpService;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Andrius
 */
public class HttpServiceTest {
    private HttpService httpService;
    private static final Logger log = Logger.getLogger(HttpServiceTest.class.toString());

    @Before
    public void setUp() {
        
        httpService = new HttpService();
    }

    @Test(expected = AbortException.class)
    public void testGetContentFromURLAsString() throws Exception {
        final String INVALID_URL = "invalidurl";
       
        httpService.getContentFromUrlAsString(INVALID_URL);
    }
    
    @Test()
    public void testtest() throws Exception {
        log.info("---------------starting----------------------");
        final String URL = "http://10.10.1.131:8010/analysis/29.xml?filter=2";
       
        String str = httpService.getContentFromUrlAsString(URL);
        log.info("---------------result----------------------");
        log.info(str);
    }
}
