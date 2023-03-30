package org.jenkinsci.plugins.codesonar.conditions;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.codesonar.CodeSonarLogger;
import org.jenkinsci.plugins.codesonar.models.CodeSonarBuildActionDTO;
import org.jenkinsci.plugins.codesonar.models.Metric;
import org.jenkinsci.plugins.codesonar.models.procedures.ProcedureRow;
import org.jenkinsci.plugins.codesonar.models.procedures.Procedures;
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
public class ProcedureCyclomaticComplexityExceededCondition extends Condition {
    private static final Logger LOGGER = Logger.getLogger(ProcedureCyclomaticComplexityExceededCondition.class.getName());

    private static final String NAME = "Cyclomatic complexity";
    private static final String RESULT_DESCRIPTION_MESSAGE_FORMAT = "cyclomatic complexity threshold={0,number,0} (count: cyclomatic complexity={1,number,0}, procedure: {2})";

    private int maxCyclomaticComplexity = 30;
    private String warrantedResult = Result.UNSTABLE.toString();

    @DataBoundConstructor
    public ProcedureCyclomaticComplexityExceededCondition(int maxCyclomaticComplexity) {
        this.maxCyclomaticComplexity = maxCyclomaticComplexity;
    }

    public int getMaxCyclomaticComplexity() {
        return maxCyclomaticComplexity;
    }

    @DataBoundSetter
    public void setMaxCyclomaticComplexity(int maxCyclomaticComplexity) {
        this.maxCyclomaticComplexity = maxCyclomaticComplexity;
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

        Procedures procedures = current.getProcedures();
        // Going to produce build failure in the case of missing necessary information
        if(procedures == null) {
            LOGGER.log(Level.SEVERE, "\"procedures\" data not found in persisted build.");
            registerResult(csLogger, CURRENT_BUILD_DATA_NOT_AVAILABLE);
            return Result.FAILURE;
        }
        List<ProcedureRow> procedureRows = procedures.getProcedureRows();
        for (ProcedureRow procedureRow : procedureRows) {
            Metric cyclomaticComplexityMetric = procedureRow.getMetricByName("Cyclomatic Complexity");

            int value = Integer.parseInt(cyclomaticComplexityMetric.getValue());
            if (value > maxCyclomaticComplexity) {
                registerResult(csLogger, RESULT_DESCRIPTION_MESSAGE_FORMAT, maxCyclomaticComplexity, value, procedureRow.getProcedure());
                return Result.fromString(warrantedResult);
            }
        }
        registerResult(csLogger, "All cyclomatic complexity values within threshold {0,number,0}", maxCyclomaticComplexity);
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
