package org.jenkinsci.plugins.codesonar.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.plugins.codesonar.models.procedures.Procedures;

/**
 *
 * @author Andrius
 */
public class ProceduresService {
	private static final Logger LOGGER = Logger.getLogger(ProceduresService.class.getName());
	
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
    	LOGGER.log(Level.WARNING, String.format("Calling getProceduresFromUri"));
    	
        InputStream xmlContent = httpService.getContentFromUrlAsInputStream(proceduresUri);

        return xmlSerializationService.deserialize(xmlContent, Procedures.class);
    }
}
