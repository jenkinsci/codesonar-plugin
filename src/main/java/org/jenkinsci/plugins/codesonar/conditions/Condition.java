package org.jenkinsci.plugins.codesonar.conditions;

import java.text.MessageFormat;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.codesonar.CodeSonarLogger;
import org.jenkinsci.plugins.codesonar.CodeSonarPluginException;
import org.jenkinsci.plugins.codesonar.services.CodeSonarHubAnalysisDataLoader;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.Launcher;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;

/**
 *
 * @author andrius
 */
public abstract class Condition implements Describable<Condition>, ExtensionPoint {
    protected static final String CURRENT_BUILD_DATA_NOT_AVAILABLE = "current build not found";
    protected static final String PREVIOUS_BUILD_DATA_NOT_AVAILABLE = "previous successful build not found";
    protected static final String DATA_LOADER_EMPTY_RESPONSE = "Data loader returned an empty response";
    private String resultDescription;
    
    public abstract Result validate(CodeSonarHubAnalysisDataLoader current, CodeSonarHubAnalysisDataLoader previous, Launcher launcher, TaskListener listener, CodeSonarLogger logger) throws CodeSonarPluginException;

    @Override
    public Descriptor<Condition> getDescriptor() {
        Jenkins instance = Jenkins.getInstanceOrNull();

        if (instance != null) {
            return (ConditionDescriptor<?>) instance.getDescriptorOrDie(getClass());
        }

        throw new NullPointerException("Jenkins is not started or is stopped");
    }

    public static DescriptorExtensionList<Condition, ConditionDescriptor<Condition>> getAll() {
        Jenkins instance = Jenkins.getInstanceOrNull();

        if (instance != null) {
            return instance.<Condition, ConditionDescriptor<Condition>>getDescriptorList(Condition.class);
        }

        throw new NullPointerException("Jenkins is not started or is stopped");
    }
    
    public String describeResult() {
        if(StringUtils.isBlank(resultDescription)) {
            return getDescriptor().getDisplayName();
        }
        return String.format("%s [%s]", getDescriptor().getDisplayName(), resultDescription);
    }
    
    protected void registerResult(CodeSonarLogger logger, String message, Object...args) {
        if(args != null && args.length > 0) {
            resultDescription = MessageFormat.format(message, args);
        } else {
            resultDescription = message;
        }
        logger.writeInfo(resultDescription);
    }
}
