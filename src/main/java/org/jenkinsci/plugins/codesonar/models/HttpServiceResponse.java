package org.jenkinsci.plugins.codesonar.models;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class HttpServiceResponse {

    private int statusCode;
    private String reasonPhrase;
    private InputStream contentInputStream;
    
    public HttpServiceResponse(int statusCode, String reasonPhrase, InputStream contentInputStream) {
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
        this.contentInputStream = contentInputStream;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public InputStream getContentInputStream() {
        return contentInputStream;
    }

    public String readContent() throws IOException {
        if (contentInputStream == null) {
            return null;
        }
        StringBuilder content = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader(contentInputStream, StandardCharsets.UTF_8))) {
            int c = 0;
            while ((c = reader.read()) != -1) {
                content.append((char) c);
            }
        }
        return content.toString();
    }
}
