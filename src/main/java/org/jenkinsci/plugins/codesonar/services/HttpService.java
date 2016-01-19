package org.jenkinsci.plugins.codesonar.services;

import hudson.AbortException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.client.fluent.Request;

/**
 *
 * @author Andrius
 */
public class HttpService implements Serializable {
    
    private static final Logger logger = Logger.getLogger(HttpService.class.getName());
    
    public String getContentFromUrlAsString(String url) throws AbortException { 
        logger.fine(String.format("Request sent to %s", url));        
        String output;
        try {
            output = Request.Get(url).execute().returnContent().asString();
        } catch (Exception e) {
            logger.log(Level.SEVERE, String.format("[CodeSonar] Error on url: %s", url), e);
            throw new AbortException(String.format("[CodeSonar] Error on url: %s%n[CodeSonar] Message is: %s", url, e.getMessage()));
        } 
        
        return output;
    }
}
