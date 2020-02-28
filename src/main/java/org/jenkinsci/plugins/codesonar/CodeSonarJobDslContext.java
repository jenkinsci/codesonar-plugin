package org.jenkinsci.plugins.codesonar;

import hudson.model.Result;
import java.util.ArrayList;
import java.util.List;
import javaposse.jobdsl.dsl.Context;
import org.jenkinsci.plugins.codesonar.conditions.Condition;
import org.jenkinsci.plugins.codesonar.conditions.NewWarningsIncreasedByPercentageCondition;
import org.jenkinsci.plugins.codesonar.conditions.WarningCountIncreaseSpecifiedScoreAndHigherCondition;
import org.jenkinsci.plugins.codesonar.conditions.ProcedureCyclomaticComplexityExceededCondition;
import org.jenkinsci.plugins.codesonar.conditions.RedAlertLimitCondition;
import org.jenkinsci.plugins.codesonar.conditions.WarningCountIncreaseOverallCondition;
import org.jenkinsci.plugins.codesonar.conditions.YellowAlertLimitCondition;
import org.jenkinsci.plugins.codesonar.conditions.WarningCountAbsoluteSpecifiedScoreAndHigherCondition;

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
        NewWarningsIncreasedByPercentageCondition condition = new NewWarningsIncreasedByPercentageCondition(Float.toString(percentage));
        if (fail) {
            condition.setWarrantedResult(Result.FAILURE.toString());
        } else {
            condition.setWarrantedResult(Result.UNSTABLE.toString());
        }
        conditions.add(condition);
    }

    public void overallWarningCountIncrease(float percentage, boolean fail) {
        WarningCountIncreaseOverallCondition condition = new WarningCountIncreaseOverallCondition(Float.toString(percentage));
        if (fail) {
            condition.setWarrantedResult(Result.FAILURE.toString());
        } else {
            condition.setWarrantedResult(Result.UNSTABLE.toString());
        }
        conditions.add(condition);
    }

    public void rankedWarningCountIncrease(int rank, float percentage, boolean fail) {
        WarningCountIncreaseSpecifiedScoreAndHigherCondition condition = new WarningCountIncreaseSpecifiedScoreAndHigherCondition(rank, Float.toString(percentage));
        if (fail) {
            condition.setWarrantedResult(Result.FAILURE.toString());
        } else {
            condition.setWarrantedResult(Result.UNSTABLE.toString());
        }
        conditions.add(condition);
    }

    public void absoluteWarningCount(int rank, int count, boolean fail) {
        WarningCountAbsoluteSpecifiedScoreAndHigherCondition condition = new WarningCountAbsoluteSpecifiedScoreAndHigherCondition(rank, count);
        if (fail) {
            condition.setWarrantedResult(Result.FAILURE.toString());
        } else {
            condition.setWarrantedResult(Result.UNSTABLE.toString());
        }
        conditions.add(condition);
    }
}
