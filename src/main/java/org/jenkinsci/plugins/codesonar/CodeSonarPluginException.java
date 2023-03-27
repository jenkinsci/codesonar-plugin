package org.jenkinsci.plugins.codesonar;

import hudson.AbortException;

public class CodeSonarPluginException extends AbortException {
    private Object[] args;
    
    public CodeSonarPluginException() {
    }

    public CodeSonarPluginException(String message, Object...args) {
        super(message);
        this.args = args;
    }
    
    @Override
    public String getMessage() {
        return CodeSonarLogger.formatMessage(super.getMessage(), args);
    }
    
}
