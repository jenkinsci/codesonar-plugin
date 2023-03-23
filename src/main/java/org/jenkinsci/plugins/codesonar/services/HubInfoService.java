package org.jenkinsci.plugins.codesonar.services;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.fluent.Request;
import org.apache.http.util.EntityUtils;
import org.jenkinsci.plugins.codesonar.CodeSonarLogger;
import org.jenkinsci.plugins.codesonar.models.CodeSonarHubClientCompatibilityInfo;
import org.jenkinsci.plugins.codesonar.models.CodeSonarHubInfo;

import com.google.common.base.Throwables;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import hudson.AbortException;

public class HubInfoService {
    private static final Logger LOGGER = Logger.getLogger(HubInfoService.class.getName());
    
    public static final String CODESONAR_HUB_CLIENT_NAME = "jenkins";
    public static final int CODESONAR_HUB_CLIENT_PROTOCOL_VERSION_NUMBER = 2;
    
    private HttpService httpService;

    public HubInfoService(HttpService httpService) {
        this.httpService = httpService;
    }

    public CodeSonarHubInfo fetchHubInfo(URI baseHubUri) throws AbortException {
        LOGGER.log(Level.INFO, String.format("Retrieving CodeSonar hub info"));
        
        CodeSonarHubInfo hubInfo = new CodeSonarHubInfo();
        
        CodeSonarHubClientCompatibilityInfo cci = fetchVersionCompatibilityInfo(baseHubUri, CODESONAR_HUB_CLIENT_NAME, CODESONAR_HUB_CLIENT_PROTOCOL_VERSION_NUMBER);
            
        if(cci != null) {
            hubInfo.setVersion(cci.getHubVersion());
            
            //If this client is supposed to be able to talk to the hub
            if(checkClientOk(cci)) {
                hubInfo.setOpenAPISupported(supportsOpenAPI(cci));
                hubInfo.setStrictQueryParametersEnforced(supportsStrictQueryParameters(cci));
            } else {
                //In this case this client has been rejected by the hub
            	String message = CodeSonarLogger.formatMessageMultiLine(
            			CodeSonarLogger.createLine("Client rejected by the hub."),
            			CodeSonarLogger.createLine("clientOK=%s", cci.getClientOK().toString())
            			);
                throw new AbortException(message);
            }
        } else {
            /*
             * We cannot ask the hub for capabilities and compatibility, but we can try
             * to extract the hub's version from different endpoint.
             */
            LOGGER.log(Level.INFO, String.format("Hub did not provide client compatibility information. Attempting to query hub version signature."));
            String hubSignatureVersion = fetchHubSignatureVersionString(baseHubUri);
            hubInfo.setVersion(hubSignatureVersion);
        }

        return hubInfo;
    }
    
    /**
     * 
     * @param baseHubUri
     * @param clientName the client name, in this case "jenkins"
     * @param clientVersion the client version
     * @throws AbortException when hub returns an unexpected response
     * @throws IOException thrown when the communication with the hub fails
     */
    private CodeSonarHubClientCompatibilityInfo fetchVersionCompatibilityInfo(URI baseHubUri, String clientName, int clientVersion) throws AbortException {
        
        URI resolvedURI = baseHubUri;
        
        resolvedURI = baseHubUri.resolve(String.format("/command/check_version/%s/?version=%d&capability=openapi&capability=strictQueryParameters", clientName, clientVersion));
        LOGGER.log(Level.INFO, "Calling " + resolvedURI.toString());
        
        HttpResponse resp;
        try {
            resp = httpService.execute(Request.Get(resolvedURI))
                    .returnResponse();
        } catch (IOException e) {
        	String message = CodeSonarLogger.formatMessageMultiLine(
        			CodeSonarLogger.createLine("Failed to get a response."),
        			CodeSonarLogger.createLine("IOException: %s", e.getMessage()),
        			CodeSonarLogger.createLine("Stack Trace: %s", Throwables.getStackTraceAsString(e))
        			);
            LOGGER.log(Level.WARNING, message);
            return null;
        }
        
        if(resp.getStatusLine() == null) {
            LOGGER.log(Level.INFO, String.format("Not able to read http status."));
            return null;
        }
        
        if(resp.getStatusLine().getStatusCode() == 404) {
            //Hub might respond with an HTTP 404, we want to keep track this special case
        	String message = CodeSonarLogger.formatMessageMultiLine(
        			CodeSonarLogger.createLine("Specified endpoint seems not to exist on the hub."),
        			CodeSonarLogger.createLine("response is \"%d, %s\"", resp.getStatusLine().getStatusCode() , resp.getStatusLine().getReasonPhrase())
        			);
            LOGGER.log(Level.INFO, message);
            return null;
        } else if(resp.getStatusLine().getStatusCode() != 200) {
            //Hub returned an unexpected response
            String responseBody = readResponseBody(resp);
        	String message = CodeSonarLogger.formatMessageMultiLine(
        			CodeSonarLogger.createLine("Response is not successfull."),
        			CodeSonarLogger.createLine("Response is \"%d, %s\"", resp.getStatusLine().getStatusCode() , resp.getStatusLine().getReasonPhrase()),
        			CodeSonarLogger.createLine("Respose body: \"%s\"", responseBody)
        			);
            throw new AbortException(message);
        }
        
        String responseBody = readResponseBody(resp);
        if(responseBody == null) {
            return null;
        }
        
        //We cannot parse the JSON response if "responseBody" is null or if it's empty
        if(StringUtils.isEmpty(responseBody)) {
        	String message = CodeSonarLogger.formatMessageMultiLine(
        			CodeSonarLogger.createLine("Response is empty."),
        			CodeSonarLogger.createLine("Response is \"%s\"", responseBody)
        			);
            LOGGER.log(Level.INFO, message);
            return null;
        }
        
        Gson gson = new Gson();
        CodeSonarHubClientCompatibilityInfo cci = null;
        try {
            cci = gson.fromJson(responseBody, CodeSonarHubClientCompatibilityInfo.class);
            LOGGER.log(Level.INFO, CodeSonarLogger.formatMessage(cci.toString()));
        } catch(JsonSyntaxException e) {
        	String message = CodeSonarLogger.formatMessageMultiLine(
        			CodeSonarLogger.createLine("Failed to parse JSON response."),
        			CodeSonarLogger.createLine("Exception: %s", e.getMessage()),
        			CodeSonarLogger.createLine("Stack Trace: %s", Throwables.getStackTraceAsString(e))
        			);
            LOGGER.log(Level.WARNING, message);
            return null;
        }
        
        return cci;
    }
    
