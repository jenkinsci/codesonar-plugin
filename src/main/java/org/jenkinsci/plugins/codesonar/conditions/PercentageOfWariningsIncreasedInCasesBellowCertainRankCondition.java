package org.jenkinsci.plugins.codesonar.conditions;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import java.util.List;
import org.jenkinsci.plugins.codesonar.CodeSonarBuildAction;
import org.jenkinsci.plugins.codesonar.models.CodeSonarBuildActionDTO;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.jenkinsci.plugins.codesonar.models.analysis.Warning;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 *
 * @author andrius
 */
public class PercentageOfWariningsIncreasedInCasesBellowCertainRankCondition extends Condition {

    private static final String NAME = "Percentage of warnings in cases below a certain rank increase";

    private int rankOfWarnings;
    private float warningPercentage;
    private String warrantedResult = Result.UNSTABLE.toString();

    @DataBoundConstructor
    public PercentageOfWariningsIncreasedInCasesBellowCertainRankCondition(int rankOfWarnings, float warningPercentage) {
        this.rankOfWarnings = rankOfWarnings;
        this.warningPercentage = warningPercentage;
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

        Analysis analysis = buildActionDTO.getAnalysisActiveWarnings();

        int totalNumberOfWarnings = analysis.getWarnings().size();

        float severeWarnings = 0.0f;
        List<Warning> warnings = analysis.getWarnings();
        for (Warning warning : warnings) {
            if (warning.getRank() < rankOfWarnings) {
                severeWarnings++;
            }
        }

        float calculatedWarningPercentage = (severeWarnings / totalNumberOfWarnings) * 100;

        if (calculatedWarningPercentage > warningPercentage) {
            Result result = Result.fromString(warrantedResult);
            return result;
        }

        return Result.SUCCESS;
    }

    public int getRankOfWarnings() {
        return rankOfWarnings;
    }

    public void setRankOfWarnings(int rankOfWarnings) {
        this.rankOfWarnings = rankOfWarnings;
    }

    public float getWarningPercentage() {
        return warningPercentage;
    }

    public void setWarningPercentage(float warningPercentage) {
        this.warningPercentage = warningPercentage;
    }

    public String getWarrantedResult() {
        return warrantedResult;
    }

    @DataBoundSetter
    public void setWarrantedResult(String warrantedResult) {
        this.warrantedResult = warrantedResult;
    }

    @Extension
    public static final class DescriptorImpl extends ConditionDescriptor<PercentageOfWariningsIncreasedInCasesBellowCertainRankCondition> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return NAME;
        }
    }
}
