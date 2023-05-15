package org.jenkinsci.plugins.codesonar.conditions;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.codesonar.CodeSonarLogger;
import org.jenkinsci.plugins.codesonar.api.CodeSonarDTOAnalysisDataLoader;
import org.jenkinsci.plugins.codesonar.api.CodeSonarHubAnalysisDataLoader;
import org.jenkinsci.plugins.codesonar.models.ProcedureMetric;
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
 *
 * @author Andrius
 */
public class ProcedureCyclomaticComplexityExceededCondition extends Condition {
    private static final Logger LOGGER = Logger.getLogger(ProcedureCyclomaticComplexityExceededCondition.class.getName());

    private static final String NAME = "Cyclomatic complexity";
    private static final String RESULT_DESCRIPTION_MESSAGE_FORMAT = "threshold={0,number,0}, complexity={1,number,0} (procedure: ''{2}'')";

    private int cyclomaticComplexityThreshold = 30;
    private String warrantedResult = Result.UNSTABLE.toString();

    @DataBoundConstructor
    public ProcedureCyclomaticComplexityExceededCondition(int maxCyclomaticComplexity) {
        this.cyclomaticComplexityThreshold = maxCyclomaticComplexity;
    }

    public int getCyclomaticComplexityThreshold() {
        return cyclomaticComplexityThreshold;
    }

    @DataBoundSetter
    public void setCyclomaticComplexityThreshold(int maxCyclomaticComplexity) {
        this.cyclomaticComplexityThreshold = maxCyclomaticComplexity;
    }

    public String getWarrantedResult() {
        return warrantedResult;
    }

    @DataBoundSetter
    public void setWarrantedResult(String warrantedResult) {
        this.warrantedResult = warrantedResult;
    }

    @Override
    public Result validate(CodeSonarHubAnalysisDataLoader current, CodeSonarDTOAnalysisDataLoader previous, String visibilityFilter, String newVisibilityFilter, Launcher launcher, TaskListener listener, CodeSonarLogger csLogger) {       
        if (current == null) {
            registerResult(csLogger, CURRENT_BUILD_DATA_NOT_AVAILABLE);
            return Result.SUCCESS;
        }

        ProcedureMetric procedureMetric = null;
        try {
            procedureMetric = current.getProcedureWithMaxCyclomaticComplexity();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error calling max cyclomatic complexity on HUB API. %nException: {0}%nStack Trace: {1}", new Object[] {e.getMessage(), Throwables.getStackTraceAsString(e)});
            return Result.FAILURE;
        }
        
        // Going to produce build failures in the case of missing necessary information
        if(procedureMetric == null) {
            LOGGER.log(Level.SEVERE, "\"procedureMetric\" not available.");
            registerResult(csLogger, DATA_LOADER_EMPTY_RESPONSE);
            return Result.FAILURE;
        }
        
        registerResult(csLogger, RESULT_DESCRIPTION_MESSAGE_FORMAT, cyclomaticComplexityThreshold, procedureMetric.getMetricCyclomaticComplexity(), procedureMetric.getProcedure());
        
        if (procedureMetric.getMetricCyclomaticComplexity() > cyclomaticComplexityThreshold) {
            return Result.fromString(warrantedResult);
        }
        
        return Result.SUCCESS;
    }
    
    @Symbol("cyclomaticComplexity")
    @Extension
    public static final class DescriptorImpl extends ConditionDescriptor<ProcedureCyclomaticComplexityExceededCondition> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public @Nonnull String getDisplayName() {
            return NAME;
        }

        public FormValidation doCheckMaxCyclomaticComplexity(@QueryParameter("maxCyclomaticComplexity") int maxCyclomaticComplexity) {
            if (maxCyclomaticComplexity < 0) {
                return FormValidation.error("Cannot be a negative number");
            }

            return FormValidation.ok();
        }
    }
}
