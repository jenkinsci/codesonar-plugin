package org.jenkinsci.plugins.codesonar.services;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.http.util.EntityUtils;
import org.jenkinsci.plugins.codesonar.CodeSonarPublisher;
import org.jenkinsci.plugins.codesonar.models.VersionCompatibilityInfo;

import com.google.common.base.Throwables;
import com.google.gson.Gson;

import hudson.AbortException;

public class AuthenticationService {
	private static final Logger LOGGER = Logger.getLogger(AuthenticationService.class.getName());
    private static final String HTTP_HEADER_AUTHORIZATION = "Authorization";
	private HttpService httpService;

    public AuthenticationService(HttpService httpService) {
        this.httpService = httpService;
    }
    
    /**
     * 
     * @param baseHubUri
     * @param clientName the client name, in this case "jenkins"
     * @param clientVersion the client version
     * @throws AbortException thrown when the comunication with the hub fails
     */
    private VersionCompatibilityInfo fetchVersionCompatibilityInfo(URI baseHubUri, String clientName, String clientVersion) throws AbortException {
    	
    	URI resolvedURI = baseHubUri;
    	
    	resolvedURI = baseHubUri.resolve(String.format("/command/check_version/%s/?version=%s&capability=openapi", clientName, clientVersion));
    	LOGGER.log(Level.WARNING, "Calling " + resolvedURI.toString());
    	
    	try {
			HttpResponse resp = httpService.execute(Request.Get(resolvedURI))
			        .returnResponse();
			
			Gson gson = new Gson();

			HttpEntity entity = resp.getEntity();
			if(entity == null) {
				throw new AbortException("[CodeSonar] hub compatibility info cannot be read. %n[CodeSonar] entity is null");
			}
			
			String responseBody = EntityUtils.toString(entity, Consts.UTF_8);
			
			//Hub might respond with just the text "Not Found", in this case we must fail
			if(StringUtils.isNotEmpty(responseBody) && responseBody.equals("Not Found"))  {
				throw new AbortException(String.format("[CodeSonar] hub compatibility info have not been returned. %n[CodeSonar] response is \"%s\"", responseBody));
			}
			
			VersionCompatibilityInfo vci = gson.fromJson(responseBody, VersionCompatibilityInfo.class);
			
			//Returned object might be null if either "responseBody" is null or if it's empty, in such a case we must fail
			if(vci == null) {
				throw new AbortException(String.format("[CodeSonar] hub compatibility info cannot be parsed. %n[CodeSonar] response is \"%s\"", responseBody));
			}
			
			LOGGER.log(Level.WARNING, vci.toString());
			
			return vci;
		} catch (IOException e) {
			throw new AbortException(String.format("[CodeSonar] failed to fetch version compatibility info from the hub. %n[CodeSonar] IOException: %s%n[CodeSonar] Stack Trace: %s", e.getMessage(), Throwables.getStackTraceAsString(e)));
		}
    }
    
    public void authenticate(URI baseHubUri) throws AbortException {
    	LOGGER.log(Level.WARNING, "Starting new certificate authentication request");
    	VersionCompatibilityInfo vci = fetchVersionCompatibilityInfo(baseHubUri, CodeSonarPublisher.CODESONAR_PLUGIN_NAME, CodeSonarPublisher.CODESONAR_PLUGIN_PROTOCOL_VERSION);
    	
		//If this client is supposed to be able to talk to the hub
		if(checkClientOk(vci)) {
			if(supportsOpenAPI(vci)) {
				//If the hub supports OpenAPI, then leverage that new form of authentication
				authenticate702(baseHubUri);
			} else {
				//If the hub does not support OpenAPI, then fallback to the legacy authentication method
				authenticate701(baseHubUri);
			}
		} else {
			//In this case this client has been rejected by the hub
			throw new AbortException(String.format("[CodeSonar] client rejected by the hub. %n[CodeSonar] clientOK=%s", vci.getClientOK().toString()));
		}
    }

	private boolean supportsOpenAPI(VersionCompatibilityInfo vci) {
		return vci.getCapabilities() != null
				&& vci.getCapabilities().getOpenapi() != null
				&& vci.getCapabilities().getOpenapi().booleanValue();
	}

