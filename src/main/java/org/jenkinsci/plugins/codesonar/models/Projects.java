package org.jenkinsci.plugins.codesonar.models;

import java.io.Serializable;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author andrius
 */
@XmlRootElement(name = "projects")
@XmlAccessorType(XmlAccessType.FIELD)
public class Projects implements Serializable {

    @XmlElement(name = "project")
    private List<Project> projects;

    public Project getProjectByName(String projectName) {
        for (Project project : projects) {
            if (project.getProject().equals(projectName)) {
                return project;
            }
        }

        return null;
    }

    public List<Project> getProjects() {
        return projects;
    }

    public void setProjects(List<Project> projects) {
        this.projects = projects;
    }
}
