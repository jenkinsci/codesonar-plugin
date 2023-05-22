package org.jenkinsci.plugins.codesonar.conditions;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.codesonar.CodeSonarLogger;
import org.jenkinsci.plugins.codesonar.api.CodeSonarHubAnalysisDataLoader;
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
 * @author andrius
 */
public class WarningCountIncreaseSpecifiedScoreAndHigherCondition extends Condition {
    private static final Logger LOGGER = Logger.getLogger(WarningCountIncreaseSpecifiedScoreAndHigherCondition.class.getName());

    private static final String NAME = "Warning count increase: specified score and higher";
    private static final String RESULT_DESCRIPTION_MESSAGE_FORMAT = "score={0,number,0}, threshold={1,number,0.00}%, increase={2,number,0.00}%";

    private int rankOfWarnings = 30;
    private String warningPercentage = String.valueOf(5.0f);
    private String warrantedResult = Result.UNSTABLE.toString();

    @DataBoundConstructor
    public WarningCountIncreaseSpecifiedScoreAndHigherCondition(int rankOfWarnings, String warningPercentage) {
        this.rankOfWarnings = rankOfWarnings;
        this.warningPercentage = warningPercentage;
    }
    
    public int getRankOfWarnings() {
        return rankOfWarnings;
    }

    public void setRankOfWarnings(int rankOfWarnings) {
        this.rankOfWarnings = rankOfWarnings;
    }

    public String getWarningPercentage() {
        return warningPercentage;
    }

    public void setWarningPercentage(String warningPercentage) {
        this.warningPercentage = warningPercentage;
    }

    public String getWarrantedResult() {
        return warrantedResult;
    }

    @DataBoundSetter
    public void setWarrantedResult(String warrantedResult) {
        this.warrantedResult = warrantedResult;
    }

    @Override
    public Result validate(CodeSonarHubAnalysisDataLoader current, CodeSonarHubAnalysisDataLoader previous, String visibilityFilter, String newVisibilityFilter, Launcher launcher, TaskListener listener, CodeSonarLogger csLogger) {
        if (current == null) {
            registerResult(csLogger, CURRENT_BUILD_DATA_NOT_AVAILABLE);
            return Result.SUCCESS;
        }
        
        if (previous == null) {
            registerResult(csLogger, PREVIOUS_BUILD_DATA_NOT_AVAILABLE);
            return Result.SUCCESS;
        }
        
        Long warningsAboveThresholdForCurrent = null;
        try {
            warningsAboveThresholdForCurrent = current.getNumberOfWarningsWithScoreAboveThreshold(rankOfWarnings);
        } catch (IOException e) {
            final String applicationMsg = "Error calling number of warnings above threshold on HUB API for current analysis.";
            LOGGER.log(Level.WARNING, applicationMsg);
            registerResult(csLogger, applicationMsg);
            csLogger.writeInfo("Exception: {0}%nStack Trace: {1}", e.getMessage(), Throwables.getStackTraceAsString(e));
            return Result.FAILURE;
        }

        if(warningsAboveThresholdForCurrent == null) {
            LOGGER.log(Level.SEVERE, "\"warningsAboveThresholdForCurrent\" not available.");
            registerResult(csLogger, DATA_LOADER_EMPTY_RESPONSE);
            return Result.FAILURE;          
        }
        
        Long warningsAboveThresholdForPrevious = null;
        try {
            warningsAboveThresholdForPrevious = previous.getNumberOfWarningsWithScoreAboveThreshold(rankOfWarnings);
        } catch (IOException e) {
            final String applicationMsg = "Error calling number of warnings above threshold on HUB API for previous analysis.";
            LOGGER.log(Level.WARNING, applicationMsg);
            registerResult(csLogger, applicationMsg);
            csLogger.writeInfo("Exception: {0}%nStack Trace: {1}", e.getMessage(), Throwables.getStackTraceAsString(e));
            return Result.FAILURE;
        }

        if(warningsAboveThresholdForPrevious == null) {
            LOGGER.log(Level.SEVERE, "\"warningsAboveThresholdForPrevious\" not available.");
            registerResult(csLogger, DATA_LOADER_EMPTY_RESPONSE);
            return Result.FAILURE;          
        }
        
        float thresholdPercentage = Float.parseFloat(warningPercentage);
        float calculatedWarningPercentage = ((float) warningsAboveThresholdForCurrent / warningsAboveThresholdForPrevious) * 100;;
        
        registerResult(csLogger, RESULT_DESCRIPTION_MESSAGE_FORMAT, rankOfWarnings, thresholdPercentage, calculatedWarningPercentage);
        
        if (calculatedWarningPercentage > thresholdPercentage) {
            return Result.fromString(warrantedResult);
        }
        
        return Result.SUCCESS;
    }

    @Symbol("warningCountIncreaseSpecifiedScoreAndHigher")
    @Extension
    public static final class DescriptorImpl extends ConditionDescriptor<WarningCountIncreaseSpecifiedScoreAndHigherCondition> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public @Nonnull String getDisplayName() {
            return NAME;
        }

        public FormValidation doCheckWarningPercentage(@QueryParameter("warningPercentage") String warningPercentage) {
            if (StringUtils.isBlank(warningPercentage)) {
                return FormValidation.error("Cannot be empty");
            }

            try {
                float v = Float.parseFloat(warningPercentage);

                if (v < 0) {
                    return FormValidation.error("The provided value must be zero or greater");
                }
            } catch (NumberFormatException numberFormatException) {
                return FormValidation.error("Not a valid decimal number");
            }

            return FormValidation.ok();
        }
    }
    
}
