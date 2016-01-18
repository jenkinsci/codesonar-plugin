package org.jenkinsci.plugins.codesonar.services;

import hudson.AbortException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.cookie.Cookie;
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
        System.out.println("------------sending request----------------");
        
        System.out.println("------------url----------------");
        System.out.println(url);
        
        logger.fine(String.format("Request sent to %s", url));       

        String output;
        try {
            Response response = executor.cookieStore(httpCookieStore)
                    .execute(Request.Get(url));
            
            HttpResponse httpResponse = response.returnResponse();

            System.out.println("------------reading headers----------------");
            Header[] allHeaders = httpResponse.getAllHeaders();
            for (Header header : allHeaders) {
                System.out.println(header.getName());
                System.out.println(header.getValue());
                System.out.println("----------------------------");
            }
            
            System.out.println("------------reading cookie store----------------");
            List<Cookie> cookies = httpCookieStore.getCookies();
            for (Cookie cooky : cookies) {
                String str = String.format("cookie name %s; value %s", cooky.getName(), cooky.getValue());
                System.out.println(str);
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
