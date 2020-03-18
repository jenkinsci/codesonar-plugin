package org.jenkinsci.plugins.codesonar;

import com.google.common.collect.Iterators;
import hudson.AbortException;
import hudson.model.Job;
import hudson.model.Action;
import java.io.IOException;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 *
 * @author andrius
 */
public class CodeSonarProjectAction implements Action {

    private final Job<?, ?> project;

    public CodeSonarProjectAction(Job<?, ?> project) {
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
        return Iterators.size(project.getBuilds().iterator()) >= 2;
    }

    public void doReportGraphs(StaplerRequest req, StaplerResponse rsp) throws AbortException {
        CodeSonarBuildAction action = getLatestActionInProject();
        if (action != null) {
            try {
                action.doReportGraphs(req, rsp);
            } catch (IOException e) {
                throw new AbortException(e.getMessage());
            }
        }
    }
}
