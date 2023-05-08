package org.jenkinsci.plugins.codesonar.services;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.javatuples.Pair;
import org.jenkinsci.plugins.codesonar.AnalysisServiceFactory;
import org.jenkinsci.plugins.codesonar.CodeSonarAlertCounter;
import org.jenkinsci.plugins.codesonar.CodeSonarPluginException;
import org.jenkinsci.plugins.codesonar.models.CodeSonarAnalysisData;
import org.jenkinsci.plugins.codesonar.models.CodeSonarAnalysisWarningCount;
import org.jenkinsci.plugins.codesonar.models.CodeSonarHubInfo;
import org.jenkinsci.plugins.codesonar.models.CodeSonarWarningCount;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.jenkinsci.plugins.codesonar.models.metrics.Metrics;
import org.jenkinsci.plugins.codesonar.models.procedures.ProcedureMetric;
import org.jenkinsci.plugins.codesonar.models.procedures.Procedures;

public class CodeSonarCacheService {
    private static CodeSonarCacheService instance = null;
    private static final Logger LOGGER = Logger.getLogger(CodeSonarCacheService.class.getName());
    
    private HttpService httpService;
    private CodeSonarHubInfo hubInfo;
    private XmlSerializationService xmlSerializationService;
    private AnalysisServiceFactory analysisServiceFactory;
    private IAnalysisService analysisService;
    private MetricsService metricsService;
    private ProceduresService proceduresService;
    private AlertsService alertsService;
    private WarningsService warningsService;
    private Map<Pair<URI,Long>, CodeSonarAnalysisData> cachedAnalyses;

    private CodeSonarCacheService(HttpService httpService, CodeSonarHubInfo hubInfo) {
        this.httpService = httpService;
        this.hubInfo = hubInfo;
        cachedAnalyses = new HashMap<>();
    }
    
    /*
     * This is the method that handles the creation of a new instance (first time only)
     * as well as the one that can return that same instance when already available.
     */
    public static CodeSonarCacheService createInstance(HttpService httpService, CodeSonarHubInfo hubInfo) {
        if(instance == null) {
            instance = new CodeSonarCacheService(httpService, hubInfo);
        }
        return instance;
    }
    
    /*
     * This method is meant for those that need to get the instance, whenever already present,
     * but which don't have the ability to instantiate a new one if not present.
     */
    public static CodeSonarCacheService getInstance() {
        return instance;
    }
    
    private XmlSerializationService getXmlSerializationService() {
        if (xmlSerializationService == null) {
            xmlSerializationService = new XmlSerializationService();
        }
        return xmlSerializationService;
    }
    
    private AnalysisServiceFactory getAnalysisServiceFactory() {
        if (analysisServiceFactory == null) {
            analysisServiceFactory = new AnalysisServiceFactory();
            analysisServiceFactory.setHubInfo(hubInfo);
        }
        return analysisServiceFactory;
    }
    
    private IAnalysisService getAnalysisService() throws CodeSonarPluginException {
        if (analysisService == null) {
            analysisService = getAnalysisServiceFactory().getAnalysisService(httpService, getXmlSerializationService());
        }
        return analysisService;
    }

    private MetricsService getMetricsService() throws CodeSonarPluginException {
        if (metricsService == null) {
            metricsService = new MetricsService(httpService, getXmlSerializationService());
        }
        return metricsService;
    }

    private ProceduresService getProceduresService() throws CodeSonarPluginException {
        if (proceduresService == null) {
            proceduresService = new ProceduresService(httpService, getXmlSerializationService(), hubInfo.isStrictQueryParametersEnforced());
        }
        return proceduresService;
    }
    
    private AlertsService getAlertsService() throws CodeSonarPluginException {
        if (alertsService == null) {
            alertsService = new AlertsService(httpService);
        }
        return alertsService;
    }
    
    private WarningsService getWarningsService() throws CodeSonarPluginException {
        if (warningsService == null) {
            warningsService = new WarningsService(httpService, hubInfo.isStrictQueryParametersEnforced());
        }
        return warningsService;
    }
    
