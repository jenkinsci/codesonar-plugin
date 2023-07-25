package org.jenkinsci.plugins.codesonar;

public class CodeSonarJsonSyntaxException extends CodeSonarPluginException {
    private static final String MESSAGE = "Failed to parse JSON response.";

    public CodeSonarJsonSyntaxException(Exception e) {
        super(MESSAGE, e);
    }
}
