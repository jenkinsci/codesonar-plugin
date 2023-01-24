package org.jenkinsci.plugins.codesonar;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.javatuples.Pair;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.codesonar.conditions.Condition;
import org.jenkinsci.plugins.codesonar.conditions.ConditionDescriptor;
import org.jenkinsci.plugins.codesonar.models.CodeSonarBuildActionDTO;
import org.jenkinsci.plugins.codesonar.models.CodeSonarHubInfo;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.jenkinsci.plugins.codesonar.models.metrics.Metrics;
import org.jenkinsci.plugins.codesonar.models.procedures.Procedures;
import org.jenkinsci.plugins.codesonar.services.AuthenticationService;
import org.jenkinsci.plugins.codesonar.services.HttpService;
import org.jenkinsci.plugins.codesonar.services.HubInfoService;
import org.jenkinsci.plugins.codesonar.services.IAnalysisService;
import org.jenkinsci.plugins.codesonar.services.MetricsService;
import org.jenkinsci.plugins.codesonar.services.ProceduresService;
import org.jenkinsci.plugins.codesonar.services.XmlSerializationService;
import org.jenkinsci.plugins.plaincredentials.FileCredentials;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.CertificateCredentials;
import com.cloudbees.plugins.credentials.common.StandardCertificateCredentials;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.google.common.base.Throwables;

import hudson.AbortException;
import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.ItemGroup;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.tasks.SimpleBuildStep;

/**
 *
 * @author andrius
 */
public class CodeSonarPublisher extends Recorder implements SimpleBuildStep {
    private static final Logger LOGGER = Logger.getLogger(CodeSonarPublisher.class.getName());
    
    private String visibilityFilter = "2"; // active warnings
    private String hubAddress;
    private String projectName;
    private String protocol = "http";
    private String aid;
    private int socketTimeoutMS = -1;

    private XmlSerializationService xmlSerializationService = null;
    private HttpService httpService = null;
    private AuthenticationService authenticationService = null;
    private IAnalysisService analysisService = null;
    private MetricsService metricsService = null;
    private ProceduresService proceduresService = null;
    private HubInfoService hubInfoService = null;
    
    private AnalysisServiceFactory analysisServiceFactory = null;

    private List<Condition> conditions;

    private String credentialId;
    private StandardCredentials clientCertificateCredentials;

    private String serverCertificateCredentialId = "";
    private StandardCredentials serverCertificateCredentials;

    @DataBoundConstructor
    public CodeSonarPublisher(
            List<Condition> conditions, String protocol, String hubAddress, String projectName, String credentialId,
            String visibilityFilter
    ) {
        this.hubAddress = hubAddress;
        this.projectName = projectName;
        this.protocol = protocol;

        if (conditions == null) {
            conditions = ListUtils.EMPTY_LIST;
        }
        this.conditions = conditions;

        this.credentialId = credentialId;
        this.visibilityFilter = visibilityFilter;
    }

    @DataBoundSetter
    public void setVisibilityFilter(String visibilityFilter){
        this.visibilityFilter = visibilityFilter;
    }

    @DataBoundSetter
    public void setAid(String aid) {
        this.aid = aid;
    }

    public String getAid() {
        return aid;
    }

    @DataBoundSetter
    public void setSocketTimeoutMS(int socketTimeoutMS) {
        this.socketTimeoutMS = socketTimeoutMS;
    }

    public int getSocketTimeoutMS() {
        return socketTimeoutMS;
    }

    public String getServerCertificateCredentialId() {
        return serverCertificateCredentialId;
    }

    @DataBoundSetter
    public void setServerCertificateCredentialId(String serverCertificateCredentialId) {
        this.serverCertificateCredentialId = serverCertificateCredentialId;
    }

    public String getVisibilityFilter() {
        // Backwards compatibility!
        // Not pressed save after upgrade!
        if(StringUtils.isBlank(this.visibilityFilter)){
            return "2"; // Active warnings!
        }
        return this.visibilityFilter;
    }
    
