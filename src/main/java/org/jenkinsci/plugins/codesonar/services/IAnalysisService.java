package org.jenkinsci.plugins.codesonar.services;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;

public interface IAnalysisService {
    String getAnalysisUrlFromLogFile(List<String> logFile);

    String getLatestAnalysisUrlForAProject(URI baseHubUri, String projectName) throws IOException;

    Analysis getAnalysisFromUrl(String analysisUrl) throws IOException;

    Analysis getAnalysisFromUrlWithNewWarnings(String analysisUrl) throws IOException;

    Analysis getAnalysisFromUrlWarningsByFilter(String analysisUrl) throws IOException;

    void setVisibilityFilter(String visibilityFilter);

    String getVisibilityFilter();
}
