package org.jenkinsci.plugins.codesonar.services;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.jenkinsci.plugins.codesonar.CodeSonarPluginException;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.jenkinsci.plugins.codesonar.models.json.CodeSonarWarningCountChartRow;

public interface IAnalysisService {
    public static final String VISIBILITY_FILTER_ALL_WARNINGS_DEFAULT = "active";
    public static final String VISIBILITY_FILTER_NEW_WARNINGS_DEFAULT = "new";
    
    String getAnalysisUrlFromLogFile(List<String> logFile);

    String getLatestAnalysisUrlForAProject(URI baseHubUri, String projectName) throws CodeSonarPluginException;

    Analysis getAnalysisFromUrl(String analysisUrl) throws CodeSonarPluginException;

    Analysis getAnalysisFromUrlWithNewWarnings(URI baseHubUri, long analysisId) throws CodeSonarPluginException;

    Analysis getAnalysisFromUrlWarningsByFilter(URI baseHubUri, long analysisId) throws CodeSonarPluginException;
    
    CodeSonarWarningCountChartRow getNumberOfWarnings(URI baseHubUri, long analysisId, String filter) throws CodeSonarPluginException;

    void setVisibilityFilter(String visibilityFilter);

    String getVisibilityFilter();
    
    void setNewWarningsFilter(String visibilityFilter);
    
    String getNewWarningsFilter();
    
}
