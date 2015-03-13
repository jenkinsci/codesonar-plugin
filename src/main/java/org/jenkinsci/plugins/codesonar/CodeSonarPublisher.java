package org.jenkinsci.plugins.codesonar;

import com.google.common.collect.Lists;
import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.ArgumentListBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBException;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.fluent.Request;
import org.jenkinsci.plugins.codesonar.conditions.Condition;
import org.jenkinsci.plugins.codesonar.conditions.ConditionDescriptor;
import org.jenkinsci.plugins.codesonar.models.Analysis;
import org.jenkinsci.plugins.codesonar.models.Project;
import org.jenkinsci.plugins.codesonar.models.Projects;
import org.jenkinsci.plugins.codesonar.services.XmlSerializationService;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author andrius
 */
public class CodeSonarPublisher extends Recorder {

    private final String SERVER_ADDRESS = "10.10.1.125:8080";
    private String hubAddress;
    private String projectName;

    private XmlSerializationService xmlSerializationService;

    private List<Condition> conditions;

    @DataBoundConstructor
    public CodeSonarPublisher(List<Condition> conditions, String hubAddress, String projectName) {
        xmlSerializationService = new XmlSerializationService();
        this.hubAddress = hubAddress;
        this.projectName = projectName;

        if (conditions == null) {
            conditions = Lists.newArrayList();
        }
        this.conditions = conditions;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        List<String> logFile = IOUtils.readLines(build.getLogReader());
        
        String expandedHubAddress = build.getEnvironment(listener).expand(hubAddress);
        String expandedProjectName = build.getEnvironment(listener).expand(projectName);

        Pattern pattern = Pattern.compile(String.format("(https|http)://%s/analysis/.*", expandedHubAddress));

        System.out.println("-------------start_-----");
        
        String analysisUrl = null;
        for (String line : logFile) {
            if (pattern.matcher(line).matches()) {
                analysisUrl = line;
            }
        }
        System.out.println("analysis url: " + analysisUrl);
        if (analysisUrl == null) {
            String url = "http://" + expandedHubAddress + "/index.xml";
            String xmlContent = Request.Get(url).execute().returnContent().asString();

            Projects projects = null;
            try {
                projects = xmlSerializationService.deserialize(xmlContent, Projects.class);
            } catch (JAXBException ex) {
            }

            Project project = projects.getProjectByName(expandedProjectName);

            analysisUrl = "http://" + expandedHubAddress + project.getUrl();

        }
        System.out.println("analysis url: " + analysisUrl);
        System.out.println("------------------");
        String xmlContent = Request.Get(analysisUrl).execute().returnContent().asString();

        Analysis analysis = null;
        try {
            analysis = xmlSerializationService.deserialize(xmlContent, Analysis.class);
        } catch (JAXBException ex) {
            ex.printStackTrace(listener.getLogger());
        }

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
                new CodeSonarLatestAnalysisProjectAction(project));
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
