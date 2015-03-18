package org.jenkinsci.plugins.codesonar.services;

import hudson.AbortException;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBException;
import org.apache.http.conn.HttpHostConnectException;
import org.jenkinsci.plugins.codesonar.models.Analysis;
import org.jenkinsci.plugins.codesonar.models.Project;
import org.jenkinsci.plugins.codesonar.models.Projects;

/**
 *
 * @author Andrius
 */
public class AnalysisService {

    private final XmlSerializationService xmlSerializationService;
    private final HttpService httpService;

    public AnalysisService(XmlSerializationService xmlSerializationService, HttpService httpService) {
        this.xmlSerializationService = xmlSerializationService;
        this.httpService = httpService;
    }

    public String getAnalysisUrlFromLogFile(List<String> logFile) {
        Pattern pattern = Pattern.compile("codesonar:\\s+(.*/analysis/.*)");

        String analysisUrl = null;
        for (String line : logFile) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                analysisUrl = matcher.group(1);
            }
        }

        if (analysisUrl != null && analysisUrl.endsWith(".html")) {
            analysisUrl = analysisUrl.replaceAll(".html", ".xml");
        }

        return analysisUrl;
    }

    public String getLatestAnalysisUrlForAProject(String hubAddress, String projectName) throws IOException {
        String url = "http://" + hubAddress + "/index.xml";

        String xmlContent = null;
        try {
            xmlContent = httpService.getContentFromUrlAsString(url);
        } catch (HttpHostConnectException e) {
            throw new AbortException(e.getMessage());
        }

        Projects projects = null;
        try {
            projects = xmlSerializationService.deserialize(xmlContent, Projects.class);
        } catch (JAXBException ex) {
        }

        Project project = projects.getProjectByName(projectName);

        String analysisUrl = "http://" + hubAddress + project.getUrl();

        return analysisUrl;
    }

    public Analysis getAnalysisFromUrl(String analysisUrl) throws IOException {
        String xmlContent = httpService.getContentFromUrlAsString(analysisUrl);

        Analysis analysis = null;
        try {
            analysis = xmlSerializationService.deserialize(xmlContent, Analysis.class);
        } catch (JAXBException ex) {
        }

        return analysis;
    }
}
