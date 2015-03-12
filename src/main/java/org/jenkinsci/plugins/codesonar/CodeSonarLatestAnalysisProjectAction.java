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
public class CodeSonarLatestAnalysisProjectAction implements Action {

    private final AbstractProject<?, ?> project;

    public CodeSonarLatestAnalysisProjectAction(AbstractProject<?, ?> project) {
        this.project = project;
    }

    @Override
    public String getIconFileName() {
        return "/plugin/codesonar/icons/codesonar-logo.png";
    }

    @Override
    public String getDisplayName() {
        return "Latest CodeSonar analysis";
    }

    @Override
    public String getUrlName() {
        CodeSonarBuildAction buildAction = Utils.getLatestCodeSonarBuildActionFromProject(project);
        return buildAction.getUrlName();
    }

    public CodeSonarBuildAction getLatestActionInProject() {
        if (project.getLastSuccessfulBuild() != null) {
            return project.getLastSuccessfulBuild().getAction(CodeSonarBuildAction.class);
        }
        return null;
    }
}
