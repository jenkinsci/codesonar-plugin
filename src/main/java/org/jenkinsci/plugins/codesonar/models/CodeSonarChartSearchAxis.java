package org.jenkinsci.plugins.codesonar.models;

/**
 * @author aseno
 *
 */
public class CodeSonarChartSearchAxis {
    
    private String name;
    private String modifier;
    private String scope;
    private String search_string;
    
    public CodeSonarChartSearchAxis() {
    }
    
    public CodeSonarChartSearchAxis(String name, String modifier, String scope, String search_string) {
        this.name = name;
        this.modifier = modifier;
        this.scope = scope;
        this.search_string = search_string;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getModifier() {
        return modifier;
    }
    
    public void setModifier(String modifier) {
        this.modifier = modifier;
    }
    
    public String getScope() {
        return scope;
    }
    
    public void setScope(String scope) {
        this.scope = scope;
    }
    
    public String getSearch_string() {
        return search_string;
    }
    
    public void setSearch_string(String search_string) {
        this.search_string = search_string;
    }
    
    @Override
    public String toString() {
        return "CodeSonarChartSearchAxis [name=" + name + ", modifier=" + modifier + ", scope=" + scope
                + ", search_string=" + search_string + "]";
    }
    
}
