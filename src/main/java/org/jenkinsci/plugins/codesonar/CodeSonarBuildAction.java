package org.jenkinsci.plugins.codesonar;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.util.ChartUtil;
import hudson.util.DataSetBuilder;
import java.io.IOException;
import org.jenkinsci.plugins.codesonar.models.Analysis;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 *
 * @author andrius
 */
public class CodeSonarBuildAction implements Action {

    private final Analysis analysis;
    private final String hubAddress;
    private final AbstractBuild<?, ?> build;

    public CodeSonarBuildAction(Analysis analysis, String hubAddress, AbstractBuild<?, ?> build) {
        this.analysis = analysis;
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

    public void doReportGraphs(StaplerRequest req, StaplerResponse rsp) throws IOException {
        CodeSonarGraph graph = new CodeSonarGraph();

        DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel> dsb = new DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel>();

        ChartUtil.NumberOnlyBuildLabel label = null;

        for (CodeSonarBuildAction prqabuild = this; prqabuild != null; prqabuild = prqabuild.getPreviousAction()) {
            int totalNubmerOfWarnings = prqabuild.getAnalysis().getWarnings().size();
            label = new ChartUtil.NumberOnlyBuildLabel(prqabuild.build);

            dsb.add(totalNubmerOfWarnings, "Total number of warnings", label);
        }

        graph.drawGraph(req, rsp, dsb);
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
