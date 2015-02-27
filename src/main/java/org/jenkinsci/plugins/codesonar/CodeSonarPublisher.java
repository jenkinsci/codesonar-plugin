package org.jenkinsci.plugins.codesonar;

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
import javax.xml.bind.JAXBException;
import org.apache.http.client.fluent.Request;
import org.jenkinsci.plugins.codesonar.models.Analysis;
import org.jenkinsci.plugins.codesonar.models.Project;
import org.jenkinsci.plugins.codesonar.models.Projects;
import org.jenkinsci.plugins.codesonar.models.Warning;
import org.jenkinsci.plugins.codesonar.services.XmlSerializationService;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 *
 * @author andrius
 */
public class CodeSonarPublisher extends Recorder {

    private final String PROJECT_NAME = "project_x";
    private final File WORKING_DIR = new File("/home/andrius/projects/codesonar-plugin/codesonar/", PROJECT_NAME);
    private final String SERVER_ADDRESS = "10.10.1.125:8080";

    private XmlSerializationService xmlSerializationService;

    private int warningRank;
    private float warningPercentage;

    @DataBoundConstructor
    public CodeSonarPublisher() {
        xmlSerializationService = new XmlSerializationService();

        warningRank = 30;
        warningPercentage = 5.0f;
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

        int totalNumberOfWarnings = analysis.getWarnings().size();

        float severeWarnings = 0.0f;
        for (Warning warning : analysis.getWarnings()) {
            if (warning.getRank() < warningRank) {
                severeWarnings++;
            }
        }

        float calculatedWarningPercentage = (severeWarnings / totalNumberOfWarnings) * 100;

        if (calculatedWarningPercentage > warningPercentage) {
            build.setResult(Result.UNSTABLE);
        }

        build.addAction(new CodeSonarBuildAction(analysis, build));

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

    public int getWarningRank() {
        return warningRank;
    }

    @DataBoundSetter
    public void setWarningRank(int warningRank) {
        this.warningRank = warningRank;
    }

    public float getWarningPercentage() {
        return warningPercentage;
    }

    @DataBoundSetter
    public void setWarningPercentage(float warningPercentage) {
        this.warningPercentage = warningPercentage;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> type) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Code Sonar";
        }
    }

}
