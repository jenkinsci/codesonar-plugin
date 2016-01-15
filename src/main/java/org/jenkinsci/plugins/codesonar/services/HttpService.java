package org.jenkinsci.plugins.codesonar.services;

import hudson.AbortException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;

/**
 *
 * @author Andrius
 */
public class HttpService implements Serializable {
    
    private static final Logger logger = Logger.getLogger(HttpService.class.getName());
    
    public String getContentFromUrlAsString(String url) throws AbortException { 
        System.out.println("------------sending request----------------");
        logger.fine(String.format("Request sent to %s", url));        
        String output;
        try {
            Response response = Request.Get(url).execute();
            
            HttpResponse httpResponse = response.returnResponse();

            System.out.println("------------reading headers----------------");
            Header[] allHeaders = httpResponse.getAllHeaders();
            for (Header header : allHeaders) {
                System.out.println(header.getName());
                System.out.println(header.getValue());
                System.out.println("----------------------------");
            }
            InputStream contentStream = httpResponse.getEntity().getContent();
            output = IOUtils.toString(contentStream); 
//                output = response.returnContent().asString();
//            output = Request.Get(url).execute().returnContent().asString();
        } catch (Exception e) {
            logger.log(Level.SEVERE, String.format("[CodeSonar] Error on url: %s", url), e);
            throw new AbortException(String.format("[CodeSonar] Error on url: %s%n[CodeSonar] Message is: %s", url, e.getMessage()));
        } 
        
        return output;
    }
}
