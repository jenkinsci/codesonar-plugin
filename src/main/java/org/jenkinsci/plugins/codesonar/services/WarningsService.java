package org.jenkinsci.plugins.codesonar.services;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.utils.URIBuilder;
import org.javatuples.Pair;
import org.jenkinsci.plugins.codesonar.CodeSonarPluginException;
import org.jenkinsci.plugins.codesonar.models.HttpServiceResponse;
import org.jenkinsci.plugins.codesonar.models.JsonStringPairSerializer;
import org.jenkinsci.plugins.codesonar.models.json.CodeSonarWarningSearchData;
import org.jenkinsci.plugins.codesonar.models.json.SearchConfigData;

import com.google.common.base.Throwables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

public class WarningsService {
    private static final Logger LOGGER = Logger.getLogger(WarningsService.class.getName());
    
    final private HttpService httpService;
    private boolean strictQueryParameters;

    public WarningsService(HttpService httpService, boolean strictQueryParameters) {
        this.httpService = httpService;
        this.strictQueryParameters = strictQueryParameters;
    }
    
    public Long getNumberOfWarningsWithScoreAboveThreshold(URI baseHubUri, long analysisId, int threshold) throws IOException {
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
        uriBuilder.addParameter("filter", Utils.formatParameter("all", strictQueryParameters));
        uriBuilder.addParameter("swarnings_json", searchConfigAsJson);
        uriBuilder.addParameter("query", String.format("score>%d", threshold));
        
        URI requestUri = null;
        try {
            requestUri = uriBuilder.build();
        } catch (URISyntaxException e) {
            throw new CodeSonarPluginException("Request URI for Warning Seach API contains a syntax error. %nException: {0}%nStack Trace: {1}", e.getMessage(), Throwables.getStackTraceAsString(e));
        }
        String requestUriString = requestUri.toASCIIString();
        
        HttpServiceResponse response = httpService.getResponseFromUrl(requestUriString);
        
        if(response.getStatusCode() != 200) {
            throw new CodeSonarPluginException("Unexpected error in the response communicating with CodeSonar Hub. %nURI: {0}%nHTTP status code: {1} - {2} %nHTTP Body: {3}", requestUriString, response.getStatusCode(), response.getReasonPhrase(), response.readContent());
        }

        Gson gsonDeserializer = new Gson();
        CodeSonarWarningSearchData warningSearchData = null;
        try {
            warningSearchData = gsonDeserializer.fromJson(new InputStreamReader(response.getContentInputStream(), StandardCharsets.UTF_8), CodeSonarWarningSearchData.class);
            LOGGER.log(Level.INFO, "response warningSearchData={0}", warningSearchData.toString());
        } catch(JsonSyntaxException e) {
            LOGGER.log(Level.WARNING, "failed to parse JSON response. %nException: {0}%nStack Trace: {1}", new Object[] {e.getMessage(), Throwables.getStackTraceAsString(e)});
            return null;
        }
        
        return warningSearchData.getCount();
    }

}
