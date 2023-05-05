package org.jenkinsci.plugins.codesonar.models;

/**
 * @author aseno
 *
 */
public class CodeSonarWarningCount {
    private int scoreAboveThresholdCounter;
    
    public CodeSonarWarningCount() {
        scoreAboveThresholdCounter = 0;
    }

    public int getScoreAboveThresholdCounter() {
        return scoreAboveThresholdCounter;
    }

    public void setScoreAboveThresholdCounter(int scoreAboveThresholdCounter) {
        this.scoreAboveThresholdCounter = scoreAboveThresholdCounter;
    }

    @Override
    public String toString() {
        return "CodeSonarWarningCount [scoreAboveThresholdCounter=" + scoreAboveThresholdCounter + "]";
    }
    
}
