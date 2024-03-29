package org.jenkinsci.plugins.codesonar.models.metrics;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.jenkinsci.plugins.codesonar.CodeSonarPluginException;

/**
 *
 * @author Andrius
 */
@XmlRootElement(name = "metrics")
@XmlAccessorType(XmlAccessType.FIELD)
public class Metrics implements Serializable {

    @XmlElement(name = "metric")
    private List<Metric> metrics;
    
    private CodeSonarPluginException createError(String msg, Object...args) {
        return new CodeSonarPluginException(msg, args);
    }

    public Metric getMetricByName(String name) throws CodeSonarPluginException {
        if (metrics == null) {
            metrics = Collections.EMPTY_LIST;
        }

        for (Metric metric : metrics) {
            if (metric.getName().equals(name)) {
                return metric;
            }
        }

        throw createError("Metric by the name {0} was not found on the hub", name);
    }

    public List<Metric> getMetrics() {
        if (metrics == null) {
            return Collections.EMPTY_LIST;
        }

        return metrics;
    }

    public void setMetrics(List<Metric> metrics) {
        this.metrics = metrics;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.metrics);
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
        final Metrics other = (Metrics) obj;
        if (!Objects.equals(this.metrics, other.metrics)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Metrics{" + "metrics=" + metrics + '}';
    }
}
