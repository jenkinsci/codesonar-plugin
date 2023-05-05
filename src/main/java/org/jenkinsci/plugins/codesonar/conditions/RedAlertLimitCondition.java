package org.jenkinsci.plugins.codesonar.conditions;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.codesonar.CodeSonarLogger;
import org.jenkinsci.plugins.codesonar.models.CodeSonarAlertFrequencies;
import org.jenkinsci.plugins.codesonar.models.CodeSonarAnalysisData;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.util.FormValidation;

/**
 *
 * @author Andrius
 */
public class RedAlertLimitCondition extends Condition {
    private static final Logger LOGGER = Logger.getLogger(RedAlertLimitCondition.class.getName());

    private static final String NAME = "Red alerts";
    private static final String RESULT_DESCRIPTION_MESSAGE_FORMAT = "threshold={0,number,0}, count={1,number,0}";

    private int alertLimit = 1;
    private String warrantedResult = Result.UNSTABLE.toString();

    @DataBoundConstructor
    public RedAlertLimitCondition(int alertLimit) {
        this.alertLimit = alertLimit;
    }

    public int getAlertLimit() {
        return alertLimit;
    }

    @DataBoundSetter
    public void setAlertLimit(int alertLimit) {
        this.alertLimit = alertLimit;
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
        
        CodeSonarAlertFrequencies alertFrequencies = current.getAlertFrequencies();
        
        // Going to produce build failure in the case of missing necessary information
        if(alertFrequencies == null) {
            LOGGER.log(Level.SEVERE, "\"alertFrequencies\" data not found.");
            registerResult(csLogger, CURRENT_BUILD_DATA_NOT_AVAILABLE);
            return Result.FAILURE;
        }

        int redAlerts = alertFrequencies.getRed();
        
        registerResult(csLogger, RESULT_DESCRIPTION_MESSAGE_FORMAT, alertLimit, redAlerts);
        
        if (redAlerts > alertLimit) {
            return Result.fromString(warrantedResult);
        }
        
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
        
        List<Alert> redAlerts = currentAnalysisActiveWarnings.getRedAlerts();
        if (redAlerts.size() > alertLimit) {
            registerResult(csLogger, RESULT_DESCRIPTION_MESSAGE_FORMAT, alertLimit, redAlerts.size());
            return Result.fromString(warrantedResult);
        }

        registerResult(csLogger, RESULT_DESCRIPTION_MESSAGE_FORMAT, alertLimit, redAlerts.size());
        */
        return Result.SUCCESS;
    }

    @Symbol("redAlerts")
    @Extension
    public static final class DescriptorImpl extends ConditionDescriptor<RedAlertLimitCondition> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public @Nonnull String getDisplayName() {
            return NAME;
        }

        public FormValidation doCheckAlertLimit(@QueryParameter("alertLimit") int alertLimit) {
            if (alertLimit < 0) {
                return FormValidation.error("Cannot be a negative number");
            }

            return FormValidation.ok();
        }
    }
}
