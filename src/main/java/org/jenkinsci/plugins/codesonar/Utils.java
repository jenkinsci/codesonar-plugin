 package org.jenkinsci.plugins.codesonar;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;

/**
 *
 * @author andrius
 */
public class Utils {

    public static CodeSonarBuildAction getLatestCodeSonarBuildActionFromProject(AbstractProject<?, ?> project) {
        AbstractBuild lastBuild = project.getLastBuild();

        if (lastBuild == null) {
            return null;
        }

        CodeSonarBuildAction action = lastBuild.getAction(CodeSonarBuildAction.class);

        if (action != null) {
            return action;
        }

        AbstractBuild build = lastBuild.getPreviousBuild();
        do {
            action = build.getAction(CodeSonarBuildAction.class);

            if (action != null) {
                return action;
            }

            build = build.getPreviousBuild();
        } while (build != null);

        return null;
    }
}
