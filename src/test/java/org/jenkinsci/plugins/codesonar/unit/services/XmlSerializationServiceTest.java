package org.jenkinsci.plugins.codesonar.unit.services;

import hudson.AbortException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.jenkinsci.plugins.codesonar.models.Metric;
import org.jenkinsci.plugins.codesonar.models.projects.Project42;
import org.jenkinsci.plugins.codesonar.services.XmlSerializationService;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author andrius
 */
public class XmlSerializationServiceTest {

    private XmlSerializationService xmlSerializationService;

    @Before
    public void setUp() {
        xmlSerializationService = new XmlSerializationService();
    }
    
    @Test
    public void providedValidXML_deserializesTheXml() throws Exception {
        final InputStream VALID_XML_CONTENT = new ByteArrayInputStream((
                "<project url=\"/analysis/8.xml?filter=2&prj_filter=10\">\n"
                + "<name>coverity</name>\n"
                + "<state>Finished</state>\n"
                + "<started>Fri Feb 13 17:33:18 2015</started>\n"
                + "<metric name=\"Lines with Code\">0</metric>\n"
                + "</project>").getBytes());
        
        Project42 EXPECTED_RESULT = new Project42();
        EXPECTED_RESULT.setUrl("/analysis/8.xml?filter=2&prj_filter=10");
        EXPECTED_RESULT.setName("coverity");
        EXPECTED_RESULT.setState("Finished");
        EXPECTED_RESULT.setStarted("Fri Feb 13 17:33:18 2015");
        EXPECTED_RESULT.setMetric(new Metric("Lines with Code", "0"));

        Project42 result = xmlSerializationService.deserialize(VALID_XML_CONTENT, Project42.class);

        assertEquals(EXPECTED_RESULT, result);
    }

    @Test(expected = AbortException.class)
    public void providedInvalidXML_throwsAbortException() throws AbortException {
        final InputStream INVALID_XML_CONTENT = new ByteArrayInputStream((
                "project url=\"/analysis/8.xml?filter=2&prj_filter=10\">\n"
                + "<name>coverity</name>\n"
                + "<state>Finished</state>\n"
                + "started>Fri Feb 13 17:33:18 2015</started>\n"
                + "<metric name=\"Lines with Code\">0</metric>\n"
                + "</project>").getBytes());

        xmlSerializationService.deserialize(INVALID_XML_CONTENT, Project42.class);
    }
}
