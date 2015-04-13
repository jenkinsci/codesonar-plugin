package org.jenkinsci.plugins.codesonar.conditions;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import org.jenkinsci.plugins.codesonar.CodeSonarBuildAction;
import org.jenkinsci.plugins.codesonar.models.CodeSonarBuildActionDTO;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class NewWarningsIncreasedByPercentageCondition extends Condition {

    private static final String NAME = "Number of new warnings increased by a percentage";
    private float percentage = 5.0f;
    private String warrantedResult = Result.UNSTABLE.toString();

    @DataBoundConstructor
    public NewWarningsIncreasedByPercentageCondition(float percentage) {
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

        Analysis currentActiveWarnings = buildActionDTO.getAnalysisActiveWarnings();
        Analysis currentNewWarnings = buildActionDTO.getAnalysisNewWarnings();

        float activeWarningCount = (float) currentActiveWarnings.getWarnings().size();
        float newWarningCount = (float) currentNewWarnings.getWarnings().size();

        float result = (newWarningCount * 100.0f) / activeWarningCount; 

        if (result > percentage) {
            return Result.fromString(warrantedResult);
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
    @DataBoundSetter
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
    public static final class DescriptorImpl extends ConditionDescriptor<NewWarningsIncreasedByPercentageCondition> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return NAME;
        }

    }
}
