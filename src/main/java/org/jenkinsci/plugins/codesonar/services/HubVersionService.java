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
import org.apache.http.client.fluent.Request;
import org.apache.http.util.EntityUtils;
import org.jenkinsci.plugins.codesonar.CodeSonarPublisher;
import org.jenkinsci.plugins.codesonar.models.HubVersion;
import org.jenkinsci.plugins.codesonar.models.VersionCompatibilityInfo;

import com.google.common.base.Throwables;
import com.google.gson.Gson;

import hudson.AbortException;

/**
 *
 * @author Andrius
 */
public class HubVersionService {
    private static final Logger LOGGER = Logger.getLogger(HubVersionService.class.getName());
    
    private HttpService httpService;
    private VersionCompatibilityInfo vci = null;

    public HubVersionService(HttpService httpService) {
        this.httpService = httpService;
    }

    public HubVersion getHubVersion(URI baseHubUri) throws AbortException {
        LOGGER.log(Level.WARNING, String.format("Retrieving CodeSonar hub version"));
        
        HubVersion hubVersion = new HubVersion();
        
        VersionCompatibilityInfo vci;
        try {
            vci = fetchVersionCompatibilityInfo(baseHubUri, CodeSonarPublisher.CODESONAR_PLUGIN_NAME, CodeSonarPublisher.CODESONAR_PLUGIN_PROTOCOL_VERSION);

            hubVersion.setVersion(vci.getHubVersion());
            
            //If this client is supposed to be able to talk to the hub
            if(checkClientOk(vci)) {
                hubVersion.setSupportsOpenAPI(supportsOpenAPI(vci));
            } else {
                //In this case this client has been rejected by the hub
                throw new AbortException(String.format("[CodeSonar] client rejected by the hub. %n[CodeSonar] clientOK=%s", vci.getClientOK().toString()));
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, String.format("[CodeSonar] failed to fetch version compatibility info from the hub. %n[CodeSonar] IOException: %s%n[CodeSonar] Stack Trace: %s", e.getMessage(), Throwables.getStackTraceAsString(e)));
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
    private VersionCompatibilityInfo fetchVersionCompatibilityInfo(URI baseHubUri, String clientName, String clientVersion) throws IOException {
        
        URI resolvedURI = baseHubUri;
        
        resolvedURI = baseHubUri.resolve(String.format("/command/check_version/%s/?version=%s&capability=openapi", clientName, clientVersion));
        LOGGER.log(Level.WARNING, "Calling " + resolvedURI.toString());
        
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
    
    private String fetchHubVersionLegacy(URI baseHubUri) throws AbortException {
        String info;
        try {
            URI endpoint = baseHubUri.resolve("/command/anon_info/");
            LOGGER.log(Level.WARNING, "Calling " + endpoint.toString());
            info = httpService.getContentFromUrlAsString(endpoint);
        } catch (AbortException e) {
            // /command/anon_info/ is not available which means the hub is > 4.2
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
