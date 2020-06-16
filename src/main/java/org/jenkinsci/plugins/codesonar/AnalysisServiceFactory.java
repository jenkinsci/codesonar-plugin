package org.jenkinsci.plugins.codesonar;

import hudson.AbortException;
import org.jenkinsci.plugins.codesonar.services.AnalysisService;
import org.jenkinsci.plugins.codesonar.services.HttpService;
import org.jenkinsci.plugins.codesonar.services.IAnalysisService;
import org.jenkinsci.plugins.codesonar.services.XmlSerializationService;

public class AnalysisServiceFactory {
    private float version;

    public AnalysisServiceFactory() {
        this.version = -1.0f;
    }
    
    public IAnalysisService getAnalysisService(HttpService httpService, XmlSerializationService xmlSerializationService) throws AbortException {
        if (version < 0.0f) {
            throw new AbortException("[Codesonar] version could not be determined");
        }
        
        return new AnalysisService(httpService, xmlSerializationService,"0");
    }

    public float getVersion() {
        return version;
    }

    public void setVersion(float version) {
        this.version = version;
    }
}
