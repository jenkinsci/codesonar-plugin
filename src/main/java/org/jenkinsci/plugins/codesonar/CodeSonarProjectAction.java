package org.jenkinsci.plugins.codesonar;

import hudson.model.AbstractProject;
import hudson.model.Action;
import java.io.IOException;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 *
 * @author andrius
 */
public class CodeSonarProjectAction implements Action {

    private final AbstractProject<?, ?> project;

    public CodeSonarProjectAction(AbstractProject<?, ?> project) {
        this.project = project;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return "CodeSonar";
    }

    public CodeSonarBuildAction getLatestActionInProject() {
        if (project.getLastSuccessfulBuild() != null) {
            return project.getLastSuccessfulBuild().getAction(CodeSonarBuildAction.class);
        }
        return null;
    }

    public boolean isDrawGraphs() {
        return project.getBuilds().size() >= 2;
    }

    public void doReportGraphs(StaplerRequest req, StaplerResponse rsp) {
        CodeSonarBuildAction action = getLatestActionInProject();
        if (action != null) {
            try {
                action.doReportGraphs(req, rsp);
            } catch (IOException exception) {
            }
        }
    }
}
