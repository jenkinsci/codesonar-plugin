package org.jenkinsci.plugins.codesonar.models;

import javax.xml.bind.annotation.*;
import java.io.Serializable;

@XmlRootElement(name = "projecttree")
@XmlAccessorType(XmlAccessType.FIELD)
public class ProjectTree implements Serializable {

    @XmlAttribute
    private String url;
    private String name;
    private String state;
    private String started;
    @XmlElement
    private Metric metric;
    private String type;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProjectTree that = (ProjectTree) o;

        if (!url.equals(that.url)) return false;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = url.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ProjectTree{" +
                "url='" + url + '\'' +
                ", name='" + name + '\'' +
                ", state='" + state + '\'' +
                ", started='" + started + '\'' +
                ", metric=" + metric +
                ", type='" + type + '\'' +
                '}';
    }
}
