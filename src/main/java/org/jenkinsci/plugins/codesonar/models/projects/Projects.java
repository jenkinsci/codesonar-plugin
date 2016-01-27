package org.jenkinsci.plugins.codesonar.models.projects;

import hudson.AbortException;
import java.io.Serializable;
import java.util.Collections;
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

    public Project getProjectByName(String projectName) throws AbortException {
        if (projects == null) {
            projects = Collections.EMPTY_LIST;
        }
            
        for (Project project : projects) {
            if (project.getProject().equals(projectName)) {
                return project;
            }
        }

        throw new AbortException(String.format("Project by the name %s was not found on the hub", projectName));
    }

    public List<Project> getProjects() {
        return projects;
    }

    public void setProjects(List<Project> projects) {
        this.projects = projects;
    }
}
