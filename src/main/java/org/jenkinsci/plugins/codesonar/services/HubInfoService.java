package org.jenkinsci.plugins.codesonar.services;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.fluent.Request;
import org.jenkinsci.plugins.codesonar.CodeSonarHubCommunicationException;
import org.jenkinsci.plugins.codesonar.CodeSonarJsonSyntaxException;
import org.jenkinsci.plugins.codesonar.CodeSonarLogger;
import org.jenkinsci.plugins.codesonar.CodeSonarPluginException;
import org.jenkinsci.plugins.codesonar.models.CodeSonarHubClientCompatibilityInfo;
import org.jenkinsci.plugins.codesonar.models.CodeSonarHubInfo;

import com.google.common.base.Throwables;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class HubInfoService extends AbstractService {
    private static final Logger LOGGER = Logger.getLogger(HubInfoService.class.getName());
    
    public static final String CODESONAR_HUB_CLIENT_NAME = "jenkins";
    public static final int CODESONAR_HUB_CLIENT_PROTOCOL_VERSION_NUMBER = 2;
    
    private HttpService httpService;

    public HubInfoService(HttpService httpService) {
        this.httpService = httpService;
    }
    
    private CodeSonarPluginException createError(String msg, Object...args) {
        return new CodeSonarPluginException(msg, args);
    }

    public CodeSonarHubInfo fetchHubInfo(URI baseHubUri) throws CodeSonarPluginException {
        LOGGER.log(Level.INFO, String.format("Retrieving CodeSonar hub info"));
        
        CodeSonarHubInfo hubInfo = new CodeSonarHubInfo();
        
        CodeSonarHubClientCompatibilityInfo cci = fetchVersionCompatibilityInfo(baseHubUri, CODESONAR_HUB_CLIENT_NAME, CODESONAR_HUB_CLIENT_PROTOCOL_VERSION_NUMBER);
            
        if(cci != null) {
            hubInfo.setVersion(cci.getHubVersion());
            
            //If this client is supposed to be able to talk to the hub
            if(checkClientOk(cci)) {
                hubInfo.setOpenAPISupported(supportsOpenAPI(cci));
                hubInfo.setStrictQueryParametersEnforced(supportsStrictQueryParameters(cci));
                hubInfo.setJsonGridConfigSupported(supportsJsonGridConfig(cci));
            } else {
                //In this case this client has been rejected by the hub
                throw createError("client rejected by the hub. %nclientOK={0}", cci.getClientOK().toString());
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
     * @throws CodeSonarPluginException when hub returns an unexpected response
     * @throws IOException thrown when the communication with the hub fails
     */
    private CodeSonarHubClientCompatibilityInfo fetchVersionCompatibilityInfo(URI baseHubUri, String clientName, int clientVersion) throws CodeSonarPluginException {
        
        URI resolvedURI = baseHubUri;
        
        resolvedURI = baseHubUri.resolve(String.format("/command/check_version/%s/?version=%d&capability=openapi&capability=strictQueryParameters&capability=gridConfigJson", clientName, clientVersion));
        LOGGER.log(Level.INFO, "Calling " + resolvedURI.toString());
        
        HttpServiceResponse response;
        try {
            response = httpService.execute(Request.Get(resolvedURI));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "failed to get a response. %nIOException: {0}%nStack Trace: {1}", new Object[] {e.getMessage(), Throwables.getStackTraceAsString(e)});
            return null;
        }
        
        if(response.getStatusCode() == 404) {
            //Hub might respond with an HTTP 404, we want to keep track this special case
            LOGGER.log(Level.INFO, "specified endpoint seems not to exist on the hub. %nresponse is \"{0,number,integer}, {1}\"", new Object[]{response.getStatusCode() , response.getReasonPhrase()});
            return null;
        } else if(response.getStatusCode() != 200) {
            throw new CodeSonarHubCommunicationException(resolvedURI, response.getStatusCode(), response.getReasonPhrase(), readResponseContent(response, resolvedURI));
        }
        
        String responseBody = readResponseContent(response, resolvedURI);
        if(responseBody == null) {
            return null;
        }
        
        //We cannot parse the JSON response if "responseBody" is null or if it's empty
        if(StringUtils.isEmpty(responseBody)) {
            LOGGER.log(Level.INFO, "response is empty. %nresponse is \"{0}\"", responseBody);
            return null;
        }
        
        Gson gson = new Gson();
        CodeSonarHubClientCompatibilityInfo cci = null;
        try {
            cci = gson.fromJson(responseBody, CodeSonarHubClientCompatibilityInfo.class);
            LOGGER.log(Level.INFO, CodeSonarLogger.formatMessage(cci.toString()));
        } catch(JsonSyntaxException e) {
            throw new CodeSonarJsonSyntaxException(e);
        }
        
        return cci;
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
    
    private boolean supportsJsonGridConfig(CodeSonarHubClientCompatibilityInfo cci) {
        return cci.getCapabilities() != null
                && cci.getCapabilities().getGridConfigJson() != null
                && cci.getCapabilities().getGridConfigJson().booleanValue();
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
     * @throws CodeSonarPluginException 
     */
    private String fetchHubSignatureVersionString(URI baseHubUri) throws CodeSonarPluginException {
        URI endpoint = baseHubUri.resolve("/command/anon_info/");
        LOGGER.log(Level.INFO, "Calling " + endpoint.toString());
        HttpServiceResponse response;
        String bodyContent = null;
        try {
            response = httpService.getResponseFromUrl(endpoint);
            bodyContent = readResponseContent(response, endpoint);
            if(response.getStatusCode() != 200) {
                throw new CodeSonarHubCommunicationException(endpoint, response.getStatusCode(), response.getReasonPhrase(), bodyContent);
            }
        } catch (CodeSonarPluginException e) {
            // /command/anon_info/ is not available. Assume hub is older than v4.2
            return "4.0";
        }
    
        Pattern pattern = Pattern.compile("Version:\\s(\\d+\\.\\d+)");
        Matcher matcher = pattern.matcher(bodyContent);
        if (matcher.find()) {
            String version = matcher.group(1);
            if(StringUtils.isBlank(version)) {
                throw createError(CodeSonarLogger.formatMessage("Hub version cannot be parsed"));
            }
            return version;
        }

        LOGGER.log(Level.WARNING, "Version info could not be determined by data:\n"+response); // No version could be found

        throw createError(CodeSonarLogger.formatMessage("Hub version could not be determined"));
    }
    
}
