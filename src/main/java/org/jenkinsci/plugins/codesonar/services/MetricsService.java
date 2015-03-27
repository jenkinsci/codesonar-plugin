package org.jenkinsci.plugins.codesonar.services;

import java.io.IOException;
import java.io.Serializable;
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

    public String getMetricsUrlFromAnAnalysisId(String hubAddress, String analysisId) {
        return String.format("http://%s/metrics/%s.xml", hubAddress, analysisId);
    }
    
    public Metrics getMetricsFromUrl(String metricsUrl) throws IOException {
        String xmlContent = httpService.getContentFromUrlAsString(metricsUrl);

        Metrics metrics = xmlSerializationService.deserialize(xmlContent, Metrics.class);

        return metrics;
    }
}
