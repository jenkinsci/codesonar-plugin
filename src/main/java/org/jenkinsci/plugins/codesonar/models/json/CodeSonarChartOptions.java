package org.jenkinsci.plugins.codesonar.models.json;

/**
 * @author aseno
 *
 */
public class CodeSonarChartOptions {
    
    private boolean sort_ascending;
    private boolean show_legend;
    private boolean is_stacked;
    private boolean show_empty_items;
    private String sampling;
    private String sample_size;
    
    public CodeSonarChartOptions() {
        sort_ascending = true;
        show_legend = true;
        is_stacked = false;
        show_empty_items = false;
        sampling = "";
        sample_size = "100";
    }
    
    public boolean isSort_ascending() {
        return sort_ascending;
    }
    
    public void setSort_ascending(boolean sort_ascending) {
        this.sort_ascending = sort_ascending;
    }
    
    public boolean isShow_legend() {
        return show_legend;
    }
    
    public void setShow_legend(boolean show_legend) {
        this.show_legend = show_legend;
    }
    
    public boolean isIs_stacked() {
        return is_stacked;
    }
    
    public void setIs_stacked(boolean is_stacked) {
        this.is_stacked = is_stacked;
    }
    
    public boolean isShow_empty_items() {
        return show_empty_items;
    }
    
    public void setShow_empty_items(boolean show_empty_items) {
        this.show_empty_items = show_empty_items;
    }
    
    public String getSampling() {
        return sampling;
    }
    
    public void setSampling(String sampling) {
        this.sampling = sampling;
    }
    
    public String getSample_size() {
        return sample_size;
    }
    
    public void setSample_size(String sample_size) {
        this.sample_size = sample_size;
    }

    @Override
    public String toString() {
        return "CodeSonarChartOptions [sort_ascending=" + sort_ascending + ", show_legend=" + show_legend
                + ", is_stacked=" + is_stacked + ", show_empty_items=" + show_empty_items + ", sampling=" + sampling
                + ", sample_size=" + sample_size + "]";
    }
    
}
