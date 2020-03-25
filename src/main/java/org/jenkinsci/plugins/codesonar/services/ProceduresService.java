package org.jenkinsci.plugins.codesonar.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import org.jenkinsci.plugins.codesonar.models.procedures.Procedures;

/**
 *
 * @author Andrius
 */
public class ProceduresService {
    final private HttpService httpService;
    final private XmlSerializationService xmlSerializationService;

    public ProceduresService(HttpService httpService, XmlSerializationService xmlSerializationService) {
        this.httpService = httpService;
        this.xmlSerializationService = xmlSerializationService;
    }

    public URI getProceduresUriFromAnAnalysisId(URI baseHubUri, String analysisId) {
        return baseHubUri.resolve(String.format("/analysis/%s-procedures.xml", analysisId));
    }
    
    public Procedures getProceduresFromUri(URI proceduresUri) throws IOException {
        InputStream xmlContent = httpService.getContentFromUrlAsInputStream(proceduresUri);

        return xmlSerializationService.deserialize(xmlContent, Procedures.class);
    }
}
