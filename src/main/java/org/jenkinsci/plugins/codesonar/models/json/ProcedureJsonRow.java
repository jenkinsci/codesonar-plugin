package org.jenkinsci.plugins.codesonar.models.json;

import java.util.Objects;

public class ProcedureJsonRow implements Comparable<ProcedureJsonRow> {
    private String procedure;
    private int metricCyclomaticComplexity;

    public ProcedureJsonRow() {
        this(null, 0);
    }

    public ProcedureJsonRow(String procedure, int metricCyclomaticComplexity) {
        this.procedure = procedure;
        this.metricCyclomaticComplexity = metricCyclomaticComplexity;
    }

    public String getProcedure() {
        return procedure;
    }
    
    public void setProcedure(String procedure) {
        this.procedure = procedure;
    }

    public int getMetricCyclomaticComplexity() {
        return metricCyclomaticComplexity;
    }
    
    public void setMetricCyclomaticComplexity(int metricCyclomaticComplexity) {
        this.metricCyclomaticComplexity = metricCyclomaticComplexity;
    }
    
    @Override
    public String toString() {
        return String.format(
                    "ProcedureMetric [procedure='%s', metricCyclomaticComplexity='%s']",
                    procedure,
                    metricCyclomaticComplexity);
    }

    /*
     * Provide an ordering compatible with assumptions made by ProceduresService.getProcedureWithMaxCyclomaticComplexity().
     * It first sorts them in decreasing order by cyclomatic complexity, then in ascending order by procedure.
     */
    @Override
    public int compareTo(ProcedureJsonRow metric) {
        int order = Integer.compare(metric.metricCyclomaticComplexity, metricCyclomaticComplexity);
        
        // If comparing cyclomatic complexity results in equality, then evaluate the second sorting criteria
        if(order == 0) {
            order = procedure.compareTo(metric.procedure);
        }
        
        return order;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(metricCyclomaticComplexity, procedure);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ProcedureJsonRow other = (ProcedureJsonRow) obj;
        return metricCyclomaticComplexity == other.metricCyclomaticComplexity
                && Objects.equals(procedure, other.procedure);
    }

}
