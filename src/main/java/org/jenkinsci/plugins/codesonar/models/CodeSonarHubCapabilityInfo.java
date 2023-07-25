package org.jenkinsci.plugins.codesonar.models;

public class CodeSonarHubCapabilityInfo {
    private Boolean openapi;
    private Boolean strictQueryParameters;
    private Boolean gridConfigJson;

    public Boolean getOpenapi() {
        return openapi;
    }

    public void setOpenapi(Boolean openapi) {
        this.openapi = openapi;
    }
    
    public Boolean getStrictQueryParameters() {
        return strictQueryParameters;
    }

    public void setStrictQueryParameters(Boolean strictQueryParameters) {
        this.strictQueryParameters = strictQueryParameters;
    }
    
    public Boolean getGridConfigJson() {
        return gridConfigJson;
    }

    public void setGridConfigJson(Boolean gridConfigJson) {
        this.gridConfigJson = gridConfigJson;
    }

    @Override
    public String toString() {
        return "CodeSonarHubCapabilityInfo [openapi=" + openapi + ", strictQueryParameters=" + strictQueryParameters
                + ", gridConfigJson=" + gridConfigJson + "]";
    }
    
}
