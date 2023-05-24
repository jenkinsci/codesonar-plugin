package org.jenkinsci.plugins.codesonar;

import java.io.PrintStream;
import java.text.MessageFormat;

import com.google.common.base.Throwables;

public class CodeSonarLogger {
    private PrintStream printStream = null;
    private static final String LOG_PREFIX = "[CodeSonar]";
    
    public CodeSonarLogger(PrintStream pw) {
        this.printStream = pw;
    }
    
    public void writeInfo(String message, Object...args) {
        printStream.println(formatMessage(message, args));
    }
    
    public void writeException(Exception e) {
        printStream.println(formatMessage("Exception: {0}%nStack Trace: {1}", e.getMessage(), Throwables.getStackTraceAsString(e)));
    }
    
    public static String formatMessage(String message, Object...args) {
        String msgReplacedLineBreaks = message;
        //If present, replacing platform-independent line breaks first
        if(msgReplacedLineBreaks.indexOf("%n") >= 0) {
            msgReplacedLineBreaks = String.format(message);
        }
        //Replacing message arguments
        String msgReplacedArguments = MessageFormat.format(msgReplacedLineBreaks, args);
        return String.format("%s %s", LOG_PREFIX, msgReplacedArguments);
    }
    
}
