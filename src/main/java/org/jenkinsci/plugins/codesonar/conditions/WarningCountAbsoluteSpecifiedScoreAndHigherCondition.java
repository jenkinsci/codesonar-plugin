package org.jenkinsci.plugins.codesonar.conditions;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

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

/**
 * @author oehc
 */
public class WarningCountAbsoluteSpecifiedScoreAndHigherCondition extends Condition {
    private static final Logger LOGGER = Logger.getLogger(WarningCountAbsoluteSpecifiedScoreAndHigherCondition.class.getName());

    private static final String NAME = "Warning count absolute: specified score and higher";
    private static final String RESULT_DESCRIPTION_MESSAGE_FORMAT = "rank={0,number,0}, threshold={1,number,0}, count={2,number,0}";

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
    public Result validate(CodeSonarHubAnalysisDataLoader current, CodeSonarHubAnalysisDataLoader previous, Launcher launcher, TaskListener listener, CodeSonarLogger csLogger) throws CodeSonarPluginException {
        if (current == null) {
            registerResult(csLogger, CURRENT_BUILD_DATA_NOT_AVAILABLE);
            return Result.SUCCESS;
        }
        
        long warningsAboveThreshold = current.getNumberOfWarningsWithScoreAboveThreshold(rankOfWarnings);

        registerResult(csLogger, RESULT_DESCRIPTION_MESSAGE_FORMAT, rankOfWarnings, warningCountThreshold, warningsAboveThreshold);
        
        if (warningsAboveThreshold > warningCountThreshold) {
            return Result.fromString(warrantedResult);
        }

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
