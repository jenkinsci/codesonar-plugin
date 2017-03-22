package org.jenkinsci.plugins.codesonar.services;

import hudson.AbortException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.impl.client.BasicCookieStore;

import java.io.IOException;
import java.net.URI;

/**
 *
 * @author Andrius
 */
public class HttpService {

    private CookieStore httpCookieStore;
    private Executor executor;

    public HttpService() {
        httpCookieStore = new BasicCookieStore();
        executor = Executor.newInstance().use(httpCookieStore);
    }

    public String getContentFromUrlAsString(URI uri) throws AbortException {
        return getContentFromUrlAsString(uri.toString());
    }

    public String getContentFromUrlAsString(String url) throws AbortException {
        String output;
        try {
            output = executor.execute(Request.Get(url)).returnContent().asString();
        } catch (Exception e) {
            throw new AbortException(String.format("[CodeSonar] Error on url: %s%n[CodeSonar] Message is: %s", url, e.getMessage()));
        }

        return output;
    }
        
    public Response execute(Request request) throws IOException {
        return executor.execute(request);
    }
    
    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }
    
    public CookieStore getHttpCookieStore() {
        return httpCookieStore;
    }

    public void setHttpCookieStore(CookieStore httpCookieStore) {
        this.httpCookieStore = httpCookieStore;
    }
}
