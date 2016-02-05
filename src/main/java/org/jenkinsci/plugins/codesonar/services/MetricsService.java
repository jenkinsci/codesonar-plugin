package org.jenkinsci.plugins.codesonar.services;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import org.jenkinsci.plugins.codesonar.models.metrics.Metrics;

/**
 *
 * @author Andrius
 */
public class MetricsService implements Serializable {
    private HttpService httpService;
    private XmlSerializationService xmlSerializationService;

    public MetricsService(HttpService httpService, XmlSerializationService xmlSerializationService) {
        this.httpService = httpService;
        this.xmlSerializationService = xmlSerializationService;
    }

    public URI getMetricsUriFromAnAnalysisId(URI baseHubUri, String analysisId) {
        return baseHubUri.resolve(String.format("/metrics/%s.xml", analysisId));
    }
    
    public Metrics getMetricsFromUri(URI metricsUri) throws IOException {
        String xmlContent = httpService.getContentFromUrlAsString(metricsUri);

        Metrics metrics = xmlSerializationService.deserialize(xmlContent, Metrics.class);

        return metrics;
    }

}
