package org.jenkinsci.plugins.codesonar.models;

import hudson.AbortException;
import org.jenkinsci.plugins.codesonar.models.projects.Project40;
import org.jenkinsci.plugins.codesonar.models.projects.Project42;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "search_results")
@XmlAccessorType(XmlAccessType.FIELD)
public class SearchResults implements Serializable {
    @XmlElement(name = "project")
    private List<Project40> projects;

    public Project40 getProjectByName(String projectName) throws AbortException {
        if (getProjects().size() > 1) {
            throw new AbortException("multiple projects found with name: " + projectName);
        }

        for (Project40 project : getProjects()) {
            if (project.getProject().equals(projectName)) {
                return project;
            }
        }

        throw new AbortException(String.format("Project by the name %s was not found on the hub", projectName));
    }

    public List<Project40> getProjects() {
        if (projects == null) {
            projects = new ArrayList<>();
        }

        return projects;
    }

    public void setProjects(List<Project40> projects) {
        this.projects = projects;
    }

    @Override
    public String toString() {
        return "SearchResults{" +
                "projects=" + getProjects() +
                '}';
    }
}
