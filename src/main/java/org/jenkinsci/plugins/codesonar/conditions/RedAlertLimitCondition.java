package org.jenkinsci.plugins.codesonar.conditions;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import java.util.List;
import org.jenkinsci.plugins.codesonar.CodeSonarBuildAction;
import org.jenkinsci.plugins.codesonar.models.analysis.Alert;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 *
 * @author Andrius
 */
public class RedAlertLimitCondition extends Condition {
    private static final String NAME = "Red alert limit";
   
    private int alertLimit = 0;
    private String warrantedResult = Result.UNSTABLE.toString();

    @DataBoundConstructor
    public RedAlertLimitCondition(int alertLimit) {
        this.alertLimit = alertLimit;
    }
    
    @Override
    public Result validate(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        CodeSonarBuildAction buildAction = build.getAction(CodeSonarBuildAction.class);
        if (buildAction == null) {
            return Result.SUCCESS;
        }

        Analysis analysis = buildAction.getAnalysis();
        if (analysis == null) {
            return Result.SUCCESS;
        }
        
        List<Alert> redAlerts = analysis.getRedAlerts();
        if (redAlerts.size() > alertLimit) {
            Result result = Result.fromString(warrantedResult);
            return result;
        }

        return Result.SUCCESS;
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

    @Extension
    public static final class DescriptorImpl extends ConditionDescriptor<RedAlertLimitCondition> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return NAME;
        }
    }
}
