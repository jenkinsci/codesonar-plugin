package org.jenkinsci.plugins.codesonar.conditions;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.codesonar.models.CodeSonarBuildActionDTO;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.util.FormValidation;

public class WarningCountIncreaseOverallCondition extends Condition {
    private static final Logger LOGGER = Logger.getLogger(WarningCountIncreaseOverallCondition.class.getName());

    private static final String NAME = "Warning count increase: overall";
    private String percentage = String.valueOf(5.0f);
    private String warrantedResult = Result.UNSTABLE.toString();

    @DataBoundConstructor
    public WarningCountIncreaseOverallCondition(String percentage) {
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
    public Result validate(CodeSonarBuildActionDTO current, CodeSonarBuildActionDTO previous, Launcher launcher, TaskListener listener) {
        if (current == null) {
            return Result.SUCCESS;
        }        

        if (previous == null) {
            return Result.SUCCESS;
        }
        
        // Going to produce build failures in the case of missing necessary information
        Analysis previousAnalysisActiveWarnings = previous.getAnalysisActiveWarnings();
        if(previousAnalysisActiveWarnings == null) {
            LOGGER.log(Level.SEVERE, "\"analysisActiveWarnings\" data not found in persisted build.");
            return Result.FAILURE;
        }
        Analysis currentAnalysisActiveWarnings = current.getAnalysisActiveWarnings();
        if(currentAnalysisActiveWarnings == null) {
            LOGGER.log(Level.SEVERE, "\"analysisNewWarnings\" data not found in persisted build.");
            return Result.FAILURE;
        }    

        float previousCount = (float) previousAnalysisActiveWarnings.getWarnings().size();
        float currentCount = (float) currentAnalysisActiveWarnings.getWarnings().size();
        float diff = currentCount - previousCount;

        if ((diff / previousCount) * 100 > Float.parseFloat(percentage)) {
            return Result.fromString(warrantedResult);
        }

        return Result.SUCCESS;
    }

    @Symbol("warningCountIncreaseOverall")
    @Extension
    public static final class DescriptorImpl extends ConditionDescriptor<WarningCountIncreaseOverallCondition> {

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
