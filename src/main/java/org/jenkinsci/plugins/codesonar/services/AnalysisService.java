package org.jenkinsci.plugins.codesonar.services;

import static org.jenkinsci.plugins.codesonar.models.json.CodeSonarChartConfigData.CHART_KIND_BAR;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.jenkinsci.plugins.codesonar.CodeSonarPluginException;
import org.jenkinsci.plugins.codesonar.models.HttpServiceResponse;
import org.jenkinsci.plugins.codesonar.models.SearchResults;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.jenkinsci.plugins.codesonar.models.json.CodeSonarWarningCountChartRow;
import org.jenkinsci.plugins.codesonar.models.json.CodeSonarChartConfigData;
import org.jenkinsci.plugins.codesonar.models.json.CodeSonarWarningCountChartData;
import org.jenkinsci.plugins.codesonar.models.json.CodeSonarChartGroup;
import org.jenkinsci.plugins.codesonar.models.json.CodeSonarChartSearchAxis;
import org.jenkinsci.plugins.codesonar.models.projects.Project;

import com.google.common.base.Throwables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 *
 * @author Andrius
 */
public class AnalysisService implements IAnalysisService {
    private static final Logger LOGGER = Logger.getLogger(AnalysisService.class.getName());
    
    final private HttpService httpService;
    final private XmlSerializationService xmlSerializationService;
    private String visibilityFilter;
    private String newWarningsFilter;
    private boolean strictQueryParameters;


