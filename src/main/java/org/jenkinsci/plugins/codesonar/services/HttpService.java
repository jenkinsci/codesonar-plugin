package org.jenkinsci.plugins.codesonar.services;

import java.io.IOException;
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
import org.jenkinsci.plugins.codesonar.CodeSonarPluginException;
import org.jenkinsci.plugins.codesonar.models.HttpServiceResponse;

import com.google.common.base.Throwables;

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

    public HttpService(Collection<? extends Certificate> serverCertificates, KeyStore clientCertificateKeyStore, Secret clientCertificatePassword, int socketTimeoutMS) throws CodeSonarPluginException {
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
                LOGGER.log(Level.INFO, "Server certificates list size {0}", serverCertificates.size());
                try {
                    sslContextBuilder.loadTrustMaterial(new CertificateFileTrustStrategy(serverCertificates));
                } catch (NoSuchAlgorithmException | KeyStoreException e) {
                    throw createError("Error setting up server certificates  %n{0}: {1}%nStack Trace: {2}", e.getClass().getName(), e.getMessage(), Throwables.getStackTraceAsString(e));
                }
            }
            
            //If a client certificate is available, then set it in the SSL context so that it will be used during the authentication process
            if(clientCertificateKeyStore != null && clientCertificatePassword != null) {
                LOGGER.log(Level.INFO, "Adding client certificate to the SSL context");
                try {
                    sslContextBuilder.loadKeyMaterial(clientCertificateKeyStore, clientCertificatePassword.getPlainText().toCharArray());
                } catch (UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException e) {
                    throw createError("Error setting up client certificate  %n{0}: {1}%nStack Trace: {2}", e.getClass().getName(), e.getMessage(), Throwables.getStackTraceAsString(e));
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
                throw createError("Error initiating SSL context.%nException message: {0}", e.getMessage());
            }
        }
        
        CloseableHttpClient httpClient = httpClientBuilder.evictExpiredConnections()
                .build();
        executor = Executor.newInstance(httpClient).use(httpCookieStore);
        LOGGER.log(Level.INFO, "HttpService initialized");
    }
    
    private CodeSonarPluginException createError(String msg, Object...args) {
        return new CodeSonarPluginException(msg, args);
    }
    
    public void setSocketTimeoutMS(int socketTimeoutMS) {
        LOGGER.log(Level.FINE, "HttpService - setSocketTimeoutMS to {0}", socketTimeoutMS);
        this.socketTimeoutMS = socketTimeoutMS;
    }

    public HttpServiceResponse getResponseFromUrl(URI uri) throws CodeSonarPluginException {
        return getResponseFromUrl(uri.toString());
    }
    
    public HttpServiceResponse getResponseFromUrl(String url) throws CodeSonarPluginException {
        if (!url.contains("response_try_plaintext")) {
            url = (url.contains("?")) ? url + "#response_try_plaintext=1" : url + "?response_try_plaintext=1";
        }
        LOGGER.log(Level.INFO, "getResponseFromUrl({0})", url);
        Request req = Request.Get(url);
        if (socketTimeoutMS != -1)
            req.socketTimeout(socketTimeoutMS);
//        LOGGER.log(Level.INFO, String.format("Listing cookies held by coockie store (instance: %s)", httpCookieStore.getClass().getName()));
//        httpCookieStore.getCookies().forEach(cookie -> LOGGER.log(Level.INFO, String.format("%s  %s=%s", cookie.getClass().getName(), cookie.getName(), cookie.getValue())));
        HttpServiceResponse serviceResponse = null;
        try {
            HttpResponse resp = getExecutor().execute(req).returnResponse();
            serviceResponse = new HttpServiceResponse(resp.getStatusLine().getStatusCode(), resp.getStatusLine().getReasonPhrase(), resp.getEntity().getContent());
        } catch (IOException e) {
            throw createError("Error requesting URL: {0}%nException message: {1}%nStack Trace: {2}", url, e.getMessage(), Throwables.getStackTraceAsString(e));
        }
        
        return serviceResponse;
    }
    
    public Response execute(Request request) throws IOException {
        if(socketTimeoutMS != -1) request.socketTimeout(socketTimeoutMS);
        return getExecutor().execute(request);
    }
    
    public Executor getExecutor() {
        return this.executor;
    }
}