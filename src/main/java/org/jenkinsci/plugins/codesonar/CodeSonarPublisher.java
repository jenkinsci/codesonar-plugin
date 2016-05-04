package org.jenkinsci.plugins.codesonar;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.CertificateCredentials;
import com.cloudbees.plugins.credentials.common.StandardCertificateCredentials;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.AbortException;
import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.ItemGroup;
import hudson.model.Result;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.javatuples.Pair;
import org.jenkinsci.plugins.codesonar.conditions.Condition;
import org.jenkinsci.plugins.codesonar.conditions.ConditionDescriptor;
import org.jenkinsci.plugins.codesonar.models.CodeSonarBuildActionDTO;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.jenkinsci.plugins.codesonar.models.metrics.Metrics;
import org.jenkinsci.plugins.codesonar.models.procedures.Procedures;
import org.jenkinsci.plugins.codesonar.services.AuthenticationService;
import org.jenkinsci.plugins.codesonar.services.HttpService;
import org.jenkinsci.plugins.codesonar.services.IAnalysisService;
import org.jenkinsci.plugins.codesonar.services.MetricsService;
import org.jenkinsci.plugins.codesonar.services.ProceduresService;
import org.jenkinsci.plugins.codesonar.services.XmlSerializationService;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 *
 * @author andrius
 */
public class CodeSonarPublisher extends Recorder {

    private String hubAddress;
    private String projectName;
    private String protocol = "http";

    
    private transient XmlSerializationService xmlSerializationService = null;
    private transient HttpService httpService = null;
    private transient AuthenticationService authenticationService = null;
    private transient IAnalysisService analysisService = null;
    private transient MetricsService metricsService = null;
    private transient ProceduresService proceduresService = null;

    private List<Condition> conditions;

    private String credentialId;

