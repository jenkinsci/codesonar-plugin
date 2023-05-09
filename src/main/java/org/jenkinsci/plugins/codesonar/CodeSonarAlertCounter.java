package org.jenkinsci.plugins.codesonar;

/**
 * @author aseno
 *
 */
public class CodeSonarAlertCounter {
    private int red;
    private int yellow;
    private int blue;
    private int green;
    
    public CodeSonarAlertCounter() {
        red = 0;
        yellow = 0;
        blue = 0;
        green = 0;
    }
    
    public int getAlertCount(CodeSonarAlertLevels color) {
        switch(color) {
        case RED:
            return this.red;
        case YELLOW:
            return this.yellow;
        case BLUE:
            return this.blue;
        case GREEN:
            return this.green;
        default:
            return -1;
        }
    }
    
    public void incrementOf(CodeSonarAlertLevels color, int increment) {
        switch(color) {
        case RED:
            this.red += increment;
            break;
        case YELLOW:
            this.yellow += increment;
            break;
        case BLUE:
            this.blue += increment;
            break;
        case GREEN:
            this.green += increment;
            break;
        default:
            // Do nothing
        }
    }
    
    @Override
    public String toString() {
        return "CodeSonarAlertCounter [red=" + red + ", yellow=" + yellow + ", blue=" + blue + ", green=" + green
                + "]";
    }

}
