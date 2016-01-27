package org.jenkinsci.plugins.codesonar;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.util.ChartUtil;
import hudson.util.DataSetBuilder;
import java.io.IOException;
import java.util.List;
import org.javatuples.Pair;
import org.jenkinsci.plugins.codesonar.models.CodeSonarBuildActionDTO;
import org.jenkinsci.plugins.codesonar.models.metrics.Metric;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 *
 * @author andrius
 */
public class CodeSonarBuildAction implements Action {

    private final CodeSonarBuildActionDTO buildActionDTO;

    private final AbstractBuild<?, ?> build;

    public CodeSonarBuildAction(CodeSonarBuildActionDTO buildActionDTO, AbstractBuild<?, ?> build) {
        this.buildActionDTO = buildActionDTO;
        this.build = build;
    }

    @Override
    public String getIconFileName() {
        return "/plugin/codesonar/icons/codesonar-logo.png";
    }

    @Override
    public String getDisplayName() {
        return "CodeSonar analysis";
    }

    @Override
    public String getUrlName() {
        String hubAddress = buildActionDTO.getBaseHubUri();
        String analysisId = buildActionDTO.getAnalysisActiveWarnings().getAnalysisId();
        
        return String.format("http://%s/analysis/%s.html", hubAddress, analysisId);
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

        DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel> dsb = new DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel>();

        ChartUtil.NumberOnlyBuildLabel label = null;

        if (graphName.equals("totalWarnings")) {
            String title = "Total number of warnings";
            for (CodeSonarBuildAction codeSonarBuildAction = this; codeSonarBuildAction != null; codeSonarBuildAction = codeSonarBuildAction.getPreviousAction()) {
                CodeSonarBuildActionDTO prevBuildActionDTO = codeSonarBuildAction.getBuildActionDTO();
                if (prevBuildActionDTO == null) {
                    continue;
                }

                int totalNubmerOfWarnings = prevBuildActionDTO.getAnalysisActiveWarnings().getWarnings().size();
                label = new ChartUtil.NumberOnlyBuildLabel(codeSonarBuildAction.build);

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

                Metric metric = prevBuildActionDTO.getMetrics().getMetricByName("LCodeOnly");
                int value = Integer.parseInt(metric.getValue());

                label = new ChartUtil.NumberOnlyBuildLabel(codeSonarBuildAction.build);

                dsb.add(value, title, label);

            }

            graph.drawGraph(req, rsp, dsb, title);
        }

    }

    public CodeSonarBuildAction getPreviousAction() {
        return getPreviousAction(build);
    }

    private CodeSonarBuildAction getPreviousAction(AbstractBuild<?, ?> base) {
        CodeSonarBuildAction action = null;
        AbstractBuild<?, ?> start = base;
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
