package org.jenkinsci.plugins.codesonar.conditions;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.codesonar.CodeSonarLogger;
import org.jenkinsci.plugins.codesonar.models.CodeSonarAnalysisData;
import org.jenkinsci.plugins.codesonar.models.CodeSonarWarningCount;
import org.jenkinsci.plugins.codesonar.services.CodeSonarCacheService;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.google.common.base.Throwables;

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
    public Result validate(CodeSonarAnalysisData current, CodeSonarAnalysisData previous, Launcher launcher, TaskListener listener, CodeSonarLogger csLogger, CodeSonarCacheService cacheService) {
        if (current == null) {
            registerResult(csLogger, CURRENT_BUILD_DATA_NOT_AVAILABLE);
            return Result.SUCCESS;
        }
        
        if(cacheService == null) {
            final String msg = "\"CacheService\" not available.";
            LOGGER.log(Level.SEVERE, msg);
            registerResult(csLogger, msg);
            return Result.FAILURE;
        }
        
        CodeSonarWarningCount warningsAboveThreshold = null;
        try {
            warningsAboveThreshold = cacheService.getWarningCountAbsoluteWithScoreAboveThreshold(current.getBaseHubUri(), current.getAnalysisId(), rankOfWarnings);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "failed to parse JSON response. %nException: {0}%nStack Trace: {1}", new Object[] {e.getMessage(), Throwables.getStackTraceAsString(e)});
            return Result.FAILURE;
        }

        if(warningsAboveThreshold == null) {
            LOGGER.log(Level.INFO, "Warning Search service returned an empty response");
            return Result.FAILURE;          
        }
        
        registerResult(csLogger, RESULT_DESCRIPTION_MESSAGE_FORMAT, rankOfWarnings, warningCountThreshold, warningsAboveThreshold.getScoreAboveThresholdCounter());
        
        if (warningsAboveThreshold.getScoreAboveThresholdCounter() > warningCountThreshold) {
            return Result.fromString(warrantedResult);
        }

        return Result.SUCCESS;
        
        /*
         * Temporarily commented previous implementation, probably until
         * when backward compatibility theme will be addressed.
         */
        
        /*
        Analysis currentAnalysisActiveWarnings = current.getAnalysisActiveWarnings();
        
        // Going to produce build failure in the case of missing necessary information
        if(currentAnalysisActiveWarnings == null) {
            LOGGER.log(Level.SEVERE, "\"analysisActiveWarnings\" data not found.");
            registerResult(csLogger, CURRENT_BUILD_DATA_NOT_AVAILABLE);
            return Result.FAILURE;
        }

        int severeWarnings = 0;
        List<Warning> warnings = currentAnalysisActiveWarnings.getWarnings();
        for (Warning warning : warnings) {
            if (warning.getScore() >= rankOfWarnings) {
                severeWarnings++;
            }
        }

        if (severeWarnings > warningCountThreshold) {
            registerResult(csLogger, RESULT_DESCRIPTION_MESSAGE_FORMAT, rankOfWarnings, warningCountThreshold, severeWarnings);
            return Result.fromString(warrantedResult);
        }

        registerResult(csLogger, RESULT_DESCRIPTION_MESSAGE_FORMAT, rankOfWarnings, warningCountThreshold, severeWarnings);
        return Result.SUCCESS;
        */
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
