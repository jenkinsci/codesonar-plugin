package org.jenkinsci.plugins.codesonar.services;

import java.io.IOException;
import java.net.URI;

import org.jenkinsci.plugins.codesonar.CodeSonarPluginException;

public abstract class AbstractService {
    
    protected String readResponseContent(HttpServiceResponse response, URI endpoint) throws CodeSonarPluginException {
        return readResponseContent(response, endpoint.toASCIIString());
    }

    protected String readResponseContent(HttpServiceResponse response, String endpoint) throws CodeSonarPluginException {
        try {
            return response.readContent();
        } catch (IOException e) {
            throw new CodeSonarPluginException("Unable to read response content. %nURI: {0}", e, endpoint);
        }
    }
}