	private boolean checkClientOk(VersionCompatibilityInfo vci) {
		//If "clientOk" is either "true" or "null", this means that this client is supposed to be able to talk to the hub
		return vci.getClientOK() == null || vci.getClientOK().booleanValue();
	}

    /**
     * OpenAPI certificate authentication
     * @param baseHubUri
     * @param username
     * @throws AbortException
     */
    private void authenticate702(URI baseHubUri) throws AbortException {
    	LOGGER.log(Level.WARNING, "OpenAPI certificate authentication request");
		
        List<NameValuePair> loginForm = Form.form()
                .add("key", "cookie")
                .build();

        int status = -1;
        String reason = "";
        String body = "";
        URI resolvedURI = baseHubUri;

        try {
            resolvedURI = baseHubUri.resolve("/session/create-tls-client-certificate/");

            HttpResponse resp = httpService.execute(Request.Post(resolvedURI)
            		.addHeader("X-CSHUB-USE-TLS-CLIENT-AUTH", "1")
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

	private void authenticate701(URI baseHubUri) throws AbortException {
		LOGGER.log(Level.WARNING, "Legacy certificate authentication request");

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
    	LOGGER.log(Level.WARNING, "Starting new password authentication request");
    	VersionCompatibilityInfo vci = fetchVersionCompatibilityInfo(baseHubUri, CodeSonarPublisher.CODESONAR_PLUGIN_NAME, CodeSonarPublisher.CODESONAR_PLUGIN_PROTOCOL_VERSION);
    	
		//If this client is supposed to be able to talk to the hub
		if(checkClientOk(vci)) {
			if(supportsOpenAPI(vci)) {
				//If the hub supports OpenAPI, then leverage that new form of authentication
				authenticate702(baseHubUri, username, password);
			} else {
				//If the hub does not support OpenAPI, then fallback to the legacy authentication method
				authenticate701(baseHubUri, username, password);
			}
		} else {
			//In this case this client has been rejected by the hub
			throw new AbortException(String.format("[CodeSonar] client rejected by the hub. %n[CodeSonar] clientOK=%s", vci.getClientOK().toString()));
		}
    }

    /**
     * OpenAPI password authentication endpoint
     * @param baseHubUri
     * @param username
     * @param password
     * @throws AbortException
     */
    private void authenticate702(URI baseHubUri, String username, String password) throws AbortException {
    	LOGGER.log(Level.WARNING, "OpenAPI password authentication request");
        List<NameValuePair> loginForm = Form.form()
                .add("key", "cookie")
                .build();
        int status = -1;
        String reason = "";
        String body = "";
        URI resolvedURI = baseHubUri;
        try {
            resolvedURI = baseHubUri.resolve("/session/create-basic-auth/");
            HttpResponse resp = httpService.execute(Request.Post(resolvedURI)
            		.addHeader(HTTP_HEADER_AUTHORIZATION, formatBasicAuthHeader(username, password))
            		.bodyForm(loginForm))
                    .returnResponse();
            
            status = resp.getStatusLine().getStatusCode();
            reason = resp.getStatusLine().getReasonPhrase();
            Header[] allHeaders = resp.getAllHeaders();
            LOGGER.log(Level.WARNING, "Response headers:");
            for (int i = 0; i < allHeaders.length; i++) {
            	LOGGER.log(Level.WARNING, String.format("%s:%s", allHeaders[i].getName(), allHeaders[i].getValue()));
			}
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

	private void authenticate701(URI baseHubUri, String username, String password) throws AbortException {
		LOGGER.log(Level.WARNING, "Legacy password authentication request");
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
            Header[] allHeaders = resp.getAllHeaders();
            LOGGER.log(Level.WARNING, "Response headers:");
            for (int i = 0; i < allHeaders.length; i++) {
            	LOGGER.log(Level.WARNING, String.format("%s:%s", allHeaders[i].getName(), allHeaders[i].getValue()));
			}
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

    /**
     * Encode username/password into HTTP Basic Authentication, Authorization header value.
     */
    private static String formatBasicAuthHeader(String user, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(Charset.forName("UTF-8")));
        
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
