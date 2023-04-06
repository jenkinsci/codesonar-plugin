package org.jenkinsci.plugins.codesonar.models;

public class CodeSonarHubInfo {
    
    public static final String VERSION_NOT_INITIALIZED = "none";

    private String version;
    private boolean openAPISupported;
    private boolean strictQueryParametersEnforced;
    
    public CodeSonarHubInfo() {
        version = VERSION_NOT_INITIALIZED;
        openAPISupported = false;
        strictQueryParametersEnforced = false;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isOpenAPISupported() {
        return openAPISupported;
    }

    public void setOpenAPISupported(boolean openAPISupported) {
        this.openAPISupported = openAPISupported;
    }

    public boolean isStrictQueryParametersEnforced() {
        return strictQueryParametersEnforced;
    }

    public void setStrictQueryParametersEnforced(boolean strictQueryParametersEnforced) {
        this.strictQueryParametersEnforced = strictQueryParametersEnforced;
    }
    
}
