package org.jenkinsci.plugins.codesonar.models;

/**
 * @author aseno
 *
 */
public class CodeSonarWarningSearchData {
    private int count;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "CodeSonarWarningSearchData [count=" + count + "]";
    }
    
}
