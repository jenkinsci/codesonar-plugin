package org.jenkinsci.plugins.codesonar.services;

import com.google.common.base.Throwables;
import hudson.AbortException;
import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.List;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

public class AuthenticationService {
    private HttpService httpService;

    public AuthenticationService(HttpService httpService) {
        this.httpService = httpService;
    }

    public void authenticate(URI baseHubUri, KeyStore keyStore, String password) throws AbortException {
        SSLContext sslContext;
        try {
            sslContext = SSLContexts.custom().loadKeyMaterial(keyStore, password.toCharArray()).build();
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException e) {
            throw new AbortException(String.format("[CodeSonar] failed to authenticate. %n[CodeSonar] %s: %s%n[CodeSonar] Stack Trace: %s", e.getClass().getName(), e.getMessage(), Throwables.getStackTraceAsString(e)));        }

        HttpClient httpClient = HttpClients.custom().setSSLContext(sslContext).build();

        httpService.setExecutor(Executor.newInstance(httpClient).use(httpService.getHttpCookieStore()));

        List<NameValuePair> loginForm = Form.form()
                .add("sif_use_tls", "yes")
                .add("sif_sign_in", "yes")
                .add("sif_log_out_competitor", "yes")
                .add("response_try_plaintext", "1")
                .build();

        int status = -1;
        String reason = "";
        String body = "";
        URI resolvedURI = baseHubUri;

        try {
            resolvedURI = baseHubUri.resolve("/sign_in.html?response_try_plaintext=1");

            HttpResponse resp = httpService.execute(Request.Post(resolvedURI)
                    .bodyForm(loginForm))
                    .returnResponse();

            status = resp.getStatusLine().getStatusCode();
            reason = resp.getStatusLine().getReasonPhrase();
            body = EntityUtils.toString(resp.getEntity(), "UTF-8");
        } catch (IOException e) {
            throw new AbortException(String.format("[CodeSonar] failed to authenticate. %n[CodeSonar] IOException: %s%n[CodeSonar] Stack Trace: %s", e.getMessage(), Throwables.getStackTraceAsString(e)));
        }

        if(status == 301) { //HTTP 301 - MOVED PERMANENTLY
            if(baseHubUri.getScheme().equalsIgnoreCase("http")) {
                throw new AbortException(String.format("[CodeSonar] failed to authenticate. Possible reason could be the CodeSonar hub running on https, while protocol http was specified.%n[CodeSonar] HTTP status code: %s - %s %n[CodeSonar] HTTP Body: %s", status, reason, body));
            }
        }

        if (status != 200) {
            throw new AbortException(String.format("[CodeSonar] failed to authenticate. %n[CodeSonar] HTTP status code: %s - %s %n[CodeSonar] HTTP Body: %s", status, reason, body));
        }
    }

    public void authenticate(URI baseHubUri, String username, String password) throws AbortException {
        List<NameValuePair> loginForm = Form.form()
                .add("sif_username", username)
                .add("sif_password", password)
                .add("sif_sign_in", "yes")
                .add("sif_log_out_competitor", "yes")
                .add("response_try_plaintext", "1")
                .build();

        int status = -1;
        String reason = "";
        String body = "";
        URI resolvedURI = baseHubUri;
        try {
            resolvedURI = baseHubUri.resolve("/sign_in.html?response_try_plaintext=1");
            HttpResponse resp = httpService.execute(Request.Post(resolvedURI)
                    .bodyForm(loginForm))
                    .returnResponse();
            
            status = resp.getStatusLine().getStatusCode();
            reason = resp.getStatusLine().getReasonPhrase();
            body = EntityUtils.toString(resp.getEntity(), "UTF-8");
        } catch (IOException e) {
            throw new AbortException(String.format("[CodeSonar] failed to authenticate. %n[CodeSonar] IOException: %s%n[CodeSonar] Stack Trace: %s", e.getMessage(), Throwables.getStackTraceAsString(e)));
        }

        if(status == 301) { //HTTP 301 - MOVED PERMANENTLY
            if(baseHubUri.getScheme().equalsIgnoreCase("http")) {
                throw new AbortException(String.format("[CodeSonar] failed to authenticate. Possible reason could be the CodeSonar hub running on https, while protocol http was specified.%n[CodeSonar] HTTP status code: %s - %s %n[CodeSonar] HTTP Body: %s", status, reason, body));
            }
        }

        if (status != 200) {
            throw new AbortException(String.format("[CodeSonar] failed to authenticate. %n[CodeSonar] HTTP status code: %s - %s %n[CodeSonar] HTTP Body: %s", status, reason, body));
        }
    }

    public void signOut(URI baseHubUri) throws AbortException {
        try {
            HttpResponse resp = httpService.execute(Request.Get(baseHubUri.resolve("/sign_out.html?response_try_plaintext=1"))).returnResponse();

            int statusCode = resp.getStatusLine().getStatusCode();
            
            if (statusCode != 200) {
                String reason = resp.getStatusLine().getReasonPhrase();;
                String body = EntityUtils.toString(resp.getEntity(), "UTF-8");
                throw new AbortException(String.format("[CodeSonar] failed to sign out. %n[CodeSonar] HTTP status code: %s - %s %n[CodeSonar] HTTP Body: %s", statusCode, reason, body));
            }
        } catch (IOException ex) {
            throw new AbortException(String.format("[CodeSonar] Failed to sign out.%n[CodeSonar] Message is: %s", ex.getMessage()));
        }
    }
}
