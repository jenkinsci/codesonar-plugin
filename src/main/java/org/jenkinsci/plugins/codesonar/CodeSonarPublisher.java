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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBException;
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

    private final String PROJECT_NAME = "project_x";
    //private final File WORKING_DIR = new File("/home/andrius/projects/codesonar-plugin/codesonar/", PROJECT_NAME);
    private final String SERVER_ADDRESS = "10.10.1.125:8080";
    private String hubAddress;
    private String projectLocation;
    

    private XmlSerializationService xmlSerializationService;

    private List<Condition> conditions = Lists.newArrayList();

    @DataBoundConstructor
    public CodeSonarPublisher(List<Condition> conditions, String hubAddress, String projectLocation) {
        xmlSerializationService = new XmlSerializationService();
        this.hubAddress = hubAddress;
        this.projectLocation = projectLocation;
        this.conditions = conditions;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        ArgumentListBuilder argumentListBuilder = new ArgumentListBuilder();
        argumentListBuilder.add("codesonar");
        argumentListBuilder.add("analyze");
        argumentListBuilder.add("/home/andrius/projects/codesonar-plugin/_codesonar/" + PROJECT_NAME);
        argumentListBuilder.add("-foreground");
        argumentListBuilder.add(SERVER_ADDRESS);

        int result = launcher.launch()
                .pwd(build.getWorkspace())
                .cmds(argumentListBuilder)
                .stdout(listener).join();
        if (result != 0) {
            return false;
        }

        String url = "http://" + SERVER_ADDRESS + "/index.xml";
        String xmlContent = Request.Get(url).execute().returnContent().asString();

        Projects projects = null;
        try {
            projects = xmlSerializationService.deserialize(xmlContent, Projects.class);
        } catch (JAXBException ex) {
        }

        Project project = projects.getProjectByName(PROJECT_NAME);

        url = "http://" + SERVER_ADDRESS + project.getUrl();
        xmlContent = Request.Get(url).execute().returnContent().asString();

        Analysis analysis = null;
        try {
            analysis = xmlSerializationService.deserialize(xmlContent, Analysis.class);
        } catch (JAXBException ex) {
        }

        build.addAction(new CodeSonarBuildAction(analysis, build));

        for (Condition condition : conditions) {
            Result validationResult = condition.validate(build, launcher, listener);
            build.setResult(validationResult);
        }
        
        return true;
    }

    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        return new CodeSonarProjectAction(project);
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
    public String getProjectLocation() {
        return projectLocation;
    }

    /**
     * @param projectLocation the projectLocation to set
     */
    public void setProjectLocation(String projectLocation) {
        this.projectLocation = projectLocation;
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
            return "Code Sonar";
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
