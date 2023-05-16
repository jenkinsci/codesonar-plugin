package org.jenkinsci.plugins.codesonar.api;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.plugins.codesonar.models.CodeSonarBuildActionDTO;
import org.jenkinsci.plugins.codesonar.models.CodeSonarHubInfo;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.jenkinsci.plugins.codesonar.models.procedures.Procedures;
import org.jenkinsci.plugins.codesonar.services.HttpService;

public class CodeSonarDTOAnalysisDataLoader extends CodeSonarHubAnalysisDataLoader {
    private static final Logger LOGGER = Logger.getLogger(CodeSonarDTOAnalysisDataLoader.class.getName());
    
    private CodeSonarBuildActionDTO previousBuildDTO;

    public CodeSonarDTOAnalysisDataLoader(HttpService httpService, CodeSonarHubInfo hubInfo, CodeSonarBuildActionDTO previousBuildDTO, String visibilityFilter, String newWarningsVisibilityFilter) {
        super(httpService, hubInfo, previousBuildDTO.getBaseHubUri(), previousBuildDTO.getAnalysisId(), visibilityFilter, newWarningsVisibilityFilter);
        this.previousBuildDTO = previousBuildDTO;
    }

    @Override
    protected Analysis getAnalysisViewActive() throws IOException {
        if (!isLoadFromDTOSupported()) {
            LOGGER.log(Level.INFO, "Cannot load AnalysisViewActive from DTO, DTO version does not permit it.");
            return null;
        }
        LOGGER.log(Level.INFO, "Retrieving AnalysisViewActive from DTO");
        return previousBuildDTO.getAnalysisActiveWarnings();
    }

    @Override
    protected Analysis getAnalysisViewNew() throws IOException {
        if (!isLoadFromDTOSupported()) {
            LOGGER.log(Level.INFO, "Cannot load AnalysisViewNew from DTO, DTO version does not permit it.");
            return null;
        }
        LOGGER.log(Level.INFO, "Retrieving AnalysisViewNew from DTO");
        return previousBuildDTO.getAnalysisNewWarnings();
    }

    @Override
    protected Procedures getProcedures() throws IOException {
        if (!isLoadFromDTOSupported()) {
            LOGGER.log(Level.INFO, "Cannot load Procedures from DTO, DTO version does not permit it.");
            return null;
        }
        LOGGER.log(Level.INFO, "Retrieving Procedures from DTO");
        return previousBuildDTO.getProcedures();
    }
    
    /**
     * Data can be loaded directly from DTO only if it is a fat DTO,
     * which implies that it is a self-contained DTO that doesn't
     * need further data to be retrieved from the hub.
     * @return true if data can be loaded directly from the DTO
     */
    public boolean isLoadFromDTOSupported() {
        return previousBuildDTO.getDtoVersion() == CodeSonarBuildActionDTO.VERSION_FAT;
    }
    
}
