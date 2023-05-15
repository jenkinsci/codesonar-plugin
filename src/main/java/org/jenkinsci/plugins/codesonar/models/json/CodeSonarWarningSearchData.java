package org.jenkinsci.plugins.codesonar.models.json;

/**
 * @author aseno
 *
 */
public class CodeSonarWarningSearchData {
    private long count;

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "CodeSonarWarningSearchData [count=" + count + "]";
    }
    
}
