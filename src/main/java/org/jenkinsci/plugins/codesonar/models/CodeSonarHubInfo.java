package org.jenkinsci.plugins.codesonar.models;

public class CodeSonarHubInfo {
    
    public static final String VERSION_NOT_INITIALIZED = "none";

    private String version;
    private boolean openAPISupported;
    
    public CodeSonarHubInfo() {
        version = VERSION_NOT_INITIALIZED;
        openAPISupported = false;
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
    
}
