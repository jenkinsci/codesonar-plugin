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
        if (!isBuildActionPresent()) {
            return null;
        }
        return "/plugin/codesonar/icons/codesonar-logo.png";
    }

    @Override
    public String getDisplayName() {
        if (!isBuildActionPresent()) {
            return null;
        }
        return "Latest CodeSonar Analysis";
    }

    @Override
    public String getUrlName() {
        CodeSonarBuildAction buildAction = Utils.getLatestCodeSonarBuildActionFromProject(project);

        if (buildAction == null) {
            return null;
        }

        return buildAction.getUrlName();
    }

    public boolean isBuildActionPresent() {
        return Utils.getLatestCodeSonarBuildActionFromProject(project) != null;
    }
}
