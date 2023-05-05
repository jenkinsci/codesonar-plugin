package org.jenkinsci.plugins.codesonar.models;

import java.net.URI;

import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.jenkinsci.plugins.codesonar.models.metrics.Metrics;
import org.jenkinsci.plugins.codesonar.models.procedures.ProcedureMetric;
import org.jenkinsci.plugins.codesonar.models.procedures.Procedures;

/**
 * @author aseno
 *
 */
public class CodeSonarAnalysisData {
   
    private URI baseHubUri;
    private long analysisId;
    private CodeSonarAnalysisWarningCount activeWarningsCount;
    private CodeSonarAnalysisWarningCount newWarningsCount;
    private Analysis analysisActiveWarnings;
    private Analysis analysisNewWarnings;
    private Metrics metrics;
    private Procedures procedures;
    private ProcedureMetric procedureWithMaxCyclomaticComplexity;
    private CodeSonarAlertFrequencies alertFrequencies;
    private CodeSonarWarningCount warningCountAbsoluteWithScoreAboveThreshold;
    private CodeSonarWarningCount warningCountIncreaseWithScoreAboveThreshold;
    
    public CodeSonarAnalysisData(URI baseHubUri, long analysisId) {
        this.baseHubUri = baseHubUri;
        this.analysisId = analysisId;
    }

    public CodeSonarAnalysisWarningCount getActiveWarningsCount() {
        return activeWarningsCount;
    }

    public void setActiveWarningsCount(CodeSonarAnalysisWarningCount activeWarningsCount) {
        this.activeWarningsCount = activeWarningsCount;
    }

    public CodeSonarAnalysisWarningCount getNewWarningsCount() {
        return newWarningsCount;
    }

    public void setNewWarningsCount(CodeSonarAnalysisWarningCount newWarningsCount) {
        this.newWarningsCount = newWarningsCount;
    }
    
    public Analysis getAnalysisActiveWarnings() {
        return analysisActiveWarnings;
    }

    public void setAnalysisActiveWarnings(Analysis analysisActiveWarnings) {
        this.analysisActiveWarnings = analysisActiveWarnings;
    }

    public Analysis getAnalysisNewWarnings() {
        return analysisNewWarnings;
    }

    public void setAnalysisNewWarnings(Analysis analysisNewWarnings) {
        this.analysisNewWarnings = analysisNewWarnings;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    public Procedures getProcedures() {
        return procedures;
    }

    public void setProcedures(Procedures procedures) {
        this.procedures = procedures;
    }
    
    public ProcedureMetric getProcedureWithMaxCyclomaticComplexity() {
        return procedureWithMaxCyclomaticComplexity;
    }

    public void setProcedureWithMaxCyclomaticComplexity(ProcedureMetric procedureWithMaxCyclomaticComplexity) {
        this.procedureWithMaxCyclomaticComplexity = procedureWithMaxCyclomaticComplexity;
    }

    public CodeSonarAlertFrequencies getAlertFrequencies() {
        return alertFrequencies;
    }

    public void setAlertFrequencies(CodeSonarAlertFrequencies alertFrequencies) {
        this.alertFrequencies = alertFrequencies;
    }

    public CodeSonarWarningCount getWarningCountAbsoluteWithScoreAboveThreshold() {
        return warningCountAbsoluteWithScoreAboveThreshold;
    }

    public void setWarningCountAbsoluteWithScoreAboveThreshold(
            CodeSonarWarningCount warningCountAbsoluteWithScoreAboveThreshold) {
        this.warningCountAbsoluteWithScoreAboveThreshold = warningCountAbsoluteWithScoreAboveThreshold;
    }
    
    public CodeSonarWarningCount getWarningCountIncreaseWithScoreAboveThreshold() {
        return warningCountIncreaseWithScoreAboveThreshold;
    }

    public void setWarningCountIncreaseWithScoreAboveThreshold(
            CodeSonarWarningCount warningCountIncreaseWithScoreAboveThreshold) {
        this.warningCountIncreaseWithScoreAboveThreshold = warningCountIncreaseWithScoreAboveThreshold;
    }

    public URI getBaseHubUri() {
        return baseHubUri;
    }

    public void setBaseHubUri(URI baseHubUri) {
        this.baseHubUri = baseHubUri;
    }

    public long getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(long analysisId) {
        this.analysisId = analysisId;
    }

}
