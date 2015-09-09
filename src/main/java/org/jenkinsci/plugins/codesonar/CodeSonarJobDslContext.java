package org.jenkinsci.plugins.codesonar;

import hudson.model.Result;
import java.util.ArrayList;
import java.util.List;
import javaposse.jobdsl.dsl.Context;
import org.jenkinsci.plugins.codesonar.conditions.Condition;
import org.jenkinsci.plugins.codesonar.conditions.NewWarningsIncreasedByPercentageCondition;
import org.jenkinsci.plugins.codesonar.conditions.PercentageOfWariningsIncreasedInCasesBellowCertainRankCondition;
import org.jenkinsci.plugins.codesonar.conditions.ProcedureCyclomaticComplexityExceededCondition;
import org.jenkinsci.plugins.codesonar.conditions.RedAlertLimitCondition;
import org.jenkinsci.plugins.codesonar.conditions.WarningCountIncreasedByPercentageCondition;
import org.jenkinsci.plugins.codesonar.conditions.YellowAlertLimitCondition;

class CodeSonarJobDslContext implements Context {

    List<Condition> conditions = new ArrayList<>();

    public void cyclomaticComplexity(int max, boolean fail) {
        ProcedureCyclomaticComplexityExceededCondition condition = new ProcedureCyclomaticComplexityExceededCondition(max);
        if (fail) {
            condition.setWarrantedResult(Result.FAILURE.toString());
        } else {
            condition.setWarrantedResult(Result.UNSTABLE.toString());
        }
        conditions.add(condition);
    }

    public void redAlert(int max, boolean fail) {
        RedAlertLimitCondition condition = new RedAlertLimitCondition(max);
        if (fail) {
            condition.setWarrantedResult(Result.FAILURE.toString());
        } else {
            condition.setWarrantedResult(Result.UNSTABLE.toString());
        }
        conditions.add(condition);
    }

    public void yellowAlert(int max, boolean fail) {
        YellowAlertLimitCondition condition = new YellowAlertLimitCondition(max);
        if (fail) {
            condition.setWarrantedResult(Result.FAILURE.toString());
        } else {
            condition.setWarrantedResult(Result.UNSTABLE.toString());
        }
        conditions.add(condition);
    }

    public void newWarningCountIncrease(float percentage, boolean fail) {
        NewWarningsIncreasedByPercentageCondition condition = new NewWarningsIncreasedByPercentageCondition(percentage);
        if (fail) {
            condition.setWarrantedResult(Result.FAILURE.toString());
        } else {
            condition.setWarrantedResult(Result.UNSTABLE.toString());
        }
        conditions.add(condition);
    }

    public void overallWarningCountIncrease(float percentage, boolean fail) {
        WarningCountIncreasedByPercentageCondition condition = new WarningCountIncreasedByPercentageCondition(percentage);
        if (fail) {
            condition.setWarrantedResult(Result.FAILURE.toString());
        } else {
            condition.setWarrantedResult(Result.UNSTABLE.toString());
        }
        conditions.add(condition);
    }

    public void rankedWarningCountIncrease(int rank, float percentage, boolean fail) {
        PercentageOfWariningsIncreasedInCasesBellowCertainRankCondition condition = new PercentageOfWariningsIncreasedInCasesBellowCertainRankCondition(rank, percentage);
        if (fail) {
            condition.setWarrantedResult(Result.FAILURE.toString());
        } else {
            condition.setWarrantedResult(Result.UNSTABLE.toString());
        }
        conditions.add(condition);
    }
}
