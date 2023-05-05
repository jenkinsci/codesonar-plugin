package org.jenkinsci.plugins.codesonar.conditions;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
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
    public Result validate(CodeSonarAnalysisData current, CodeSonarAnalysisData previous, Launcher launcher, TaskListener listener, CodeSonarLogger csLogger) {
        if (current == null) {
            registerResult(csLogger, CURRENT_BUILD_DATA_NOT_AVAILABLE);
            return Result.SUCCESS;
        }
        
        if (previous == null) {
            registerResult(csLogger, PREVIOUS_BUILD_DATA_NOT_AVAILABLE);
            return Result.SUCCESS;
        }
        
        CodeSonarCacheService cacheService = CodeSonarCacheService.getInstance();
        
        if(cacheService == null) {
            final String msg = "\"CacheService\" not available.";
            LOGGER.log(Level.SEVERE, msg);
            registerResult(csLogger, msg);
            return Result.FAILURE;
        }
        
        CodeSonarWarningCount warningsAboveThresholdForCurrent = null;
        try {
            warningsAboveThresholdForCurrent = cacheService.getWarningCountIncreaseWithScoreAboveThreshold(current.getBaseHubUri(), current.getAnalysisId(), rankOfWarnings);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "failed to parse JSON response for current build. %nException: {0}%nStack Trace: {1}", new Object[] {e.getMessage(), Throwables.getStackTraceAsString(e)});
            return Result.FAILURE;
        }

        if(warningsAboveThresholdForCurrent == null) {
            LOGGER.log(Level.INFO, "Warning Search service returned an empty response for current build");
            return Result.FAILURE;          
        }
        
        CodeSonarWarningCount warningsAboveThresholdForPrevious = null;
        try {
            warningsAboveThresholdForPrevious = cacheService.getWarningCountIncreaseWithScoreAboveThreshold(previous.getBaseHubUri(), previous.getAnalysisId(), rankOfWarnings);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "failed to parse JSON response for previous build. %nException: {0}%nStack Trace: {1}", new Object[] {e.getMessage(), Throwables.getStackTraceAsString(e)});
            return Result.FAILURE;
        }

        if(warningsAboveThresholdForPrevious == null) {
            LOGGER.log(Level.INFO, "Warning Search service returned an empty response for previous build");
            return Result.FAILURE;          
        }
        
        float thresholdPercentage = Float.parseFloat(warningPercentage);
        float calculatedWarningPercentage = ((float) warningsAboveThresholdForCurrent.getScoreAboveThresholdCounter() / warningsAboveThresholdForPrevious.getScoreAboveThresholdCounter()) * 100;;
        
        registerResult(csLogger, RESULT_DESCRIPTION_MESSAGE_FORMAT, rankOfWarnings, thresholdPercentage, calculatedWarningPercentage);
        
        if (calculatedWarningPercentage > thresholdPercentage) {
            return Result.fromString(warrantedResult);
        }
        
        return Result.SUCCESS;
        
        /*
        Analysis currentAnalysisActiveWarnings = current.getAnalysisActiveWarnings();
        
        // Going to produce build failure in the case of missing necessary information
        if(currentAnalysisActiveWarnings == null) {
            LOGGER.log(Level.SEVERE, "\"analysisActiveWarnings\" data not found.");
            registerResult(csLogger, CURRENT_BUILD_DATA_NOT_AVAILABLE);
            return Result.FAILURE;
        }

        int totalNumberOfWarnings = currentAnalysisActiveWarnings.getWarnings().size();

        int severeWarnings = 0;
        List<Warning> warnings = currentAnalysisActiveWarnings.getWarnings();
        for (Warning warning : warnings) {
            if (warning.getScore() > rankOfWarnings) {
                severeWarnings++;
            }
        }
        
        float calculatedWarningPercentage;
        //If there are no warnings, redefine percentage of new warnings
        if(totalNumberOfWarnings == 0) {
            calculatedWarningPercentage = severeWarnings > 0 ? 100f : 0f;
            LOGGER.log(Level.INFO, "no warnings found, forcing severe warning percentage to {0,number,0.00}%", calculatedWarningPercentage);
        } else {
            calculatedWarningPercentage = ((float) severeWarnings / totalNumberOfWarnings) * 100;
            LOGGER.log(Level.INFO, "severe warnings percentage = {0,number,0.00}%", calculatedWarningPercentage);
        }

        float thresholdPercentage = Float.parseFloat(warningPercentage);
        if (calculatedWarningPercentage > thresholdPercentage) {
            registerResult(csLogger, RESULT_DESCRIPTION_MESSAGE_FORMAT, rankOfWarnings, thresholdPercentage, calculatedWarningPercentage);
            return Result.fromString(warrantedResult);
        }
        
        registerResult(csLogger, RESULT_DESCRIPTION_MESSAGE_FORMAT, rankOfWarnings, thresholdPercentage, calculatedWarningPercentage);
        return Result.SUCCESS;
        */
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
