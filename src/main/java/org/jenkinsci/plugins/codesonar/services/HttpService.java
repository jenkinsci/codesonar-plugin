package org.jenkinsci.plugins.codesonar.services;

import hudson.AbortException;
import hudson.model.BuildListener;
import java.io.Serializable;
import org.apache.http.client.fluent.Request;

/**
 *
 * @author Andrius
 */
public class HttpService implements Serializable {
    private transient BuildListener listener;

    public String getContentFromUrlAsString(String url) throws AbortException {
        log(String.format("Request sent to %s", url));
        
        String output;
        try {
            output = Request.Get(url).execute().returnContent().asString();
        } catch (Exception e) {
            log(String.format("Error on url: %s", url));
            log(String.format("Exception message is: %s", e.getMessage()));
            throw new AbortException(e.getMessage());
        } 
        
        return output;
    }
    
    public void setListener(BuildListener listener) {
        this.listener = listener;
    }
    
    private void log(String content) {
        if(listener != null) {
            listener.getLogger().println("[CodeSonar] "+content);
        }
    }
}