    private String readResponseBody(HttpResponse resp) {
        HttpEntity entity = resp.getEntity();
        if(entity == null) {
        	String message = CodeSonarLogger.formatMessageMultiLine(
        			CodeSonarLogger.createLine("Hub compatibility info cannot be read."),
        			CodeSonarLogger.createLine("Entity is null")
        			);
            LOGGER.log(Level.INFO, message);
            return null;
        }
        
        try {
            return EntityUtils.toString(entity, Consts.UTF_8);
        } catch (ParseException | IOException e) {
        	String message = CodeSonarLogger.formatMessageMultiLine(
        			CodeSonarLogger.createLine("Failed to read the response."),
        			CodeSonarLogger.createLine("Exception: %s", e.getMessage()),
        			CodeSonarLogger.createLine("Stack Trace: %s", Throwables.getStackTraceAsString(e))
        			);
            LOGGER.log(Level.WARNING, message);
            return null;
        }
    }
    
    private boolean supportsOpenAPI(CodeSonarHubClientCompatibilityInfo cci) {
        return cci.getCapabilities() != null
                && cci.getCapabilities().getOpenapi() != null
                && cci.getCapabilities().getOpenapi().booleanValue();
    }
    
    private boolean supportsStrictQueryParameters(CodeSonarHubClientCompatibilityInfo cci) {
        return cci.getCapabilities() != null
                && cci.getCapabilities().getStrictQueryParameters() != null
                && cci.getCapabilities().getStrictQueryParameters().booleanValue();
    }

    private boolean checkClientOk(CodeSonarHubClientCompatibilityInfo cci) {
        //If "clientOk" is either "true" or "null", this means that this client is supposed to be able to talk to the hub
        return cci.getClientOK() == null || cci.getClientOK().booleanValue();
    }
    
    /**
     * The most of this method comes from the previous implementation written by Eficode, which
     * I kept as a fallback.
     * @param baseHubUri
     * @return
     * @throws AbortException
     */
    private String fetchHubSignatureVersionString(URI baseHubUri) throws AbortException {
        String info;
        try {
            URI endpoint = baseHubUri.resolve("/command/anon_info/");
            LOGGER.log(Level.INFO, "Calling " + endpoint.toString());
            info = httpService.getContentFromUrlAsString(endpoint);
        } catch (AbortException e) {
            // /command/anon_info/ is not available. Assume hub is older than v4.2
            return "4.0";
        }

        Pattern pattern = Pattern.compile("Version:\\s(\\d+\\.\\d+)");

        Matcher matcher = pattern.matcher(info);
        if (matcher.find()) {
            String version = matcher.group(1);
            if(StringUtils.isBlank(version)) {
                throw new AbortException(CodeSonarLogger.formatMessage("Hub version cannot be parsed"));
            }
            return version;
        }

        LOGGER.log(Level.WARNING, "Version info could not be determined by data:\n"+info); // No version could be found

        throw new AbortException(CodeSonarLogger.formatMessage("Hub version could not be determined"));
    }
}
