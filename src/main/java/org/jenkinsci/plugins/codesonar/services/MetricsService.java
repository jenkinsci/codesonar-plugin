package org.jenkinsci.plugins.codesonar.services;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.plugins.codesonar.CodeSonarHubCommunicationException;
import org.jenkinsci.plugins.codesonar.CodeSonarPluginException;
import org.jenkinsci.plugins.codesonar.models.metrics.Metrics;

/**
 *
 * @author Andrius
 */
public class MetricsService extends AbstractService {
    private static final Logger LOGGER = Logger.getLogger(MetricsService.class.getName());
    
    private HttpService httpService;
    private XmlSerializationService xmlSerializationService;

    public MetricsService(HttpService httpService, XmlSerializationService xmlSerializationService) {
        this.httpService = httpService;
        this.xmlSerializationService = xmlSerializationService;
    }

    public URI getMetricsUriFromAnAnalysisId(URI baseHubUri, String analysisId) {
        return baseHubUri.resolve(String.format("/metrics/%s.xml", analysisId));
    }
    
    public Metrics getMetricsFromUri(URI metricsUri) throws CodeSonarPluginException {
        LOGGER.log(Level.INFO, String.format("Calling getMetricsFromUri"));
        HttpServiceResponse response = httpService.getResponseFromUrl(metricsUri);
        
        if(response.getStatusCode() != 200) {
            throw new CodeSonarHubCommunicationException(metricsUri, response.getStatusCode(), response.getReasonPhrase(), readResponseContent(response, metricsUri));
        }
        
        return  xmlSerializationService.deserialize(response.getContentInputStream(), Metrics.class);
    }
}
