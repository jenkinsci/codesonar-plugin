package org.jenkinsci.plugins.codesonar.services;

import hudson.AbortException;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.jenkinsci.plugins.codesonar.CodeSonarPublisher;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Andrius
 */
public class HttpService {

    private static final Logger LOGGER = Logger.getLogger(CodeSonarPublisher.class.getName());

    private CookieStore httpCookieStore;
    private Executor executor;
    private int socketTimeoutMS = -1;

    public HttpService(X509Certificate certificate) throws AbortException {
        httpCookieStore = new BasicCookieStore();
        Collection<X509Certificate> certsLocalCopy = new ArrayList<X509Certificate>();
        if(certificate != null) certsLocalCopy.add(certificate);
        try {
            SSLContext sslContext = SSLContextBuilder
                    .create()
                    .loadTrustMaterial(new CertificateFileTrustStrategy(certsLocalCopy))
                    .build();

            final SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);
            CloseableHttpClient httpClient = HttpClients.custom()
                    .setSSLSocketFactory(csf)
                    .evictExpiredConnections()
                    .build();
            executor = Executor.newInstance(httpClient).use(httpCookieStore);
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new AbortException(String.format("[CodeSonar] Error initiating httpClient.%n[CodeSonar] Exception message: %s", e.getMessage()));
        }
    }

    public int getSocketTimeoutMS() {
        return socketTimeoutMS;
    }

    public void setSocketTimeoutMS(int socketTimeoutMS) {
        LOGGER.log(Level.FINE, "[CodeSonar] HttpService - setSocketTimeoutMS to {0}", socketTimeoutMS);
        this.socketTimeoutMS = socketTimeoutMS;
    }

    public String getContentFromUrlAsString(URI uri) throws AbortException {
        return getContentFromUrlAsString(uri.toString());
    }
    
    public String getContentFromUrlAsString(String url) throws AbortException {
        if(!url.contains("response_try_plaintext")) {
            url = (url.contains("?")) ? url + "#response_try_plaintext=1" : url + "?response_try_plaintext=1";
        }
        int status = -1;
        String reason = "";
        String body = "";

        try {
            Request req = Request.Get(url);
            if(getSocketTimeoutMS() != -1) req.socketTimeout(getSocketTimeoutMS());
            HttpResponse resp = executor.execute(req).returnResponse();
            status = resp.getStatusLine().getStatusCode();
            reason = resp.getStatusLine().getReasonPhrase();
            body = EntityUtils.toString(resp.getEntity(), "UTF-8");
        } catch (Exception e) {
            throw new AbortException(String.format("[CodeSonar] Error on url: %s%n[CodeSonar] Message is: %s", url, e.getMessage()));
        }
        if (status != 200) {
            throw new AbortException(String.format("[CodeSonar] Error communicating with CodeSonar Hub. %n[CodeSonar] URI: %s%n[CodeSonar] HTTP status code: %s - %s %n[CodeSonar] HTTP Body: %s", url, status, reason, body));
        }
        return body;
    }
    
    public InputStream getContentFromUrlAsInputStream(URI uri) throws AbortException {
        return getContentFromUrlAsInputStream(uri.toString());
    }
    
    public InputStream getContentFromUrlAsInputStream(String url) throws AbortException {
        InputStream is;
        if(!url.contains("response_try_plaintext")) {
            url = (url.contains("?")) ? url + "#response_try_plaintext=1" : url + "?response_try_plaintext=1";
        }
        int status = -1;
        String reason = "";
        String body = "";

        try {
            Request req = Request.Get(url);
            if(getSocketTimeoutMS() != -1) req.socketTimeout(getSocketTimeoutMS());
            HttpResponse resp = executor.execute(req).returnResponse();
            is = resp.getEntity().getContent();
            status = resp.getStatusLine().getStatusCode();
            reason = resp.getStatusLine().getReasonPhrase();
            body = EntityUtils.toString(resp.getEntity(), "UTF-8");
        } catch (Exception e) {
            throw new AbortException(String.format("[CodeSonar] Error on url: %s%n[CodeSonar] Message is: %s", url, e.getMessage()));
        }
        if (status != 200) {
            throw new AbortException(String.format("[CodeSonar] Error communicating with CodeSonar Hub. %n[CodeSonar] URI: %s%n[CodeSonar] HTTP status code: %s - %s %n[CodeSonar] HTTP Body: %s", url, status, reason, body));
        }
        return is;
    }
    
    public Response execute(Request request) throws IOException {
        if(getSocketTimeoutMS() != -1) request.socketTimeout(getSocketTimeoutMS());
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