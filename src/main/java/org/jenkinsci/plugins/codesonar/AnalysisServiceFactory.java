package org.jenkinsci.plugins.codesonar;

import org.jenkinsci.plugins.codesonar.models.CodeSonarHubInfo;
import org.jenkinsci.plugins.codesonar.services.AnalysisService;
import org.jenkinsci.plugins.codesonar.services.HttpService;
import org.jenkinsci.plugins.codesonar.services.IAnalysisService;
import org.jenkinsci.plugins.codesonar.services.XmlSerializationService;

import hudson.AbortException;

public class AnalysisServiceFactory {
    private CodeSonarHubInfo hubInfo;

    public AnalysisServiceFactory() {
        this.hubInfo = null;
    }
    
    public IAnalysisService getAnalysisService(HttpService httpService, XmlSerializationService xmlSerializationService) throws AbortException {
        if (hubInfo == null || hubInfo.getVersion() == null) {
            throw new AbortException(CodeSonarLogger.formatMessage("Version could not be determined"));
        }
        
        return new AnalysisService(httpService, xmlSerializationService, IAnalysisService.VISIBILITY_FILTER_ALL_WARNINGS_DEFAULT, IAnalysisService.VISIBILITY_FILTER_NEW_WARNINGS_DEFAULT, hubInfo.isStrictQueryParametersEnforced());
    }

    public void setHubInfo(CodeSonarHubInfo hubInfo) {
        this.hubInfo = hubInfo;
    }

}
