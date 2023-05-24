package org.jenkinsci.plugins.codesonar;

import com.google.common.base.Throwables;

public class CodeSonarJsonSyntaxException extends CodeSonarPluginException {
    private static final String MESSAGE = "Failed to parse JSON response. %nException: {0}%nStack Trace: {1}";

    public CodeSonarJsonSyntaxException(Exception e) {
        super(MESSAGE, e.getMessage(), Throwables.getStackTraceAsString(e));
    }
}
