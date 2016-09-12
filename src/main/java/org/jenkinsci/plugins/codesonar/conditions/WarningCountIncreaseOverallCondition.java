package org.jenkinsci.plugins.codesonar.conditions;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.util.FormValidation;
import org.jenkinsci.plugins.codesonar.CodeSonarBuildAction;
import org.jenkinsci.plugins.codesonar.models.CodeSonarBuildActionDTO;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

public class WarningCountIncreaseOverallCondition extends Condition {

    private static final String NAME = "Warning count increase: overall";
    private float percentage = 5.0f;
    private String warrantedResult = Result.UNSTABLE.toString();

    @DataBoundConstructor
    public WarningCountIncreaseOverallCondition(float percentage) {
        this.percentage = percentage;
    }

    @Override
    public Result validate(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        CodeSonarBuildAction buildAction = build.getAction(CodeSonarBuildAction.class);
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

        if ((diff / previousCount) * 100 > percentage) {
            Result result = Result.fromString(warrantedResult);
            return result;
        }

        return Result.SUCCESS;
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

    public String getWarrantedResult() {
        return warrantedResult;
    }

    @DataBoundSetter
    public void setWarrantedResult(String warrantedResult) {
        this.warrantedResult = warrantedResult;
    }

    @Extension
    public static final class DescriptorImpl extends ConditionDescriptor<WarningCountIncreaseOverallCondition> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return NAME;
        }
        
        public FormValidation doCheckPercentage(@QueryParameter("percentage") String percentage) {
            try {
                Float.parseFloat(percentage);
            } catch (NumberFormatException nex) {
                return FormValidation.error("The input must be a number"); 
            }
            
            float value = Float.parseFloat(percentage);
            
            if(value < 0) {
                return FormValidation.error("The provided value must be zero or greater"); 
            }
            
            return FormValidation.ok();
        }

    }
}
