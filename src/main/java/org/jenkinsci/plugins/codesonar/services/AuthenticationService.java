package org.jenkinsci.plugins.codesonar.services;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.NameValuePair;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.jenkinsci.plugins.codesonar.CodeSonarHubCommunicationException;
import org.jenkinsci.plugins.codesonar.CodeSonarPluginException;
import org.jenkinsci.plugins.codesonar.models.CodeSonarHubInfo;

public class AuthenticationService extends AbstractService {
    private static final Logger LOGGER = Logger.getLogger(AuthenticationService.class.getName());
    private static final String HTTP_HEADER_AUTHORIZATION = "Authorization";
    private final HttpService httpService;

    public AuthenticationService(HttpService httpService) {
        this.httpService = httpService;
    }
    
    private CodeSonarPluginException createError(String msg, Object ...args) {
        return new CodeSonarPluginException(msg, args);
    }
    
    private CodeSonarPluginException createError(String msg, Throwable cause, Object ...args) {
        return new CodeSonarPluginException(msg, cause, args);
    }

    /** might return null */
    private CodeSonarHubInfo getHubInfo() {
        return this.httpService.getHubInfo();
    }

    public void authenticate(URI baseHubUri) throws CodeSonarPluginException {
        LOGGER.log(Level.INFO, "Starting new certificate authentication request");
        CodeSonarHubInfo hubInfo = getHubInfo();
        if(hubInfo != null && hubInfo.isOpenAPISupported()) {
            //If the hub supports OpenAPI, then leverage that new form of authentication
            authenticate702(baseHubUri);
        } else {
            //If the hub does not support OpenAPI, then fallback to the legacy authentication method
            authenticate701(baseHubUri);
        }
    }

    /**
     * OpenAPI certificate authentication
     * @param baseHubUri
     * @param username
     * @throws CodeSonarPluginException 
     */
    private void authenticate702(URI baseHubUri) throws CodeSonarPluginException {
        //The implementation of this function comes from authenticate701(URI baseHubUri)
        LOGGER.log(Level.INFO, "OpenAPI certificate authentication request");
        
        List<NameValuePair> loginForm = Form.form()
                .add("key", "cookie")
                .build();

        URI resolvedURI = baseHubUri;
        HttpServiceResponse response = null;
        try {
            resolvedURI = baseHubUri.resolve("/session/create-tls-client-certificate/");
            response = httpService.execute(Request.Post(resolvedURI)
                    .addHeader("X-CSHUB-USE-TLS-CLIENT-AUTH", "1")
                    .bodyForm(loginForm));
        } catch (IOException e) {
            throw createError("failed to authenticate.", e);
        }

        if(response.getStatusCode() == 301) { //HTTP 301 - MOVED PERMANENTLY
            if(baseHubUri.getScheme().equalsIgnoreCase("http")) {
                throw createError("failed to authenticate. Possible reason could be the CodeSonar hub running on https, while protocol http was specified.%nHTTP status code: {0} - {1} %nHTTP Body: {2}", response.getStatusCode(), response.getReasonPhrase(), readResponseContent(response, resolvedURI));
            }
        }

        if (response.getStatusCode() != 200) {
            throw new CodeSonarHubCommunicationException(resolvedURI, response.getStatusCode(), response.getReasonPhrase(), readResponseContent(response, resolvedURI));
        }
    }

    private void authenticate701(URI baseHubUri) throws CodeSonarPluginException {
        LOGGER.log(Level.INFO, "Legacy certificate authentication request");

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
        HttpServiceResponse response = null;
        try {
            resolvedURI = baseHubUri.resolve("/sign_in.html?response_try_plaintext=1");
            response = httpService.execute(Request.Post(resolvedURI)
                    .bodyForm(loginForm));
        } catch (IOException e) {
            throw createError("failed to authenticate.", e);
        }

        if(response.getStatusCode() == 301) { //HTTP 301 - MOVED PERMANENTLY
            if(baseHubUri.getScheme().equalsIgnoreCase("http")) {
                throw createError("failed to authenticate. Possible reason could be the CodeSonar hub running on https, while protocol http was specified.%nHTTP status code: {0} - {1} %nHTTP Body: {2}", response.getStatusCode(), response.getReasonPhrase(), readResponseContent(response, resolvedURI));
            }
        }

        if (response.getStatusCode() != 200) {
            throw new CodeSonarHubCommunicationException(resolvedURI, response.getStatusCode(), response.getReasonPhrase(), readResponseContent(response, resolvedURI));
        }
    }
    