    @DataBoundConstructor
    public CodeSonarPublisher(List<Condition> conditions, String protocol, String hubAddress, String projectName, String credentialId) {
        this.hubAddress = hubAddress;
        this.projectName = projectName;
        this.protocol = protocol;

        if (conditions == null) {
            conditions = ListUtils.EMPTY_LIST;
        }
        this.conditions = conditions;

        this.credentialId = credentialId;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException, AbortException {
        
        xmlSerializationService = new XmlSerializationService();
        httpService = new HttpService();
        authenticationService = new AuthenticationService(httpService);
        metricsService = new MetricsService(httpService, xmlSerializationService);
        proceduresService = new ProceduresService(httpService, xmlSerializationService);
        
        String expandedHubAddress = build.getEnvironment(listener).expand(Util.fixNull(hubAddress));
        String expandedProjectName = build.getEnvironment(listener).expand(Util.fixNull(projectName));

        if (expandedHubAddress.isEmpty()) {
            throw new AbortException("Hub address not provided");
        }
        if (expandedProjectName.isEmpty()) {
            throw new AbortException("Project name not provided");
        }

        URI baseHubUri = URI.create(String.format("%s://%s", getProtocol(), expandedHubAddress));

        authenticate(build, baseHubUri);

        float hubVersion = getHubVersion(baseHubUri);
        
        AnalysisServiceFactory analysisServiceFactory = new AnalysisServiceFactory(hubVersion);
        analysisService = analysisServiceFactory.getAnalysisService(httpService, xmlSerializationService);
        
        List<String> logFile = IOUtils.readLines(build.getLogReader());
        String analysisUrl = analysisService.getAnalysisUrlFromLogFile(logFile);

        if (analysisUrl == null) {
            analysisUrl = analysisService.getLatestAnalysisUrlForAProject(baseHubUri, expandedProjectName);
        }
        
        Analysis analysisActiveWarnings = analysisService.getAnalysisFromUrlWithActiveWarnings(analysisUrl);

        URI metricsUri = metricsService.getMetricsUriFromAnAnalysisId(baseHubUri, analysisActiveWarnings.getAnalysisId());
        Metrics metrics = metricsService.getMetricsFromUri(metricsUri);

        URI proceduresUri = proceduresService.getProceduresUriFromAnAnalysisId(baseHubUri, analysisActiveWarnings.getAnalysisId());
        Procedures procedures = proceduresService.getProceduresFromUri(proceduresUri);

        Analysis analysisNewWarnings = analysisService.getAnalysisFromUrlWithNewWarnings(analysisUrl);
        
        List<Pair<String, String>> conditionNamesAndResults = new ArrayList<Pair<String, String>>();

        CodeSonarBuildActionDTO buildActionDTO = new CodeSonarBuildActionDTO(analysisActiveWarnings,
                analysisNewWarnings, metrics, procedures, baseHubUri);

        build.addAction(new CodeSonarBuildAction(buildActionDTO, build));

        for (Condition condition : conditions) {
            Result validationResult = condition.validate(build, launcher, listener);

            Pair<String, String> pair = Pair.with(condition.getDescriptor().getDisplayName(), validationResult.toString());
            conditionNamesAndResults.add(pair);

            build.setResult(validationResult);
            listener.getLogger().println(String.format(("'%s' marked the build as %s"), condition.getDescriptor().getDisplayName(), validationResult.toString()));
        }

        build.getAction(CodeSonarBuildAction.class).getBuildActionDTO()
                .setConditionNamesAndResults(conditionNamesAndResults);

        authenticationService.signOut(baseHubUri);

        return true;
    }
    
    private float getHubVersion(URI baseHubUri) throws AbortException {
        String info = httpService.getContentFromUrlAsString(baseHubUri.resolve("/command/info/"));
        
        Pattern pattern = Pattern.compile("Version:\\s(\\d+\\.\\d+)");
        
        Matcher matcher = pattern.matcher(info);
        if (matcher.find()) {
            return Float.valueOf(matcher.group(1));
        }
        
        throw new AbortException("Hub version could not be determined");
    }

    private void authenticate(AbstractBuild<?, ?> build, URI baseHubUri) throws AbortException {
        StandardCredentials credentials = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(StandardCredentials.class, build.getParent(), ACL.SYSTEM,
                        Collections.<DomainRequirement>emptyList()), CredentialsMatchers.withId(credentialId));

        if (credentials instanceof StandardUsernamePasswordCredentials) {

            UsernamePasswordCredentials c = (UsernamePasswordCredentials)credentials;

            authenticationService.authenticate(baseHubUri,
                    c.getUsername(),
                    c.getPassword().getPlainText());
        }
        if (credentials instanceof StandardCertificateCredentials) {
            if (protocol.equals("http"))
                throw new AbortException("[CodeSonar] Authentication using a certificate is only available while SSL is enabled.");

            StandardCertificateCredentials c = (StandardCertificateCredentials)credentials;

            authenticationService.authenticate(baseHubUri,
                    c.getKeyStore(),
                    c.getPassword().getPlainText());
        }
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
     * @return the protocol
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * @param protocol the protocol to set
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
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

    public void setXmlSerializationService(XmlSerializationService xmlSerializationService) {
        this.xmlSerializationService = xmlSerializationService;
    }

    public void setHttpService(HttpService httpService) {
        this.httpService = httpService;
    }

    public void setAnalysisService(IAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    public void setMetricsService(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    public void setProceduresService(ProceduresService proceduresService) {
        this.proceduresService = proceduresService;
    }

    public void setAuthenticationService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
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
            return "Codesonar";
        }

        public List<ConditionDescriptor<?>> getAllConditions() {
            DescriptorExtensionList<Condition, ConditionDescriptor<Condition>> all = Condition.getAll();

            List<ConditionDescriptor<?>> list = new ArrayList<ConditionDescriptor<?>>();
            for (ConditionDescriptor<?> d : all) {
                list.add(d);
            }

            return list;
        }

        public FormValidation doCheckHubAddress(@QueryParameter("hubAddress") String hubAddress) {
            if (!StringUtils.isBlank(hubAddress)) {
                return FormValidation.ok();
            }
            return FormValidation.error("Hub address cannot be empty.");
        }

        public FormValidation doCheckProjectName(@QueryParameter("projectName") String projectName) {
            if (!StringUtils.isBlank(projectName)) {
                return FormValidation.ok();
            }
            return FormValidation.error("Project name cannot be empty.");
        }

        public ListBoxModel doFillCredentialIdItems(final @AncestorInPath ItemGroup<?> context) {
            final List<StandardCredentials> credentials = CredentialsProvider.lookupCredentials(StandardCredentials.class, context, ACL.SYSTEM, Collections.<DomainRequirement>emptyList());

            return new StandardListBoxModel()
                    .withEmptySelection()
                    .withMatching(CredentialsMatchers.anyOf(
                            CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class),
                            CredentialsMatchers.instanceOf(CertificateCredentials.class)
                    ), credentials);
        }

        public ListBoxModel doFillProtocolItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("http", "http");
            items.add("https", "https");
            return items;
        }
    }

}
