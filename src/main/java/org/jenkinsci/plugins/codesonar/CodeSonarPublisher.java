package org.jenkinsci.plugins.codesonar;

import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.impl.bootstrap.HttpServer;
import org.jenkinsci.plugins.codesonar.conditions.Condition;
import org.jenkinsci.plugins.codesonar.conditions.ConditionDescriptor;
import org.jenkinsci.plugins.codesonar.models.Analysis;
import org.jenkinsci.plugins.codesonar.services.AnalysisService;
import org.jenkinsci.plugins.codesonar.services.HttpService;
import org.jenkinsci.plugins.codesonar.services.XmlSerializationService;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author andrius
 */
public class CodeSonarPublisher extends Recorder {

    private String hubAddress;
    private String projectName;

    private XmlSerializationService xmlSerializationService;
    private HttpService httpService;
    private AnalysisService analysisService;

    private List<Condition> conditions;

    @DataBoundConstructor
    public CodeSonarPublisher(List<Condition> conditions, String hubAddress, String projectName) {
        xmlSerializationService = new XmlSerializationService();
        httpService = new HttpService();
        analysisService = new AnalysisService(xmlSerializationService, httpService);

        this.hubAddress = hubAddress;
        this.projectName = projectName;

        if (conditions == null) {
            conditions = ListUtils.EMPTY_LIST;
        }
        this.conditions = conditions;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        String expandedHubAddress = build.getEnvironment(listener).expand(Util.fixNull(hubAddress));
        String expandedProjectName = build.getEnvironment(listener).expand(Util.fixNull(projectName));

        List<String> logFile = IOUtils.readLines(build.getLogReader());
        String analysisUrl = analysisService.getAnalysisUrlFromLogFile(logFile);

        if (analysisUrl == null) {
            analysisUrl = analysisService.getLatestAnalysisUrlForAProject(expandedHubAddress, expandedProjectName);
        }

        Analysis analysis = analysisService.getAnalysisFromUrl(analysisUrl);

        build.addAction(new CodeSonarBuildAction(analysis, expandedHubAddress, build));

        for (Condition condition : conditions) {
            Result validationResult = condition.validate(build, launcher, listener);
            build.setResult(validationResult);
            listener.getLogger().println(String.format(("'%s' marked the build as %s"), condition.getDescriptor().getDisplayName(), validationResult.toString()));
        }

        return true;
    }

    @Override
    public Collection<? extends Action> getProjectActions(AbstractProject<?, ?> project) {
        return Arrays.asList(
                new CodeSonarProjectAction(project),
                new CodeSonarLatestAnalysisProjectAction(project)
        );
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(List<Condition> conditions) {
        this.conditions = conditions;
    }

    @Override
    public BuildStepDescriptor getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * @return the hubAddress
     */
    public String getHubAddress() {
        return hubAddress;
    }

    /**
     * @param hubAddress the hubAddress to set
     */
    public void setHubAddress(String hubAddress) {
        this.hubAddress = hubAddress;
    }

    /**
     * @return the projectLocation
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * @param projectName the projectLocation to set
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> type) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "CodeSonar";
        }

        public List<ConditionDescriptor<?>> getAllConditions() {
            DescriptorExtensionList<Condition, ConditionDescriptor<Condition>> all = Condition.getAll();

            List<ConditionDescriptor<?>> list = new ArrayList<ConditionDescriptor<?>>();
            for (ConditionDescriptor<?> d : all) {
                list.add(d);
            }

            return list;
        }
    }

}
