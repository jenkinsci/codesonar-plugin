package org.jenkinsci.plugins.codesonar.api;

import java.util.logging.Logger;

import org.jenkinsci.plugins.codesonar.AnalysisServiceFactory;
import org.jenkinsci.plugins.codesonar.CodeSonarPluginException;
import org.jenkinsci.plugins.codesonar.models.CodeSonarHubInfo;
import org.jenkinsci.plugins.codesonar.services.AlertsService;
import org.jenkinsci.plugins.codesonar.services.HttpService;
import org.jenkinsci.plugins.codesonar.services.IAnalysisService;
import org.jenkinsci.plugins.codesonar.services.MetricsService;
import org.jenkinsci.plugins.codesonar.services.ProceduresService;
import org.jenkinsci.plugins.codesonar.services.WarningsService;
import org.jenkinsci.plugins.codesonar.services.XmlSerializationService;

public class CodeSonarServices {
    private static final Logger LOGGER = Logger.getLogger(CodeSonarServices.class.getName());
    
    private HttpService httpService;
    private CodeSonarHubInfo hubInfo;
    private XmlSerializationService xmlSerializationService;
    private AnalysisServiceFactory analysisServiceFactory;
    private IAnalysisService analysisService;
    private MetricsService metricsService;
    private ProceduresService proceduresService;
    private AlertsService alertsService;
    private WarningsService warningsService;

    public CodeSonarServices(HttpService httpService, CodeSonarHubInfo hubInfo) {
        this.httpService = httpService;
        this.hubInfo = hubInfo;
    }
    
    private XmlSerializationService getXmlSerializationService() {
        if (xmlSerializationService == null) {
            xmlSerializationService = new XmlSerializationService();
        }
        return xmlSerializationService;
    }
    
    public AnalysisServiceFactory getAnalysisServiceFactory() {
        if (analysisServiceFactory == null) {
            analysisServiceFactory = new AnalysisServiceFactory();
            analysisServiceFactory.setHubInfo(hubInfo);
        }
        return analysisServiceFactory;
    }
    
    public IAnalysisService getAnalysisService() throws CodeSonarPluginException {
        if (analysisService == null) {
            analysisService = getAnalysisServiceFactory().getAnalysisService(httpService, getXmlSerializationService());
        }
        return analysisService;
    }

    public MetricsService getMetricsService() throws CodeSonarPluginException {
        if (metricsService == null) {
            metricsService = new MetricsService(httpService, getXmlSerializationService());
        }
        return metricsService;
    }

    public ProceduresService getProceduresService() throws CodeSonarPluginException {
        if (proceduresService == null) {
            proceduresService = new ProceduresService(httpService, getXmlSerializationService(), hubInfo.isStrictQueryParametersEnforced());
        }
        return proceduresService;
    }
    
    public AlertsService getAlertsService() throws CodeSonarPluginException {
        if (alertsService == null) {
            alertsService = new AlertsService(httpService);
        }
        return alertsService;
    }
    
    public WarningsService getWarningsService() throws CodeSonarPluginException {
        if (warningsService == null) {
            warningsService = new WarningsService(httpService, hubInfo.isStrictQueryParametersEnforced());
        }
        return warningsService;
    }
    
}
