package org.jenkinsci.plugins.codesonar.conditions;

import static com.google.common.base.Preconditions.*;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import org.jenkinsci.plugins.codesonar.CodeSonarBuildAction;
import org.jenkinsci.plugins.codesonar.models.Analysis;
import org.jenkinsci.plugins.codesonar.models.Warning;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author andrius
 */
public class PercentageOfWariningsIncreasedInCasesBellowCertainRank extends Condition {

    private static final String NAME = "Warning percentage bellow a certain rank";

    private int rankOfWarnings;
    private float warningPercentage;
    private Result warantedResult;
    
    @DataBoundConstructor
    public PercentageOfWariningsIncreasedInCasesBellowCertainRank(int rankOfWarnings, float warningPercentage) {
        this.rankOfWarnings = rankOfWarnings;
        this.warningPercentage = warningPercentage;
    }

    @Override
    public Result validate(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        CodeSonarBuildAction buildAction = build.getAction(CodeSonarBuildAction.class);
        checkState(buildAction != null, "CondeSonar build action not present");
        
        Analysis analysis = buildAction.getAnalysis();
        checkState(analysis != null, "CodeSonar build action not present");
        
        int totalNumberOfWarnings = analysis.getWarnings().size();

        float severeWarnings = 0.0f;
        for (Warning warning : analysis.getWarnings()) {
            if (warning.getRank() < rankOfWarnings) {
                severeWarnings++;
            }
        }

        float calculatedWarningPercentage = (severeWarnings / totalNumberOfWarnings) * 100;

        System.out.println("----------------calculatedWarningPercentage----------------------");
        System.out.println(calculatedWarningPercentage);
        System.out.println("--------------------------------------");
        
        if (calculatedWarningPercentage > warningPercentage) {
            build.setResult(Result.FAILURE);
        }
        
        return Result.SUCCESS;
    }

    public int getRankOfWarnings() {
        return rankOfWarnings;
    }

    public void setRankOfWarnings(int rankOfWarnings) {
        this.rankOfWarnings = rankOfWarnings;
    }

    public float getWarningPercentageIncrease() {
        return warningPercentage;
    }

    public void setWarningPercentageIncrease(float warningPercentageIncrease) {
        this.warningPercentage = warningPercentageIncrease;
    }

    @Extension
    public static final class DescriptorImpl extends ConditionDescriptor<PercentageOfWariningsIncreasedInCasesBellowCertainRank> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return NAME;
        }
    }
}
