package org.jenkinsci.plugins.codesonar.models.projects;

import hudson.AbortException;
import org.jenkinsci.plugins.codesonar.models.ProjectTree;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author andrius
 */
@XmlRootElement(name = "projects")
@XmlAccessorType(XmlAccessType.FIELD)
public class Projects implements Serializable {

    @XmlElement(name = "projecttree")
    private List<ProjectTree> projectTrees;

    @XmlElement(name = "project")
    private List<Project> projects;

    public Project getProjectByName(String projectName) throws AbortException {
        for (Project project : getProjects()) {
            if (project.getName().equals(projectName)) {
                return project;
            }
        }
        
        throw new AbortException(String.format("Project by the name %s was not found on the hub", projectName));
    }

    public List<ProjectTree> getProjectTrees() {
        if (projectTrees == null) {
            projectTrees = new ArrayList<>();
        }

        return projectTrees;
    }

    public void setProjectTrees(List<ProjectTree> projectTrees) {
        this.projectTrees = projectTrees;
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
        return "Projects{" +
                "projectTrees=" + getProjectTrees() +
                ", projects=" + getProjects() +
                '}';
    }
}
