package org.jenkinsci.plugins.codesonar.models.json;

public class CodeSonarUndefinedFunction {
    private String function;
    private long count;
    private String url;
    
    public String getFunction() {
        return function;
    }
    public void setFunction(String function) {
        this.function = function;
    }
    public long getCount() {
        return count;
    }
    public void setCount(long count) {
        this.count = count;
    }
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    @Override
    public String toString() {
        return "CodeSonarUndefinedFunction [function=" + function + ", count=" + count + ", url=" + url + "]";
    }

}
