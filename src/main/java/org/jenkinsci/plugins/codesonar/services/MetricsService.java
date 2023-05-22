package org.jenkinsci.plugins.codesonar.services;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.plugins.codesonar.CodeSonarPluginException;
import org.jenkinsci.plugins.codesonar.models.HttpServiceResponse;
import org.jenkinsci.plugins.codesonar.models.metrics.Metrics;

import com.google.common.base.Throwables;

/**
 *
 * @author Andrius
 */
public class MetricsService {
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
            try {
                throw new CodeSonarPluginException("Unexpected error in the response communicating with CodeSonar Hub. %nURI: {0}%nHTTP status code: {1} - {2} %nHTTP Body: {3}", metricsUri, response.getStatusCode(), response.getReasonPhrase(), response.readContent());
            } catch (IOException e) {
                throw new CodeSonarPluginException("Unable to read response content. %nURI: {0}%nException: {1}%nStack Trace: {2}", metricsUri, e.getMessage(), Throwables.getStackTraceAsString(e));
            }
        }
        
        return  xmlSerializationService.deserialize(response.getContentInputStream(), Metrics.class);
    }
}
