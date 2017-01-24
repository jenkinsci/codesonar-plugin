package org.jenkinsci.plugins.codesonar;

import hudson.model.Run;
import hudson.model.Job;

/**
 *
 * @author andrius
 */
public class Utils {

    public static CodeSonarBuildAction getLatestCodeSonarBuildActionFromProject(Job<?, ?> job) {
        Run lastBuild = job.getLastBuild();

        if (lastBuild == null) {
            return null;
        }

        CodeSonarBuildAction action = lastBuild.getAction(CodeSonarBuildAction.class);

        if (action != null) {
            return action;
        }

        Run build = lastBuild.getPreviousBuild();
        if (build == null) {
            return null;
        }
        
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
