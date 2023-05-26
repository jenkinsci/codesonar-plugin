package org.jenkinsci.plugins.codesonar.services;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.utils.URIBuilder;
import org.javatuples.Pair;
import org.jenkinsci.plugins.codesonar.CodeSonarHubCommunicationException;
import org.jenkinsci.plugins.codesonar.CodeSonarPluginException;
import org.jenkinsci.plugins.codesonar.CodeSonarRequestURISyntaxException;
import org.jenkinsci.plugins.codesonar.models.json.ProcedureJsonRow;
import org.jenkinsci.plugins.codesonar.models.json.SearchConfigData;
import org.jenkinsci.plugins.codesonar.models.procedures.Procedures;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 *
 * @author Andrius
 */
public class ProceduresService extends AbstractService {
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
            throw new CodeSonarHubCommunicationException(proceduresUri, response.getStatusCode(), response.getReasonPhrase(), readResponseContent(response, proceduresUri));
        }

        return xmlSerializationService.deserialize(response.getContentInputStream(), Procedures.class);
    }
    
    public ProcedureJsonRow getProcedureWithMaxCyclomaticComplexity(URI baseHubUri, long analysisId) throws CodeSonarPluginException {
        //Configure search parameters in order to extract the desired data
        SearchConfigData searchConfig = new SearchConfigData();

        /*
         * We want to find a unique procedure with maximal cyclomatic complexity.
         * We do this by searching for procedures and ordering by cyclomatic complexity in descending order.
         * Since there may be many procedures all sharing the maximal cyclomatic complexity value,
         * we want to further sort by procedure name in order to produce a deterministic result between different analyses.
         * Currently in CodeSonar 7.3, the hub API allows us to sort search results on a single field only.
         * This means that we must do some of the sorting ourself.
         * We don't know how many rows have the maximal cyclomatic complexity,
         * so in principle, we must fetch all rows with the maximum and sort them by procedure name.
         * Since the number of rows is theoretically too large to fit in memory, doing such a sort is expensive to implement here.
         * As a work-around, we fetch 100 rows only, under the assumption that it is unlikely that more than 100 procedures all have the same, maximal cyclomatic complexity.
         * If this assumption is wrong, then the results will not be deterministic between analyses,
         * but the critical data is the numeric value of the maximal cyclomatic complexity, not the procedure name,
         * so we are willing to live with this deficiency.
         * Sorting procedures by name will happen inside the MaxCyclomaticComplexityJsonReader.
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
            throw new CodeSonarRequestURISyntaxException(e);
        }
        String requestUriString = requestUri.toASCIIString();
        
        HttpServiceResponse response = httpService.getResponseFromUrl(requestUriString);
        
        if(response.getStatusCode() != 200) {
            throw new CodeSonarHubCommunicationException(requestUriString, response.getStatusCode(), response.getReasonPhrase(), readResponseContent(response, requestUriString));
        }
        
        MaxCyclomaticComplexityJsonReader reader = new MaxCyclomaticComplexityJsonReader(response.getContentInputStream());

        try {
            return reader.readProcedureMetric();
        } catch (IOException e) {
            throw new CodeSonarPluginException("Unable to parse response content. %nURI: {0}", e, requestUriString);
        }
    }
    
}
