package org.jenkinsci.plugins.codesonar.models;

public class Capabilities {
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
