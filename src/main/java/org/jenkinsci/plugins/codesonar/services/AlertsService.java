package org.jenkinsci.plugins.codesonar.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.utils.URIBuilder;
import org.jenkinsci.plugins.codesonar.CodeSonarPluginException;
import org.jenkinsci.plugins.codesonar.models.CodeSonarAlertData;
import org.jenkinsci.plugins.codesonar.models.CodeSonarAlertFrequencies;

import com.google.common.base.Throwables;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class AlertsService {
    private static final Logger LOGGER = Logger.getLogger(AlertsService.class.getName());
    
    final private HttpService httpService;

    public AlertsService(HttpService httpService) {
        this.httpService = httpService;
    }
    
    public CodeSonarAlertFrequencies getAlertFrequencies(URI baseHubUri, long analysisId) throws IOException {
        CodeSonarAlertFrequencies frequencies = new CodeSonarAlertFrequencies();
        //Loop through all possible kind values, currently from 0 to 26.
        for(long kind = 0; kind < 26; kind++) {
            //Build char_table request URL
            URIBuilder uriBuilder = new URIBuilder(baseHubUri);
            uriBuilder.setPath(String.format("/analysis/%d-alert%d.json", analysisId, kind));
            
            URI requestUri = null;
            try {
                requestUri = uriBuilder.build();
            } catch (URISyntaxException e) {
                LOGGER.log(Level.WARNING, "Request URI for Alerts API contains a syntax error. %nException: {0}%nStack Trace: {1}", new Object[] {e.getMessage(), Throwables.getStackTraceAsString(e)});
                return null;
            }
            
            LOGGER.log(Level.INFO, "Querying alerts with kind={0}", kind);
            InputStream jsonContent = null;
            try {
                jsonContent = httpService.getContentFromUrlAsInputStream(requestUri.toASCIIString());
            } catch(CodeSonarPluginException e) {
                LOGGER.log(Level.INFO, "Error querying alerts, skipping current kind", e);
                continue;
            }
    
            Gson gsonDeserializer = new Gson();
            CodeSonarAlertData alertData = null;
            try {
                alertData = gsonDeserializer.fromJson(new InputStreamReader(jsonContent, StandardCharsets.UTF_8), CodeSonarAlertData.class);
                LOGGER.log(Level.INFO, "response alertData={0}", alertData.toString());
            } catch(JsonSyntaxException e) {
                LOGGER.log(Level.WARNING, "failed to parse JSON response. %nException: {0}%nStack Trace: {1}", new Object[] {e.getMessage(), Throwables.getStackTraceAsString(e)});
                return null;
            }
            
            if(alertData.getUndef_funcs() != null && alertData.getUndef_funcs().size() > 0) {
                LOGGER.log(Level.INFO, "Counting {0} new alerts from \"undef_funcs\" for color {1}", new Object[] {alertData.getUndef_funcs().size(), alertData.getColor()});
                frequencies.incrementOf(alertData.getColor(), alertData.getUndef_funcs().size());
            }
            if(alertData.getIssues() != null && alertData.getIssues().size() > 0) {
                LOGGER.log(Level.INFO, "Counting {0} new alerts from \"issues\" for color {1}", new Object[] {alertData.getIssues().size(), alertData.getColor()});
                frequencies.incrementOf(alertData.getColor(), alertData.getIssues().size());
            }
            if(alertData.getFacts() != null && alertData.getFacts().size() > 0) {
                LOGGER.log(Level.INFO, "Counting {0} new alerts from \"facts\" for color {1}", new Object[] {alertData.getFacts().size(), alertData.getColor()});
                frequencies.incrementOf(alertData.getColor(), alertData.getFacts().size());
            }
        }
        
        return frequencies;
    }

}
