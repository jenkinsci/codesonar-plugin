package org.jenkinsci.plugins.codesonar.models.projects;

import org.jenkinsci.plugins.codesonar.models.Metric;
import java.io.Serializable;
import java.util.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "project")
@XmlAccessorType(XmlAccessType.FIELD)
public class Project40 implements Serializable {

    @XmlAttribute
    private String url;
    private String project;
    private String state;
    private String started;
    @XmlElement
    private Metric metric;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }
    
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStarted() {
        return started;
    }

    public void setStarted(String started) {
        this.started = started;
    }

    public Metric getMetric() {
        return metric;
    }

    public void setMetric(Metric metric) {
        this.metric = metric;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Project40)) {
            return false;
        }
        Project40 other = (Project40) obj;

        if (!this.project.equals(other.project)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(this.project);
        return hash;
    }

    @Override
    public String toString() {
        return "Project40{" + "url=" + url + ", project=" + project + ", state=" + state + ", started=" + started + ", metric=" + metric + '}';
    }

}
