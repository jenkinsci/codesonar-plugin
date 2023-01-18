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
import org.jenkinsci.plugins.codesonar.CodeSonarPublisher;
import org.jenkinsci.plugins.codesonar.models.CodeSonarHubInfo;
import org.jenkinsci.plugins.codesonar.models.VersionCompatibilityInfo;

import com.google.common.base.Throwables;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import hudson.AbortException;

public class HubVersionService {
    private static final Logger LOGGER = Logger.getLogger(HubVersionService.class.getName());
    
    private HttpService httpService;

    public HubVersionService(HttpService httpService) {
        this.httpService = httpService;
    }

    public CodeSonarHubInfo getHubVersion(URI baseHubUri) throws AbortException {
        LOGGER.log(Level.WARNING, String.format("Retrieving CodeSonar hub version"));
        
        CodeSonarHubInfo hubVersion = new CodeSonarHubInfo();
        
        VersionCompatibilityInfo vci = fetchVersionCompatibilityInfo(baseHubUri, CodeSonarPublisher.CODESONAR_HUB_CLIENT_NAME, CodeSonarPublisher.CODESONAR_HUB_CLIENT_PROTOCOL_VERSION_NUMBER);
            
        if(vci != null) {
            hubVersion.setVersion(vci.getHubVersion());
            
            //If this client is supposed to be able to talk to the hub
            if(checkClientOk(vci)) {
                hubVersion.setOpenAPISupported(supportsOpenAPI(vci));
            } else {
                //In this case this client has been rejected by the hub
                throw new AbortException(String.format("[CodeSonar] client rejected by the hub. %n[CodeSonar] clientOK=%s", vci.getClientOK().toString()));
            }
        } else {
            /*
             * In such a case the hub version is most likely to be <= 7.0, as a result
             * let's try loading the hub version through the legacy endpoint (/command/anon_info/)
             */
            LOGGER.log(Level.WARNING, String.format("Falling back to legacy version endpoint (/command/anon_info/)"));
            String hubVersionLegacy = fetchHubVersionLegacy(baseHubUri);
            hubVersion.setVersion(hubVersionLegacy);
        }

        return hubVersion;
    }
    
    /**
     * 
     * @param baseHubUri
     * @param clientName the client name, in this case "jenkins"
     * @param clientVersion the client version
     * @throws IOException thrown when the communication with the hub fails
     */
    private VersionCompatibilityInfo fetchVersionCompatibilityInfo(URI baseHubUri, String clientName, String clientVersion) {
        
        URI resolvedURI = baseHubUri;
        
        resolvedURI = baseHubUri.resolve(String.format("/command/check_version/%s/?version=%s&capability=openapi", clientName, clientVersion));
        LOGGER.log(Level.WARNING, "Calling " + resolvedURI.toString());
        
        HttpResponse resp;
        try {
            resp = httpService.execute(Request.Get(resolvedURI))
                    .returnResponse();
        } catch (IOException e) {
            LOGGER.warning(String.format("[CodeSonar] failed to get a response. %n[CodeSonar] IOException: %s%n[CodeSonar] Stack Trace: %s", e.getMessage(), Throwables.getStackTraceAsString(e)));
            return null;
        }
        
        Gson gson = new Gson();

        HttpEntity entity = resp.getEntity();
        if(entity == null) {
            LOGGER.warning("[CodeSonar] hub compatibility info cannot be read. %n[CodeSonar] entity is null");
            return null;
        }
        
        //Hub might respond with an HTTP 404, in this case we must fail
        if(resp.getStatusLine() != null && resp.getStatusLine().getStatusCode() == 404) {
            LOGGER.warning(String.format("[CodeSonar] hub compatibility info have not been returned. %n[CodeSonar] response is \"%s\"", resp.getStatusLine().getReasonPhrase()));
            return null;
        }
        
        String responseBody;
        try {
            responseBody = EntityUtils.toString(entity, Consts.UTF_8);
        } catch (ParseException | IOException e) {
            LOGGER.warning(String.format("[CodeSonar] failed to read the response. %n[CodeSonar] Exception: %s%n[CodeSonar] Stack Trace: %s", e.getMessage(), Throwables.getStackTraceAsString(e)));
            return null;
        }
        
        //We cannot parse the JSON response if "responseBody" is null or if it's empty
        if(StringUtils.isEmpty(responseBody)) {
            LOGGER.warning(String.format("[CodeSonar] response is empty. %n[CodeSonar] response is \"%s\"", responseBody));
            return null;
        }
        
        VersionCompatibilityInfo vci = null;
        try {
            vci = gson.fromJson(responseBody, VersionCompatibilityInfo.class);
            LOGGER.log(Level.WARNING, String.format("[CodeSonar] %s", vci.toString()));
        } catch(JsonSyntaxException e) {
            LOGGER.warning(String.format("[CodeSonar] failed to parse JSON response. %n[CodeSonar] Exception: %s%n[CodeSonar] Stack Trace: %s", e.getMessage(), Throwables.getStackTraceAsString(e)));
            return null;
        }
        
        return vci;
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
     * The most of this method comes from the previous implementation written by Eficode, which
     * I kept as a fallback.
     * @param baseHubUri
     * @return
     * @throws AbortException
     */
    private String fetchHubVersionLegacy(URI baseHubUri) throws AbortException {
        String info;
        try {
            URI endpoint = baseHubUri.resolve("/command/anon_info/");
            LOGGER.log(Level.WARNING, "Calling " + endpoint.toString());
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
                throw new AbortException("[CodeSonar] Hub version cannot be parsed");
            }
            return version;
        }

        LOGGER.log(Level.WARNING, "[CodeSonar] Version info could not be determined by data:\n"+info); // No version could be found

        throw new AbortException("[CodeSonar] Hub version could not be determined");
    }
}
