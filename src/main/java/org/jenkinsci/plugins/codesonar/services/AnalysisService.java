package org.jenkinsci.plugins.codesonar.services;

import hudson.AbortException;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.conn.HttpHostConnectException;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.jenkinsci.plugins.codesonar.models.projects.Project;
import org.jenkinsci.plugins.codesonar.models.projects.Projects;

/**
 *
 * @author Andrius
 */
public class AnalysisService implements Serializable {

    private HttpService httpService;
    private XmlSerializationService xmlSerializationService;

    public XmlSerializationService getXmlSerializationService() {
        return xmlSerializationService;
    }

    public AnalysisService(HttpService httpService, XmlSerializationService xmlSerializationService) {
        this.httpService = httpService;
        this.xmlSerializationService = xmlSerializationService;
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
        String url = String.format("http://%s/index.xml", hubAddress);

        String xmlContent = null;
        try {
            xmlContent = httpService.getContentFromUrlAsString(url);
        } catch (HttpHostConnectException e) {
            throw new AbortException(e.getMessage());
        }

        Projects projects = null;
        projects = xmlSerializationService.deserialize(xmlContent, Projects.class);

        Project project = projects.getProjectByName(projectName);

        String analysisUrl = "http://" + hubAddress + project.getUrl();

        return analysisUrl;
    }

    public Analysis getAnalysisFromUrl(String analysisUrl) throws IOException {
        String xmlContent = httpService.getContentFromUrlAsString(analysisUrl);

        Analysis analysis = xmlSerializationService.deserialize(xmlContent, Analysis.class);

        return analysis;
    }
}
