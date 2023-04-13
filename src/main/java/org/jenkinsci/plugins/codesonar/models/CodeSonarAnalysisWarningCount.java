package org.jenkinsci.plugins.codesonar.models;

/**
 * @author aseno
 *
 */
public class CodeSonarAnalysisWarningCount {
    
    private String analysisName;
    private long numberOfWarnings;
    
    public CodeSonarAnalysisWarningCount() {
        numberOfWarnings = -1;
    }

    public String getAnalysisName() {
        return analysisName;
    }

    public void setAnalysisName(String analysisName) {
        this.analysisName = analysisName;
    }

    public long getNumberOfWarnings() {
        return numberOfWarnings;
    }

    public void setNumberOfWarnings(long numberOfWarnings) {
        this.numberOfWarnings = numberOfWarnings;
    }

    @Override
    public String toString() {
        return "AnalysisWarningCount [analysisName=" + analysisName + ", numberOfWarnings=" + numberOfWarnings + "]";
    }

}
