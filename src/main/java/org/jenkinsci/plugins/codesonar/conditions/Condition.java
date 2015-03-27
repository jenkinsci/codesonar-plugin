package org.jenkinsci.plugins.codesonar.conditions;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Result;
import javax.annotation.CheckForNull;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.codesonar.CodeSonarBuildAction;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;

/**
 *
 * @author andrius
 */
public abstract class Condition implements Describable<Condition>, ExtensionPoint {

    public abstract Result validate(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener);
    
    @CheckForNull
    public Analysis getPreviousAnalysisResult(AbstractBuild<?, ?> build) {       
        for(AbstractBuild<?,?> it = build.getPreviousBuild(); it!= null; it = it.getPreviousBuild()) {
            if(it.getAction(CodeSonarBuildAction.class) != null && it.getAction(CodeSonarBuildAction.class).getAnalysis() !=  null) {
                return it.getAction(CodeSonarBuildAction.class).getAnalysis();
            }
        }            
        return null;
    }
    
    @CheckForNull
    public Analysis getAnalysis(AbstractBuild<?,?> build) {
        if(build != null && build.getAction(CodeSonarBuildAction.class) != null) {
            return build.getAction(CodeSonarBuildAction.class).getAnalysis();
        }
        return null;
    }
    
    @Override
    public Descriptor<Condition> getDescriptor() {
        return (ConditionDescriptor<?>) Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    public static DescriptorExtensionList<Condition, ConditionDescriptor<Condition>> getAll() {
        return Jenkins.getInstance().<Condition, ConditionDescriptor<Condition>>getDescriptorList(Condition.class);
    }
}
