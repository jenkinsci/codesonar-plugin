package org.jenkinsci.plugins.codesonar;

import java.io.IOException;

public class CodeSonarPluginException extends IOException {
    private Object[] args;
    
    public CodeSonarPluginException() {
    }

    public CodeSonarPluginException(String message, Object...args) {
        super(message);
        this.args = args;
    }
    
    public CodeSonarPluginException(String message, Throwable cause, Object...args) {
        super(message, cause);
        this.args = args;
    }
    
    @Override
    public String getMessage() {
        return CodeSonarLogger.formatMessage(super.getMessage(), args);
    }

}