    private static final class DetermineAid implements FileCallable<String> {
        @Override
        public String invoke(File file, VirtualChannel vc) throws IOException, InterruptedException {
            LOGGER.log(Level.INFO, "Finding aid....");
            if(file != null ) {
                File[] files = file.listFiles();
                if(files != null) {
                    LOGGER.log(Level.INFO, "Enumerating files to find aid....");
                    for(File f : files) {
                        if (f.isDirectory() && f.getName().endsWith(".prj_files")) {
                            //String foundAid = new String(Files.readAllBytes(), StandardCharsets.UTF_8);
                            File aidPath = new File(f, "aid.txt");
                            FilePath fp = new FilePath(aidPath);
                            LOGGER.log(Level.INFO, "Found project aid: "+aidPath);
                            return fp.readToString();
                        }
                    }
                } else {
                    LOGGER.log(Level.SEVERE, "No files, this is not supposed to happen!");
                }
            } else {
                LOGGER.log(Level.WARNING, "No workspace!");
            }
            IOException ex = new IOException("Could not find a .prj files folder for project"); 
            LOGGER.log(Level.SEVERE, "Could not find a .prj files folder for project", ex);
            throw ex;
        }

        @Override
        public void checkRoles(RoleChecker rc) throws SecurityException { }
 
    }
       
    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher,
            @Nonnull TaskListener listener)
            throws InterruptedException, IOException
    {
        xmlSerializationService = getXmlSerializationService();
        httpService = getHttpService(run);
        authenticationService = getAuthenticationService(run);
        metricsService = getMetricsService(run);
        proceduresService = getProceduresService(run);
        hubInfoService = getHubInfoService(run);
        analysisServiceFactory = getAnalysisServiceFactory();

        String expandedHubAddress = run.getEnvironment(listener).expand(Util.fixNull(hubAddress));
        String expandedProjectName = run.getEnvironment(listener).expand(Util.fixNull(projectName));

        if (expandedHubAddress.isEmpty()) {
            throw new AbortException("[CodeSonar] Hub address not provided");
        }
        if (expandedProjectName.isEmpty()) {
            throw new AbortException("[CodeSonar] Project name not provided");
        }

        URI baseHubUri = URI.create(String.format("%s://%s", getProtocol(), expandedHubAddress));
        listener.getLogger().println("[Codesonar] Using hub URI: "+baseHubUri);

        CodeSonarHubInfo hubInfo = hubInfoService.fetchHubInfo(baseHubUri);
        LOGGER.log(Level.FINE, "hub version: {0}", hubInfo.getVersion());
        
        authenticate(run, baseHubUri, hubInfo.isOpenAPISupported());

        analysisServiceFactory = getAnalysisServiceFactory();
        analysisServiceFactory.setVersion(hubInfo.getVersion());
        analysisService = analysisServiceFactory.getAnalysisService(httpService, xmlSerializationService);
        analysisService.setVisibilityFilter(getVisibilityFilter());

        if(StringUtils.isBlank(aid)) {
            LOGGER.log(Level.INFO, "[CodeSonar] Determining analysis id...");
            aid = workspace.act(new DetermineAid());
        } else {
            LOGGER.log(Level.INFO, "[CodeSonar] Using override analysis id: '" + aid + "'.");
        }
        
        String analysisUrl = baseHubUri.toString() + "/analysis/" + aid + ".xml";

        Analysis analysisWarnings = analysisService.getAnalysisFromUrlWarningsByFilter(analysisUrl);
        URI metricsUri = metricsService.getMetricsUriFromAnAnalysisId(baseHubUri, aid);
        Metrics metrics = metricsService.getMetricsFromUri(metricsUri);
        URI proceduresUri = proceduresService.getProceduresUriFromAnAnalysisId(baseHubUri, aid);
        Procedures procedures = proceduresService.getProceduresFromUri(proceduresUri);

        Analysis analysisNewWarnings = analysisService.getAnalysisFromUrlWithNewWarnings(analysisUrl);
        List<Pair<String, String>> conditionNamesAndResults = new ArrayList<>();

        CodeSonarBuildActionDTO buildActionDTO = new CodeSonarBuildActionDTO(analysisWarnings, analysisNewWarnings, metrics, procedures, baseHubUri);
        CodeSonarBuildAction csba = new CodeSonarBuildAction(buildActionDTO, run, expandedProjectName, analysisUrl);
        
        listener.getLogger().println("[Codesonar] Finding previous builds for comparison");
        
        CodeSonarBuildActionDTO compareDTO = null;
        Run<?,?> previosSuccess = run.getPreviousSuccessfulBuild();
        if(previosSuccess != null) {
            listener.getLogger().println("[Codesonar] Found previous build to compare to");
            List<CodeSonarBuildAction> actions = previosSuccess.getActions(CodeSonarBuildAction.class).stream().filter(c -> c.getProjectName() != null && c.getProjectName().equals(expandedProjectName)).collect(Collectors.toList());
            if(actions != null && !actions.isEmpty() && actions.size() < 2) {
                listener.getLogger().println("[Codesonar] Found comparison data");
                compareDTO = actions.get(0).getBuildActionDTO();
            }
        }
        
        listener.getLogger().println("[Codesonar] Evaluating conditions");

        for (Condition condition : conditions) {
            Result validationResult = condition.validate(buildActionDTO, compareDTO, launcher, listener);
            Pair<String, String> pair = Pair.with(condition.getDescriptor().getDisplayName(), validationResult.toString());
            conditionNamesAndResults.add(pair);
            run.setResult(validationResult);
            listener.getLogger().println(String.format("[CodeSonar] '%s' marked the build as %s", condition.getDescriptor().getDisplayName(), validationResult.toString()));
        }
        
        listener.getLogger().println("[Codesonar] Done evaulating conditions");
        
        csba.getBuildActionDTO().setConditionNamesAndResults(conditionNamesAndResults);
        run.addAction(csba);
        authenticationService.signOut(baseHubUri);
            
        listener.getLogger().println("[Codesonar] Done performing codesonar actions");
    }

    private void authenticate(Run<?, ?> run, URI baseHubUri, boolean supportsOpenAPI) throws AbortException {
        //If clientCertificateCredentials is null, then authenticate as anonymous
        if(clientCertificateCredentials != null) {
            if (clientCertificateCredentials instanceof StandardUsernamePasswordCredentials) {
                LOGGER.log(Level.INFO, "[CodeSonar] Authenticating using username and password");
                UsernamePasswordCredentials c = (UsernamePasswordCredentials) clientCertificateCredentials;
    
                authenticationService.authenticate(baseHubUri,
                        supportsOpenAPI,
                        c.getUsername(),
                        c.getPassword().getPlainText());
            } else if (clientCertificateCredentials instanceof StandardCertificateCredentials) {
                LOGGER.log(Level.INFO, "[CodeSonar] Authenticating using SSL certificate");
                if (protocol.equals("http")) {
                    throw new AbortException("[CodeSonar] Authentication using a certificate is only available while SSL is enabled.");
                }
    
                authenticationService.authenticate(baseHubUri,
                        supportsOpenAPI);
            }
        } else {
            LOGGER.log(Level.INFO, "[CodeSonar] Authenticating as anonymous");
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

    public HttpService getHttpService(@Nonnull Run<?, ?> run) throws AbortException {
        if (httpService == null) {
            Collection<? extends Certificate> serverCertificates = null;
            if(StringUtils.isNotEmpty(serverCertificateCredentialId)) {
                serverCertificateCredentials = CredentialsMatchers.firstOrNull(
                        CredentialsProvider.lookupCredentials(StandardCredentials.class, run.getParent(), ACL.SYSTEM,
                                Collections.<DomainRequirement>emptyList()), CredentialsMatchers.withId(getServerCertificateCredentialId()));
    
                if(serverCertificateCredentials instanceof FileCredentials) {
                    LOGGER.log(Level.INFO, "[CodeSonar] Found FileCredentials provided as Hub HTTPS certificate");
                    FileCredentials f = (FileCredentials) serverCertificateCredentials;
                    try {
                        CertificateFactory cf = CertificateFactory.getInstance("X.509");
                        serverCertificates = cf.generateCertificates(f.getContent());
                        LOGGER.log(Level.INFO, "[CodeSonar] X509Certificate initialized");
                    } catch (IOException | CertificateException e ) {
                        throw new AbortException(String.format("[CodeSonar] Failed to create X509Certificate from Secret File Credential. %n[CodeSonar] %s: %s%n[CodeSonar] Stack Trace: %s", e.getClass().getName(), e.getMessage(), Throwables.getStackTraceAsString(e)));
                    }
                } else {
                    if(serverCertificateCredentials != null) {
                        LOGGER.log(Level.INFO, "[CodeSonar] Found {0} provided as Hub HTTPS certificate", serverCertificateCredentials.getClass().getName());
                        throw new AbortException(String.format("[CodeSonar] The Jenkins Credentials provided as Hub HTTPS certificate is of type %s.%n[CodeSonar] Please provide a credential of type FileCredentials", serverCertificateCredentials.getClass().getName()));
                    }
                    LOGGER.log(Level.INFO, "[CodeSonar] Credentials with id '{0}' not found", getServerCertificateCredentialId());
                    throw new AbortException(String.format("[CodeSonar] Credentials with id '{0}' not found", getServerCertificateCredentialId()));
                }
            }
            
            
            //If credentialId is null, then authenticate as anonymous
            KeyStore clientCertificateKeyStore = null;
            Secret clientCertificatePassword = null;
            if (StringUtils.isNotEmpty(credentialId)) {
                clientCertificateCredentials = CredentialsMatchers.firstOrNull(
                        CredentialsProvider.lookupCredentials(StandardCredentials.class, run.getParent(), ACL.SYSTEM,
                                Collections.<DomainRequirement>emptyList()), CredentialsMatchers.withId(credentialId));

                if (clientCertificateCredentials instanceof StandardCertificateCredentials) {
                    if (protocol.equals("http")) {
                        throw new AbortException("[CodeSonar] Authentication using a certificate is only available while SSL is enabled.");
                    }
        
                    LOGGER.log(Level.INFO, "[CodeSonar] Configuring HttpClient with certificate authentication");
                    
                    StandardCertificateCredentials c = (StandardCertificateCredentials) clientCertificateCredentials;
        
                    clientCertificateKeyStore = c.getKeyStore();
                    clientCertificatePassword = c.getPassword();
                }
            }

            httpService = new HttpService(serverCertificates, clientCertificateKeyStore, clientCertificatePassword, getSocketTimeoutMS());
        }
        return httpService;
    }

    public AuthenticationService getAuthenticationService(@Nonnull Run<?, ?> run) throws AbortException {
        if (authenticationService == null) {
            authenticationService = new AuthenticationService(getHttpService(run));
        }
        return authenticationService;
    }

    public MetricsService getMetricsService(@Nonnull Run<?, ?> run) throws AbortException {
        if (metricsService == null) {
            metricsService = new MetricsService(getHttpService(run), getXmlSerializationService());
        }
        return metricsService;
    }

    public ProceduresService getProceduresService(@Nonnull Run<?, ?> run) throws AbortException {
        if (proceduresService == null) {
            proceduresService = new ProceduresService(getHttpService(run), getXmlSerializationService());
        }
        return proceduresService;
    }
    
    public HubInfoService getHubInfoService(@Nonnull Run<?, ?> run) throws AbortException {
        if (hubInfoService == null) {
            hubInfoService = new HubInfoService(getHttpService(run));
        }
        return hubInfoService;
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

        public FormValidation doCheckVisibilityFilter(@QueryParameter("visibilityFilter") String visibilityFilter){
            if (!StringUtils.isBlank(visibilityFilter)) {
                return FormValidation.ok();
            }
            if(!StringUtils.isNumeric(visibilityFilter) && Integer.parseInt(visibilityFilter) < 0){
                return FormValidation.error("The visibility filter must be a positive integer");
            }
            // It's a bit tricky to check if the visibility filter number is actually defined,
            // as there's no URL to check this. The URLs contain the entire query string, which
            // we can't retrieve. So assume that
            return FormValidation.ok();
        }

        public ListBoxModel doFillCredentialIdItems(final @AncestorInPath ItemGroup<?> context) {
            final List<StandardCredentials> credentials = CredentialsProvider.lookupCredentials(StandardCredentials.class, context, ACL.SYSTEM, Collections.<DomainRequirement>emptyList());

            return new StandardListBoxModel()
                    .includeEmptyValue()
                    .withMatching(CredentialsMatchers.anyOf(
                                    CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class),
                                    CredentialsMatchers.instanceOf(CertificateCredentials.class)
                            ), credentials);
        }

        public ListBoxModel doFillServerCertificateCredentialIdItems(final @AncestorInPath ItemGroup<?> context) {
            final List<StandardCredentials> credentials = CredentialsProvider.lookupCredentials(StandardCredentials.class, context, ACL.SYSTEM, Collections.<DomainRequirement>emptyList());

            return new StandardListBoxModel()
                    .includeEmptyValue()
                    .withMatching(CredentialsMatchers.anyOf(
                            CredentialsMatchers.instanceOf(FileCredentials.class)
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
