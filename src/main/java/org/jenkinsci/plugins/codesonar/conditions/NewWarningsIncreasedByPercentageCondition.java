package org.jenkinsci.plugins.codesonar.conditions;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.codesonar.CodeSonarLogger;
import org.jenkinsci.plugins.codesonar.CodeSonarPluginException;
import org.jenkinsci.plugins.codesonar.services.CodeSonarHubAnalysisDataLoader;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.util.FormValidation;

public class NewWarningsIncreasedByPercentageCondition extends Condition {
    private static final Logger LOGGER = Logger.getLogger(NewWarningsIncreasedByPercentageCondition.class.getName());

    private static final String NAME = "Warning count increase: new only";
    private static final String RESULT_DESCRIPTION_MESSAGE_FORMAT = "threshold={0,number,0.00}%, increase={1,number,0.00}% (count: new={2,number,0}, total={3,number,0})";
    private String percentage = "5.0f";
    private String warrantedResult = Result.UNSTABLE.toString();
    
    @DataBoundConstructor
    public NewWarningsIncreasedByPercentageCondition(String percentage) {
        this.percentage = percentage;
    }
    
    /**
     * @return the percentage
     */
    public String getPercentage() {
        return percentage;
    }

    /**
     * @param percentage the percentage to set
     */
    @DataBoundSetter
    public void setPercentage(String percentage) {
        this.percentage = percentage;
    }

    public String getWarrantedResult() {
        return warrantedResult;
    }

    @DataBoundSetter
    public void setWarrantedResult(String warrantedResult) {
        this.warrantedResult = warrantedResult;
    }

    @Override
    public Result validate(CodeSonarHubAnalysisDataLoader current, CodeSonarHubAnalysisDataLoader previous, Launcher launcher, TaskListener listener, CodeSonarLogger csLogger) throws CodeSonarPluginException {
        if (current == null) {
            registerResult(csLogger, CURRENT_BUILD_DATA_NOT_AVAILABLE);
            return Result.SUCCESS;
        }
        
        long numberOfActiveWarnings = current.getNumberOfActiveWarnings();
        long numberOfNewWarnings = current.getNumberOfNewWarnings();
        
        float activeWarningCount = numberOfActiveWarnings;
        float newWarningCount = numberOfNewWarnings;
        
        float result;
        //If there are no active warnings, redefine percentage of new warnings
        if(activeWarningCount == 0f) {
            result = newWarningCount > 0 ? 100f : 0f;
            LOGGER.log(Level.INFO, "no active warnings found, forcing new warning percentage to {0,number,0.00}%", result);
        } else {
            result = (newWarningCount * 100f) / activeWarningCount;
            LOGGER.log(Level.INFO, "new warning percentage = {0,number,0.00}%", result);
        }
        
        float thresholdPercentage = Float.parseFloat(percentage);
        LOGGER.log(Level.INFO, "threshold percentage = {0,number,0.00}%", thresholdPercentage);
        
        registerResult(csLogger, RESULT_DESCRIPTION_MESSAGE_FORMAT, thresholdPercentage, result, newWarningCount, activeWarningCount);
        
        if (result > thresholdPercentage) {
            return Result.fromString(warrantedResult);
        }
        return Result.SUCCESS;
    }
    
    @Symbol("warningCountIncreaseNewOnly")
    @Extension
    public static final class DescriptorImpl extends ConditionDescriptor<NewWarningsIncreasedByPercentageCondition> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public @Nonnull String getDisplayName() {
            return NAME;
        }
        
        public FormValidation doCheckPercentage(@QueryParameter("percentage") String percentage) {
            if (StringUtils.isBlank(percentage)) {
                return FormValidation.error("Cannot be empty");
            }
            
            try {
                float v = Float.parseFloat(percentage);

                if(v < 0) {
                    return FormValidation.error("The provided value must be zero or greater");
                }
            } catch (NumberFormatException numberFormatException) {
                return FormValidation.error("Not a valid decimal number");
            }

            return FormValidation.ok();
        }
    }
}
