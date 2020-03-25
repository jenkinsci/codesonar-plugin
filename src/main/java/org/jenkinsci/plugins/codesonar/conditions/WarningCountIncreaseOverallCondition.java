package org.jenkinsci.plugins.codesonar.conditions;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.Result;
import hudson.util.FormValidation;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.codesonar.CodeSonarBuildAction;
import org.jenkinsci.plugins.codesonar.models.CodeSonarBuildActionDTO;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;

public class WarningCountIncreaseOverallCondition extends Condition {

    private static final String NAME = "Warning count increase: overall";
    private String percentage = String.valueOf(5.0f);
    private String warrantedResult = Result.UNSTABLE.toString();

    @DataBoundConstructor
    public WarningCountIncreaseOverallCondition(String percentage) {
        this.percentage = percentage;
    }

    @Override
    public Result validate(Run<?, ?> run, Launcher launcher, TaskListener listener) {
        CodeSonarBuildAction buildAction = run.getAction(CodeSonarBuildAction.class);
        if (buildAction == null) {
            return Result.SUCCESS;
        }

        CodeSonarBuildActionDTO buildActionDTO = buildAction.getBuildActionDTO();
        if (buildActionDTO == null) {
            return Result.SUCCESS;
        }
        
        Analysis current = buildActionDTO.getAnalysisActiveWarnings();
        
        CodeSonarBuildAction previousBuildAction = buildAction.getPreviousAction();
        if (previousBuildAction == null) {
            return Result.SUCCESS;
        }

        CodeSonarBuildActionDTO prevBuildActionDTO = buildAction.getBuildActionDTO();
        if (prevBuildActionDTO == null) {
            return Result.SUCCESS;
        }
        
        Analysis previous = prevBuildActionDTO.getAnalysisActiveWarnings();
        
        float previousCount = (float) previous.getWarnings().size();
        float currentCount = (float) current.getWarnings().size();
        float diff = currentCount - previousCount;

        if ((diff / previousCount) * 100 > Float.parseFloat(percentage)) {
            return Result.fromString(warrantedResult);
        }

        return Result.SUCCESS;
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
