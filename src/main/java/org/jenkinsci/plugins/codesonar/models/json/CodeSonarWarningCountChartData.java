package org.jenkinsci.plugins.codesonar.models.json;

import java.util.List;

/**
 * @author aseno
 *
 */
public class CodeSonarWarningCountChartData {
    
    private List<CodeSonarWarningCountChartRow> rows;

    public List<CodeSonarWarningCountChartRow> getRows() {
        return rows;
    }

    public void setRows(List<CodeSonarWarningCountChartRow> rows) {
        this.rows = rows;
    }

    @Override
    public String toString() {
        return "CodeSonarChartData [rows=" + rows + "]";
    }

}
