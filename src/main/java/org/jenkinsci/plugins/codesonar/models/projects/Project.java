package org.jenkinsci.plugins.codesonar.models.projects;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "project")
@XmlAccessorType(XmlAccessType.FIELD)
public class Project implements Serializable {

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
        Project other = (Project) obj;

        if (!this.url.equals(other.url)) {
            return false;
        }
        if (!this.project.equals(other.project)) {
            return false;
        }
        if (!this.state.equals(other.state)) {
            return false;
        }
        if (!this.started.equals(other.started)) {
            return false;
        }
        if (!this.metric.equals(other.metric)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("url: ").append(url);
        sb.append("\n");
        sb.append("project: ").append(project);
        sb.append("\n");
        sb.append("state: ").append(state);
        sb.append("\n");
        sb.append("started: ").append(started);
        sb.append("\n");
        sb.append("metric: ").append(metric);
        sb.append("\n");

        return sb.toString();
    }
}
