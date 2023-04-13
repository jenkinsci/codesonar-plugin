package org.jenkinsci.plugins.codesonar.models;

import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.jenkinsci.plugins.codesonar.models.metrics.Metrics;
import org.jenkinsci.plugins.codesonar.models.procedures.Procedures;

/**
 * @author aseno
 *
 */
public class CodeSonarAnalysisData {
    
    private CodeSonarAnalysisWarningCount activeWarningsCount;
    private CodeSonarAnalysisWarningCount newWarningsCount;
    private Analysis analysisActiveWarnings;
    private Analysis analysisNewWarnings;
    private Metrics metrics;
    private Procedures procedures;
    
    public CodeSonarAnalysisData() {
        
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

}