    public AnalysisService(HttpService httpService, XmlSerializationService xmlSerializationService, String visibilityFilter, String newWarningsFilter, boolean strictQueryParameters) {
        this.httpService = httpService;
        this.xmlSerializationService = xmlSerializationService;
        this.visibilityFilter = visibilityFilter;
        this.newWarningsFilter = newWarningsFilter;
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
    public String getLatestAnalysisUrlForAProject(URI baseHubUri, String projectName) throws CodeSonarPluginException {
        String encode;
        try {
            encode = URLEncoder.encode("\"" + projectName + "\"", "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new CodeSonarPluginException("Request URI for analysis url for a project contains a syntax error. %nException: {0}%nStack Trace: {1}", e.getMessage(), Throwables.getStackTraceAsString(e));
        }
        URI uri = baseHubUri.resolve("/project_search.xml?query=" + encode + "&scope=all");

        HttpServiceResponse response = httpService.getResponseFromUrl(uri);
        
        if(response.getStatusCode() != 200) {
            try {
                throw new CodeSonarPluginException("Unexpected error in the response communicating with CodeSonar Hub. %nURI: {0}%nHTTP status code: {1} - {2} %nHTTP Body: {3}", uri, response.getStatusCode(), response.getReasonPhrase(), response.readContent());
            } catch (IOException e) {
                throw new CodeSonarPluginException("Unable to read response content. %nURI: {0}%nException: {1}%nStack Trace: {2}", uri, e.getMessage(), Throwables.getStackTraceAsString(e));
            }
        }

        SearchResults searchResults = xmlSerializationService.deserialize(response.getContentInputStream(), SearchResults.class);
        Project project = searchResults.getProjectByName(projectName);

        return baseHubUri.resolve(project.getUrl()).toString();
    }

    @Override
    public Analysis getAnalysisFromUrl(String analysisUrl) throws CodeSonarPluginException {
        HttpServiceResponse response = httpService.getResponseFromUrl(analysisUrl);
        
        if(response.getStatusCode() != 200) {
            try {
                throw new CodeSonarPluginException("Unexpected error in the response communicating with CodeSonar Hub. %nURI: {0}%nHTTP status code: {1} - {2} %nHTTP Body: {3}", analysisUrl, response.getStatusCode(), response.getReasonPhrase(), response.readContent());
            } catch (IOException e) {
                throw new CodeSonarPluginException("Unable to read response content. %nURI: {0}%nException: {1}%nStack Trace: {2}", analysisUrl, e.getMessage(), Throwables.getStackTraceAsString(e));
            }
        }

        return xmlSerializationService.deserialize(response.getContentInputStream(), Analysis.class);
    }

    /**
     * Retrieves analysis data for "new" warnings, in particular it returns the whole list of warnings.
     */
    @Override
    public Analysis getAnalysisFromUrlWithNewWarnings(URI baseHubUri, long analysisId) throws CodeSonarPluginException {
        LOGGER.log(Level.INFO, String.format("Calling getAnalysisFromUrlWithNewWarnings"));
        URIBuilder uriBuilder = new URIBuilder(baseHubUri);
        uriBuilder.setPath(String.format("/analysis/%d.xml", analysisId));
        String newWarningsFilterOrDefault = Utils.formatParameter(getNewWarningsFilterOrDefault(), strictQueryParameters);
        LOGGER.log(Level.INFO, "Passing filter = {0}", newWarningsFilterOrDefault);
        uriBuilder.addParameter("filter", newWarningsFilterOrDefault);

        return getAnalysisFromUrl(uriBuilder.toString());
    }
    
    /**
     * Retrieves analysis data for "all" warnings, in particular it returns the whole list of warnings.
     */
    @Override
    public Analysis getAnalysisFromUrlWarningsByFilter(URI baseHubUri, long analysisId) throws CodeSonarPluginException {
        LOGGER.log(Level.INFO, String.format("Calling getAnalysisFromUrlWarningsByFilter"));
        URIBuilder uriBuilder = new URIBuilder(baseHubUri);
        uriBuilder.setPath(String.format("/analysis/%d.xml", analysisId));
        String visibilityFilterOrDefault = Utils.formatParameter(getVisibilityFilterOrDefault(), strictQueryParameters);
        LOGGER.log(Level.INFO, "Passing filter = {0}", visibilityFilterOrDefault);
        uriBuilder.addParameter("filter", visibilityFilterOrDefault);

        return getAnalysisFromUrl(uriBuilder.toString());
    }
    
    @Override
    public CodeSonarWarningCountChartRow getNumberOfWarnings(URI baseHubUri, long analysisId, String filter) throws CodeSonarPluginException {
        //Configure chart data in order to extract desired data
        CodeSonarChartConfigData chartConfigData = new CodeSonarChartConfigData(CHART_KIND_BAR, "Number of Warnings", "", "");
        chartConfigData.getSearch_axes().add(new CodeSonarChartSearchAxis("Warnings", null, String.format("aid:%s",analysisId), ""));
        chartConfigData.getGroups().add(new CodeSonarChartGroup("Analysis","ID"));
        
        //Serialize chart configuration data as JSON
        Gson gsonSerializer = new GsonBuilder().create();
        String configdata = gsonSerializer.toJson(chartConfigData);

        //Build char_table request URL
        URIBuilder uriBuilder = new URIBuilder(baseHubUri);
        uriBuilder.setPath("/chart_table.json");
        uriBuilder.addParameter("chart", configdata);
        uriBuilder.addParameter("filter", Utils.formatParameter(filter, strictQueryParameters));
        
        URI requestUri = null;
        try {
            requestUri = uriBuilder.build();
        } catch (URISyntaxException e) {
            LOGGER.log(Level.WARNING, "Request URI for Char API contains a syntax error. %nException: {0}%nStack Trace: {1}", new Object[] {e.getMessage(), Throwables.getStackTraceAsString(e)});
            return null;
        }
        String requestUriString = requestUri.toASCIIString();
        
        HttpServiceResponse response = httpService.getResponseFromUrl(requestUriString);
        
        if(response.getStatusCode() != 200) {
            try {
                throw new CodeSonarPluginException("Unexpected error in the response communicating with CodeSonar Hub. %nURI: {0}%nHTTP status code: {1} - {2} %nHTTP Body: {3}", requestUriString, response.getStatusCode(), response.getReasonPhrase(), response.readContent());
            } catch (IOException e) {
                throw new CodeSonarPluginException("Unable to read response content. %nURI: {0}%nException: {1}%nStack Trace: {2}", requestUriString, e.getMessage(), Throwables.getStackTraceAsString(e));
            }
        }

        Gson gsonDeserializer = new Gson();
        CodeSonarWarningCountChartData chartData = null;
        try {
            chartData = gsonDeserializer.fromJson(new InputStreamReader(response.getContentInputStream(), StandardCharsets.UTF_8), CodeSonarWarningCountChartData.class);
            LOGGER.log(Level.INFO, "response chartData={0}", chartData.toString());
        } catch(JsonSyntaxException e) {
            LOGGER.log(Level.WARNING, "failed to parse JSON response. %nException: {0}%nStack Trace: {1}", new Object[] {e.getMessage(), Throwables.getStackTraceAsString(e)});
            return null;
        }
        
        if(chartData.getRows() == null) {
            LOGGER.log(Level.INFO, "chart data is empty.");
            //Explicitly returning the warning counter set to zero
            return new CodeSonarWarningCountChartRow(0);
        }
        
        if(chartData.getRows().size() == 0) {
            LOGGER.log(Level.INFO, "chart contains no rows.");
            //Explicitly returning the warning counter set to zero
            return new CodeSonarWarningCountChartRow(0);
        }
        
        if(chartData.getRows().size() > 1) {
            LOGGER.log(Level.INFO, "chart data does not contain a single row ({0,number,integer}).", chartData.getRows().size());
            return null;
        }
        
        return chartData.getRows().get(0);
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
    public void setNewWarningsFilter(String visibilityFilter) {
        this.newWarningsFilter = visibilityFilter;
    }

    @Override
    public String getNewWarningsFilter() {
        return this.newWarningsFilter;
    }
    
    public String getNewWarningsFilterOrDefault() {
        return StringUtils.isNotBlank(newWarningsFilter) ? newWarningsFilter : IAnalysisService.VISIBILITY_FILTER_NEW_WARNINGS_DEFAULT;
    }

}
