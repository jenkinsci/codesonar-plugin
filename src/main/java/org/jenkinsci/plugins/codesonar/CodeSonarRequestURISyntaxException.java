package org.jenkinsci.plugins.codesonar;

public class CodeSonarRequestURISyntaxException extends CodeSonarPluginException {
    private static final String MESSAGE = "Request URI for API contains a syntax error.";

    public CodeSonarRequestURISyntaxException(Exception e) {
        super(MESSAGE, e);
    }
}
