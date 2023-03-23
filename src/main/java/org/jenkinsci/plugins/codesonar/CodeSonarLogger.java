package org.jenkinsci.plugins.codesonar;

import java.io.PrintStream;

import org.javatuples.Pair;

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
	
	@SafeVarargs
	public static String formatMessageMultiLine(Pair<String, Object[]>...lines) {
		StringBuilder multiLineMessage = new StringBuilder();
		for (int i = 0; i < lines.length; i++) {
			String prefixFormat = "%s %s";
			//Add extra platform independent new line if this is not the last line of the message
			if(i + 1 < lines.length) {
				prefixFormat += "%n";
			}
			multiLineMessage.append(String.format(prefixFormat, LOG_PREFIX, String.format(lines[i].getValue0(), lines[i].getValue1())));
		}
		return multiLineMessage.toString();
	}
	
	public static Pair<String, Object[]> createLine(String message, Object...parameters) {
		return new Pair<String, Object[]>(message, parameters);
	}
	
}
