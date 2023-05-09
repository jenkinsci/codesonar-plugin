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
import org.javatuples.Pair;
import org.jenkinsci.plugins.codesonar.CodeSonarPluginException;
import org.jenkinsci.plugins.codesonar.models.CodeSonarWarningCount;
import org.jenkinsci.plugins.codesonar.models.PairAdapter;
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
    
    public CodeSonarWarningCount getNumberOfWarningsWithScoreAboveThreshold(URI baseHubUri, long analysisId, int threshold) throws IOException {
        CodeSonarWarningCount result = new CodeSonarWarningCount();
        
        //Configure search parameters in order to extract the desired data
        SearchConfigData searchConfig = new SearchConfigData();
        //Avoid returning rows, only rows counter is needed.
        searchConfig.setCount(true);
        searchConfig.setLimit(0);
        
        //Serialize search configuration data as JSON
        Gson gsonSerializer = new GsonBuilder()
                .registerTypeAdapter(Pair.class, new PairAdapter())
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
            LOGGER.log(Level.WARNING, "Request URI for Warning Seach API contains a syntax error. %nException: {0}%nStack Trace: {1}", new Object[] {e.getMessage(), Throwables.getStackTraceAsString(e)});
            return null;
        }
        
        InputStream jsonContent = null;
        try {
            jsonContent = httpService.getContentFromUrlAsInputStream(requestUri.toASCIIString());
        } catch(CodeSonarPluginException e) {
            LOGGER.log(Level.INFO, "Error querying Warning Search API", e);
            return null;
        }

        Gson gsonDeserializer = new Gson();
        CodeSonarWarningSearchData warningSearchData = null;
        try {
            warningSearchData = gsonDeserializer.fromJson(new InputStreamReader(jsonContent, StandardCharsets.UTF_8), CodeSonarWarningSearchData.class);
            LOGGER.log(Level.INFO, "response warningSearchData={0}", warningSearchData.toString());
        } catch(JsonSyntaxException e) {
            LOGGER.log(Level.WARNING, "failed to parse JSON response. %nException: {0}%nStack Trace: {1}", new Object[] {e.getMessage(), Throwables.getStackTraceAsString(e)});
            return null;
        }
        
        result.setScoreAboveThresholdCounter(warningSearchData.getCount());
        return result;
    }

}
