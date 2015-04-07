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

    public enum UrlFilters {

        NEW("4"), ACTIVE("2");

        private final String value;

        private UrlFilters(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

    }
}
