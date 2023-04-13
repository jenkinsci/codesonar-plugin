package org.jenkinsci.plugins.codesonar.services;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.javatuples.Pair;
import org.jenkinsci.plugins.codesonar.AnalysisServiceFactory;
import org.jenkinsci.plugins.codesonar.CodeSonarPluginException;
import org.jenkinsci.plugins.codesonar.models.CodeSonarAnalysisData;
import org.jenkinsci.plugins.codesonar.models.CodeSonarAnalysisWarningCount;
import org.jenkinsci.plugins.codesonar.models.CodeSonarHubInfo;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.jenkinsci.plugins.codesonar.models.metrics.Metrics;
import org.jenkinsci.plugins.codesonar.models.procedures.Procedures;

import hudson.model.Run;

public class CodeSonarCacheService {
    
    private HttpService httpService;
    private CodeSonarHubInfo hubInfo;
    private XmlSerializationService xmlSerializationService;
    private AnalysisServiceFactory analysisServiceFactory;
    private IAnalysisService analysisService;
    private MetricsService metricsService;
    private ProceduresService proceduresService;
    private Map<Pair<URI,Long>, CodeSonarAnalysisData> cachedAnalyses;

    public CodeSonarCacheService(HttpService httpService, CodeSonarHubInfo hubInfo) {
        this.httpService = httpService;
        this.hubInfo = hubInfo;
        cachedAnalyses = new HashMap<>();
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
    
    private IAnalysisService getAnalysisService(@Nonnull Run<?, ?> run) throws CodeSonarPluginException {
        if (analysisService == null) {
            analysisService = getAnalysisServiceFactory().getAnalysisService(httpService, getXmlSerializationService());
        }
        return analysisService;
    }

    private MetricsService getMetricsService(@Nonnull Run<?, ?> run) throws CodeSonarPluginException {
        if (metricsService == null) {
            metricsService = new MetricsService(httpService, getXmlSerializationService());
        }
        return metricsService;
    }

    private ProceduresService getProceduresService(@Nonnull Run<?, ?> run) throws CodeSonarPluginException {
        if (proceduresService == null) {
            proceduresService = new ProceduresService(httpService, getXmlSerializationService());
        }
        return proceduresService;
    }
    
    private CodeSonarAnalysisData getAnalysisDataFromCache(URI baseHubUri, long analysisId) {
        Pair<URI, Long> key = Pair.with(baseHubUri, analysisId);
        CodeSonarAnalysisData analysisData = cachedAnalyses.get(key);
        if(analysisData == null) {
            analysisData = new CodeSonarAnalysisData();
            cachedAnalyses.put(key, analysisData);
        }
        return analysisData;
    }
    
    public CodeSonarAnalysisWarningCount getNumberOfActiveWarnings(URI baseHubUri, long analysisId, String visibilityFilter) throws IOException {
        CodeSonarAnalysisData analysisData = getAnalysisDataFromCache(baseHubUri, analysisId);
        if(analysisData.getActiveWarningsCount() == null) {
            analysisData.setActiveWarningsCount(analysisService.getNumberOfWarnings(baseHubUri, analysisId, visibilityFilter));
        }
        return analysisData.getActiveWarningsCount();
    }
    
    public CodeSonarAnalysisWarningCount getNumberOfNewWarnings(URI baseHubUri, long analysisId, String visibilityFilter) throws IOException {
        CodeSonarAnalysisData analysisData = getAnalysisDataFromCache(baseHubUri, analysisId);
        if(analysisData.getNewWarningsCount() == null) {
            analysisData.setNewWarningsCount(analysisService.getNumberOfWarnings(baseHubUri, analysisId, visibilityFilter));
        }
        return analysisData.getNewWarningsCount();
    }
    
    public Analysis getAnalysisActiveWarnings(URI baseHubUri, long analysisId, String visibilityFilter) throws IOException {
        analysisService.setVisibilityFilter(visibilityFilter);
        CodeSonarAnalysisData analysisData = getAnalysisDataFromCache(baseHubUri, analysisId);
        if(analysisData.getAnalysisActiveWarnings() == null) {
            analysisData.setAnalysisActiveWarnings(analysisService.getAnalysisFromUrlWarningsByFilter(baseHubUri, analysisId));
        }
        return analysisData.getAnalysisActiveWarnings();
    }
    
    public Analysis getAnalysisNewWarnings(URI baseHubUri, long analysisId, String visibilityFilter) throws IOException {
        analysisService.setNewWarningsFilter(visibilityFilter);
        CodeSonarAnalysisData analysisData = getAnalysisDataFromCache(baseHubUri, analysisId);
        if(analysisData.getAnalysisNewWarnings() == null) {
            analysisData.setAnalysisNewWarnings(analysisService.getAnalysisFromUrlWithNewWarnings(baseHubUri, analysisId));
        }
        return analysisData.getAnalysisNewWarnings();
    }
    
    public Metrics getMetrics(URI baseHubUri, long analysisId) throws IOException {
        CodeSonarAnalysisData analysisData = getAnalysisDataFromCache(baseHubUri, analysisId);
        if(analysisData.getMetrics() == null) {
            URI metricsUri = metricsService.getMetricsUriFromAnAnalysisId(baseHubUri, String.valueOf(analysisId));
            analysisData.setMetrics(metricsService.getMetricsFromUri(metricsUri));
        }
        return analysisData.getMetrics();
    }
    
    public Procedures getProcedures(URI baseHubUri, long analysisId) throws IOException {
        CodeSonarAnalysisData analysisData = getAnalysisDataFromCache(baseHubUri, analysisId);
        if(analysisData.getProcedures() == null) {
            URI proceduresUri = proceduresService.getProceduresUriFromAnAnalysisId(baseHubUri, String.valueOf(analysisId));
            analysisData.setProcedures(proceduresService.getProceduresFromUri(proceduresUri));
        }
        return analysisData.getProcedures();
    }
    
    public CodeSonarAnalysisData getCodeSonarAnalysisData(URI baseHubUri, long analysisId, String visibilityFilterAll, String visibilityFilterNew) throws IOException {
        getNumberOfActiveWarnings(baseHubUri, analysisId, visibilityFilterAll);
        getNumberOfNewWarnings(baseHubUri, analysisId, visibilityFilterNew);
        getAnalysisActiveWarnings(baseHubUri, analysisId, visibilityFilterAll);
        getAnalysisNewWarnings(baseHubUri, analysisId, visibilityFilterNew);
        getMetrics(baseHubUri, analysisId);
        getProcedures(baseHubUri, analysisId);
        return getAnalysisDataFromCache(baseHubUri, analysisId);
    }

}