    private CodeSonarAnalysisData getAnalysisDataFromCache(URI baseHubUri, long analysisId) {
        LOGGER.log(Level.INFO, "Getting analysis from cache: baseHubUri = {0}, analysisId = {1,number,0}", new Object[] {baseHubUri, analysisId});
        Pair<URI, Long> key = Pair.with(baseHubUri, analysisId);
        CodeSonarAnalysisData analysisData = cachedAnalyses.get(key);
        if(analysisData == null) {
            LOGGER.log(Level.INFO, "Analysis data not found from cache, putting new instance into cache");
            /*
             * It's convenient to keep both baseHubUri and analysisId in this object, in order
             * for subsequent requests to the hub to be satisfied (i.e. on demand data retrieval).
             */
            analysisData = new CodeSonarAnalysisData(baseHubUri, analysisId);
            cachedAnalyses.put(key, analysisData);
        }
        return analysisData;
    }
    
    public CodeSonarAnalysisWarningCount getNumberOfActiveWarnings(URI baseHubUri, long analysisId, String visibilityFilter) throws IOException {
        LOGGER.log(Level.INFO, "getNumberOfActiveWarnings");
        CodeSonarAnalysisData analysisData = getAnalysisDataFromCache(baseHubUri, analysisId);
        if(analysisData.getActiveWarningsCount() == null) {
            LOGGER.log(Level.INFO, "ActiveWarningsCount not already set, loading from corresponding service");
            analysisData.setActiveWarningsCount(getAnalysisService().getNumberOfWarnings(baseHubUri, analysisId, visibilityFilter));
        }
        CodeSonarAnalysisWarningCount activeWarningsCount = analysisData.getActiveWarningsCount();
        LOGGER.log(Level.INFO, "ActiveWarningsCount new instance {0}", activeWarningsCount);
        return activeWarningsCount;
    }
    
    public CodeSonarAnalysisWarningCount getNumberOfNewWarnings(URI baseHubUri, long analysisId, String visibilityFilter) throws IOException {
        LOGGER.log(Level.INFO, "getNumberOfNewWarnings");
        CodeSonarAnalysisData analysisData = getAnalysisDataFromCache(baseHubUri, analysisId);
        if(analysisData.getNewWarningsCount() == null) {
            LOGGER.log(Level.INFO, "NewWarningsCount not already set, loading from corresponding service");
            analysisData.setNewWarningsCount(getAnalysisService().getNumberOfWarnings(baseHubUri, analysisId, visibilityFilter));
        }
        CodeSonarAnalysisWarningCount newWarningsCount = analysisData.getNewWarningsCount();
        LOGGER.log(Level.INFO, "NewWarningsCount new instance {0}", newWarningsCount);
        return newWarningsCount;
    }
    
    public Analysis getAnalysisActiveWarnings(URI baseHubUri, long analysisId, String visibilityFilter) throws IOException {
        LOGGER.log(Level.INFO, "getAnalysisActiveWarnings");
        getAnalysisService().setVisibilityFilter(visibilityFilter);
        CodeSonarAnalysisData analysisData = getAnalysisDataFromCache(baseHubUri, analysisId);
        if(analysisData.getAnalysisActiveWarnings() == null) {
            LOGGER.log(Level.INFO, "AnalysisActiveWarnings not already set, loading from corresponding service");
            analysisData.setAnalysisActiveWarnings(analysisService.getAnalysisFromUrlWarningsByFilter(baseHubUri, analysisId));
        }
        Analysis analysisActiveWarnings = analysisData.getAnalysisActiveWarnings();
        LOGGER.log(Level.INFO, "AnalysisActiveWarnings new instance {0}", analysisActiveWarnings);
        return analysisActiveWarnings;
    }
    
