package org.jenkinsci.plugins.codesonar.models;

import java.net.URI;
import java.util.List;
import org.javatuples.Pair;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.jenkinsci.plugins.codesonar.models.metrics.Metrics;
import org.jenkinsci.plugins.codesonar.models.procedures.Procedures;

/**
 *
 * @author Andrius
 */
public class CodeSonarBuildActionDTO {

    private final Analysis analysisActiveWarnings;
    private final Analysis analysisNewWarnings;
    private final Metrics metrics;
    private final Procedures procedures;
    private final URI baseHubUri;
    private List<Pair<String, String>> conditionNamesAndResults;

    public CodeSonarBuildActionDTO(Analysis analysisActiveWarnings,
            Analysis analysisNewWarnings, Metrics metrics, Procedures procedures,
            URI baseHubUri) {
        this.analysisActiveWarnings = analysisActiveWarnings;
        this.analysisNewWarnings = analysisNewWarnings;
        this.metrics = metrics;
        this.procedures = procedures;
        this.baseHubUri = baseHubUri;
        
    }    
    
    public Analysis getAnalysisActiveWarnings() {
        return analysisActiveWarnings;
    }

    public Analysis getAnalysisNewWarnings() {
        return analysisNewWarnings;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public Procedures getProcedures() {
        return procedures;
    }

    public URI getBaseHubUri() {
        return baseHubUri;
    }

    public List<Pair<String, String>> getConditionNamesAndResults() {
        return conditionNamesAndResults;
    }
    
    public void setConditionNamesAndResults(List<Pair<String, String>> conditionNamesAndResults) {
        this.conditionNamesAndResults = conditionNamesAndResults;
    }
}
