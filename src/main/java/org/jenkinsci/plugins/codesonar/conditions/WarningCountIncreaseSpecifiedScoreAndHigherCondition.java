package org.jenkinsci.plugins.codesonar.conditions;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
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
public class WarningCountIncreaseSpecifiedScoreAndHigherCondition extends Condition {

    private static final String NAME = "Warning count increase: specified score and higher";

    private int rankOfWarnings = 30;
    private float warningPercentage = 5.0f;
    private String warrantedResult = Result.UNSTABLE.toString();

    @DataBoundConstructor
    public WarningCountIncreaseSpecifiedScoreAndHigherCondition(int rankOfWarnings, float warningPercentage) {
        this.rankOfWarnings = rankOfWarnings;
        this.warningPercentage = warningPercentage;
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
        
        listener.getLogger().println(String.format("[codesonar debug] specified score of warnings: %s", rankOfWarnings));
        listener.getLogger().println(String.format("[codesonar debug] specified warning precentage: %s", warningPercentage));

        Analysis analysis = buildActionDTO.getAnalysisActiveWarnings();
        
        int totalNumberOfWarnings = analysis.getWarnings().size();
        
        listener.getLogger().println(String.format("[codesonar debug] total number of active warnings: %s", totalNumberOfWarnings));
        
        float severeWarnings = 0.0f;
        List<Warning> warnings = analysis.getWarnings();
        for (Warning warning : warnings) {
            if (warning.getScore() > rankOfWarnings) {
                severeWarnings++;
            }
        }
        
        listener.getLogger().println(String.format("[codesonar debug] number of warning found with score above the specified limit: %s", severeWarnings));

        float calculatedWarningPercentage = (severeWarnings / totalNumberOfWarnings) * 100;
        
        listener.getLogger().println(String.format("[codesonar debug] calculated warning precentage: %s", calculatedWarningPercentage));
        
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
    public static final class DescriptorImpl extends ConditionDescriptor<WarningCountIncreaseSpecifiedScoreAndHigherCondition> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return NAME;
        }
    }
}
