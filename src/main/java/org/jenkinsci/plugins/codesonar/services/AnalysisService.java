package org.jenkinsci.plugins.codesonar.services;

import hudson.AbortException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.client.utils.URIBuilder;
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

    public String getLatestAnalysisUrlForAProject(URI uri, String projectName) throws IOException {
        URIBuilder uriBuilder = new URIBuilder(uri);
        uriBuilder.setPath("/index.xml");
        String xmlContent = null;
        try {
            xmlContent = httpService.getContentFromUrlAsString(uriBuilder.build());
        } catch (URISyntaxException ex) {
            throw new AbortException(String.format("[CodeSonar] %s", ex.getMessage()));
        }

        Projects projects = xmlSerializationService.deserialize(xmlContent, Projects.class);

        Project project = projects.getProjectByName(projectName);
        uriBuilder.setPath(project.getUrl());

        try {
            return uriBuilder.build().toString();
        } catch (URISyntaxException ex) {
            throw new AbortException(String.format("[CodeSonar] %s", ex.getMessage()));
        }
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
