package org.jenkinsci.plugins.codesonar.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.utils.URIBuilder;
import org.javatuples.Pair;
import org.jenkinsci.plugins.codesonar.models.SearchConfigData;
import org.jenkinsci.plugins.codesonar.models.PairAdapter;
import org.jenkinsci.plugins.codesonar.models.procedures.ProcedureMetric;
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
    
    public Procedures getProceduresFromUri(URI proceduresUri) throws IOException {
        LOGGER.log(Level.INFO, String.format("Calling getProceduresFromUri"));
        
        InputStream xmlContent = httpService.getContentFromUrlAsInputStream(proceduresUri);

        return xmlSerializationService.deserialize(xmlContent, Procedures.class);
    }
    
    public ProcedureMetric getProcedureWithMaxCyclomaticComplexity(URI baseHubUri, long analysisId) throws IOException {
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
                .registerTypeAdapter(Pair.class, new PairAdapter())
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
        
        InputStream contentInputStream = httpService.getContentFromUrlAsInputStream(requestUri.toASCIIString());
        
        MaxCyclomaticComplexityJsonParser parser = new MaxCyclomaticComplexityJsonParser(contentInputStream);

        return parser.parseObject();
    }
    
//    public static void main(String[] args) {
//        //Configure search parameters in order to extract the desired data
//      AnalysisProceduresConfigData searchConfig = new AnalysisProceduresConfigData();
//      searchConfig.setLimit(1);
//      searchConfig.getColumns().add("metricCyclomaticComplexity");
//      searchConfig.getColumns().add("procedure");
//      searchConfig.addOrderByCondition("metricCyclomaticComplexity", AnalysisProceduresConfigData.SortingOrder.DESCENDING);
//      
//        //Serialize search configuration data as JSON
//        Gson gsonSerializer = new GsonBuilder()
//              .registerTypeAdapter(Pair.class, new PairAdapter())
//              .create();
//        String searchConfigAsJson = gsonSerializer.toJson(searchConfig);
//        
//        
//        System.out.println("searchConfigAsJson=" + searchConfigAsJson);
//  }
}
