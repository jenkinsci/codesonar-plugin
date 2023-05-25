package org.jenkinsci.plugins.codesonar.services;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.plugins.codesonar.CodeSonarPluginException;
import org.jenkinsci.plugins.codesonar.models.CodeSonarAlertLevels;
import org.jenkinsci.plugins.codesonar.models.CodeSonarHubInfo;
import org.jenkinsci.plugins.codesonar.models.Metric;
import org.jenkinsci.plugins.codesonar.models.ProcedureMetric;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.jenkinsci.plugins.codesonar.models.analysis.Warning;
import org.jenkinsci.plugins.codesonar.models.json.CodeSonarWarningCountChartRow;
import org.jenkinsci.plugins.codesonar.models.metrics.Metrics;
import org.jenkinsci.plugins.codesonar.models.procedures.ProcedureRow;
import org.jenkinsci.plugins.codesonar.models.procedures.Procedures;

/**
 * Loads and caches data related to a single analysis, for use by "Condition" classes.
 * This class implements two distinct modes for loading data: a "legacy" mode, and the current mode.
 * The legacy mode loads large lists of warnings and procedures into main memory.
 * This was the only mode implemented through plugin version 3.3.x.
 * The current mode fetches analysis data in a more memory-safe way, but it requires CodeSonar hub JSON APIs,
 * available only in CodeSonar 7.3 and later (i.e. with the "gridConfigJson" capability).
 * 
 * @author aseno
 *
 */
public class CodeSonarHubAnalysisDataLoader {
    private static final Logger LOGGER = Logger.getLogger(CodeSonarHubAnalysisDataLoader.class.getName());
    
    protected CodeSonarServices services;
    protected CodeSonarHubInfo hubInfo;
    private URI baseHubUri;
    private Long analysisId;
    private String visibilityFilter;
    private String newWarningsVisibilityFilter;
    //------------------------------------------------------------
    //Set of fields that will be loaded through CodeSonar services
    //------------------------------------------------------------
    private CodeSonarWarningCountChartRow activeWarningsForAnalysis;
    private CodeSonarWarningCountChartRow newWarningsForAnalysis;
    private Analysis analysisViewActive;
    private Analysis analysisViewNew;
    private Metrics metrics;
    private Procedures procedures;
    private ProcedureMetric procedureWithMaxCyclomaticComplexity;
    private CodeSonarAlertCounter alertCounter;
    private Map<Integer,Long> numberOfWarningsAboveThreshold = new HashMap<>();
    //------------------------------------------------------------
    
    public CodeSonarHubAnalysisDataLoader(HttpService httpService, CodeSonarHubInfo hubInfo, URI baseHubUri, Long analysisId, String visibilityFilter, String newWarningsVisibilityFilter) {
        this.services = new CodeSonarServices(httpService, hubInfo);
        this.hubInfo = hubInfo;
        this.baseHubUri = baseHubUri;
        this.analysisId = analysisId;
        this.visibilityFilter = visibilityFilter;
        this.newWarningsVisibilityFilter = newWarningsVisibilityFilter;
    }
    
    public URI getBaseHubUri() {
        return baseHubUri;
    }
    
    protected Long getAnalysisId() {
        return analysisId;
    }

    protected String getVisibilityFilter() {
        return visibilityFilter;
    }

    protected String getNewWarningsVisibilityFilter() {
        return newWarningsVisibilityFilter;
    }
    
    protected Analysis getLegacyAnalysisViewActive() throws CodeSonarPluginException {
        LOGGER.log(Level.INFO, "AnalysisViewActive not already set, loading from corresponding legacy service");
        services.getAnalysisService().setVisibilityFilter(visibilityFilter);
        return services.getAnalysisService().getAnalysisFromUrlWarningsByFilter(getBaseHubUri(), getAnalysisId());
    }

    protected Analysis getLegacyAnalysisViewNew() throws CodeSonarPluginException {
        LOGGER.log(Level.INFO, "AnalysisViewNew not already set, loading from corresponding legacy service");
        services.getAnalysisService().setVisibilityFilter(newWarningsVisibilityFilter);
        return services.getAnalysisService().getAnalysisFromUrlWithNewWarnings(getBaseHubUri(), getAnalysisId());
    }

    protected Procedures getLegacyProcedures() throws CodeSonarPluginException {
        LOGGER.log(Level.INFO, "Procedures not already set, loading from corresponding legacy service");
        URI proceduresUri = services.getProceduresService().getProceduresUriFromAnAnalysisId(baseHubUri, String.valueOf(analysisId));
        return services.getProceduresService().getProceduresFromUri(proceduresUri);
    }
    
    protected Metrics getLegacyMetrics() throws CodeSonarPluginException {
        LOGGER.log(Level.INFO, "Metrics not already set, loading from corresponding legacy service");
        URI metricsUri = services.getMetricsService().getMetricsUriFromAnAnalysisId(getBaseHubUri(), String.valueOf(analysisId));
        return services.getMetricsService().getMetricsFromUri(metricsUri);
    }
    
    public long getNumberOfActiveWarnings() throws CodeSonarPluginException {
        LOGGER.log(Level.INFO, "getNumberOfActiveWarnings");
        if(hubInfo.isJsonGridConfigSupported()) {
            if(activeWarningsForAnalysis == null) {
                LOGGER.log(Level.INFO, "ActiveWarningsCount not already set, loading from corresponding service");
                activeWarningsForAnalysis = services.getAnalysisService().getNumberOfWarnings(getBaseHubUri(), getAnalysisId(), getVisibilityFilter());
                LOGGER.log(Level.INFO, "ActiveWarningsCount new instance {0}", activeWarningsForAnalysis);
            }
            return activeWarningsForAnalysis.getNumberOfWarnings();
        } else {
            if(analysisViewActive == null) {
                analysisViewActive = getLegacyAnalysisViewActive();
                LOGGER.log(Level.INFO, "Legacy AnalysisViewActive new instance {0}", analysisViewActive);
            }
            return (long) analysisViewActive.getWarnings().size();
        }
    }
    
