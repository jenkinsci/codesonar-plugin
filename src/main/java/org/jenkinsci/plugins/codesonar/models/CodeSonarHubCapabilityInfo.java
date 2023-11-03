package org.jenkinsci.plugins.codesonar.models;

public class CodeSonarHubCapabilityInfo {
    private Boolean openapi;
    private Boolean strictQueryParameters;
    private Boolean gridConfigJson;
    private Boolean userSessionPool;

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

    public Boolean getUserSessionPool() {
        return userSessionPool;
    }

    public void setUserSessionPool(Boolean userSessionPool) {
        this.userSessionPool = userSessionPool;
    }

    @Override
    public String toString() {
        return String.format(
            "CodeSonarHubCapabilityInfo [openapi=%s, strictQueryParameters=%s, gridConfigJson=%s, userSessionPool=%s]",
            openapi,
            strictQueryParameters,
            gridConfigJson,
            userSessionPool);
    }
}
