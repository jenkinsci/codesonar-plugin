package org.jenkinsci.plugins.codesonar;

import org.jenkinsci.plugins.codesonar.services.AnalysisService40;
import org.jenkinsci.plugins.codesonar.services.AnalysisService42;
import org.jenkinsci.plugins.codesonar.services.HttpService;
import org.jenkinsci.plugins.codesonar.services.IAnalysisService;
import org.jenkinsci.plugins.codesonar.services.XmlSerializationService;

public class AnalysisServiceFactory {
    private final float version;
    
    public AnalysisServiceFactory(float version) {
        this.version = version;
    }
    
    public IAnalysisService getAnalysisService(HttpService httpService, XmlSerializationService xmlSerializationService) {
        if (version >= 4.2) {
            return new AnalysisService42(httpService, xmlSerializationService);
        }
        else {
            return new AnalysisService40(httpService, xmlSerializationService);
        }
    }
}
