/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.codesonar.conditions;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author Mads
 */
public class NewWarningsLessThanTotalCondition extends Condition {
    
    private static final String NAME = "Maximum number of new warnings compared to total";
    private float percentage;
 
    @DataBoundConstructor
    public NewWarningsLessThanTotalCondition(float percentage) {
        this.percentage = percentage;
    }
    
    @Override
    public Result validate(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    @Extension
    public static final class DescriptorImpl extends ConditionDescriptor<NewWarningsLessThanTotalCondition> {

        public DescriptorImpl() {
            load();
        }
        
        @Override
        public String getDisplayName() {
            return NAME;
        }
        
    }

    /**
     * @return the percentage
     */
    public float getPercentage() {
        return percentage;
    }

    /**
     * @param percentage the percentage to set
     */
    public void setPercentage(float percentage) {
        this.percentage = percentage;
    }

}
