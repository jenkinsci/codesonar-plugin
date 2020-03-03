package org.jenkinsci.plugins.codesonar.conditions;

import hudson.AbortException;
import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.Launcher;
import hudson.model.Run;
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

    public abstract Result validate(Run<?, ?> run, Launcher launcher, TaskListener listener) throws AbortException;
    
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
}
