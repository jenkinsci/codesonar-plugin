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
    private static final Logger LOGGER = Logger.getLogger(CodeSonarBuildActionDTO.class.getName());

    private Long analysisId;
    private Analysis analysisActiveWarnings;
    private Analysis analysisNewWarnings;
    private Metrics metrics;
    private Procedures procedures;
    private URI baseHubUri;
    private List<Pair<String, String>> conditionNamesAndResults;

    public CodeSonarBuildActionDTO(Long analysisId, URI baseHubUri) {
        this.analysisId = analysisId;
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
    
    protected Object readResolve() {
        if (analysisId == null) {
            LOGGER.log(Level.WARNING, "Found empty analysis id on persisted analysis");
        } else {
            LOGGER.log(Level.INFO, "Found analysis di {0} on persisted analysis", analysisId.toString());
        }
        
        if (analysisActiveWarnings == null) {
            LOGGER.log(Level.WARNING, "Found empty analysisActiveWarnings on persisted analysis");
        } else {
            if(analysisId == null) {
                analysisId = Long.valueOf(analysisActiveWarnings.getAnalysisId());
                LOGGER.log(Level.INFO, "Migrating analysis di {0} from active warnings to analysisId", analysisId.toString());
            }
        }
        
        if (analysisNewWarnings == null) {
            LOGGER.log(Level.WARNING, "Found empty analysisNewWarnings on persisted analysis");
        } else {
            if(analysisId == null) {
                analysisId = Long.valueOf(analysisNewWarnings.getAnalysisId());
                LOGGER.log(Level.INFO, "Migrating analysis di {0} from new warnings to analysisId", analysisId.toString());
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
