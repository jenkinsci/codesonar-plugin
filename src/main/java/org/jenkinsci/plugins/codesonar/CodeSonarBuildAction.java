package org.jenkinsci.plugins.codesonar;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.util.ChartUtil;
import hudson.util.DataSetBuilder;
import java.io.IOException;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.jenkinsci.plugins.codesonar.models.metrics.Metric;
import org.jenkinsci.plugins.codesonar.models.metrics.Metrics;
import org.jenkinsci.plugins.codesonar.models.procedures.Procedures;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 *
 * @author andrius
 */
public class CodeSonarBuildAction implements Action {

    private final Analysis analysis;
    private final Metrics metrics;
    private final Procedures procedures;
    private final String hubAddress;
    private final AbstractBuild<?, ?> build;

    public CodeSonarBuildAction(Analysis analysis, Metrics metrics, Procedures procedures, String hubAddress, AbstractBuild<?, ?> build) {
        this.analysis = analysis;
        this.metrics = metrics;
        this.procedures = procedures;
        this.hubAddress = hubAddress;
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
        return String.format("http://%s/analysis/%s.html", hubAddress, getAnalysis().getAnalysisId());
    }

    public Analysis getAnalysis() {
        return analysis;
    }

    public Metrics getMetrics() {
        return metrics;
    }
    
    public Procedures getProcedures() {
        return procedures;
    }

    public void doReportGraphs(StaplerRequest req, StaplerResponse rsp) throws IOException {
        String graphName = req.getParameter("name");

        CodeSonarGraph graph = new CodeSonarGraph();

        DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel> dsb = new DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel>();

        ChartUtil.NumberOnlyBuildLabel label = null;

        if (graphName.equals("totalWarnings")) {
            String title = "Total number of warnings";
            for (CodeSonarBuildAction codeSonarBuildAction = this; codeSonarBuildAction != null; codeSonarBuildAction = codeSonarBuildAction.getPreviousAction()) {
                if (codeSonarBuildAction.getAnalysis() == null) {
                    continue;
                }

                int totalNubmerOfWarnings = codeSonarBuildAction.getAnalysis().getWarnings().size();
                label = new ChartUtil.NumberOnlyBuildLabel(codeSonarBuildAction.build);

                dsb.add(totalNubmerOfWarnings, title, label);
            }

            graph.drawGraph(req, rsp, dsb, title);
        } else if (graphName.equals("loc")) {
            String title = "Lines Of Code";
            for (CodeSonarBuildAction codeSonarBuildAction = this; codeSonarBuildAction != null; codeSonarBuildAction = codeSonarBuildAction.getPreviousAction()) {
                if (codeSonarBuildAction.getMetrics() == null) {
                    continue;
                }

                Metric metric = codeSonarBuildAction.getMetrics().getMetricByName("LCodeOnly");
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
