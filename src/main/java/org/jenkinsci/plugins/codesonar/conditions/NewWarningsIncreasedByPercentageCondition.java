package org.jenkinsci.plugins.codesonar.conditions;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import org.kohsuke.stapler.DataBoundConstructor;

public class NewWarningsIncreasedByPercentageCondition extends Condition {
    
    private static final String NAME = "Number of new warnings increased by a percentage";
    private float percentage;
 
    @DataBoundConstructor
    public NewWarningsIncreasedByPercentageCondition(float percentage) {
        this.percentage = percentage;
    }
    
    @Override
    public Result validate(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    /*@Extension
    public static final class DescriptorImpl extends ConditionDescriptor<NewWarningsLessThanTotalCondition> {

        public DescriptorImpl() {
            load();
        }
        
        @Override
        public String getDisplayName() {
            return NAME;
        }
        
    }*/

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