    public Analysis getAnalysisNewWarnings(URI baseHubUri, long analysisId, String visibilityFilter) throws IOException {
        LOGGER.log(Level.INFO, "getAnalysisNewWarnings");
        getAnalysisService().setNewWarningsFilter(visibilityFilter);
        CodeSonarAnalysisData analysisData = getAnalysisDataFromCache(baseHubUri, analysisId);
        if(analysisData.getAnalysisNewWarnings() == null) {
            LOGGER.log(Level.INFO, "AnalysisNewWarnings not already set, loading from corresponding service");
            analysisData.setAnalysisNewWarnings(analysisService.getAnalysisFromUrlWithNewWarnings(baseHubUri, analysisId));
        }
        Analysis analysisNewWarnings = analysisData.getAnalysisNewWarnings();
        LOGGER.log(Level.INFO, "AnalysisNewWarnings new instance {0}", analysisNewWarnings);
        return analysisNewWarnings;
    }
    
    public Metrics getMetrics(URI baseHubUri, long analysisId) throws IOException {
        LOGGER.log(Level.INFO, "getMetrics");
        CodeSonarAnalysisData analysisData = getAnalysisDataFromCache(baseHubUri, analysisId);
        if(analysisData.getMetrics() == null) {
            LOGGER.log(Level.INFO, "Metrics not already set, loading from corresponding service");
            URI metricsUri = getMetricsService().getMetricsUriFromAnAnalysisId(baseHubUri, String.valueOf(analysisId));
            analysisData.setMetrics(getMetricsService().getMetricsFromUri(metricsUri));
        }
        Metrics metrics = analysisData.getMetrics();
        LOGGER.log(Level.INFO, "Metrics new instance {0}", metrics);
        return metrics;
    }
    
    public Procedures getProcedures(URI baseHubUri, long analysisId) throws IOException {
        LOGGER.log(Level.INFO, "getProcedures");
        CodeSonarAnalysisData analysisData = getAnalysisDataFromCache(baseHubUri, analysisId);
        if(analysisData.getProcedures() == null) {
            LOGGER.log(Level.INFO, "Procedures not already set, loading from corresponding service");
            URI proceduresUri = getProceduresService().getProceduresUriFromAnAnalysisId(baseHubUri, String.valueOf(analysisId));
            analysisData.setProcedures(getProceduresService().getProceduresFromUri(proceduresUri));
        }
        Procedures procedures = analysisData.getProcedures();
        LOGGER.log(Level.INFO, "Procedures new instance {0}", procedures);
        return procedures;
    }
    
    public ProcedureMetric getProcedureWithMaxCyclomaticComplexity(URI baseHubUri, long analysisId) throws IOException {
        LOGGER.log(Level.INFO, "getProcedureWithMaxCyclomaticComplexity");
        CodeSonarAnalysisData analysisData = getAnalysisDataFromCache(baseHubUri, analysisId);
        if(analysisData.getProcedureWithMaxCyclomaticComplexity() == null) {
            LOGGER.log(Level.INFO, "ProcedureWithMaxCyclomaticComplexity not already set, loading from corresponding service");
            analysisData.setProcedureWithMaxCyclomaticComplexity(getProceduresService().getProcedureWithMaxCyclomaticComplexity(baseHubUri, analysisId));
        }
        ProcedureMetric procedureWithMaxCyclomaticComplexity = analysisData.getProcedureWithMaxCyclomaticComplexity();
        LOGGER.log(Level.INFO, "ProcedureWithMaxCyclomaticComplexity new instance {0}", procedureWithMaxCyclomaticComplexity);
        return procedureWithMaxCyclomaticComplexity;
    }
    
    public CodeSonarAlertCounter getAlerts(URI baseHubUri, long analysisId) throws IOException {
        LOGGER.log(Level.INFO, "getAlerts");
        CodeSonarAnalysisData analysisData = getAnalysisDataFromCache(baseHubUri, analysisId);
        if(analysisData.getAlertFrequencies() == null) {
            LOGGER.log(Level.INFO, "Alert frequencies not already set, loading from corresponding service");
            analysisData.setAlertFrequencies(getAlertsService().getAlertFrequencies(baseHubUri, analysisId));
        }
        CodeSonarAlertCounter frequencies = analysisData.getAlertFrequencies();
        LOGGER.log(Level.INFO, "CodeSonarAlertFrequencies new instance {0}", frequencies);
        return frequencies;
    }
    
