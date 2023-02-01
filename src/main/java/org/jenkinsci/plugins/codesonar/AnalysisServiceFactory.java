package org.jenkinsci.plugins.codesonar;

import org.jenkinsci.plugins.codesonar.services.AnalysisService;
import org.jenkinsci.plugins.codesonar.services.HttpService;
import org.jenkinsci.plugins.codesonar.services.IAnalysisService;
import org.jenkinsci.plugins.codesonar.services.XmlSerializationService;

import hudson.AbortException;

public class AnalysisServiceFactory {
    private String version;

    public AnalysisServiceFactory() {
        this.version = null;
    }
    
    public IAnalysisService getAnalysisService(HttpService httpService, XmlSerializationService xmlSerializationService) throws AbortException {
        if (version == null) {
            throw new AbortException("[Codesonar] version could not be determined");
        }
        
        return new AnalysisService(httpService, xmlSerializationService,"0");
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
