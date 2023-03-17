package org.jenkinsci.plugins.codesonar.models;

public class CodeSonarHubCapabilityInfo {
    private Boolean openapi;
    private Boolean strictQueryParameters;

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

    @Override
    public String toString() {
        return "Capabilities [openapi=" + openapi + ", strictQueryParameters=" + strictQueryParameters +"]";
    }
    
}
