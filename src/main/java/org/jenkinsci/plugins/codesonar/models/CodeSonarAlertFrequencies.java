package org.jenkinsci.plugins.codesonar.models;

/**
 * @author aseno
 *
 */
public class CodeSonarAlertFrequencies {
    private int red;
    private int yellow;
    private int blue;
    private int green;
    
    public CodeSonarAlertFrequencies() {
        red = 0;
        yellow = 0;
        blue = 0;
        green = 0;
    }
    
    public int getRed() {
        return red;
    }
    public void setRed(int red) {
        this.red = red;
    }
    public int getYellow() {
        return yellow;
    }
    public void setYellow(int yellow) {
        this.yellow = yellow;
    }
    public int getBlue() {
        return blue;
    }
    public void setBlue(int blue) {
        this.blue = blue;
    }
    public int getGreen() {
        return green;
    }
    public void setGreen(int green) {
        this.green = green;
    }
    
    public void incrementOf(CodeSonarAlertData.Color color, int increment) {
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
        return "CodeSonarAlertFrequencies [red=" + red + ", yellow=" + yellow + ", blue=" + blue + ", green=" + green
                + "]";
    }

}
