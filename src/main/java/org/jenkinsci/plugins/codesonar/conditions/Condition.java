package org.jenkinsci.plugins.codesonar.conditions;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Result;
import jenkins.model.Jenkins;

/**
 *
 * @author andrius
 */
public abstract class Condition implements Describable<Condition>, ExtensionPoint {

    public abstract Result validate(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener);

    @Override
    public Descriptor<Condition> getDescriptor() {
        return (ConditionDescriptor<?>) Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    public static DescriptorExtensionList<Condition, ConditionDescriptor<Condition>> getAll() {
        return Jenkins.getInstance().<Condition, ConditionDescriptor<Condition>>getDescriptorList(Condition.class);
    }
}
