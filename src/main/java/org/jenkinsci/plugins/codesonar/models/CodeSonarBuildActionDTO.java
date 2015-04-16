package org.jenkinsci.plugins.codesonar.models;

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
    private final String hubAddress;
    private final List<Pair<String, String>> conditionNamesAndResults;

    public CodeSonarBuildActionDTO(Analysis analysisActiveWarnings,
            Analysis analysisNewWarnings, Metrics metrics, Procedures procedures,
            String hubAddress, List<Pair<String, String>> conditionNamesAndResults) {
        this.analysisActiveWarnings = analysisActiveWarnings;
        this.analysisNewWarnings = analysisNewWarnings;
        this.metrics = metrics;
        this.procedures = procedures;
        this.hubAddress = hubAddress;
        this.conditionNamesAndResults = conditionNamesAndResults;
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

    public String getHubAddress() {
        return hubAddress;
    }

    public List<Pair<String, String>> getConditionNamesAndResults() {
        return conditionNamesAndResults;
    }
}
