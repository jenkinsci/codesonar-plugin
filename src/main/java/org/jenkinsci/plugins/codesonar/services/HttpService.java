package org.jenkinsci.plugins.codesonar.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.jenkinsci.plugins.codesonar.CodeSonarLogger;

import com.google.common.base.Throwables;

import hudson.AbortException;
import hudson.util.Secret;

/**
 *
 * @author Andrius
 */
public class HttpService {

    private static final Logger LOGGER = Logger.getLogger(HttpService.class.getName());

    private CookieStore httpCookieStore;
    private Executor executor;
    private int socketTimeoutMS = -1;

    public HttpService(Collection<? extends Certificate> serverCertificates, KeyStore clientCertificateKeyStore, Secret clientCertificatePassword, int socketTimeoutMS) throws AbortException {
        LOGGER.log(Level.INFO, "Initializing HttpService");
        this.socketTimeoutMS = socketTimeoutMS;
        httpCookieStore = new BasicCookieStore();
        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        
        if(serverCertificates != null
                || (clientCertificateKeyStore != null && clientCertificatePassword != null)) {
            LOGGER.log(Level.INFO, "Initializing SSL context");
            SSLContextBuilder sslContextBuilder = SSLContextBuilder.create();
            //If a server certificates are available, then set them in the SSL context so that they can be used by the trust strategy
            if(serverCertificates != null) {
                LOGGER.log(Level.INFO, "Adding server certificates to the SSL context");
                LOGGER.log(Level.INFO, String.format("Server certificates list size %d", serverCertificates.size()));
                try {
                    sslContextBuilder.loadTrustMaterial(new CertificateFileTrustStrategy(serverCertificates));
                } catch (NoSuchAlgorithmException | KeyStoreException e) {
                	String message = CodeSonarLogger.formatMessageMultiLine(
                			CodeSonarLogger.createLine("Error setting up server certificates"),
                			CodeSonarLogger.createLine("%s: %s", e.getClass().getName(), e.getMessage()),
                			CodeSonarLogger.createLine("Stack Trace: %s", Throwables.getStackTraceAsString(e))
                			);
                    throw new AbortException(message);
                }
            }
            
            //If a client certificate is available, then set it in the SSL context so that it will be used during the authentication process
            if(clientCertificateKeyStore != null && clientCertificatePassword != null) {
                LOGGER.log(Level.INFO, "Adding client certificate to the SSL context");
                try {
                    sslContextBuilder.loadKeyMaterial(clientCertificateKeyStore, clientCertificatePassword.getPlainText().toCharArray());
                } catch (UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException e) {
                	String message = CodeSonarLogger.formatMessageMultiLine(
                			CodeSonarLogger.createLine("Error setting up client certificate"),
                			CodeSonarLogger.createLine("%s: %s", e.getClass().getName(), e.getMessage()),
                			CodeSonarLogger.createLine("Stack Trace: %s", Throwables.getStackTraceAsString(e))
                			);
                    throw new AbortException(message);
                }
            }
            //Prepare the SSL context in order to let the HTTP client using specified certificates
            SSLContext sslContext;
            try {
                sslContext = sslContextBuilder.build();
                final SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);
                httpClientBuilder.setSSLSocketFactory(csf);
                LOGGER.log(Level.INFO, "SSL context initialized");
            } catch (KeyManagementException | NoSuchAlgorithmException e) {
            	String message = CodeSonarLogger.formatMessageMultiLine(
            			CodeSonarLogger.createLine("Error initiating SSL context."),
            			CodeSonarLogger.createLine("Exception message: %s", e.getMessage())
            			);
                throw new AbortException(message);
            }
        }
        
