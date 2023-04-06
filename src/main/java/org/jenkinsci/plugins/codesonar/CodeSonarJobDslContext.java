package org.jenkinsci.plugins.codesonar;

import java.util.ArrayList;
import java.util.List;

import org.jenkinsci.plugins.codesonar.conditions.Condition;
import org.jenkinsci.plugins.codesonar.conditions.NewWarningsIncreasedByPercentageCondition;
import org.jenkinsci.plugins.codesonar.conditions.ProcedureCyclomaticComplexityExceededCondition;
import org.jenkinsci.plugins.codesonar.conditions.RedAlertLimitCondition;
import org.jenkinsci.plugins.codesonar.conditions.WarningCountAbsoluteSpecifiedScoreAndHigherCondition;
import org.jenkinsci.plugins.codesonar.conditions.WarningCountIncreaseOverallCondition;
import org.jenkinsci.plugins.codesonar.conditions.WarningCountIncreaseSpecifiedScoreAndHigherCondition;
import org.jenkinsci.plugins.codesonar.conditions.YellowAlertLimitCondition;

import hudson.model.Result;
import javaposse.jobdsl.dsl.Context;

/**
Provides for optional parameters in a CodeSonar plugin job-dsl declaration implemented by CodeSonarJobDslExtension.
*/
public class CodeSonarJobDslContext implements Context {

    private List<Condition> conditions = new ArrayList<>();
    private int socketTimeoutMS = -1;
    private String sslCertificateCredentialId;
    private String projectFile;
    private String newWarningsFilter;
    
    public void socketTimeoutMS(int value) {
        socketTimeoutMS = value;
    }
    
    public void sslCertificateCredentialId(String value) {
        sslCertificateCredentialId = value;
    }
    
    public void projectFile(String value) {
        projectFile = value;
    }
    
    public void newWarningsFilter(String value) {
        newWarningsFilter = value;
    }

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

    public List<Condition> getConditions() {
        return conditions;
    }

    public int getSocketTimeoutMS() {
        return socketTimeoutMS;
    }

    public String getSslCertificateCredentialId() {
        return sslCertificateCredentialId;
    }

    public String getProjectFile() {
        return projectFile;
    }

    public String getNewWarningsFilter() {
        return newWarningsFilter;
    }
}
