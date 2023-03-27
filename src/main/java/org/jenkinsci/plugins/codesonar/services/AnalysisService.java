package org.jenkinsci.plugins.codesonar.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.jenkinsci.plugins.codesonar.CodeSonarPluginException;
import org.jenkinsci.plugins.codesonar.models.SearchResults;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.jenkinsci.plugins.codesonar.models.projects.Project;

/**
 *
 * @author Andrius
 */
public class AnalysisService implements IAnalysisService {
    private static final Logger LOGGER = Logger.getLogger(AnalysisService.class.getName());
    
    final private HttpService httpService;
    final private XmlSerializationService xmlSerializationService;
    private String visibilityFilter;
    private String visibilityFilterNewWarnings;
    private boolean strictQueryParameters;


    public AnalysisService(HttpService httpService, XmlSerializationService xmlSerializationService, String visibilityFilter, String visibilityFilterNewWarnings, boolean strictQueryParameters) {
        this.httpService = httpService;
        this.xmlSerializationService = xmlSerializationService;
        this.visibilityFilter = visibilityFilter;
        this.visibilityFilterNewWarnings = visibilityFilterNewWarnings;
        this.strictQueryParameters = strictQueryParameters;
    }
    
    private CodeSonarPluginException createError(String msg, Object...args) {
        return new CodeSonarPluginException(msg, args);
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
        String encode = URLEncoder.encode("\"" + projectName + "\"", "UTF-8");
        URI uri = baseHubUri.resolve("/project_search.xml?query=" + encode + "&scope=all");

        InputStream xmlContent = httpService.getContentFromUrlAsInputStream(uri);

        SearchResults searchResults = xmlSerializationService.deserialize(xmlContent, SearchResults.class);
        Project project = searchResults.getProjectByName(projectName);

        return baseHubUri.resolve(project.getUrl()).toString();
    }

    @Override
    public Analysis getAnalysisFromUrl(String analysisUrl) throws CodeSonarPluginException {
        InputStream xmlContent = httpService.getContentFromUrlAsInputStream(analysisUrl);

        return xmlSerializationService.deserialize(xmlContent, Analysis.class);
    }

    @Override
    public Analysis getAnalysisFromUrlWithNewWarnings(String analysisUrl) throws CodeSonarPluginException {
        LOGGER.log(Level.INFO, String.format("Calling getAnalysisFromUrlWithNewWarnings"));
        URIBuilder uriBuilder;
        try {
            uriBuilder = new URIBuilder(analysisUrl);
            String visibilityFilterNewWarningsOrDefault = formatParameter(getVisibilityFilterNewWarningsOrDefault());
            LOGGER.log(Level.INFO, "Passing filter = {0}", visibilityFilterNewWarningsOrDefault);
            uriBuilder.addParameter("filter", visibilityFilterNewWarningsOrDefault);
        } catch (URISyntaxException ex) {
            throw createError(ex.getMessage());
        }

        return getAnalysisFromUrl(uriBuilder.toString());
    }

    @Override
    public Analysis getAnalysisFromUrlWarningsByFilter(String analysisUrl) throws CodeSonarPluginException {
        LOGGER.log(Level.INFO, String.format("Calling getAnalysisFromUrlWarningsByFilter"));
        URIBuilder uriBuilder;
        try {
            uriBuilder = new URIBuilder(analysisUrl);
            String visibilityFilterOrDefault = formatParameter(getVisibilityFilterOrDefault());
            LOGGER.log(Level.INFO, "Passing filter = {0}", visibilityFilterOrDefault);
            uriBuilder.addParameter("filter", visibilityFilterOrDefault);
        } catch (URISyntaxException ex) {
            throw createError(ex.getMessage());
        }

        return getAnalysisFromUrl(uriBuilder.toString());
    }
    
    private String formatParameter(String parameter) {
        //Remove any unwanted leading or trailing white space character
        parameter = StringUtils.strip(parameter);
        try {
            Long.parseLong(parameter);
        } catch(NumberFormatException e) {
            if(strictQueryParameters) {
                //Surround with double quotes only if parameter is not numeric and the hub supports strict query parameters
                parameter = String.format("\"%s\"", parameter);
            }
        }
        return parameter;
    }

    @Override
    public void setVisibilityFilter(String visibilityFilter) {
        this.visibilityFilter = visibilityFilter;
    }

    @Override
    public String getVisibilityFilter() {
        return this.visibilityFilter;
    }
    
    public String getVisibilityFilterOrDefault() {
        return StringUtils.isNotBlank(visibilityFilter) ? visibilityFilter : IAnalysisService.VISIBILITY_FILTER_ALL_WARNINGS_DEFAULT;
    }

    @Override
    public void setVisibilityFilterNewWarnings(String visibilityFilter) {
        this.visibilityFilterNewWarnings = visibilityFilter;
    }

    @Override
    public String getVisibilityFilterNewWarnings() {
        return this.visibilityFilterNewWarnings;
    }
    
    public String getVisibilityFilterNewWarningsOrDefault() {
        return StringUtils.isNotBlank(visibilityFilterNewWarnings) ? visibilityFilterNewWarnings : IAnalysisService.VISIBILITY_FILTER_NEW_WARNINGS_DEFAULT;
    }

}
