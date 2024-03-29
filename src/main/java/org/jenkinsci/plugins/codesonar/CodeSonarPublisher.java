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
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
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
import org.jenkinsci.plugins.codesonar.services.AuthenticationService;
import org.jenkinsci.plugins.codesonar.services.CodeSonarHubAnalysisDataLoader;
import org.jenkinsci.plugins.codesonar.services.HttpService;
import org.jenkinsci.plugins.codesonar.services.HubInfoService;
import org.jenkinsci.plugins.codesonar.services.IAnalysisService;
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
    private String comparisonAnalysis;

    private HttpService httpService = null;
    private AuthenticationService authenticationService = null;

    private List<Condition> conditions;

    private String credentialId;

    private String serverCertificateCredentialId = "";


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
    
    private CodeSonarPluginException createError(String msg, Throwable cause) {
        return new CodeSonarPluginException(msg, cause);
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
    
    public String getComparisonAnalysis() {
        return comparisonAnalysis;
    }

    @DataBoundSetter
    public void setComparisonAnalysis(String comparisonAnalysis) {
        this.comparisonAnalysis = comparisonAnalysis;
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

    public String getHubAddress() {
        return hubAddress;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public void setHubAddress(String hubAddress) {
        this.hubAddress = hubAddress;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    private HttpService getHttpService(@Nonnull Run<?,?> run) throws CodeSonarPluginException {
        if (this.httpService == null)
        {
            this.httpService = createHttpService(run);
        }
        return this.httpService;
    }

    /** Used by unit tests. */
    public void setHttpService(HttpService httpService) {
        this.httpService = httpService;
    }

    private AuthenticationService getAuthenticationService(@Nonnull Run<?,?> run) throws CodeSonarPluginException {
        if (this.authenticationService == null) {
            this.authenticationService = new AuthenticationService(
                getHttpService(run));
        }
        return this.authenticationService;
    }

    /** Used by unit tests. */
    public void setAuthenticationService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    private StandardCredentials lookupCredentials(String credentialId, Run<?,?> run) {
        StandardCredentials hubUserCredentials = null;
        if (StringUtils.isNotBlank(credentialId)) {
            hubUserCredentials = CredentialsMatchers.firstOrNull(
                    CredentialsProvider.lookupCredentials(
                        StandardCredentials.class,
                        run.getParent(),
                        ACL.SYSTEM,
                        Collections.<DomainRequirement>emptyList()),
                    CredentialsMatchers.withId(credentialId));
        }
        return hubUserCredentials;
    }

    public static final class DetermineAid implements FileCallable<String> {
        
        private static final String FILE_AID_TXT = "aid.txt";
        private String projectFile;
        
        public DetermineAid(String projectFile) {
            this.projectFile = projectFile;
        }

        private IOException createError(String msg, Object...args) {
            IOException e = new IOException(MessageFormat.format(msg, args));
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
                throw createError("Specified CodeSonar Project File \"{0}\" does not represent a file or a directory neither", projectFilePath.toString());
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
                    throw createError("Specified CodeSonar Project File \"{0}\" does not represent a file path", projectFile);
                }
                Path prjFilesDirectoryPath = getPrjFilesDirectory(projectFilePath);
                Path prjFilesDirectoryAbsolutePath = resolveRelativePath(jenkinsPipelineCWD, prjFilesDirectoryPath);
                //Check that prj_files directory exists
                if(Files.isDirectory(prjFilesDirectoryAbsolutePath)) {
                    LOGGER.log(Level.INFO, "Finding aid.txt into {0}....", prjFilesDirectoryAbsolutePath.toString());
                    File aidFile = new File(prjFilesDirectoryAbsolutePath.toFile(), FILE_AID_TXT);
                    return readAidFileContent(aidFile);
                } else {
                    throw createError(".prj_files directory \"{0}\" seems not to exist", prjFilesDirectoryAbsolutePath.toString());
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

        HttpService httpService = getHttpService(run);
        HubInfoService hubInfoService = new HubInfoService(httpService);
        CodeSonarHubInfo hubInfo = hubInfoService.fetchHubInfo(baseHubUri);
        // Set hubInfo on httpService so that later users of httpService can get it.
        //  TODO: It would be better if httpService could fetch hubInfo this itself.
        httpService.setHubInfo(hubInfo);
        LOGGER.log(Level.INFO, "hub version: {0}", hubInfo.getVersion());

        AuthenticationService authenticationService = getAuthenticationService(run);
        authenticate(authenticationService, baseHubUri, csLogger, run);

        String currentAnalysisIdString = null;
        if(StringUtils.isBlank(aid)) {
            LOGGER.log(Level.INFO, "Determining analysis id...");
            currentAnalysisIdString = workspace.act(new DetermineAid(expandedProjectFile));
            LOGGER.log(Level.INFO, "Found analysis id: {0}", currentAnalysisIdString);
        } else {
            currentAnalysisIdString = aid;
            LOGGER.log(Level.INFO, "Using override analysis id: \"" + aid + "\".");
        }
        
        Long currentAnalysisId = null;
        try {
            currentAnalysisId = Long.valueOf(currentAnalysisIdString);
        } catch(NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Unable to parse analysis id \"" + currentAnalysisIdString + "\" as long integer.");
        }
        
        if(currentAnalysisId == null) {
            throw createError("No valid analysis id available");
        }
        
        csLogger.writeInfo("Current analysis: analysisId {0} from hub \"{1}\"", String.valueOf(currentAnalysisId), baseHubUri);
        CodeSonarHubAnalysisDataLoader currentDataLoader = new CodeSonarHubAnalysisDataLoader(httpService, hubInfo, baseHubUri, currentAnalysisId, getVisibilityFilterOrDefault(), getNewWarningsFilterOrDefault());

        CodeSonarBuildActionDTO currentBuildActionDTO = new CodeSonarBuildActionDTO(currentAnalysisId, baseHubUri);
        CodeSonarBuildAction csba = new CodeSonarBuildAction(currentBuildActionDTO, run, expandedProjectName);

        List<Pair<String, String>> conditionNamesAndResults = new ArrayList<>();
        
        CodeSonarHubAnalysisDataLoader previousDataLoader = null;
        if (StringUtils.isBlank(comparisonAnalysis)) {
            previousDataLoader = findPreviousBuildAnalysisDataLoader(
                    run,
                    expandedProjectName,
                    csLogger,
                    baseHubUri,
                    hubInfo);
        } else {
            Long comparisonAnalysisId = null;
            try {
                comparisonAnalysisId = Long.valueOf(comparisonAnalysis);
            } catch(NumberFormatException e) {
                throw createError("Unable to parse base analysis ID", e);
            }
            previousDataLoader = new CodeSonarHubAnalysisDataLoader(
                    httpService,
                    hubInfo,
                    baseHubUri,
                    comparisonAnalysisId,
                    getVisibilityFilterOrDefault(),
                    getNewWarningsFilterOrDefault());
            csLogger.writeInfo(
                "Using analysis {0} on hub {1} for comparison",
                String.valueOf(comparisonAnalysisId),
                baseHubUri);
        }

        csLogger.writeInfo("Evaluating conditions...");

        for (Condition condition : conditions) {
            Result validationResult = condition.validate(currentDataLoader, previousDataLoader, launcher, listener, csLogger);
            Pair<String, String> pair = Pair.with(condition.describeResult(), validationResult.toString());
            conditionNamesAndResults.add(pair);
            run.setResult(validationResult);
            csLogger.writeInfo("\"{0}\" marked the build as {1}", condition.getDescriptor().getDisplayName(), validationResult.toString());
        }
        
        csLogger.writeInfo("Done evaluating conditions.");
        
        csba.getBuildActionDTO().setConditionNamesAndResults(conditionNamesAndResults);
        run.addAction(csba);
        authenticationService.signOut(baseHubUri);
            
        csLogger.writeInfo("Done performing codesonar actions");
    }

    private CodeSonarHubAnalysisDataLoader findPreviousBuildAnalysisDataLoader(
                Run<?, ?> run,
                String expandedProjectName,
                CodeSonarLogger csLogger,
                URI baseHubUri,
                CodeSonarHubInfo hubInfo) {
        CodeSonarHubAnalysisDataLoader previousDataLoader = null;
        // Search for a previous, successful build that has the same ProjectName and hub URI.
        //  We match hub URI since generally it is not reliable to compare results between different hubs.
        //  Also, even if we want to consider other hubs, we may still be unable to communicate with them
        //   due to CA certificate and user credential differences.
        csLogger.writeInfo("Finding previous builds for comparison...");
        Run<?,?> previousRun = run.getPreviousSuccessfulBuild();
        while(previousRun != null 
            && previousDataLoader == null
        ) {
            String previousBuildName = previousRun.getDisplayName();
            csLogger.writeInfo("Checking previous successful build: \"{0}\"...", previousBuildName);
            List<CodeSonarBuildAction> buildActions = previousRun.getActions(CodeSonarBuildAction.class);
            List<CodeSonarBuildAction> projectBuildActions = buildActions.stream()
                    .filter(c -> 
                        c.getProjectName() != null
                        && c.getProjectName().equals(expandedProjectName))
                    .collect(Collectors.toList());
            if(projectBuildActions == null || projectBuildActions.isEmpty()) {
                csLogger.writeInfo(
                    "Ignoring build since it has no matching build data. build=\"{0}\", project=\"{1}\"",
                    previousBuildName,
                    expandedProjectName);
            } else if (projectBuildActions.size() > 1) {
                csLogger.writeInfo(
                    "Ignoring build since it has too many matching build actions. build=\"{0}\", project=\"{1}\", matches={2}",
                    previousBuildName,
                    expandedProjectName,
                    projectBuildActions.size());
            } else {
                CodeSonarBuildAction previousBuildAction = projectBuildActions.get(0);
                CodeSonarBuildActionDTO previousDTO = previousBuildAction.getBuildActionDTO();
                URI previousBuildBaseHubUri = previousDTO.getBaseHubUri();
                if(!baseHubUri.equals(previousBuildBaseHubUri)) {
                    // TODO: we could try signing-in to the previous build hub;
                    //  if it works, then perhaps we can still compare results?
                    csLogger.writeInfo(
                        "Ignoring build since hub URI does not match current build. build=\"{0}\", hub=\"{1}\"",
                        previousBuildName,
                        previousBuildBaseHubUri);
                } else {
                    Long previousAnalysisId = previousDTO.getAnalysisId();
                    previousDataLoader = new CodeSonarHubAnalysisDataLoader(
                            httpService,
                            hubInfo,
                            previousBuildBaseHubUri,
                            previousAnalysisId,
                            getVisibilityFilterOrDefault(),
                            getNewWarningsFilterOrDefault());
                    csLogger.writeInfo(
                        "Found previous build for comparison: build=\"{0}\", analysisId={1}, hub=\"{2}\"",
                        previousBuildName,
                        String.valueOf(previousAnalysisId),
                        previousBuildBaseHubUri);
                }
            }
            if (previousDataLoader == null) {
                // Try an earlier run, maybe this hub has been used before:
                Run<?,?> previousRun2 = previousRun.getPreviousSuccessfulBuild();
                if (previousRun2 == null) {
                    previousRun = null;
                } else if (previousRun2.getId().equals(previousRun.getId())) {
                    previousRun = null;
                } else {
                    previousRun = previousRun2;
                }
            }
        }
   
        if (previousDataLoader == null) {
            csLogger.writeInfo("Could not find a previous build with compatible analysis for comparison.");
        }
        return previousDataLoader;
    }
    
    private void authenticate(
                AuthenticationService authenticationService,
                URI baseHubUri,
                CodeSonarLogger csLogger,
                Run<?, ?> run)
            throws CodeSonarPluginException {
        StandardCredentials hubUserCredentials = null;
        String hubUserCredentialId = getCredentialId();
        if (StringUtils.isNotBlank(hubUserCredentialId)) {
            hubUserCredentials = lookupCredentials(hubUserCredentialId, run);
            if (hubUserCredentials == null) {
                throw createError("Credentials not found: \"{0}\"", hubUserCredentialId);
            }
        }
        if (hubUserCredentials == null) {
            csLogger.writeInfo("Authenticating as Anonymous.");
        } else if (hubUserCredentials instanceof StandardUsernamePasswordCredentials) {
            csLogger.writeInfo("Authenticating with username and password.");
            UsernamePasswordCredentials userPassCredentials = (UsernamePasswordCredentials)hubUserCredentials;

            authenticationService.authenticate(
                    baseHubUri,
                    userPassCredentials.getUsername(),
                    userPassCredentials.getPassword().getPlainText());
        } else if (hubUserCredentials instanceof StandardCertificateCredentials
                || hubUserCredentials instanceof FileCredentials) {
            csLogger.writeInfo("Authenticating with client certificate.");
            if (!protocol.equals("https")) {
                throw createError("Certificate authentication requires HTTPS protocol.");
            }

            // Client certificate credentials must be applied to the authenticationService
            //  via the HttpService during creation;
            //  so assume that the credentials are already applied:
            authenticationService.authenticate(baseHubUri);
        } else {
            throw createError(
                    "Unrecognized credential type for credential \"{0}\": {1}",
                    hubUserCredentialId,
                    hubUserCredentials.getClass().getName());
        }
    }

    private HttpService createHttpService(@Nonnull Run<?, ?> run) throws CodeSonarPluginException {
        StandardCredentials serverCertificateCredentials;
        String serverCertificateCredentialId = getServerCertificateCredentialId();
        Collection<? extends Certificate> serverCertificates = null;
        if (StringUtils.isNotEmpty(serverCertificateCredentialId)) {
            serverCertificateCredentials = lookupCredentials(
                serverCertificateCredentialId,
                run);
            if (serverCertificateCredentials == null) {
                throw createError("Credentials with id \"{0}\" not found",
                        serverCertificateCredentialId);
            }
            else if (serverCertificateCredentials instanceof FileCredentials) {
                LOGGER.log(Level.INFO, "Found FileCredentials provided as Hub HTTPS certificate");
                FileCredentials fileCredentials = (FileCredentials)serverCertificateCredentials;
                try {
                    CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                    serverCertificates = certificateFactory.generateCertificates(fileCredentials.getContent());
                    LOGGER.log(Level.INFO, "X509Certificate initialized");
                } catch (IOException | CertificateException e ) {
                    throw createError("Failed to create X509Certificate from Secret File Credential.", e);
                }
            } else {
                throw createError("Invalid credential type provided for hub HTTPS certificate: {0}",
                        serverCertificateCredentials.getClass().getName());
            }
        }
        
        KeyStore clientCertificateKeyStore = null;
        Secret clientCertificatePassword = null;
        String hubUserCredentialId = getCredentialId();
        StandardCredentials hubUserCredentials = lookupCredentials(hubUserCredentialId, run);
        if (StringUtils.isNotBlank(hubUserCredentialId) && hubUserCredentials == null) {
            throw createError("Hub user credential ID not found: \"{0}\"", hubUserCredentialId);
        } else if (hubUserCredentials != null) {
            if (hubUserCredentials instanceof StandardCertificateCredentials) {
                if (protocol.equals("http")) {
                    throw createError("Authentication using a certificate is only available when using HTTPS protocol.");
                }
                LOGGER.log(Level.INFO, "Configuring HttpClient with certificate authentication using \"Certificate\" credentials parameter kind");
                StandardCertificateCredentials certificateCredentials = (StandardCertificateCredentials) hubUserCredentials;
                clientCertificateKeyStore = certificateCredentials.getKeyStore();
                clientCertificatePassword = certificateCredentials.getPassword();
            } else if(hubUserCredentials instanceof FileCredentials) {
                LOGGER.log(Level.INFO, "Configuring HttpClient with certificate authentication using \"Secret File\" credentials parameter kind");
                FileCredentials certificateFileCredentials = (FileCredentials) hubUserCredentials;
                try {
                    //Specify an empty-password secret
                    clientCertificatePassword = Secret.fromString("");
                    KeyStore keystore = KeyStore.getInstance("PKCS12");
                    keystore.load(
                        certificateFileCredentials.getContent(),
                        clientCertificatePassword.getPlainText().toCharArray());
                    clientCertificateKeyStore = keystore;
                    LOGGER.log(Level.INFO, "Client PKCS12 keystore successfully imported");
                } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
                    throw createError("Failed to create PKCS12 keystore from Secret File Credential.", e);
                }
            }
        }

        return new HttpService(
                serverCertificates,
                clientCertificateKeyStore,
                clientCertificatePassword,
                getSocketTimeoutMS());
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