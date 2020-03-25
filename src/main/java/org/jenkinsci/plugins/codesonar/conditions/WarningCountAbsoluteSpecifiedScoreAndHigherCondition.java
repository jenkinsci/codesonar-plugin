package org.jenkinsci.plugins.codesonar.conditions;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.Result;

import java.util.List;

import hudson.util.FormValidation;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.codesonar.CodeSonarBuildAction;
import org.jenkinsci.plugins.codesonar.models.CodeSonarBuildActionDTO;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.jenkinsci.plugins.codesonar.models.analysis.Warning;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;

/**
 * @author oehc
 */
public class WarningCountAbsoluteSpecifiedScoreAndHigherCondition extends Condition {

    private static final String NAME = "Warning count absolute: specified score and higher";

    private int rankOfWarnings = 30;
    private int warningCountThreshold = 20;
    private String warrantedResult = Result.UNSTABLE.toString();

    @DataBoundConstructor
    public WarningCountAbsoluteSpecifiedScoreAndHigherCondition(int rankOfWarnings, int warningCountThreshold) {
        this.rankOfWarnings = rankOfWarnings;
        this.warningCountThreshold = warningCountThreshold;
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

        Analysis analysis = buildActionDTO.getAnalysisActiveWarnings();

        int severeWarnings = 0;
        List<Warning> warnings = analysis.getWarnings();
        for (Warning warning : warnings) {
            if (warning.getScore() >= rankOfWarnings) {
                severeWarnings++;
            }
        }

        if (severeWarnings > warningCountThreshold) {
            return Result.fromString(warrantedResult);
        }

        return Result.SUCCESS;
    }

    public int getRankOfWarnings() {
        return rankOfWarnings;
    }

    public void setRankOfWarnings(int rankOfWarnings) {
        this.rankOfWarnings = rankOfWarnings;
    }

    public int getWarningCountThreshold() {
        return warningCountThreshold;
    }

    public void setWarningCountThreshold(int warningCountThreshold) {
        this.warningCountThreshold = warningCountThreshold;
    }

    public String getWarrantedResult() {
        return warrantedResult;
    }

    @DataBoundSetter
    public void setWarrantedResult(String warrantedResult) {
        this.warrantedResult = warrantedResult;
    }

    @Symbol("warningCountAbsoluteSpecifiedScoreAndHigher")
    @Extension
    public static final class DescriptorImpl extends ConditionDescriptor<WarningCountAbsoluteSpecifiedScoreAndHigherCondition> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public @Nonnull String getDisplayName() {
            return NAME;
        }

        public FormValidation doCheckWarningCountThreshold(@QueryParameter("warningCountThreshold") int warningCountThreshold) {

            if (warningCountThreshold < 0) {
                return FormValidation.error("The provided value must be zero or greater");
            }

            return FormValidation.ok();
        }
    }
}
