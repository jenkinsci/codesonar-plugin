package org.jenkinsci.plugins.codesonar.models.procedures;

import hudson.AbortException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.jenkinsci.plugins.codesonar.models.Metric;

/**
 *
 * @author Andrius
 */
@XmlRootElement(name = "procedure_row")
@XmlAccessorType(XmlAccessType.FIELD)
public class ProcedureRow implements Serializable {

    private String file;
    private String procedure;
    @XmlElement(name = "metric")
    private List<Metric> metrics;

    public Metric getMetricByName(String metricName) {
        if (metrics == null) {
            metrics = Collections.EMPTY_LIST;
        }

        for (Metric metric : metrics) {
            if (metric.getName().equals(metricName)) {
                return metric;
            }
        }
        return null;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getProcedure() {
        return procedure;
    }

    public void setProcedure(String procedure) {
        this.procedure = procedure;
    }

    public List<Metric> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<Metric> metrics) {
        this.metrics = metrics;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 23 * hash + Objects.hashCode(this.file);
        hash = 23 * hash + Objects.hashCode(this.procedure);
        hash = 23 * hash + Objects.hashCode(this.metrics);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ProcedureRow other = (ProcedureRow) obj;
        if (!Objects.equals(this.file, other.file)) {
            return false;
        }
        if (!Objects.equals(this.procedure, other.procedure)) {
            return false;
        }
        if (!Objects.equals(this.metrics, other.metrics)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ProcedureRow{" + "file=" + file + ", procedure=" + procedure + ", metrics=" + metrics + '}';
    }
}
