package org.jenkinsci.plugins.codesonar.services;

import hudson.AbortException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.client.CookieStore;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.impl.client.BasicCookieStore;

/**
 *
 * @author Andrius
 */
public class HttpService implements Serializable {
    CookieStore httpCookieStore;
    Executor executor;
    
    public HttpService() {
        httpCookieStore = new BasicCookieStore();
        executor = Executor.newInstance();
    }
    
    private static final Logger logger = Logger.getLogger(HttpService.class.getName());
    
    public String getContentFromUrlAsString(String url) throws AbortException { 
        logger.fine(String.format("Request sent to %s", url));        
        String output;
        try {
            output = executor.cookieStore(httpCookieStore)
                    .execute(Request.Get(url)).returnContent().asString();
        } catch (Exception e) {
            logger.log(Level.SEVERE, String.format("[CodeSonar] Error on url: %s", url), e);
            throw new AbortException(String.format("[CodeSonar] Error on url: %s%n[CodeSonar] Message is: %s", url, e.getMessage()));
        } 
        
        return output;
    }
}
