package org.jenkinsci.plugins.codesonar.models;

import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.javatuples.Pair;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.jenkinsci.plugins.codesonar.models.metrics.Metrics;
import org.jenkinsci.plugins.codesonar.models.procedures.Procedures;

/**
 *
 * @author Andrius
 */
public class CodeSonarBuildActionDTO {
    /*
     *  This is the legacy version of this DTO, which implies that all its fields are
     *  populated and thus persisted.
     */
    public static final int MEMORY_GREEDY_VERSION = 1;
    /*
     *  This is the new memory optimized version of this DTO, which requires only fields
     *  "dtoVersion", "analysisId" and "baseHubUri" to be populated and persisted.
     */
    public static final int MEMORY_OPTIMIZED_VERSION = 2;

    private static final Logger LOGGER = Logger.getLogger(CodeSonarBuildActionDTO.class.getName());

    private int dtoVersion;
    private Long analysisId;
    private Analysis analysisActiveWarnings;
    private Analysis analysisNewWarnings;
    private Metrics metrics;
    private Procedures procedures;
    private URI baseHubUri;
    private List<Pair<String, String>> conditionNamesAndResults;
    
    public CodeSonarBuildActionDTO(Long analysisId, URI baseHubUri) {
        this(MEMORY_OPTIMIZED_VERSION, analysisId, null, null, null, null, baseHubUri);
    }
    
    public CodeSonarBuildActionDTO(Long analysisId, Analysis analysisActiveWarnings, Analysis analysisNewWarnings,
            Metrics metrics, Procedures procedures, URI baseHubUri) {
        this(MEMORY_GREEDY_VERSION, analysisId, analysisActiveWarnings, analysisNewWarnings, metrics, procedures, baseHubUri);
    }
    
    private CodeSonarBuildActionDTO(int dtoVersion, Long analysisId, Analysis analysisActiveWarnings, Analysis analysisNewWarnings,
            Metrics metrics, Procedures procedures, URI baseHubUri) {
        this.dtoVersion = dtoVersion;
        this.analysisId = analysisId;
        this.analysisActiveWarnings = analysisActiveWarnings;
        this.analysisNewWarnings = analysisNewWarnings;
        this.metrics = metrics;
        this.procedures = procedures;
        this.baseHubUri = baseHubUri;
    }

    public Long getAnalysisId() {
        return analysisId;
    }

    public Analysis getAnalysisActiveWarnings() {
        return analysisActiveWarnings;
    }

    public Analysis getAnalysisNewWarnings() {
        return analysisNewWarnings;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public Procedures getProcedures() {
        return procedures;
    }

    public URI getBaseHubUri() {
        return baseHubUri;
    }

    public List<Pair<String, String>> getConditionNamesAndResults() {
        return conditionNamesAndResults;
    }
    
    public void setConditionNamesAndResults(List<Pair<String, String>> conditionNamesAndResults) {
        this.conditionNamesAndResults = conditionNamesAndResults;
    }
    
    public int getDtoVersion() {
        return dtoVersion;
    }

    protected Object readResolve() {
        if (dtoVersion == 0) {
            LOGGER.log(Level.WARNING, "Found unassigned DTO version on persisted build, setting it to {0}", MEMORY_GREEDY_VERSION);
            dtoVersion = MEMORY_GREEDY_VERSION;
        }
        
        if (analysisId == null) {
            LOGGER.log(Level.WARNING, "Found empty analysis id on persisted analysis");
        } else {
            LOGGER.log(Level.INFO, "Found analysis id {0} on persisted analysis", analysisId.toString());
        }
        
        if (analysisActiveWarnings == null) {
            LOGGER.log(Level.WARNING, "Found empty analysisActiveWarnings on persisted analysis");
        } else {
            if(analysisId == null) {
                analysisId = Long.valueOf(analysisActiveWarnings.getAnalysisId());
                LOGGER.log(Level.INFO, "Migrating analysis id {0} from active warnings to analysisId", analysisId.toString());
            }
        }
        
        if (analysisNewWarnings == null) {
            LOGGER.log(Level.WARNING, "Found empty analysisNewWarnings on persisted analysis");
        } else {
            if(analysisId == null) {
                analysisId = Long.valueOf(analysisNewWarnings.getAnalysisId());
                LOGGER.log(Level.INFO, "Migrating analysis id {0} from new warnings to analysisId", analysisId.toString());
            }
        }
        
        if (metrics == null) {
            LOGGER.log(Level.WARNING, "Found empty metrics on persisted analysis");
        }
        
        if (procedures == null) {
            LOGGER.log(Level.WARNING, "Found empty procedures on persisted analysis");
        }
        
        if (baseHubUri == null) {
            LOGGER.log(Level.WARNING, "Found empty baseHubUri on persisted analysis");
        }
        
        return this;
    }
}
