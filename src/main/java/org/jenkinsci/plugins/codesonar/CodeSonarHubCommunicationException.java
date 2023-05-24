package org.jenkinsci.plugins.codesonar;

import java.net.URI;

public class CodeSonarHubCommunicationException extends CodeSonarPluginException {
    private static final String MESSAGE = "Unexpected error in the response communicating with CodeSonar Hub. %nURI: {0}%nHTTP status code: {1} - {2} %nHTTP Body: {3}";

    public CodeSonarHubCommunicationException(URI hubUri, int statusCode, String reasonPhrase, String bodyContent) {
        super(MESSAGE, hubUri, statusCode, reasonPhrase, bodyContent);
    }
    
    public CodeSonarHubCommunicationException(String hubUri, int statusCode, String reasonPhrase, String bodyContent) {
        super(MESSAGE, hubUri, statusCode, reasonPhrase, bodyContent);
    }

}
