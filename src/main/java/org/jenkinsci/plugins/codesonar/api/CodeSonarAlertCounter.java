package org.jenkinsci.plugins.codesonar.api;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.jenkinsci.plugins.codesonar.models.CodeSonarAlertLevels;

/**
 * @author aseno
 *
 */
public class CodeSonarAlertCounter {
    private int[] counterByLevel;
    
    public CodeSonarAlertCounter() {
        counterByLevel = new int[CodeSonarAlertLevels.values().length];
    }
    
    public int getAlertCount(CodeSonarAlertLevels level) {
        return counterByLevel[level.ordinal()];
    }
    
    public void incrementOf(CodeSonarAlertLevels level, int increment) {
        counterByLevel[level.ordinal()] += increment;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CodeSonarAlertCounter levels=[");
        sb.append(Arrays.asList(CodeSonarAlertLevels.values())
            .stream()
            .map(x -> x.name())
            .collect(Collectors.joining(", ")));
        sb.append("], counters=[");
        sb.append(Arrays.stream(counterByLevel)
                .mapToObj(i -> String.valueOf(i))
                .collect(Collectors.joining(", ")));
        sb.append("]");
        return sb.toString();
    }

}
