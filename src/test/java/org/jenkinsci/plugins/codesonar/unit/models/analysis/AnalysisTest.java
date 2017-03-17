package org.jenkinsci.plugins.codesonar.unit.models.analysis;

import org.jenkinsci.plugins.codesonar.models.analysis.Alert;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 *
 * @author Andrius
 */
public class AnalysisTest {
    
    @Test
    public void returnsEmptyListWhenRedAlertsDoNotExist() {
        Analysis analysis = new Analysis();
        analysis.setAlerts(null);
        
        List<Alert> redAlerts = analysis.getRedAlerts();
        
        Assert.assertNotNull(redAlerts);
    }
    
    @Test
    public void returnsEmptyListWhenYellowAlertsDoNotExist() {
        Analysis analysis = new Analysis();
        analysis.setAlerts(null);
        
        List<Alert> YellowAlerts = analysis.getYellowAlerts();
        
        Assert.assertNotNull(YellowAlerts);
    }
}
