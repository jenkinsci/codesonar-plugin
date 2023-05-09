package org.jenkinsci.plugins.codesonar.models.json;

import java.util.List;

/**
 * @author aseno
 *
 */
public class CodeSonarChartData {
    
    private List<CodeSonarAnalysisWarningCount> rows;

    public List<CodeSonarAnalysisWarningCount> getRows() {
        return rows;
    }

    public void setRows(List<CodeSonarAnalysisWarningCount> rows) {
        this.rows = rows;
    }

    @Override
    public String toString() {
        return "CodeSonarChartData [rows=" + rows + "]";
    }

}
