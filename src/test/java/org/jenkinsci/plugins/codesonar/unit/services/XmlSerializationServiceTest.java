package org.jenkinsci.plugins.codesonar.unit.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.jenkinsci.plugins.codesonar.CodeSonarPluginException;
import org.jenkinsci.plugins.codesonar.models.Metric;
import org.jenkinsci.plugins.codesonar.models.projects.Project;
import org.jenkinsci.plugins.codesonar.services.XmlSerializationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author andrius
 */
class XmlSerializationServiceTest {

    private XmlSerializationService xmlSerializationService;

    @BeforeEach
    void setUp() {
        xmlSerializationService = new XmlSerializationService();
    }

    @Test
    void providedValidXML_deserializesTheXml() throws CodeSonarPluginException {
        final InputStream VALID_XML_CONTENT = new ByteArrayInputStream((
                "<project url=\"/analysis/8.xml?filter=2&amp;prj_filter=10\">\n"
                + "<name>coverity</name>\n"
                + "<state>Finished</state>\n"
                + "<started>Fri Feb 13 17:33:18 2015</started>\n"
                + "<metric name=\"Lines with Code\">0</metric>\n"
                + "</project>").getBytes());

        Project EXPECTED_RESULT = new Project();
        EXPECTED_RESULT.setUrl("/analysis/8.xml?filter=2&prj_filter=10");
        EXPECTED_RESULT.setName("coverity");
        EXPECTED_RESULT.setState("Finished");
        EXPECTED_RESULT.setStarted("Fri Feb 13 17:33:18 2015");
        EXPECTED_RESULT.setMetric(new Metric("Lines with Code", "0"));

        Project result = xmlSerializationService.deserialize(VALID_XML_CONTENT, Project.class);

        assertEquals(EXPECTED_RESULT, result);
    }

    @Test
    void providedInvalidXML_throwsAbortException() {
        final InputStream INVALID_XML_CONTENT = new ByteArrayInputStream((
                    "project url=\"/analysis/8.xml?filter=2&prj_filter=10\">\n"
                            + "<name>coverity</name>\n"
                            + "<state>Finished</state>\n"
                            + "started>Fri Feb 13 17:33:18 2015</started>\n"
                            + "<metric name=\"Lines with Code\">0</metric>\n"
                            + "</project>").getBytes());
        assertThrows(CodeSonarPluginException.class, () ->
            xmlSerializationService.deserialize(INVALID_XML_CONTENT, Project.class));
    }
}
