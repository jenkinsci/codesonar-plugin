package org.jenkinsci.plugins.codesonar.services;

import hudson.AbortException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.message.BasicNameValuePair;
import org.jenkinsci.plugins.codesonar.Utils;
import org.jenkinsci.plugins.codesonar.Utils.UrlFilters;
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

    public String getLatestAnalysisUrlForAProject(String hubAddress, String projectName) throws AbortException {
        String url = String.format("http://%s/index.xml", hubAddress);

        String xmlContent = null;
        xmlContent = httpService.getContentFromUrlAsString(url);

        Projects projects = null;
        projects = xmlSerializationService.deserialize(xmlContent, Projects.class);

        Project project = projects.getProjectByName(projectName);

        String analysisUrl = "http://" + hubAddress + project.getUrl();

        return analysisUrl;
    }

    public Analysis getAnalysisFromUrl(String analysisUrl) throws IOException {
        String xmlContent = httpService.getContentFromUrlAsString(analysisUrl);

        return xmlSerializationService.deserialize(xmlContent, Analysis.class);
    }
    
    public Analysis getAnalysisFromUrl(String analysisUrl, UrlFilters urlFilter) throws IOException {
        URIBuilder uriBuilder;
        try {
            uriBuilder = new URIBuilder(analysisUrl);
            uriBuilder.addParameter("filter", urlFilter.getValue());
        } catch (URISyntaxException ex) {
            throw new AbortException(ex.getMessage());
        }
        
        return getAnalysisFromUrl(uriBuilder.toString());
    }
}
