package org.jenkinsci.plugins.codesonar.api;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.plugins.codesonar.CodeSonarAlertCounter;
import org.jenkinsci.plugins.codesonar.CodeSonarAlertLevels;
import org.jenkinsci.plugins.codesonar.models.CodeSonarBuildActionDTO;
import org.jenkinsci.plugins.codesonar.models.CodeSonarHubInfo;
import org.jenkinsci.plugins.codesonar.models.Metric;
import org.jenkinsci.plugins.codesonar.models.ProcedureMetric;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.jenkinsci.plugins.codesonar.models.analysis.Warning;
import org.jenkinsci.plugins.codesonar.models.json.CodeSonarAnalysisWarningCount;
import org.jenkinsci.plugins.codesonar.models.metrics.Metrics;
import org.jenkinsci.plugins.codesonar.models.procedures.ProcedureRow;
import org.jenkinsci.plugins.codesonar.models.procedures.Procedures;
import org.jenkinsci.plugins.codesonar.services.HttpService;

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
    private CodeSonarAnalysisWarningCount activeWarningsCount;
    private CodeSonarAnalysisWarningCount newWarningsCount;
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
    
    protected Analysis getAnalysisViewActive() throws IOException {
        LOGGER.log(Level.INFO, "AnalysisViewActive not already set, loading from corresponding service");
        services.getAnalysisService().setVisibilityFilter(visibilityFilter);
        return services.getAnalysisService().getAnalysisFromUrlWarningsByFilter(getBaseHubUri(), getAnalysisId());
    }

    protected Analysis getAnalysisViewNew() throws IOException {
        LOGGER.log(Level.INFO, "AnalysisViewNew not already set, loading from corresponding service");
        services.getAnalysisService().setVisibilityFilter(newWarningsVisibilityFilter);
        return services.getAnalysisService().getAnalysisFromUrlWithNewWarnings(getBaseHubUri(), getAnalysisId());
    }

    protected Procedures getProcedures() throws IOException {
        LOGGER.log(Level.INFO, "Procedures not already set, loading from corresponding service");
        URI proceduresUri = services.getProceduresService().getProceduresUriFromAnAnalysisId(baseHubUri, String.valueOf(analysisId));
        return services.getProceduresService().getProceduresFromUri(proceduresUri);
    }
    
    protected Metrics getMetrics() throws IOException {
        LOGGER.log(Level.INFO, "Metrics not already set, loading from corresponding service");
        URI metricsUri = services.getMetricsService().getMetricsUriFromAnAnalysisId(getBaseHubUri(), String.valueOf(analysisId));
        return services.getMetricsService().getMetricsFromUri(metricsUri);
    }
    
    public Long getNumberOfActiveWarnings() throws IOException {
        LOGGER.log(Level.INFO, "getNumberOfActiveWarnings");
        if(hubInfo.isJsonGridConfigSupported()) {
            if(activeWarningsCount == null) {
                LOGGER.log(Level.INFO, "ActiveWarningsCount not already set, loading from corresponding service");
                activeWarningsCount = services.getAnalysisService().getNumberOfWarnings(getBaseHubUri(), getAnalysisId(), getVisibilityFilter());
                LOGGER.log(Level.INFO, "ActiveWarningsCount new instance {0}", activeWarningsCount);
                if(activeWarningsCount == null) {
                    return null;
                }
            }
            return activeWarningsCount.getNumberOfWarnings();
        } else {
            if(analysisViewActive == null) {
                analysisViewActive = getAnalysisViewActive();
                LOGGER.log(Level.INFO, "AnalysisViewActive new instance {0}", analysisViewActive);
                if(analysisViewActive == null) {
                    return null;
                }
            }
            return (long) analysisViewActive.getWarnings().size();
        }
    }
    
    public Long getNumberOfNewWarnings() throws IOException {
        LOGGER.log(Level.INFO, "getNumberOfNewWarnings");
        if(hubInfo.isJsonGridConfigSupported()) {
            if(newWarningsCount == null) {
                LOGGER.log(Level.INFO, "NewWarningsCount not already set, loading from corresponding service");
                newWarningsCount = services.getAnalysisService().getNumberOfWarnings(getBaseHubUri(), getAnalysisId(), getNewWarningsVisibilityFilter());
                LOGGER.log(Level.INFO, "NewWarningsCount new instance {0}", newWarningsCount);
                if(newWarningsCount == null) {
                    return null;
                }
            }
            return newWarningsCount.getNumberOfWarnings();
        } else {
            if(analysisViewNew == null) {
                analysisViewNew = getAnalysisViewNew();
                LOGGER.log(Level.INFO, "AnalysisViewNew new instance {0}", analysisViewNew);
                if(analysisViewNew == null) {
                    return null;
                }
            }
            return (long) analysisViewNew.getWarnings().size();
        }
    }

    public ProcedureMetric getProcedureWithMaxCyclomaticComplexity() throws IOException {
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
                procedures = getProcedures();
                LOGGER.log(Level.INFO, "Procedures new instance {0}", procedures);
                if(procedures == null) {
                    return null;
                }
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

    public Integer getNumberOfAlerts(CodeSonarAlertLevels level) throws IOException {
        LOGGER.log(Level.INFO, "getNumberOfAlerts");
        if(hubInfo.isJsonGridConfigSupported()) {
            if(alertCounter == null) {
                LOGGER.log(Level.INFO, "AlertCounter not already set, loading from corresponding service");
                alertCounter = services.getAlertsService().getAlertCounter(getBaseHubUri(), getAnalysisId());
                LOGGER.log(Level.INFO, "AlertCounter new instance {0}", alertCounter);
                if(alertCounter == null) {
                    return null;
                }
            }
            return alertCounter.getAlertCount(level);
        } else {
            if(analysisViewActive == null) {
                LOGGER.log(Level.INFO, "AnalysisViewActive not already set, loading from corresponding service");
                analysisViewActive = getAnalysisViewActive();
                LOGGER.log(Level.INFO, "AnalysisViewActive new instance {0}", analysisViewActive);
                if(analysisViewActive == null) {
                    return null;
                }
            }
            return analysisViewActive.getRedAlerts().size();
        }
    }

    public Long getNumberOfWarningsWithScoreAboveThreshold(int threshold) throws IOException {
        LOGGER.log(Level.INFO, "getNumberOfWarningsWithScoreAboveThreshold");
        if(hubInfo.isJsonGridConfigSupported()) {
            if(numberOfWarningsAboveThreshold.get(threshold) == null) {
                LOGGER.log(Level.INFO, "NumberOfWarningsAboveThreshold not already set for threshold {0}, loading from corresponding service", threshold);
                Long numberOfWarnings = services.getWarningsService().getNumberOfWarningsWithScoreAboveThreshold(getBaseHubUri(), getAnalysisId(), threshold);
                numberOfWarningsAboveThreshold.put(threshold, numberOfWarnings);
                LOGGER.log(Level.INFO, "NumberOfWarningsAboveThreshold new value {0}", numberOfWarnings);
            }
            return numberOfWarningsAboveThreshold.get(threshold);
        } else {
            if(analysisViewActive == null) {
                LOGGER.log(Level.INFO, "AnalysisViewActive not already set, loading from corresponding service");
                analysisViewActive = getAnalysisViewActive();
                LOGGER.log(Level.INFO, "AnalysisViewActive new instance {0}", analysisViewActive);
                if(analysisViewActive == null) {
                    return null;
                }
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

    /**
     * Preemptive loading is necessary only when a CodeSonar hub with JSON capabilities
     * is not available and we want to keep compatibility with legacy fat style DTOs from
     * previous Jenkins' builds.
     * @return A <code>CodeSonarBuildActionDTO</code> fat style object containing all
     * the data that need to be persisted on disk.
     * @throws IOException
     */
    public CodeSonarBuildActionDTO preloadAll() throws IOException {
        //Preload analysis view active
        if(analysisViewActive == null) {
            analysisViewActive = getAnalysisViewActive();
            LOGGER.log(Level.INFO, "AnalysisViewActive new instance {0}", analysisViewActive);
        }
        //Preload analysis view new
        if(analysisViewNew == null) {
            analysisViewNew = getAnalysisViewNew();
            LOGGER.log(Level.INFO, "AnalysisViewNew new instance {0}", analysisViewNew);
        }
        //Preload metrics
        if(metrics == null) {
            metrics = getMetrics();
            LOGGER.log(Level.INFO, "Metrics new instance {0}", metrics);
        }
        //Preload procedures
        if(procedures == null) {
            procedures = getProcedures();
            LOGGER.log(Level.INFO, "Procedures new instance {0}", procedures);
        }
        
        return new CodeSonarBuildActionDTO(CodeSonarBuildActionDTO.VERSION_FAT,
                analysisId,
                analysisViewActive,
                analysisViewNew,
                metrics,
                procedures,
                baseHubUri);
    }
}
