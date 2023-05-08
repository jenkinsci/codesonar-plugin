package org.jenkinsci.plugins.codesonar;


public class ResponseErrorException extends CodeSonarPluginException {
    private String url;
    private int status;
    private String reason;
    private String body;
    
    public ResponseErrorException(String message, String url, int status, String reason, String body) {
        super(message, new Object[] {url, status, reason, body});
        this.url = url;
        this.status = status;
        this.reason = reason;
        this.body = body;
    }

    public String getUrl() {
        return url;
    }

    public int getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public String getBody() {
        return body;
    }
    
}
