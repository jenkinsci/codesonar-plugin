package org.jenkinsci.plugins.codesonar;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.*;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.*;
import hudson.model.*;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.javatuples.Pair;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.codesonar.conditions.Condition;
import org.jenkinsci.plugins.codesonar.conditions.ConditionDescriptor;
import org.jenkinsci.plugins.codesonar.models.CodeSonarBuildActionDTO;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.jenkinsci.plugins.codesonar.models.metrics.Metrics;
import org.jenkinsci.plugins.codesonar.models.procedures.Procedures;
import org.jenkinsci.plugins.codesonar.services.*;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author andrius
 */
public class CodeSonarPublisher extends Recorder implements SimpleBuildStep  {
    private static final Logger LOGGER = Logger.getLogger(CodeSonarPublisher.class.getName());
    private String hubAddress;
    private String projectName;
    private String protocol = "http";

    private XmlSerializationService xmlSerializationService = null;
    private HttpService httpService = null;
    private AuthenticationService authenticationService = null;
    private IAnalysisService analysisService = null;
    private MetricsService metricsService = null;
    private ProceduresService proceduresService = null;

    private AnalysisServiceFactory analysisServiceFactory = null;

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
    public void perform(Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener)
            throws InterruptedException, IOException
    {
        xmlSerializationService = getXmlSerializationService();
        httpService = getHttpService();
        authenticationService = getAuthenticationService();
        metricsService = getMetricsService();
        proceduresService = getProceduresService();
        analysisServiceFactory = getAnalysisServiceFactory();
        
        String expandedHubAddress = run.getEnvironment(listener).expand(Util.fixNull(hubAddress));
        String expandedProjectName = run.getEnvironment(listener).expand(Util.fixNull(projectName));

        if (expandedHubAddress.isEmpty()) {
            throw new AbortException("Hub address not provided");
        }
        if (expandedProjectName.isEmpty()) {
            throw new AbortException("Project name not provided");
        }

        URI baseHubUri = URI.create(String.format("%s://%s", getProtocol(), expandedHubAddress));
        listener.getLogger().println("[Codesonar] Using hub URI: "+baseHubUri);

        float hubVersion = getHubVersion(baseHubUri);
        LOGGER.log(Level.FINE, "hub version: {0}", hubVersion);
        
        authenticate(run, baseHubUri);

        analysisServiceFactory = getAnalysisServiceFactory();
        analysisServiceFactory.setVersion(hubVersion);
        analysisService = analysisServiceFactory.getAnalysisService(httpService, xmlSerializationService);
        List<String> logFile = IOUtils.readLines(run.getLogReader());
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
        List<Pair<String, String>> conditionNamesAndResults = new ArrayList<>();

        CodeSonarBuildActionDTO buildActionDTO = new CodeSonarBuildActionDTO(analysisActiveWarnings,
                analysisNewWarnings, metrics, procedures, baseHubUri);

        run.addAction(new CodeSonarBuildAction(buildActionDTO, run));
        for (Condition condition : conditions) {
            Result validationResult = condition.validate(run, launcher, listener);

            Pair<String, String> pair = Pair.with(condition.getDescriptor().getDisplayName(), validationResult.toString());
            conditionNamesAndResults.add(pair);

            run.setResult(validationResult);
            listener.getLogger().println(String.format("'%s' marked the build as %s", condition.getDescriptor().getDisplayName(), validationResult.toString()));
        }

        run.getAction(CodeSonarBuildAction.class).getBuildActionDTO()
                .setConditionNamesAndResults(conditionNamesAndResults);

        authenticationService.signOut(baseHubUri);
    }

    private float getHubVersion(URI baseHubUri) throws AbortException {
        String info;
        try {
            info = httpService.getContentFromUrlAsString(baseHubUri.resolve("/command/anon_info/"));
        } catch (AbortException e) {
            // /command/anon_info/ is not available which means the hub is > 4.2
            return 4.0f;
        }

        Pattern pattern = Pattern.compile("Version:\\s(\\d+\\.\\d+)");

        Matcher matcher = pattern.matcher(info);
        if (matcher.find()) {
            return Float.valueOf(matcher.group(1));
        }

        LOGGER.log(Level.WARNING, "Version info could not be determined by data:\n"+info); // No version could be found

        throw new AbortException("Hub version could not be determined");
    }

    private void authenticate(Run<?, ?> run, URI baseHubUri) throws AbortException {
        StandardCredentials credentials = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(StandardCredentials.class, run.getParent(), ACL.SYSTEM,
                        Collections.<DomainRequirement>emptyList()), CredentialsMatchers.withId(credentialId));

        if (credentials instanceof StandardUsernamePasswordCredentials) {
            LOGGER.log(Level.FINE, "authenticating using username and password");
            UsernamePasswordCredentials c = (UsernamePasswordCredentials) credentials;

            authenticationService.authenticate(baseHubUri,
                    c.getUsername(),
                    c.getPassword().getPlainText());
        }
        if (credentials instanceof StandardCertificateCredentials) {
             LOGGER.log(Level.FINE, "authenticating using ssl certificate");
            if (protocol.equals("http")) {
                throw new AbortException("[CodeSonar] Authentication using a certificate is only available while SSL is enabled.");
            }

            StandardCertificateCredentials c = (StandardCertificateCredentials) credentials;

            authenticationService.authenticate(baseHubUri,
                    c.getKeyStore(),
                    c.getPassword().getPlainText());
        }
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

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public void setAnalysisServiceFactory(AnalysisServiceFactory analysisServiceFactory) {
        this.analysisServiceFactory = analysisServiceFactory;
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

    public XmlSerializationService getXmlSerializationService() {
        if (xmlSerializationService == null) {
            xmlSerializationService = new XmlSerializationService();
        }
        return xmlSerializationService;
    }

    public HttpService getHttpService() {
        if (httpService == null) {
            httpService = new HttpService();
        }
        return httpService;
    }

    public AuthenticationService getAuthenticationService() {
        if (authenticationService == null) {
            authenticationService = new AuthenticationService(getHttpService());
        }
        return authenticationService;
    }

    public MetricsService getMetricsService() {
        if (metricsService == null) {
            metricsService = new MetricsService(getHttpService(), getXmlSerializationService());
        }
        return metricsService;
    }

    public ProceduresService getProceduresService() {
        if (proceduresService == null) {
            proceduresService = new ProceduresService(getHttpService(), getXmlSerializationService());
        }
        return proceduresService;
    }
    
    public AnalysisServiceFactory getAnalysisServiceFactory() {
        if (analysisServiceFactory == null) {
            analysisServiceFactory = new AnalysisServiceFactory();
        }
        return analysisServiceFactory;
    }

    @Symbol("codesonar")
    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public DescriptorImpl() {
            super(CodeSonarPublisher.class);
            load();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> type) {
            return true;
        }

        @Override
        public @Nonnull String getDisplayName() {
            return "Codesonar";
        }

        public List<ConditionDescriptor<?>> getAllConditions() {
            DescriptorExtensionList<Condition, ConditionDescriptor<Condition>> all = Condition.getAll();
            return new ArrayList<>(all);
        }

        public FormValidation doCheckHubAddress(@QueryParameter("hubAddress") String hubAddress) {
            if (!StringUtils.isBlank(hubAddress)) {
                return FormValidation.ok();
            }

            if(hubAddress.startsWith("http://") || hubAddress.startsWith("https://")) {
                return FormValidation.error("Protocol should not be part of the hub address, protocol is selected in seperate field");
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
