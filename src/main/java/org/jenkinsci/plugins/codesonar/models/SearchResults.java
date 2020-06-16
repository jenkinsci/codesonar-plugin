package org.jenkinsci.plugins.codesonar.models;

import hudson.AbortException;
import org.jenkinsci.plugins.codesonar.models.projects.Project;

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
    private List<Project> projects;

    public Project getProjectByName(String projectName) throws AbortException {
        List<Project> duplicates = new ArrayList<>();
        for (Project project : getProjects()) {
            if (project.getName().equals(projectName)) {
                duplicates.add(project);
            }
        }
        if (duplicates.size() > 1) {
            throw new AbortException("Multiple projects found with name: " + projectName + "\nMake sure projects do not share the same name.");
        } else if (duplicates.size() == 0) {
            throw new AbortException(String.format("Project by the name %s was not found on the hub", projectName));
        }

        return duplicates.get(0);
    }

    public List<Project> getProjects() {
        if (projects == null) {
            projects = new ArrayList<>();
        }

        return projects;
    }

    public void setProjects(List<Project> projects) {
        this.projects = projects;
    }

    @Override
    public String toString() {
        return "SearchResults{" +
                "projects=" + getProjects() +
                '}';
    }
}
