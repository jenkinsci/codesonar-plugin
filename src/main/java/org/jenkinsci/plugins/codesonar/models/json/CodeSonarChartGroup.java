package org.jenkinsci.plugins.codesonar.models.json;

/**
 * @author aseno
 *
 */
public class CodeSonarChartGroup {
    
    private String name;
    private String order;
    
    public CodeSonarChartGroup() {
        
    }
    
    public CodeSonarChartGroup(String name, String order) {
        this.name = name;
        this.order = order;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    @Override
    public String toString() {
        return "CodeSonarChartGroup [name=" + name + ", order=" + order + "]";
    }
    
}
