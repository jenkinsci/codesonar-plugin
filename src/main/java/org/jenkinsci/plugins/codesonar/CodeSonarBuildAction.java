package org.jenkinsci.plugins.codesonar;

import hudson.model.Action;
import hudson.model.Run;
import hudson.util.ChartUtil;
import hudson.util.DataSetBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import org.javatuples.Pair;
import org.jenkinsci.plugins.codesonar.models.CodeSonarBuildActionDTO;
import org.jenkinsci.plugins.codesonar.models.metrics.Metric;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import jenkins.tasks.SimpleBuildStep;
import java.util.Arrays;
        
/**
 *
 * @author andrius
 */
public class CodeSonarBuildAction implements Action, SimpleBuildStep.LastBuildAction {

//    private static final Logger LOGGER = Logger.getLogger("totalWarningGraph");
    
    private final CodeSonarBuildActionDTO buildActionDTO;

    private final Run<?, ?> run;

    public CodeSonarBuildAction(CodeSonarBuildActionDTO buildActionDTO, Run<?, ?> run) {
        this.buildActionDTO = buildActionDTO;
        this.run = run;
    }

    @Override
    public String getIconFileName() {
        return "/plugin/codesonar/icons/codesonar-logo.png";
    }

    @Override
    public String getDisplayName() {
        return "Codesonar analysis";
    }

    @Override
    public String getUrlName() {
        URI baseHubUri = buildActionDTO.getBaseHubUri();
        String analysisId = buildActionDTO.getAnalysisActiveWarnings().getAnalysisId();
        
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
//            LOGGER.fine("generating total warnings graph");
            
            String title = "Total number of warnings";
            for (CodeSonarBuildAction codeSonarBuildAction = this; codeSonarBuildAction != null; codeSonarBuildAction = codeSonarBuildAction.getPreviousAction()) {
                CodeSonarBuildActionDTO prevBuildActionDTO = codeSonarBuildAction.getBuildActionDTO();
                if (prevBuildActionDTO == null) {
                    continue;
                }
                 
                int totalNubmerOfWarnings = prevBuildActionDTO.getAnalysisActiveWarnings().getWarnings().size();
                
//                LOGGER.log(Level.FINE, "total number of warnings {0}", totalNubmerOfWarnings );
                
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

                Metric metric = prevBuildActionDTO.getMetrics().getMetricByName("LCodeOnly");
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

    @Override
    public Collection<? extends Action> getProjectActions() {
             return Arrays.asList(
                new CodeSonarProjectAction(run.getParent()),
                new CodeSonarLatestAnalysisProjectAction(run.getParent())
        );
    }

}
