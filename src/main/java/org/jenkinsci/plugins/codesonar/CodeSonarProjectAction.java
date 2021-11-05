package org.jenkinsci.plugins.codesonar;

import com.google.common.collect.Iterators;
import hudson.AbortException;
import hudson.model.Job;
import hudson.model.Action;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 *
 * @author andrius
 */
public class CodeSonarProjectAction implements Action {

    private final Job<?, ?> project;
    private final String projectName;

    public CodeSonarProjectAction(Job<?, ?> project, String projectName) {
        this.project = project;
        this.projectName = projectName;
    }

    @Override
    public String getIconFileName() {
       //return "/plugin/codesonar/icons/codesonar-logo.png";
       return null;
    }

    @Override
    public String getDisplayName() {
        //return "Latest analysis: "+projectName;
        return null;
    }

    @Override
    public String getUrlName() {
        return "CodeSonar";
    }

    public Collection<CodeSonarBuildAction> getLatestActionsInProject() {
        if (project.getLastSuccessfulBuild() != null) {
            
            return project.getLastSuccessfulBuild().getActions(CodeSonarBuildAction.class)
                    .stream().filter(a -> a.getProjectName().equals(projectName))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public boolean isDrawGraphs() {
        return Iterators.size(project.getBuilds().iterator()) >= 2;
    }

    public void doReportGraphs(StaplerRequest req, StaplerResponse rsp) throws AbortException {
        Collection<CodeSonarBuildAction> actions = getLatestActionsInProject();
        for(CodeSonarBuildAction a : actions) {
            try {
                a.doReportGraphs(req, rsp);
            } catch (IOException e) {
                throw new AbortException(e.getMessage());
            }
        }
    }
}
