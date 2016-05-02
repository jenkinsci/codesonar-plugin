package org.jenkinsci.plugins.codesonar;

import hudson.AbortException;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    
    public static float getVersion(String info) throws AbortException {
        Pattern pattern = Pattern.compile("Version:\\s(\\d+\\.\\d+)");
        
        Matcher matcher = pattern.matcher(info);
        if (matcher.find()) {
            return Float.valueOf(matcher.group(1));
        }
        
        throw new AbortException("Hub version could not be determined");
    }

    public enum UrlFilters {

        NEW("5"), ACTIVE("2"), OLD_NEW("4");

        private final String value;

        private UrlFilters(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

    }
}
