package org.jenkinsci.plugins.codesonar.models;

public class CodeSonarHubCapabilityInfo {
	private Boolean openapi;

	public Boolean getOpenapi() {
		return openapi;
	}

	public void setOpenapi(Boolean openapi) {
		this.openapi = openapi;
	}

	@Override
	public String toString() {
		return "Capabilities [openapi=" + openapi + "]";
	}
	
}
