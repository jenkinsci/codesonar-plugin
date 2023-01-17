package org.jenkinsci.plugins.codesonar.models;

public class HubVersion {
    
    public static final String VERSION_NOT_INITIALIZED = "none";

    private String version;
    private boolean supportsOpenAPI;
    
    public HubVersion() {
        version = VERSION_NOT_INITIALIZED;
        supportsOpenAPI = false;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isSupportsOpenAPI() {
        return supportsOpenAPI;
    }

    public void setSupportsOpenAPI(boolean supportsOpenAPI) {
        this.supportsOpenAPI = supportsOpenAPI;
    }
    
}
