package org.jenkinsci.plugins.codesonar.models;

import java.util.ArrayList;
import java.util.List;

/**
 * @author aseno
 *
 */
public class CodeSonarChartConfigData {
    public static final String CHART_KIND_BAR = "bar";
    
    private String kind;
    private List<CodeSonarChartSearchAxis> search_axes;
    private List<CodeSonarChartGroup> groups;
    private CodeSonarChartOptions options;
    private String title_x;
    private String title;
    private String title_y;
    
    public CodeSonarChartConfigData() {
    }
    
    public CodeSonarChartConfigData(String kind, String title_x, String title, String title_y) {
        this.kind = kind;
        this.title_x = title_x;
        this.title = title;
        this.title_y = title_y;
        search_axes = new ArrayList<>();
        groups = new ArrayList<>();
        options = new CodeSonarChartOptions();
    }
    
    public String getKind() {
        return kind;
    }
    
    public void setKind(String kind) {
        this.kind = kind;
    }
    
    public List<CodeSonarChartSearchAxis> getSearch_axes() {
        return search_axes;
    }
    
    public void setSearch_axes(List<CodeSonarChartSearchAxis> search_axes) {
        this.search_axes = search_axes;
    }
    
    public List<CodeSonarChartGroup> getGroups() {
        return groups;
    }
    
    public void setGroups(List<CodeSonarChartGroup> groups) {
        this.groups = groups;
    }
    
    public CodeSonarChartOptions getOptions() {
        return options;
    }
    
    public void setOptions(CodeSonarChartOptions options) {
        this.options = options;
    }
    
    public String getTitle_x() {
        return title_x;
    }
    
    public void setTitle_x(String title_x) {
        this.title_x = title_x;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getTitle_y() {
        return title_y;
    }
    
    public void setTitle_y(String title_y) {
        this.title_y = title_y;
    }

    @Override
    public String toString() {
        return "CodeSonarChartConfigData [kind=" + kind + ", search_axes=" + search_axes + ", groups=" + groups
                + ", options=" + options + ", title_x=" + title_x + ", title=" + title + ", title_y=" + title_y + "]";
    }
    
}
