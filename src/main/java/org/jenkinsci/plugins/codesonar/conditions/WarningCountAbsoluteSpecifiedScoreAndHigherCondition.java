package org.jenkinsci.plugins.codesonar.conditions;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.codesonar.CodeSonarLogger;
import org.jenkinsci.plugins.codesonar.models.CodeSonarBuildActionDTO;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.jenkinsci.plugins.codesonar.models.analysis.Warning;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.util.FormValidation;

/**
 * @author oehc
 */
public class WarningCountAbsoluteSpecifiedScoreAndHigherCondition extends Condition {
    private static final Logger LOGGER = Logger.getLogger(WarningCountAbsoluteSpecifiedScoreAndHigherCondition.class.getName());

    private static final String NAME = "Warning count absolute: specified score and higher";

    private int rankOfWarnings = 30;
    private int warningCountThreshold = 20;
    private String warrantedResult = Result.UNSTABLE.toString();

    @DataBoundConstructor
    public WarningCountAbsoluteSpecifiedScoreAndHigherCondition(int rankOfWarnings, int warningCountThreshold) {
        this.rankOfWarnings = rankOfWarnings;
        this.warningCountThreshold = warningCountThreshold;
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

    @Override
    public Result validate(CodeSonarBuildActionDTO current, CodeSonarBuildActionDTO previous, Launcher launcher, TaskListener listener, CodeSonarLogger csLogger) {
        if (current == null) {
            registerResult(csLogger, CURRENT_BUILD_DATA_NOT_AVAILABLE);
            return Result.SUCCESS;
        }

        Analysis analysis = current.getAnalysisActiveWarnings();
        
        // Going to produce build failure in the case of missing necessary information
        if(analysis == null) {
            LOGGER.log(Level.SEVERE, "\"analysisActiveWarnings\" data not found in persisted build.");
            return Result.FAILURE;
        }

        int severeWarnings = 0;
        List<Warning> warnings = analysis.getWarnings();
        for (Warning warning : warnings) {
            if (warning.getScore() >= rankOfWarnings) {
                severeWarnings++;
            }
        }

        if (severeWarnings > warningCountThreshold) {
            registerResult(csLogger, "More than {0,number,0} warnings with a score of at least {1,number,0} ({2,number,0})", warningCountThreshold, rankOfWarnings, severeWarnings);
            return Result.fromString(warrantedResult);
        }

        registerResult(csLogger, "At most {0,number,0} warnings with a score of at least {1,number,0} ({2,number,0})", warningCountThreshold, rankOfWarnings, severeWarnings);
        return Result.SUCCESS;
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
