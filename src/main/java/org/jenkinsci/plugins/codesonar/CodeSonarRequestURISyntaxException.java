package org.jenkinsci.plugins.codesonar;

import com.google.common.base.Throwables;

public class CodeSonarRequestURISyntaxException extends CodeSonarPluginException {
    private static final String MESSAGE = "Request URI for API contains a syntax error. %nException: {0}%nStack Trace: {1}";

    public CodeSonarRequestURISyntaxException(Exception e) {
        super(MESSAGE, e.getMessage(), Throwables.getStackTraceAsString(e));
    }
}
