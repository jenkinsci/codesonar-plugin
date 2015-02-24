/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.codesonar.unit;

import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import org.jenkinsci.plugins.codesonar.models.Metric;
import org.jenkinsci.plugins.codesonar.services.XmlSerializationService;
import org.jenkinsci.plugins.codesonar.models.Project;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

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
    public void providedValidXML_deserializesTheXml() throws JAXBException {
        final String VALID_XML_CONTENT
                = "<project url=\"/analysis/8.xml?filter=2&prj_filter=10\">\n"
                + "<project>coverity</project>\n"
                + "<state>Finished</state>\n"
                + "<started>Fri Feb 13 17:33:18 2015</started>\n"
                + "<metric name=\"Lines with Code\">0</metric>\n"
                + "</project>";

        Project EXPECTED_RESULT = new Project();
        EXPECTED_RESULT.setUrl("/analysis/8.xml?filter=2&prj_filter=10");
        EXPECTED_RESULT.setProject("coverity");
        EXPECTED_RESULT.setState("Finished");
        EXPECTED_RESULT.setStarted("Fri Feb 13 17:33:18 2015");
        EXPECTED_RESULT.setMetric(new Metric("Lines with Code", "0"));

        Project result = xmlSerializationService.deserialize(VALID_XML_CONTENT, Project.class);

        assertEquals(EXPECTED_RESULT, result);
    }

    @Test(expected = UnmarshalException.class)
    public void providedInvalidXML_throwsException() throws JAXBException {
        final String INVALID_XML_CONTENT
                = "project url=\"/analysis/8.xml?filter=2&prj_filter=10\">\n"
                + "<project>coverity</project>\n"
                + "<state>Finished</state>\n"
                + "started>Fri Feb 13 17:33:18 2015</started>\n"
                + "<metric name=\"Lines with Code\">0</metric>\n"
                + "</project>";

        xmlSerializationService.deserialize(INVALID_XML_CONTENT, Project.class);
    }
}
