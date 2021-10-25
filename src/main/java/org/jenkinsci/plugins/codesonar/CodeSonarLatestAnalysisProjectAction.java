package org.jenkinsci.plugins.codesonar;

import hudson.model.Job;
import hudson.model.Action;
import java.util.Collection;
import jenkins.tasks.SimpleBuildStep;

/**
 *
 * @author andrius
 */
public class CodeSonarLatestAnalysisProjectAction implements SimpleBuildStep.LastBuildAction {

    private final Job<?, ?> job;

    public CodeSonarLatestAnalysisProjectAction(Job<?, ?> job) {
        this.job = job;
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
        return "Latest Codesonar Analysis";
    }

    @Override
    public String getUrlName() {
        CodeSonarBuildAction buildAction = Utils.getLatestCodeSonarBuildActionFromProject(job);

        if (buildAction == null) {
            return null;
        }

        return buildAction.getUrlName();
    }

    public boolean isBuildActionPresent() {
        return Utils.getLatestCodeSonarBuildActionFromProject(job) != null;
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
