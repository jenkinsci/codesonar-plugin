package org.jenkinsci.plugins.codesonar.services;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.utils.URIBuilder;
import org.javatuples.Pair;
import org.jenkinsci.plugins.codesonar.CodeSonarPluginException;
import org.jenkinsci.plugins.codesonar.models.HttpServiceResponse;
import org.jenkinsci.plugins.codesonar.models.JsonStringPairSerializer;
import org.jenkinsci.plugins.codesonar.models.ProcedureMetric;
import org.jenkinsci.plugins.codesonar.models.json.SearchConfigData;
import org.jenkinsci.plugins.codesonar.models.procedures.Procedures;
import org.jenkinsci.plugins.codesonar.parsers.MaxCyclomaticComplexityJsonParser;

import com.google.common.base.Throwables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 *
 * @author Andrius
 */
public class ProceduresService {
    private static final Logger LOGGER = Logger.getLogger(ProceduresService.class.getName());
    
    final private HttpService httpService;
    final private XmlSerializationService xmlSerializationService;
    private boolean strictQueryParameters;

    public ProceduresService(HttpService httpService, XmlSerializationService xmlSerializationService, boolean strictQueryParameters) {
        this.httpService = httpService;
        this.xmlSerializationService = xmlSerializationService;
        this.strictQueryParameters = strictQueryParameters;
    }

    public URI getProceduresUriFromAnAnalysisId(URI baseHubUri, String analysisId) {
        return baseHubUri.resolve(String.format("/analysis/%s-procedures.xml", analysisId));
    }
    
    public Procedures getProceduresFromUri(URI proceduresUri) throws CodeSonarPluginException {
        LOGGER.log(Level.INFO, String.format("Calling getProceduresFromUri"));
        
        HttpServiceResponse response = httpService.getResponseFromUrl(proceduresUri);
        
        if(response.getStatusCode() != 200) {
            try {
                throw new CodeSonarPluginException("Unexpected error in the response communicating with CodeSonar Hub. %nURI: {0}%nHTTP status code: {1} - {2} %nHTTP Body: {3}", proceduresUri, response.getStatusCode(), response.getReasonPhrase(), response.readContent());
            } catch (IOException e) {
                throw new CodeSonarPluginException("Unable to read response content. %nURI: {0}%nException: {1}%nStack Trace: {2}", proceduresUri, e.getMessage(), Throwables.getStackTraceAsString(e));
            }
        }

        return xmlSerializationService.deserialize(response.getContentInputStream(), Procedures.class);
    }
    
    public ProcedureMetric getProcedureWithMaxCyclomaticComplexity(URI baseHubUri, long analysisId) throws CodeSonarPluginException {
        //Configure search parameters in order to extract the desired data
        SearchConfigData searchConfig = new SearchConfigData();
        /*
         * Read a maximum of 100 procedures with potentially the same cyclomatic complexity value,
         * sort them client-side by procedure too, and pick-up the very top element in the list.
         */
        searchConfig.setLimit(100);
        searchConfig.getColumns().add("metricCyclomaticComplexity");
        searchConfig.getColumns().add("procedure");
        searchConfig.addOrderByCondition("metricCyclomaticComplexity", SearchConfigData.SortingOrder.DESCENDING);
        
        //Serialize search configuration data as JSON
        Gson gsonSerializer = new GsonBuilder()
                .registerTypeAdapter(Pair.class, new JsonStringPairSerializer())
                .create();
        String searchConfigAsJson = gsonSerializer.toJson(searchConfig);

        //Build char_table request URL
        URIBuilder uriBuilder = new URIBuilder(baseHubUri);
        uriBuilder.setPath(String.format("/analysis/%d-procedures.json", analysisId));
        uriBuilder.addParameter("aproc_json", searchConfigAsJson);
        uriBuilder.addParameter("proc_filter", Utils.formatParameter("all", strictQueryParameters));
        
        URI requestUri;
        try {
            requestUri = uriBuilder.build();
        } catch (URISyntaxException e) {
            LOGGER.log(Level.WARNING, "Request URI for retrieving max cyclomatic complexity contains a syntax error. %nException: {0}%nStack Trace: {1}", new Object[] {e.getMessage(), Throwables.getStackTraceAsString(e)});
            return null;
        }
        String requestUriString = requestUri.toASCIIString();
        
        HttpServiceResponse response = httpService.getResponseFromUrl(requestUriString);
        
        if(response.getStatusCode() != 200) {
            try {
                throw new CodeSonarPluginException("Unexpected error in the response communicating with CodeSonar Hub. %nURI: {0}%nHTTP status code: {1} - {2} %nHTTP Body: {3}", requestUriString, response.getStatusCode(), response.getReasonPhrase(), response.readContent());
            } catch (IOException e) {
                throw new CodeSonarPluginException("Unable to read response content. %nURI: {0}%nException: {1}%nStack Trace: {2}", requestUriString, e.getMessage(), Throwables.getStackTraceAsString(e));
            }
        }
        
        MaxCyclomaticComplexityJsonParser parser = new MaxCyclomaticComplexityJsonParser(response.getContentInputStream());

        try {
            return parser.parseObject();
        } catch (IOException e) {
            throw new CodeSonarPluginException("Unable to parse response content. %nURI: {0}%nException: {1}%nStack Trace: {2}", requestUriString, e.getMessage(), Throwables.getStackTraceAsString(e));
        }
    }
    
}
