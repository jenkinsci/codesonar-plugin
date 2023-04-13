package org.jenkinsci.plugins.codesonar;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.javatuples.Pair;
import org.jenkinsci.plugins.codesonar.models.CodeSonarBuildActionDTO;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.jenkinsci.plugins.codesonar.models.metrics.Metric;
import org.jenkinsci.plugins.codesonar.models.metrics.Metrics;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.model.Action;
import hudson.model.Run;
import hudson.util.ChartUtil;
import hudson.util.DataSetBuilder;
        
/**
 *
 * @author andrius
 */
public class CodeSonarBuildAction implements Action {
    private static final Logger LOGGER = Logger.getLogger(CodeSonarBuildAction.class.getName());
    
    private final CodeSonarBuildActionDTO buildActionDTO;
    private final Run<?, ?> run;
    private final String projectName;
    private String analysisUrl;
    
    public CodeSonarBuildAction(CodeSonarBuildActionDTO buildActionDTO, Run<?, ?> run, String projectName) {
        this.buildActionDTO = buildActionDTO;
        this.run = run;
        this.projectName = projectName;
    }
    
    public String getProjectName() {
        return projectName;
    }
    
    public String getAnalysisUrl() {
        return analysisUrl;
    }

    @Override
    public String getIconFileName() {
        return "/plugin/codesonar/icons/codesonar-logo.png";
    }

    @Override
    public String getDisplayName() {
        return "CodeSonar (" +projectName + ")";
    }

    @Override
    public String getUrlName() {
        URI baseHubUri = buildActionDTO.getBaseHubUri();
        if(baseHubUri == null) {
            LOGGER.log(Level.WARNING, "\"baseHubUri\" not found in persisted build, returning empty URL name");
            return StringUtils.EMPTY;
        }
        Analysis analysisActiveWarnings = buildActionDTO.getAnalysisActiveWarnings();
        if(analysisActiveWarnings == null) {
            LOGGER.log(Level.WARNING, "\"analysisActiveWarnings\" data not found in persisted build, returning empty URL name");
            return StringUtils.EMPTY;
        }
        String analysisId = analysisActiveWarnings.getAnalysisId();
        return baseHubUri.resolve(String.format("/analysis/%s.html", analysisId)).toString();
    }

    public CodeSonarBuildActionDTO getBuildActionDTO() {
        return buildActionDTO;
    }
    
    public List<Pair<String, String>> getConditionNamesAndResults() {
        return buildActionDTO.getConditionNamesAndResults();
    }
    
    public void doReportGraphs(StaplerRequest req, StaplerResponse rsp) throws IOException {
        String graphName = req.getParameter("name");
        CodeSonarGraph graph = new CodeSonarGraph();
        DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel> dsb = new DataSetBuilder<>();
        ChartUtil.NumberOnlyBuildLabel label = null;

        if (graphName.equals("totalWarnings")) {
            String title = "Total number of warnings";
            for (CodeSonarBuildAction codeSonarBuildAction = this; codeSonarBuildAction != null; codeSonarBuildAction = codeSonarBuildAction.getPreviousAction()) {
                CodeSonarBuildActionDTO prevBuildActionDTO = codeSonarBuildAction.getBuildActionDTO();
                if (prevBuildActionDTO == null) {
                    continue;
                }
                 
                int totalNubmerOfWarnings = 0;
                Analysis analysisActiveWarnings = prevBuildActionDTO.getAnalysisActiveWarnings();
                if(analysisActiveWarnings != null) {
                    totalNubmerOfWarnings = analysisActiveWarnings.getWarnings().size();
                }
                label = new ChartUtil.NumberOnlyBuildLabel((Run<?, ?>)codeSonarBuildAction.run);
                dsb.add(totalNubmerOfWarnings, title, label);
            }

            graph.drawGraph(req, rsp, dsb, title);
        } else if (graphName.equals("loc")) {
            String title = "Lines Of Code";
            for (CodeSonarBuildAction codeSonarBuildAction = this; codeSonarBuildAction != null; codeSonarBuildAction = codeSonarBuildAction.getPreviousAction()) {
                CodeSonarBuildActionDTO prevBuildActionDTO = codeSonarBuildAction.getBuildActionDTO();
                if (prevBuildActionDTO == null) {
                    continue;
                }

                Metrics metrics = prevBuildActionDTO.getMetrics();
                //Skip current build action iteration if metrics are missing
                if(metrics == null) {
                    continue;
                }
                Metric metric = metrics.getMetricByName("LCodeOnly");
                int value = Integer.parseInt(metric.getValue());
                label = new ChartUtil.NumberOnlyBuildLabel((Run<?, ?>)codeSonarBuildAction.run);
                dsb.add(value, title, label);
            }

            graph.drawGraph(req, rsp, dsb, title);
        }

    }

    public CodeSonarBuildAction getPreviousAction() {
        return getPreviousAction(run);
    }

    private CodeSonarBuildAction getPreviousAction(Run<?, ?> base) {
        CodeSonarBuildAction action = null;
        Run<?, ?> start = base;
        while (true) {
            start = start.getPreviousNotFailedBuild();
            if (start == null) {
                return null;
            }
            action = start.getAction(CodeSonarBuildAction.class);
            if (action != null) {
                return action;
            }
        }
    }
}
