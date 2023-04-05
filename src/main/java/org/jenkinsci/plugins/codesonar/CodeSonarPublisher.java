package org.jenkinsci.plugins.codesonar;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.text.MessageFormat;
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
import org.apache.commons.lang.math.NumberUtils;
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
    
    private static final String CS_PROJECT_FILE_EXTENSION = ".prj";
    private static final String CS_PROJECT_DIR_EXTENSION = ".prj_files";
    
    private String visibilityFilter;
    private String newWarningsFilter;
    private String hubAddress;
    private String projectName;
    private String protocol = "http";
    private String aid;
    private int socketTimeoutMS = -1;
    private String projectFile;

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
    
    private CodeSonarPluginException createError(String msg, Object...args) {
        return new CodeSonarPluginException(msg, args);
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
        return visibilityFilter;
    }
    
    public String getVisibilityFilterOrDefault() {
        return StringUtils.isNotBlank(visibilityFilter) ? visibilityFilter : IAnalysisService.VISIBILITY_FILTER_ALL_WARNINGS_DEFAULT;
    }
    
    public String getProjectFile() {
        return projectFile;
    }

    @DataBoundSetter
    public void setProjectFile(String projectFile) {
        this.projectFile = projectFile;
    }
    
    public String getNewWarningsFilter() {
        return newWarningsFilter;
    }
    
    public String getNewWarningsFilterOrDefault() {
        return StringUtils.isNotBlank(newWarningsFilter) ? newWarningsFilter : IAnalysisService.VISIBILITY_FILTER_NEW_WARNINGS_DEFAULT;
    }

    @DataBoundSetter
    public void setNewWarningsFilter(String newWarningsFilter) {
        this.newWarningsFilter = newWarningsFilter;
    }

    public static final class DetermineAid implements FileCallable<String> {
        
        private static final String FILE_AID_TXT = "aid.txt";
        private String projectFile;
        
        public DetermineAid(String projectFile) {
            this.projectFile = projectFile;
        }

        private IOException createError(String msg) {
            IOException e = new IOException(msg); 
            LOGGER.log(Level.SEVERE, msg, e);
            return e;
        }
        
        /**
         * Get CodeSonar Project File base name, removing known extensions.
         * @return Path representing CodeSonar Project File base name
         */
        private String getProjectFileBaseName(String fileName) {
            String baseFileName = fileName;
            //Remove extension if it is one of the known ones (.prj, .prj_files)
            if(StringUtils.endsWith(baseFileName, CS_PROJECT_FILE_EXTENSION)) {
                baseFileName = baseFileName.substring(0, baseFileName.lastIndexOf(CS_PROJECT_FILE_EXTENSION));
            } else if(StringUtils.endsWith(baseFileName, CS_PROJECT_DIR_EXTENSION)) {
                baseFileName = baseFileName.substring(0, baseFileName.lastIndexOf(CS_PROJECT_DIR_EXTENSION));
            }
            return baseFileName;
        }
        
        private Path getPrjFilesDirectory(Path projectFilePath) throws IOException {
            Path originalFileName = projectFilePath.getFileName();
            if(originalFileName == null) {
                throw createError(MessageFormat.format("Specified CodeSonar Project File \"{0}\" does not represent a file or a directory neither", projectFilePath.toString()));
            }
            String projectFileBaseName = getProjectFileBaseName(originalFileName.toString());
            Path resultingPath = Paths.get(projectFileBaseName + CS_PROJECT_DIR_EXTENSION);
            Path parentPath = projectFilePath.getParent();
            if(parentPath != null) {
                resultingPath = parentPath.resolve(resultingPath);
            }
            return resultingPath;
        }
        
        /**
         * Determine the real file system location described by path.
         * If path is absolute than there's nothing additional to do,
         * whereas if it is relative, it will be resolved to its absolute form
         * starting the resolution process from jenkin's current working directory.
         * @param jenkinsPipelineCWD Jenkin's current working directory for a given pipeline
         * @param path A path
         * @return The absolute form for parameter path
         */
        private Path resolveRelativePath(File jenkinsPipelineCWD, Path path) {
            if(!path.isAbsolute()) {
                /* If path is expressed as relative, then it is always 
                 * considered relative to pipeline's current working directory
                 */
                path = jenkinsPipelineCWD.toPath().resolve(path).normalize();
            }
            return path;
        }
        
        private String readAidFileContent(File aidFile) throws IOException, InterruptedException {
            FilePath fp = new FilePath(aidFile);
            LOGGER.log(Level.INFO, "Found aid.txt: " + aidFile);
            String aid = fp.readToString();
            if(StringUtils.isBlank(aid)) {
                throw createError("File aid.txt is empty");
            }
            return aid;
        }
        
        @Override
        public String invoke(File jenkinsPipelineCWD, VirtualChannel vc) throws IOException, InterruptedException {
            /*
             * If it has been specified parameter "projectFile" for this pipeline, then use it
             * to determine where to search into in order to retrieve the prj_files directory
             */
            if(StringUtils.isNotBlank(projectFile)) {
                Path projectFilePath = null;
                try {
                    projectFilePath = Paths.get(projectFile);
                } catch(InvalidPathException e) {
                    throw createError(MessageFormat.format("Specified CodeSonar Project File \"{0}\" does not represent a file path", projectFile));
                }
                Path prjFilesDirectoryPath = getPrjFilesDirectory(projectFilePath);
                Path prjFilesDirectoryAbsolutePath = resolveRelativePath(jenkinsPipelineCWD, prjFilesDirectoryPath);
                //Check that prj_files directory exists
                if(Files.isDirectory(prjFilesDirectoryAbsolutePath)) {
                    LOGGER.log(Level.INFO, "Finding aid.txt into {0}....", prjFilesDirectoryAbsolutePath.toString());
                    File aidFile = new File(prjFilesDirectoryAbsolutePath.toFile(), FILE_AID_TXT);
                    return readAidFileContent(aidFile);
                } else {
                    throw createError(MessageFormat.format(".prj_files directory \"{0}\" seems not to exist", prjFilesDirectoryAbsolutePath.toString()));
                }
            } else {
                
                if(jenkinsPipelineCWD != null ) {
                    LOGGER.log(Level.INFO, "Finding aid.txt into {0}....", jenkinsPipelineCWD.getAbsoluteFile());
                    File[] prjFilesDirectories = jenkinsPipelineCWD.listFiles(new FileFilter() {
                        @Override
                        public boolean accept(File pathname) {
                            return pathname.isDirectory() && pathname.getName().endsWith(CS_PROJECT_DIR_EXTENSION);
                        }
                    });
                    if(prjFilesDirectories != null && prjFilesDirectories.length > 0) {
                        File prjFilesDir = prjFilesDirectories[0];
                        if(prjFilesDirectories.length > 1) {
                            LOGGER.log(Level.WARNING, "More than one .prj_files directory found, going to take the first one: " + prjFilesDir);
                        }
                        File aidFile = new File(prjFilesDir, FILE_AID_TXT);
                        return readAidFileContent(aidFile);
                    } else {
                        LOGGER.log(Level.SEVERE, "No prj_files directory found!");
                    }
                } else {
                    LOGGER.log(Level.WARNING, "Could not determine Jenkins build working directory.");
                }
                throw createError("Could not find a .prj_files folder for project");
            }
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
        LOGGER.log(Level.INFO, "projectName: {0} expandedProjectName {1}", new String[] {projectName, expandedProjectName});
        String expandedProjectFile = run.getEnvironment(listener).expand(Util.fixNull(projectFile));
        LOGGER.log(Level.INFO, "projectFile: {0} expandedProjectFile {1}", new String[] {projectFile, expandedProjectFile});


        if (expandedHubAddress.isEmpty()) {
            throw createError("Hub address not provided");
        }
        if (expandedProjectName.isEmpty()) {
            throw createError("Project name not provided");
        }
        
        CodeSonarLogger csLogger = new CodeSonarLogger(listener.getLogger());

        URI baseHubUri = URI.create(String.format("%s://%s", getProtocol(), expandedHubAddress));
        csLogger.writeInfo("Using hub URI: {0}", baseHubUri);

        CodeSonarHubInfo hubInfo = hubInfoService.fetchHubInfo(baseHubUri);
        LOGGER.log(Level.FINE, "hub version: {0}", hubInfo.getVersion());
        
        authenticate(run, baseHubUri, hubInfo.isOpenAPISupported());

        analysisServiceFactory = getAnalysisServiceFactory();
        analysisServiceFactory.setHubInfo(hubInfo);
        analysisService = analysisServiceFactory.getAnalysisService(httpService, xmlSerializationService);
        analysisService.setVisibilityFilter(getVisibilityFilterOrDefault());
        analysisService.setNewWarningsFilter(getNewWarningsFilterOrDefault());

        String analysisId = null;
        if(StringUtils.isBlank(aid)) {
            LOGGER.log(Level.INFO, "Determining analysis id...");
            analysisId = workspace.act(new DetermineAid(expandedProjectFile));
            LOGGER.log(Level.INFO, "Found analysis id: {0}", analysisId);
        } else {
            analysisId = aid;
            LOGGER.log(Level.INFO, "Using override analysis id: \"" + aid + "\".");
        }
        
        String analysisUrl = baseHubUri.toString() + "/analysis/" + analysisId + ".xml";

        Analysis analysisWarnings = analysisService.getAnalysisFromUrlWarningsByFilter(analysisUrl);
        URI metricsUri = metricsService.getMetricsUriFromAnAnalysisId(baseHubUri, analysisId);
        Metrics metrics = metricsService.getMetricsFromUri(metricsUri);
        URI proceduresUri = proceduresService.getProceduresUriFromAnAnalysisId(baseHubUri, analysisId);
        Procedures procedures = proceduresService.getProceduresFromUri(proceduresUri);

        Analysis analysisNewWarnings = analysisService.getAnalysisFromUrlWithNewWarnings(analysisUrl);
        List<Pair<String, String>> conditionNamesAndResults = new ArrayList<>();
        
        Long lAnalysisId = null;
        try {
            lAnalysisId = Long.valueOf(analysisId);
        } catch(NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Unable to parse analysis id \"" + analysisId + "\" as long integer.");
        }
        CodeSonarBuildActionDTO buildActionDTO = new CodeSonarBuildActionDTO(lAnalysisId, analysisWarnings, analysisNewWarnings, metrics, procedures, baseHubUri);
        CodeSonarBuildAction csba = new CodeSonarBuildAction(buildActionDTO, run, expandedProjectName, analysisUrl);
        
        csLogger.writeInfo("Finding previous builds for comparison");
        
        CodeSonarBuildActionDTO compareDTO = null;
        Run<?,?> previousSuccess = run.getPreviousSuccessfulBuild();
        if(previousSuccess != null) {
            csLogger.writeInfo("Found previous build to compare to ({0})", previousSuccess.getDisplayName());
            List<CodeSonarBuildAction> actionsList = previousSuccess.getActions(CodeSonarBuildAction.class);
            List<CodeSonarBuildAction> filteredActions = actionsList.stream()
                    .filter(c -> c.getProjectName() != null && c.getProjectName().equals(expandedProjectName))
                    .collect(Collectors.toList());
            if(filteredActions != null && !filteredActions.isEmpty() && filteredActions.size() < 2) {
                csLogger.writeInfo("Found comparison data");
                compareDTO = filteredActions.get(0).getBuildActionDTO();
            }
        }
        
        csLogger.writeInfo("Evaluating conditions");

        for (Condition condition : conditions) {
            Result validationResult = condition.validate(buildActionDTO, compareDTO, launcher, listener, csLogger);
            Pair<String, String> pair = Pair.with(condition.describeResult(), validationResult.toString());
            conditionNamesAndResults.add(pair);
            run.setResult(validationResult);
            csLogger.writeInfo("\"{0}\" marked the build as {1}", condition.getDescriptor().getDisplayName(), validationResult.toString());
        }
        
        csLogger.writeInfo("Done evaluating conditions");
        
        csba.getBuildActionDTO().setConditionNamesAndResults(conditionNamesAndResults);
        run.addAction(csba);
        authenticationService.signOut(baseHubUri);
            
        csLogger.writeInfo("Done performing codesonar actions");
    }

    private void authenticate(Run<?, ?> run, URI baseHubUri, boolean supportsOpenAPI) throws CodeSonarPluginException {
        //If clientCertificateCredentials is null, then authenticate as anonymous
        if(clientCertificateCredentials != null) {
            if (clientCertificateCredentials instanceof StandardUsernamePasswordCredentials) {
                LOGGER.log(Level.INFO, "Authenticating using username and password");
                UsernamePasswordCredentials c = (UsernamePasswordCredentials) clientCertificateCredentials;
    
                authenticationService.authenticate(baseHubUri,
                        supportsOpenAPI,
                        c.getUsername(),
                        c.getPassword().getPlainText());
            } else if (clientCertificateCredentials instanceof StandardCertificateCredentials) {
                LOGGER.log(Level.INFO, "Authenticating using SSL certificate");
                if (protocol.equals("http")) {
                    throw createError("Authentication using a certificate is only available while SSL is enabled.");
                }
    
                authenticationService.authenticate(baseHubUri,
                        supportsOpenAPI);
            }
        } else {
            LOGGER.log(Level.INFO, "Authenticating as anonymous");
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

    public HttpService getHttpService(@Nonnull Run<?, ?> run) throws CodeSonarPluginException {
        if (httpService == null) {
            Collection<? extends Certificate> serverCertificates = null;
            if(StringUtils.isNotEmpty(serverCertificateCredentialId)) {
                serverCertificateCredentials = CredentialsMatchers.firstOrNull(
                        CredentialsProvider.lookupCredentials(StandardCredentials.class, run.getParent(), ACL.SYSTEM,
                                Collections.<DomainRequirement>emptyList()), CredentialsMatchers.withId(getServerCertificateCredentialId()));
    
                if(serverCertificateCredentials instanceof FileCredentials) {
                    LOGGER.log(Level.INFO, "Found FileCredentials provided as Hub HTTPS certificate");
                    FileCredentials f = (FileCredentials) serverCertificateCredentials;
                    try {
                        CertificateFactory cf = CertificateFactory.getInstance("X.509");
                        serverCertificates = cf.generateCertificates(f.getContent());
                        LOGGER.log(Level.INFO, "X509Certificate initialized");
                    } catch (IOException | CertificateException e ) {
                        throw createError("Failed to create X509Certificate from Secret File Credential. %n{0}: {1}%nStack Trace: {2}", e.getClass().getName(), e.getMessage(), Throwables.getStackTraceAsString(e));
                    }
                } else {
                    if(serverCertificateCredentials != null) {
                        LOGGER.log(Level.INFO, "Found {0} provided as Hub HTTPS certificate", serverCertificateCredentials.getClass().getName());
                        throw createError("The Jenkins Credentials provided as Hub HTTPS certificate is of type {0}.%nPlease provide a credential of type FileCredentials", serverCertificateCredentials.getClass().getName());
                    }
                    LOGGER.log(Level.INFO, "Credentials with id \"{0}\" not found", getServerCertificateCredentialId());
                    throw createError(CodeSonarLogger.formatMessage("Credentials with id \"{0}\" not found", getServerCertificateCredentialId()));
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
                        throw createError(CodeSonarLogger.formatMessage("Authentication using a certificate is only available while SSL is enabled."));
                    }
        
                    LOGGER.log(Level.INFO, "Configuring HttpClient with certificate authentication");
                    
                    StandardCertificateCredentials c = (StandardCertificateCredentials) clientCertificateCredentials;
        
                    clientCertificateKeyStore = c.getKeyStore();
                    clientCertificatePassword = c.getPassword();
                }
            }

            httpService = new HttpService(serverCertificates, clientCertificateKeyStore, clientCertificatePassword, getSocketTimeoutMS());
        }
        return httpService;
    }

    public AuthenticationService getAuthenticationService(@Nonnull Run<?, ?> run) throws CodeSonarPluginException {
        if (authenticationService == null) {
            authenticationService = new AuthenticationService(getHttpService(run));
        }
        return authenticationService;
    }

    public MetricsService getMetricsService(@Nonnull Run<?, ?> run) throws CodeSonarPluginException {
        if (metricsService == null) {
            metricsService = new MetricsService(getHttpService(run), getXmlSerializationService());
        }
        return metricsService;
    }

    public ProceduresService getProceduresService(@Nonnull Run<?, ?> run) throws CodeSonarPluginException {
        if (proceduresService == null) {
            proceduresService = new ProceduresService(getHttpService(run), getXmlSerializationService());
        }
        return proceduresService;
    }
    
    public HubInfoService getHubInfoService(@Nonnull Run<?, ?> run) throws CodeSonarPluginException {
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
            return "CodeSonar";
        }

        public List<ConditionDescriptor<?>> getAllConditions() {
            DescriptorExtensionList<Condition, ConditionDescriptor<Condition>> all = Condition.getAll();
            return new ArrayList<>(all);
        }
        
        public FormValidation doCheckHubAddress(@QueryParameter("hubAddress") String hubAddress) {
            if (StringUtils.isBlank(hubAddress)) {
                return FormValidation.error("Hub address cannot be empty.");
            }

            if(hubAddress.startsWith("http://") || hubAddress.startsWith("https://")) {
                return FormValidation.error("Protocol should not be part of the hub address, protocol is selected in seperate field");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckProjectName(@QueryParameter("projectName") String projectName) {
            if (StringUtils.isBlank(projectName)) {
                return FormValidation.error("Project name cannot be empty.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckVisibilityFilter(@QueryParameter("visibilityFilter") String visibilityFilter){
            return validateVisibilityFilter(visibilityFilter);
        }
        
        public FormValidation doCheckNewWarningsFilter(@QueryParameter("newWarningsFilter") String visibilityFilter){
            return validateVisibilityFilter(visibilityFilter);
        }

        private FormValidation validateVisibilityFilter(String visibilityFilter) {
            if (StringUtils.isBlank(visibilityFilter)) {
                //When left blank, use predefined default filter
                return FormValidation.ok();
            }
            if(NumberUtils.isNumber(visibilityFilter)) {
                try {
                    if(Integer.parseInt(visibilityFilter) < 0) {
                        return FormValidation.error("The visibility filter must be a positive integer");
                    }
                } catch(NumberFormatException e) {
                    return FormValidation.error("Invalid numeric value for visibility filter", visibilityFilter);
                }
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
