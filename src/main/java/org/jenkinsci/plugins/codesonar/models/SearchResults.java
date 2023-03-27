package org.jenkinsci.plugins.codesonar.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.jenkinsci.plugins.codesonar.CodeSonarPluginException;
import org.jenkinsci.plugins.codesonar.models.projects.Project;

@XmlRootElement(name = "search_results")
@XmlAccessorType(XmlAccessType.FIELD)
public class SearchResults implements Serializable {
    @XmlElement(name = "project")
    private List<Project> projects;
    
    private CodeSonarPluginException createError(String msg, Object...args) {
        return new CodeSonarPluginException(msg, args);
    }

    public Project getProjectByName(String projectName) throws CodeSonarPluginException {
        List<Project> duplicates = new ArrayList<>();
        for (Project project : getProjects()) {
            if (project.getName().equals(projectName)) {
                duplicates.add(project);
            }
        }
        if (duplicates.size() > 1) {
            throw createError("Multiple projects found with name: {0}%nMake sure projects do not share the same name.", projectName);
        } else if (duplicates.size() == 0) {
            throw createError("Project by the name {0} was not found on the hub", projectName);
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