        CloseableHttpClient httpClient = httpClientBuilder.evictExpiredConnections()
                .build();
        executor = Executor.newInstance(httpClient).use(httpCookieStore);
        LOGGER.log(Level.INFO, "HttpService initialized");
    }

    public void setSocketTimeoutMS(int socketTimeoutMS) {
        LOGGER.log(Level.FINE, "HttpService - setSocketTimeoutMS to {0}", socketTimeoutMS);
        this.socketTimeoutMS = socketTimeoutMS;
    }

    public String getContentFromUrlAsString(URI uri) throws AbortException {
        return getContentFromUrlAsString(uri.toString());
    }
    
    public String getContentFromUrlAsString(String url) throws AbortException {
        if(!url.contains("response_try_plaintext")) {
            url = (url.contains("?")) ? url + "#response_try_plaintext=1" : url + "?response_try_plaintext=1";
        }
        LOGGER.log(Level.INFO, String.format("getContentFromUrlAsString(%s)", url));
        int status = -1;
        String reason = "";
        String body = "";

        try {
            Request req = Request.Get(url);
            if(socketTimeoutMS != -1) req.socketTimeout(socketTimeoutMS);
//            LOGGER.log(Level.INFO, String.format("Listing cookies held by coockie store (instance: %s)", httpCookieStore.getClass().getName()));
//            httpCookieStore.getCookies().forEach(cookie -> LOGGER.log(Level.INFO, String.format("%s  %s=%s", cookie.getClass().getName(), cookie.getName(), cookie.getValue())));
            HttpResponse resp = executor.execute(req).returnResponse();
            status = resp.getStatusLine().getStatusCode();
            reason = resp.getStatusLine().getReasonPhrase();
            body = EntityUtils.toString(resp.getEntity(), "UTF-8");
        } catch (Exception e) {
        	String message = CodeSonarLogger.formatMessageMultiLine(
        			CodeSonarLogger.createLine("Error on url: %s", url),
        			CodeSonarLogger.createLine("Message is: %s", e.getMessage())
        			);
            throw new AbortException(message);
        }
        if (status != 200) {
        	String message = CodeSonarLogger.formatMessageMultiLine(
        			CodeSonarLogger.createLine("Error communicating with CodeSonar Hub."),
        			CodeSonarLogger.createLine("URI: %s", url),
        			CodeSonarLogger.createLine("HTTP status code: %s - %s", status, reason),
        			CodeSonarLogger.createLine("HTTP Body: %s", body)
        			);
            throw new AbortException(message);
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
        LOGGER.log(Level.INFO, String.format("getContentFromUrlAsInputStream(%s)", url));
        int status = -1;
        String reason = "";
        String body = "";

        try {
            Request req = Request.Get(url);
            if(socketTimeoutMS != -1) req.socketTimeout(socketTimeoutMS);
//            LOGGER.log(Level.INFO, String.format("Listing cookies held by coockie store (instance: %s)", httpCookieStore.getClass().getName()));
//            httpCookieStore.getCookies().forEach(cookie -> LOGGER.log(Level.INFO, String.format("%s  %s=%s", cookie.getClass().getName(), cookie.getName(), cookie.getValue())));
            HttpResponse resp = executor.execute(req).returnResponse();
            is = resp.getEntity().getContent();
            status = resp.getStatusLine().getStatusCode();
            reason = resp.getStatusLine().getReasonPhrase();
            body = EntityUtils.toString(resp.getEntity(), "UTF-8");
        } catch (Exception e) {
        	String message = CodeSonarLogger.formatMessageMultiLine(
        			CodeSonarLogger.createLine("Error on url: %s", url),
        			CodeSonarLogger.createLine("Message is: %s", e.getMessage())
        			);
            throw new AbortException(message);
        }
        if (status != 200) {
        	String message = CodeSonarLogger.formatMessageMultiLine(
        			CodeSonarLogger.createLine("Error communicating with CodeSonar Hub."),
        			CodeSonarLogger.createLine("URI: %s", url),
        			CodeSonarLogger.createLine("HTTP status code: %s - %s", status, reason),
        			CodeSonarLogger.createLine("HTTP Body: %s", body)
        			);
            throw new AbortException(message);
        }
        return is;
    }
    
    public Response execute(Request request) throws IOException {
        if(socketTimeoutMS != -1) request.socketTimeout(socketTimeoutMS);
        return executor.execute(request);
    }
    
}