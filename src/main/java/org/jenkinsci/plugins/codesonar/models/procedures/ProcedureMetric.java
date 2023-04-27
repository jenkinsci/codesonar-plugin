package org.jenkinsci.plugins.codesonar.models.procedures;

import java.util.Objects;

public class ProcedureMetric implements Comparable<ProcedureMetric> {
    private int metricCyclomaticComplexity;
    private String procedure;
    
    public ProcedureMetric() {
    }
    
    public int getMetricCyclomaticComplexity() {
        return metricCyclomaticComplexity;
    }
    
    public void setMetricCyclomaticComplexity(int metricCyclomaticComplexity) {
        this.metricCyclomaticComplexity = metricCyclomaticComplexity;
    }
    
    public String getProcedure() {
        return procedure;
    }
    
    public void setProcedure(String procedure) {
        this.procedure = procedure;
    }

    @Override
    public String toString() {
        return "ProcedureMetric [metricCyclomaticComplexity=" + metricCyclomaticComplexity + ", procedure=" + procedure
                + "]";
    }

    /**
     * Specify the natural ordering for ProcedureMetric objects.
     * It first sorts them in decreasing order by cyclomatic complexity, then in ascending order by procedure.
     */
    @Override
    public int compareTo(ProcedureMetric metric) {
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
        ProcedureMetric other = (ProcedureMetric) obj;
        return metricCyclomaticComplexity == other.metricCyclomaticComplexity
                && Objects.equals(procedure, other.procedure);
    }

//  public static void main(String[] args) {
//      List<ProcedureMetric> list = new ArrayList<>();
//      ProcedureMetric m1 = new ProcedureMetric();
//      m1.metricCyclomaticComplexity = 30;
//      m1.procedure = "main";
//      list.add(m1);
//      ProcedureMetric m2 = new ProcedureMetric();
//      m2.metricCyclomaticComplexity = 20;
//      m2.procedure = "add";
//      list.add(m2);
//      ProcedureMetric m3 = new ProcedureMetric();
//      m3.metricCyclomaticComplexity = 40;
//      m3.procedure = "alter";
//      list.add(m3);
//      ProcedureMetric m4 = new ProcedureMetric();
//      m4.metricCyclomaticComplexity = 20;
//      m4.procedure = "added";
//      list.add(m4);
//      
//      System.out.println(list);
//      Collections.sort(list);
//      System.out.println(list);
//  }
}
