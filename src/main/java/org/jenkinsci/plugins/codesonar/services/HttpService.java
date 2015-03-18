package org.jenkinsci.plugins.codesonar.services;

import hudson.AbortException;
import java.io.IOException;
import org.apache.http.client.fluent.Request;

/**
 *
 * @author Andrius
 */
public class HttpService {

    public String getContentFromUrlAsString(String url) throws IOException {
        String output;
        try {
            output = Request.Get(url).execute().returnContent().asString();
        } catch (Exception e) {
            throw new AbortException(e.getMessage());
        } 
        
        return output;
    }
}
