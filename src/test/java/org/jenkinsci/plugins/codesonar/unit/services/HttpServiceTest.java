package org.jenkinsci.plugins.codesonar.unit.services;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jenkinsci.plugins.codesonar.CodeSonarPluginException;
import org.jenkinsci.plugins.codesonar.services.HttpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Andrius
 */
class HttpServiceTest {

    private HttpService httpService;

    @BeforeEach
    void setUp() throws CodeSonarPluginException {
        httpService = new HttpService(null, null, null, -1);
    }

    @Test
    void testGetResponseFromUrl() {
        final String INVALID_URL = "invalidurl";
        assertThrows(CodeSonarPluginException.class, () ->
            httpService.getResponseFromUrl(INVALID_URL).readContent());
    }
}
