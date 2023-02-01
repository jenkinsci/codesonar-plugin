package org.jenkinsci.plugins.codesonar.models;

public class CodeSonarHubClientCompatibilityInfo {
    private String hubVersion;
    private long hubVersionNumber;
    private long hubProtocol;
    private Boolean clientOK;
    private String message;
    private CodeSonarHubCapabilityInfo capabilities;
    
    public String getHubVersion() {
        return hubVersion;
    }
    public void setHubVersion(String hubVersion) {
        this.hubVersion = hubVersion;
    }
    public long getHubVersionNumber() {
        return hubVersionNumber;
    }
    public void setHubVersionNumber(long hubVersionNumber) {
        this.hubVersionNumber = hubVersionNumber;
    }
    public long getHubProtocol() {
        return hubProtocol;
    }
    public void setHubProtocol(long hubProtocol) {
        this.hubProtocol = hubProtocol;
    }
    public Boolean getClientOK() {
        return clientOK;
    }
    public void setClientOK(Boolean clientOK) {
        this.clientOK = clientOK;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public CodeSonarHubCapabilityInfo getCapabilities() {
        return capabilities;
    }
    public void setCapabilities(CodeSonarHubCapabilityInfo capabilities) {
        this.capabilities = capabilities;
    }
    
    @Override
    public String toString() {
        return "VersionCompatibilityInfo [hubVersion=" + hubVersion + ", hubVersionNumber=" + hubVersionNumber
                + ", hubProtocol=" + hubProtocol + ", clientOK=" + clientOK + ", message=" + message + ", capabilities="
                + capabilities + "]";
    }
    
}
