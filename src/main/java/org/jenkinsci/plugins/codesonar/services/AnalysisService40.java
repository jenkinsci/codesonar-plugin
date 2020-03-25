package org.jenkinsci.plugins.codesonar.services;

import hudson.AbortException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.client.utils.URIBuilder;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.jenkinsci.plugins.codesonar.models.projects.Project40;
import org.jenkinsci.plugins.codesonar.models.projects.Projects40;

/**
 *
 * @author Andrius
 */
public class AnalysisService40 implements IAnalysisService {

    final private HttpService httpService;
    final private XmlSerializationService xmlSerializationService;

    private enum UrlFilters {

        NEW("4"), ACTIVE("2");

        private final String value;

        UrlFilters(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

    }
    
    public AnalysisService40(HttpService httpService, XmlSerializationService xmlSerializationService) {
        this.httpService = httpService;
        this.xmlSerializationService = xmlSerializationService;
    }

    @Override
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

    @Override
    public String getLatestAnalysisUrlForAProject(URI baseHubUri, String projectName) throws IOException {
        InputStream xmlContent = httpService.getContentFromUrlAsInputStream(baseHubUri.resolve("/index.xml"));
        
        Projects40 projects = xmlSerializationService.deserialize(xmlContent, Projects40.class);
        Project40 project = projects.getProjectByName(projectName);
        
        return baseHubUri.resolve(project.getUrl()).toString();
    }

    @Override
    public Analysis getAnalysisFromUrl(String analysisUrl) throws IOException {
        InputStream xmlContent = httpService.getContentFromUrlAsInputStream(analysisUrl);

        return xmlSerializationService.deserialize(xmlContent, Analysis.class);
    }
    
    @Override
    public Analysis getAnalysisFromUrlWithNewWarnings(String analysisUrl) throws IOException {
        URIBuilder uriBuilder;
        try {
            uriBuilder = new URIBuilder(analysisUrl);
            uriBuilder.addParameter("filter", UrlFilters.NEW.getValue());
        } catch (URISyntaxException ex) {
            throw new AbortException(ex.getMessage());
        }
        
        return getAnalysisFromUrl(uriBuilder.toString());
    }
    
    @Override
    public Analysis getAnalysisFromUrlWithActiveWarnings(String analysisUrl) throws IOException {
        URIBuilder uriBuilder;
        try {
            uriBuilder = new URIBuilder(analysisUrl);
            uriBuilder.addParameter("filter", UrlFilters.ACTIVE.getValue());
        } catch (URISyntaxException ex) {
            throw new AbortException(ex.getMessage());
        }
        
        return getAnalysisFromUrl(uriBuilder.toString());
    }
}
