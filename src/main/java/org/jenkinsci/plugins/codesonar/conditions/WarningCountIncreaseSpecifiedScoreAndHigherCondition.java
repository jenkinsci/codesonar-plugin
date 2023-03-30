package org.jenkinsci.plugins.codesonar.conditions;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
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
 * @author andrius
 */
public class WarningCountIncreaseSpecifiedScoreAndHigherCondition extends Condition {
    private static final Logger LOGGER = Logger.getLogger(WarningCountIncreaseSpecifiedScoreAndHigherCondition.class.getName());

    private static final String NAME = "Warning count increase: specified score and higher";
    private static final String RESULT_DESCRIPTION_MESSAGE_FORMAT = "increase threshold={0,number,0.00}%, rank threshold={1,number,0} increase={2,number,0.00}% (count: greater than rank={3,number,0}, total={4,number,0})";

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
    public Result validate(CodeSonarBuildActionDTO current, CodeSonarBuildActionDTO previous, Launcher launcher, TaskListener listener, CodeSonarLogger csLogger) {
        if (current == null) {
            registerResult(csLogger, CURRENT_BUILD_DATA_NOT_AVAILABLE);
            return Result.SUCCESS;
        }
        
        Analysis analysis = current.getAnalysisActiveWarnings();
        
        // Going to produce build failure in the case of missing necessary information
        if(analysis == null) {
            LOGGER.log(Level.SEVERE, "\"analysisActiveWarnings\" data not found in persisted build.");
            registerResult(csLogger, CURRENT_BUILD_DATA_NOT_AVAILABLE);
            return Result.FAILURE;
        }

        int totalNumberOfWarnings = analysis.getWarnings().size();

        int severeWarnings = 0;
        List<Warning> warnings = analysis.getWarnings();
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
            registerResult(csLogger, RESULT_DESCRIPTION_MESSAGE_FORMAT, thresholdPercentage, rankOfWarnings, calculatedWarningPercentage, severeWarnings, totalNumberOfWarnings);
            return Result.fromString(warrantedResult);
        }
        
        registerResult(csLogger, RESULT_DESCRIPTION_MESSAGE_FORMAT, thresholdPercentage, rankOfWarnings, calculatedWarningPercentage, severeWarnings, totalNumberOfWarnings);
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