    public CodeSonarWarningCount getWarningCountAbsoluteWithScoreAboveThreshold(URI baseHubUri, long analysisId, int threshold) throws IOException {
        LOGGER.log(Level.INFO, "getWarningCountAbsoluteWithScoreAboveThreshold threshold={0}", threshold);
        CodeSonarAnalysisData analysisData = getAnalysisDataFromCache(baseHubUri, analysisId);
        if(analysisData.getWarningCountAbsoluteWithScoreAboveThreshold() == null) {
            LOGGER.log(Level.INFO, "Warnings with score above threshold not already set, loading from corresponding service");
            analysisData.setWarningCountAbsoluteWithScoreAboveThreshold(getWarningsService().getNumberOfWarningsWithScoreAboveThreshold(baseHubUri, analysisId, threshold));
        }
        CodeSonarWarningCount warningCountAbsoluteWithScoreAboveThreshold = analysisData.getWarningCountAbsoluteWithScoreAboveThreshold();
        LOGGER.log(Level.INFO, "CodeSonarWarningCount new instance {0}", warningCountAbsoluteWithScoreAboveThreshold);
        return warningCountAbsoluteWithScoreAboveThreshold;
    }
    
    public CodeSonarWarningCount getWarningCountIncreaseWithScoreAboveThreshold(URI baseHubUri, long analysisId, int threshold) throws IOException {
        LOGGER.log(Level.INFO, "getWarningCountIncreaseWithScoreAboveThreshold threshold={0}", threshold);
        CodeSonarAnalysisData analysisData = getAnalysisDataFromCache(baseHubUri, analysisId);
        if(analysisData.getWarningCountIncreaseWithScoreAboveThreshold() == null) {
            LOGGER.log(Level.INFO, "Warnings with score above threshold not already set, loading from corresponding service");
            analysisData.setWarningCountIncreaseWithScoreAboveThreshold(getWarningsService().getNumberOfWarningsWithScoreAboveThreshold(baseHubUri, analysisId, threshold));
        }
        CodeSonarWarningCount warningCountIncreaseWithScoreAboveThreshold = analysisData.getWarningCountIncreaseWithScoreAboveThreshold();
        LOGGER.log(Level.INFO, "CodeSonarWarningCount new instance {0}", warningCountIncreaseWithScoreAboveThreshold);
        return warningCountIncreaseWithScoreAboveThreshold;
    } 
    
    public CodeSonarAnalysisData getCodeSonarAnalysisData(URI baseHubUri, long analysisId, String visibilityFilterAll, String visibilityFilterNew) throws IOException {
        LOGGER.log(Level.INFO, "getCodeSonarAnalysisData: baseHubUri = {0}, analysisId = {1,number,0}, visibilityFilterAll = {2}, visibilityFilterNew = {3}", new Object[] {baseHubUri, analysisId, visibilityFilterAll, visibilityFilterNew});
        getNumberOfActiveWarnings(baseHubUri, analysisId, visibilityFilterAll);
        getNumberOfNewWarnings(baseHubUri, analysisId, visibilityFilterNew);
        /*
         * Temporarily avoided calling the old XML APIs, in order for RAM memory occupation to decrease,
         * at least until when backward compatibility theme will be addressed.
         */
//        getAnalysisActiveWarnings(baseHubUri, analysisId, visibilityFilterAll);
//        getAnalysisNewWarnings(baseHubUri, analysisId, visibilityFilterNew);
//        getMetrics(baseHubUri, analysisId);
//        getProcedures(baseHubUri, analysisId);
        getProcedureWithMaxCyclomaticComplexity(baseHubUri, analysisId);
        getAlerts(baseHubUri, analysisId);
        CodeSonarAnalysisData analysisDataFromCache = getAnalysisDataFromCache(baseHubUri, analysisId);
        LOGGER.log(Level.INFO, "getCodeSonarAnalysisData is returning instance {0}", analysisDataFromCache);
        return analysisDataFromCache;
    }

}
