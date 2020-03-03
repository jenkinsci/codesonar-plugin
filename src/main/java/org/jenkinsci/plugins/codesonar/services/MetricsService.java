package org.jenkinsci.plugins.codesonar.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import org.jenkinsci.plugins.codesonar.models.metrics.Metrics;

/**
 *
 * @author Andrius
 */
public class MetricsService {
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
        InputStream xmlContent = httpService.getContentFromUrlAsInputStream(metricsUri);

        return  xmlSerializationService.deserialize(xmlContent, Metrics.class);
    }

}
