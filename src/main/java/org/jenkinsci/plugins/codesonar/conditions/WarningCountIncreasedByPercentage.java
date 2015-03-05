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
import org.jenkinsci.plugins.codesonar.models.Analysis;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author Mads
 */
public class WarningCountIncreasedByPercentage extends Condition {
    
    private static final String NAME = "Maximum warning count increase";
    private float percentage = 5f;
 
    @DataBoundConstructor
    public WarningCountIncreasedByPercentage(float percentage) {
        this.percentage = percentage;
    }
    
    @Override
    public Result validate(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        
        Analysis previous = getPreviousAnalysisResult(build);
        if(previous == null) {
            //No previous results. New analysis
            return Result.SUCCESS;
        } else {
            Analysis current = getAnalysis(build);
            float currentCount = (float)current.getWarnings().size();
            float previousCount = (float)previous.getWarnings().size();
            float diff = currentCount - previousCount;
            
            if ((diff/previousCount)*100 > percentage) {
                return Result.SUCCESS;
            } else {
                return Result.UNSTABLE;
            }

        }
        
    }
    
    @Extension
    public static final class DescriptorImpl extends ConditionDescriptor<WarningCountIncreasedByPercentage> {

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
