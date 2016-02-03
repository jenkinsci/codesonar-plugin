package org.jenkinsci.plugins.codesonar.services;

import hudson.AbortException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
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
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;

public class AuthenticationService implements Serializable {
    private HttpService httpService;

    public AuthenticationService(HttpService httpService) {
        this.httpService = httpService;
    }

    public void authenticate(URI uri, KeyStore keyStore, String password) throws AbortException {
        SSLContext sslContext;
        try {
            sslContext = SSLContexts.custom().loadKeyMaterial(keyStore, password.toCharArray()).build();
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException ex) {
            throw new AbortException(String.format("[CodeSonar] failed to authenticate.%n[CodeSonar] Message is: %s", ex.getMessage()));
        }

        HttpClient httpClient = HttpClients.custom().setSSLContext(sslContext).build();

        httpService.setExecutor(Executor.newInstance(httpClient).use(httpService.getHttpCookieStore()));

        List<NameValuePair> loginForm = Form.form()
                .add("sif_use_tls", "yes")
                .add("sif_sign_in", "yes")
                .add("sif_log_out_competitor", "yes")
                .build();

        try {
            URIBuilder uriBuilder = new URIBuilder(uri);
            uriBuilder.setPath("/sign_in.html");
            uri = uriBuilder.build();

            HttpResponse resp = httpService.execute(Request.Post(uri)
                    .bodyForm(loginForm))
                    .returnResponse();

            int statusCode = resp.getStatusLine().getStatusCode();

            if (statusCode != 200) {
                throw new AbortException("[CodeSonar] failed to authenticate.");
            }
        } catch (URISyntaxException | IOException e) {
            throw new AbortException(String.format("[CodeSonar] Error on url: %s%n[CodeSonar] Message is: %s", e.getMessage()));
        }
    }

    public void authenticate(URI uri, String username, String password) throws AbortException {
        List<NameValuePair> loginForm = Form.form()
                .add("sif_username", username)
                .add("sif_password", password)
                .add("sif_sign_in", "yes")
                .add("sif_log_out_competitor", "yes")
                .build();

        try {
            URIBuilder uriBuilder = new URIBuilder(uri);
            uriBuilder.setPath("/sign_in.html");
            uri = uriBuilder.build();

            HttpResponse resp = httpService.execute(Request.Post(uri)
                    .bodyForm(loginForm))
                    .returnResponse();

            int statusCode = resp.getStatusLine().getStatusCode();

            if (statusCode != 200) {
                throw new AbortException("[CodeSonar] failed to authenticate.");
            }
        } catch (URISyntaxException | IOException e) {
            throw new AbortException(String.format("[CodeSonar] Error on url: %s%n[CodeSonar] Message is: %s", e.getMessage()));
        }
    }

    public void signOut(URI uri) throws AbortException {
        try {
            URIBuilder uriBuilder = new URIBuilder(uri);
            uriBuilder.setPath("/sign_out.html");
            uri = uriBuilder.build();

            HttpResponse resp = httpService.execute(Request.Get(uri)).returnResponse();

            int statusCode = resp.getStatusLine().getStatusCode();
            
            if (statusCode != 200) {
                throw new AbortException("[CodeSonar] failed to sign out.");
            }
        } catch (IOException | URISyntaxException ex) {
            throw new AbortException(String.format("[CodeSonar] Failed to sign out.%n[CodeSonar] Message is: %s", ex.getMessage()));
        }
    }
}
