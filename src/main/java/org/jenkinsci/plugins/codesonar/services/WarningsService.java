package org.jenkinsci.plugins.codesonar.services;

import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.utils.URIBuilder;
import org.javatuples.Pair;
import org.jenkinsci.plugins.codesonar.CodeSonarHubCommunicationException;
import org.jenkinsci.plugins.codesonar.CodeSonarJsonSyntaxException;
import org.jenkinsci.plugins.codesonar.CodeSonarPluginException;
import org.jenkinsci.plugins.codesonar.CodeSonarRequestURISyntaxException;
import org.jenkinsci.plugins.codesonar.models.json.CodeSonarWarningSearchData;
import org.jenkinsci.plugins.codesonar.models.json.SearchConfigData;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

public class WarningsService extends AbstractService {
    private static final Logger LOGGER = Logger.getLogger(WarningsService.class.getName());
    
    final private HttpService httpService;
    private boolean strictQueryParameters;

    public WarningsService(HttpService httpService, boolean strictQueryParameters) {
        this.httpService = httpService;
        this.strictQueryParameters = strictQueryParameters;
    }
    
    public long getNumberOfWarningsWithScoreAboveThreshold(
            URI baseHubUri,
            long analysisId,
            int threshold,
            String visibilityFilter)
        throws CodeSonarPluginException {

        //Configure search parameters in order to extract the desired data
        SearchConfigData searchConfig = new SearchConfigData();
        //Avoid returning rows, only rows counter is needed.
        searchConfig.setCount(true);
        searchConfig.setLimit(0);
        
        //Serialize search configuration data as JSON
        Gson gsonSerializer = new GsonBuilder()
                .registerTypeAdapter(Pair.class, new JsonStringPairSerializer())
                .create();
        String searchConfigAsJson = gsonSerializer.toJson(searchConfig);
        
        //Build char_table request URL
        URIBuilder uriBuilder = new URIBuilder(baseHubUri);
        uriBuilder.setPath("search.json");
        uriBuilder.addParameter("scope", String.format("aid:%d", analysisId));
        if (visibilityFilter != null && visibilityFilter.length() > 0) {
            uriBuilder.addParameter("filter", Utils.formatParameter(visibilityFilter, strictQueryParameters));
        }
        uriBuilder.addParameter("swarnings_json", searchConfigAsJson);
        uriBuilder.addParameter("query", String.format("score>%d", threshold));

        URI requestUri = null;
        try {
            requestUri = uriBuilder.build();
        } catch (URISyntaxException e) {
            throw new CodeSonarRequestURISyntaxException(e);
        }
        String requestUriString = requestUri.toASCIIString();

        CodeSonarWarningSearchData warningSearchData = null;
        HttpServiceResponse response = httpService.getResponseFromUrl(requestUriString);
        try {
            if(response.getStatusCode() != 200) {
                throw new CodeSonarHubCommunicationException(
                        requestUriString,
                        response.getStatusCode(),
                        response.getReasonPhrase(),
                        readResponseContent(response, requestUriString));
            }

            Gson gsonDeserializer = new Gson();
            try {
                warningSearchData = gsonDeserializer.fromJson(
                        new InputStreamReader(response.getContentInputStream(), StandardCharsets.UTF_8),
                        CodeSonarWarningSearchData.class);
                LOGGER.log(Level.INFO, "response warningSearchData={0}", warningSearchData.toString());
            } catch(JsonSyntaxException e) {
                throw new CodeSonarJsonSyntaxException(e);
            }
        } finally {
            try {
                response.close();
            } catch (IOException ex) {
                // Ignore this exception if we are already throwing.
                if (warningSearchData == null) {
                    throw new CodeSonarPluginException("Failed to close HTTP response", ex);
                }
            }
        }

        return warningSearchData.getCount();
    }

}
