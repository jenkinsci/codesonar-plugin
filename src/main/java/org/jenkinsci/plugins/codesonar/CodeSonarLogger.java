package org.jenkinsci.plugins.codesonar;

import java.io.PrintStream;

public class CodeSonarLogger {
    private PrintStream printStream = null;
    private static final String LOG_PREFIX = "[CodeSonar]";
    
    public CodeSonarLogger(PrintStream pw) {
        this.printStream = pw;
    }
    
    public void writeInfo(String message, Object...parameters) {
        printStream.println(formatMessage(message, parameters));
    }
    
    public static String formatMessage(String message, Object...parameters) {
        return String.format("%s %s", LOG_PREFIX, String.format(message, parameters));
    }
    
}