    public long getNumberOfNewWarnings() throws CodeSonarPluginException {
        LOGGER.log(Level.INFO, "getNumberOfNewWarnings");
        if(hubInfo.isJsonGridConfigSupported()) {
            if(newWarningsForAnalysis == null) {
                LOGGER.log(Level.INFO, "NewWarningsCount not already set, loading from corresponding service");
                newWarningsForAnalysis = services.getAnalysisService().getNumberOfWarnings(getBaseHubUri(), getAnalysisId(), getNewWarningsVisibilityFilter());
                LOGGER.log(Level.INFO, "NewWarningsCount new instance {0}", newWarningsForAnalysis);
            }
            return newWarningsForAnalysis.getNumberOfWarnings();
        } else {
            if(analysisViewNew == null) {
                analysisViewNew = getLegacyAnalysisViewNew();
                LOGGER.log(Level.INFO, "Legacy AnalysisViewNew new instance {0}", analysisViewNew);
            }
            return analysisViewNew.getWarnings().size();
        }
    }

    public ProcedureMetric getProcedureWithMaxCyclomaticComplexity() throws CodeSonarPluginException {
        LOGGER.log(Level.INFO, "getProcedureWithMaxCyclomaticComplexity");
        if(hubInfo.isJsonGridConfigSupported()) {
            if(procedureWithMaxCyclomaticComplexity == null) {
                LOGGER.log(Level.INFO, "ProcedureWithMaxCyclomaticComplexity not already set, loading from corresponding service");
                procedureWithMaxCyclomaticComplexity = services.getProceduresService().getProcedureWithMaxCyclomaticComplexity(getBaseHubUri(), getAnalysisId());
            }
            LOGGER.log(Level.INFO, "ProcedureWithMaxCyclomaticComplexity new instance {0}", procedureWithMaxCyclomaticComplexity);
            return procedureWithMaxCyclomaticComplexity;
        } else {
            if(procedures == null) {
                procedures = getLegacyProcedures();
                LOGGER.log(Level.INFO, "Legacy Procedures new instance {0}", procedures);
            }
            List<ProcedureRow> procedureRows = procedures.getProcedureRows();
            String procedure = null;
            int maxCC = 0;
            for (ProcedureRow procedureRow : procedureRows) {
                Metric cyclomaticComplexityMetric = procedureRow.getMetricByName("Cyclomatic Complexity");

                int value = Integer.parseInt(cyclomaticComplexityMetric.getValue());
                if (value > maxCC) {
                    maxCC = value;
                    procedure = procedureRow.getProcedure();
                }
            }
            //If at least one procedure has been found, then return that
            if(procedure != null) {
                return new ProcedureMetric(maxCC, procedure);
            } else {
                return null;
            }
        }
    }

    public int getNumberOfAlerts(CodeSonarAlertLevels level) throws CodeSonarPluginException {
        LOGGER.log(Level.INFO, "getNumberOfAlerts");
        if(hubInfo.isJsonGridConfigSupported()) {
            if(alertCounter == null) {
                LOGGER.log(Level.INFO, "AlertCounter not already set, loading from corresponding service");
                alertCounter = services.getAlertsService().getAlertCounter(getBaseHubUri(), getAnalysisId());
                LOGGER.log(Level.INFO, "AlertCounter new instance {0}", alertCounter);
            }
            return alertCounter.getAlertCount(level);
        } else {
            if(analysisViewActive == null) {
                LOGGER.log(Level.INFO, "AnalysisViewActive not already set, loading from corresponding service");
                analysisViewActive = getLegacyAnalysisViewActive();
                LOGGER.log(Level.INFO, "AnalysisViewActive new instance {0}", analysisViewActive);
            }
            return analysisViewActive.getRedAlerts().size();
        }
    }

    public long getNumberOfWarningsWithScoreAboveThreshold(int threshold) throws CodeSonarPluginException {
        LOGGER.log(Level.INFO, "getNumberOfWarningsWithScoreAboveThreshold");
        if(hubInfo.isJsonGridConfigSupported()) {
            if(numberOfWarningsAboveThreshold.get(threshold) == null) {
                LOGGER.log(Level.INFO, "NumberOfWarningsAboveThreshold not already set for threshold {0}, loading from corresponding service", threshold);
                long numberOfWarnings = services.getWarningsService().getNumberOfWarningsWithScoreAboveThreshold(getBaseHubUri(), getAnalysisId(), threshold);
                numberOfWarningsAboveThreshold.put(threshold, numberOfWarnings);
                LOGGER.log(Level.INFO, "NumberOfWarningsAboveThreshold new value {0}", numberOfWarnings);
            }
            return numberOfWarningsAboveThreshold.get(threshold);
        } else {
            if(analysisViewActive == null) {
                LOGGER.log(Level.INFO, "AnalysisViewActive not already set, loading from corresponding service");
                analysisViewActive = getLegacyAnalysisViewActive();
                LOGGER.log(Level.INFO, "AnalysisViewActive new instance {0}", analysisViewActive);
            }
            
            long severeWarnings = 0;
            List<Warning> warnings = analysisViewActive.getWarnings();
            for (Warning warning : warnings) {
                if (warning.getScore() >= threshold) {
                    severeWarnings++;
                }
            }
            return severeWarnings;
        }
    }

}
