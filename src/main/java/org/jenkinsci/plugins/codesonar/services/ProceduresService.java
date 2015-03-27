package org.jenkinsci.plugins.codesonar.services;

import java.io.IOException;
import org.jenkinsci.plugins.codesonar.models.procedures.Procedures;

/**
 *
 * @author Andrius
 */
public class ProceduresService {
    private HttpService httpService;
    private XmlSerializationService xmlSerializationService;

    public ProceduresService(HttpService httpService, XmlSerializationService xmlSerializationService) {
        this.httpService = httpService;
        this.xmlSerializationService = xmlSerializationService;
    }

    public String getProceduresUrlFromAnAnalysisId(String hubAddress, String analysisId) {
        return String.format("http://%s/analysis/%s-procedures.xml", hubAddress, analysisId);
    }
    
    public Procedures getProceduresFromUrl(String proceduresUrl) throws IOException {
        String xmlContent = httpService.getContentFromUrlAsString(proceduresUrl);

        Procedures procedures = xmlSerializationService.deserialize(xmlContent, Procedures.class);

        return procedures;
    }
}