    public void authenticate(URI baseHubUri, String username, String password) throws CodeSonarPluginException {
        LOGGER.log(Level.INFO, "Starting new password authentication request");
        CodeSonarHubInfo hubInfo = getHubInfo();
        if(hubInfo != null && hubInfo.isOpenAPISupported()) {
            //If the hub supports OpenAPI, then leverage that new form of authentication
            authenticate702(baseHubUri, username, password);
        } else {
            //If the hub does not support OpenAPI, then fallback to the legacy authentication method
            authenticate701(baseHubUri, username, password);
        }
    }

    /**
     * OpenAPI password authentication endpoint
     * @param baseHubUri
     * @param username
     * @param password
     * @throws CodeSonarPluginException 
     */
    private void authenticate702(URI baseHubUri, String username, String password) throws CodeSonarPluginException {
        //The implementation of this function comes from authenticate7011(URI baseHubUri, String username, String password)
        LOGGER.log(Level.INFO, "OpenAPI password authentication request");
        List<NameValuePair> loginForm = Form.form()
                .add("key", "cookie")
                .build();
        URI resolvedURI = baseHubUri;
        HttpServiceResponse response = null;
        try {
            resolvedURI = baseHubUri.resolve("/session/create-basic-auth/");
            response = httpService.execute(Request.Post(resolvedURI)
                    .addHeader(HTTP_HEADER_AUTHORIZATION, formatBasicAuthHeader(username, password))
                    .bodyForm(loginForm));
            
        } catch (IOException e) {
            throw createError("failed to authenticate.", e);
        }

        if(response.getStatusCode() == 301) { //HTTP 301 - MOVED PERMANENTLY
            if(baseHubUri.getScheme().equalsIgnoreCase("http")) {
                throw createError("failed to authenticate. Possible reason could be the CodeSonar hub running on https, while protocol http was specified.%nHTTP status code: {0} - {1} %nHTTP Body: {2}", response.getStatusCode(), response.getReasonPhrase(), readResponseContent(response, resolvedURI));
            }
        }

        if (response.getStatusCode() != 200) {
            throw new CodeSonarHubCommunicationException(resolvedURI, response.getStatusCode(), response.getReasonPhrase(), readResponseContent(response, resolvedURI));
        }
    }

    private void authenticate701(URI baseHubUri, String username, String password) throws CodeSonarPluginException {
        LOGGER.log(Level.INFO, "Legacy password authentication request");
        List<NameValuePair> loginForm = Form.form()
                .add("sif_username", username)
                .add("sif_password", password)
                .add("sif_sign_in", "yes")
                .add("sif_log_out_competitor", "yes")
                .add("response_try_plaintext", "1")
                .build();

        URI resolvedURI = baseHubUri;
        HttpServiceResponse response = null;
        try {
            resolvedURI = baseHubUri.resolve("/sign_in.html?response_try_plaintext=1");
            response = httpService.execute(Request.Post(resolvedURI)
                    .bodyForm(loginForm));
        } catch (IOException e) {
            throw createError("failed to authenticate.", e);
        }

        if(response.getStatusCode() == 301) { //HTTP 301 - MOVED PERMANENTLY
            if(baseHubUri.getScheme().equalsIgnoreCase("http")) {
                throw createError("failed to authenticate. Possible reason could be the CodeSonar hub running on https, while protocol http was specified.%nHTTP status code: {0} - {1} %nHTTP Body: {2}", response.getStatusCode(), response.getReasonPhrase(), readResponseContent(response, resolvedURI));
            }
        }

        if (response.getStatusCode() != 200) {
            throw new CodeSonarHubCommunicationException(resolvedURI, response.getStatusCode(), response.getReasonPhrase(), readResponseContent(response, resolvedURI));
        }
    }

    /**
     * Encode username/password into HTTP Basic Authentication, Authorization header value.
     */
    private static String formatBasicAuthHeader(String user, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(Charset.forName("UTF-8")));
        
    }
    
    public void signOut(URI baseHubUri) throws CodeSonarPluginException {
        try {
            URI resolvedURI = baseHubUri.resolve("/sign_out.html?response_try_plaintext=1");
            HttpServiceResponse response = httpService.execute(Request.Get(resolvedURI));

            if (response.getStatusCode() != 200) {
                throw new CodeSonarHubCommunicationException(resolvedURI, response.getStatusCode(), response.getReasonPhrase(), readResponseContent(response, resolvedURI));
            }
        } catch (IOException ex) {
            throw createError("Failed to sign out.", ex);
        }
    }
}
