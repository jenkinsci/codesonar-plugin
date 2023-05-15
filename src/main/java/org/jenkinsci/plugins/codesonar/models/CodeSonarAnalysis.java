package org.jenkinsci.plugins.codesonar.models;

import java.net.URI;

public class CodeSonarAnalysis {
    private URI baseHubUri;
    private Long analysisId;
    
    public CodeSonarAnalysis(URI baseHubUri, Long analysisId) {
        this.baseHubUri = baseHubUri;
        this.analysisId = analysisId;
    }
    
    public URI getBaseHubUri() {
        return baseHubUri;
    }
    public void setBaseHubUri(URI baseHubUri) {
        this.baseHubUri = baseHubUri;
    }

    public Long getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(Long analysisId) {
        this.analysisId = analysisId;
    }

    @Override
    public String toString() {
        return "CodeSonarAnalysis [baseHubUri=" + baseHubUri + ", analysisId=" + analysisId + "]";
    }
    
}
