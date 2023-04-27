package org.jenkinsci.plugins.codesonar.services;

import static org.jenkinsci.plugins.codesonar.models.CodeSonarChartConfigData.CHART_KIND_BAR;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.jenkinsci.plugins.codesonar.CodeSonarLogger;
import org.jenkinsci.plugins.codesonar.CodeSonarPluginException;
import org.jenkinsci.plugins.codesonar.models.CodeSonarAnalysisWarningCount;
import org.jenkinsci.plugins.codesonar.models.CodeSonarChartConfigData;
import org.jenkinsci.plugins.codesonar.models.CodeSonarChartData;
import org.jenkinsci.plugins.codesonar.models.CodeSonarChartGroup;
import org.jenkinsci.plugins.codesonar.models.CodeSonarChartSearchAxis;
import org.jenkinsci.plugins.codesonar.models.SearchResults;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
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
    
    private void scanHubResult(InputStream inputStream) throws IOException {
        Scanner sc = null;
        try {
            sc = new Scanner(inputStream, "UTF-8");
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
            }
            //Scanner by default suppresses exceptions
            if (sc.ioException() != null) {
                throw sc.ioException();
            }
        } finally {
            if (sc != null) {
                sc.close();
            }
        }
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
    public CodeSonarAnalysisWarningCount getNumberOfWarnings(URI baseHubUri, long analysisId, String filter) throws IOException {
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
        
        InputStream jsonContent = httpService.getContentFromUrlAsInputStream(requestUri.toASCIIString());

        Gson gsonDeserializer = new Gson();
        CodeSonarChartData chartData = null;
        try {
            chartData = gsonDeserializer.fromJson(new InputStreamReader(jsonContent, StandardCharsets.UTF_8), CodeSonarChartData.class);
            LOGGER.log(Level.INFO, "response chartData={0}", chartData.toString());
        } catch(JsonSyntaxException e) {
            LOGGER.log(Level.WARNING, "failed to parse JSON response. %nException: {0}%nStack Trace: {1}", new Object[] {e.getMessage(), Throwables.getStackTraceAsString(e)});
            return null;
        }
        
        if(chartData.getRows() == null) {
            LOGGER.log(Level.INFO, "chart data is empty.");
            //Explicitly returning the warning counter set to zero
            return new CodeSonarAnalysisWarningCount(0);
        }
        
        if(chartData.getRows().size() == 0) {
            LOGGER.log(Level.INFO, "chart contains no rows.");
            //Explicitly returning the warning counter set to zero
            return new CodeSonarAnalysisWarningCount(0);
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
