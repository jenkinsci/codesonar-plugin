package org.jenkinsci.plugins.codesonar.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.utils.URIBuilder;
import org.jenkinsci.plugins.codesonar.CodeSonarAlertCounter;
import org.jenkinsci.plugins.codesonar.CodeSonarPluginException;
import org.jenkinsci.plugins.codesonar.ResponseErrorException;
import org.jenkinsci.plugins.codesonar.models.json.CodeSonarAlertData;

import com.google.common.base.Throwables;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class AlertsService {
    private static final Logger LOGGER = Logger.getLogger(AlertsService.class.getName());
    
    final private HttpService httpService;

    public AlertsService(HttpService httpService) {
        this.httpService = httpService;
    }
    
    public CodeSonarAlertCounter getAlertCounter(URI baseHubUri, long analysisId) throws IOException {
        CodeSonarAlertCounter counter = new CodeSonarAlertCounter();
        //Loop through all possible kind values, currently from 0 to 26. Upper bound 50 is for future alert kinds.
        for(long kind = 0; kind < 50; kind++) {
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
            } catch(ResponseErrorException e) {
                if(e.getStatus() == 404) {
                    /*
                     * A 404 response is returned anytime the request specifies an alert kind that has no
                     * occurrences on the specified analysis
                     */
                    LOGGER.log(Level.INFO, "Skipping current alert kind", e);
                    continue;
                } else if(e.getStatus() == 500) {
                    /*
                     * A 500 response is returned anytime the request specifies an alert kind that is out
                     * of range with respect to all currently supported alert kinds.
                     * This makes status 500 the right one to establish when to stop looping through kinds.
                     */
                    LOGGER.log(Level.INFO, MessageFormat.format("Stop looping through alert kinds at kind id {0}", kind), e);
                    break;
                } else {
                    /*
                     * A server-side error prevented the response from being satisfied, try skipping current alert kind
                     */
                    LOGGER.log(Level.INFO, "Found an unexpected error in the response, skipping current alert kind", e);
                    continue;
                }
            } catch(CodeSonarPluginException e) {
                /*
                 * An generic error prevented the response from being received, try skipping current alert kind
                 */
                LOGGER.log(Level.INFO, "Unexpected error querying the hub, skipping current alert kind", e);
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
                counter.incrementOf(alertData.getColor(), alertData.getUndef_funcs().size());
            }
            if(alertData.getIssues() != null && alertData.getIssues().size() > 0) {
                LOGGER.log(Level.INFO, "Counting {0} new alerts from \"issues\" for color {1}", new Object[] {alertData.getIssues().size(), alertData.getColor()});
                counter.incrementOf(alertData.getColor(), alertData.getIssues().size());
            }
            if(alertData.getFacts() != null && alertData.getFacts().size() > 0) {
                LOGGER.log(Level.INFO, "Counting {0} new alerts from \"facts\" for color {1}", new Object[] {alertData.getFacts().size(), alertData.getColor()});
                counter.incrementOf(alertData.getColor(), alertData.getFacts().size());
            }
        }
        
        return counter;
    }

}
