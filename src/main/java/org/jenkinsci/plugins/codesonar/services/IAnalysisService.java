package org.jenkinsci.plugins.codesonar.services;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;

public interface IAnalysisService {
    public String getAnalysisUrlFromLogFile(List<String> logFile);
    
    public String getLatestAnalysisUrlForAProject(URI baseHubUri, String projectName) throws IOException;
    
    public Analysis getAnalysisFromUrl(String analysisUrl) throws IOException;
    
    public Analysis getAnalysisFromUrlWithNewWarnings(String analysisUrl) throws IOException;
    
    public Analysis getAnalysisFromUrlWithActiveWarnings(String analysisUrl) throws IOException;
}
