package org.jenkinsci.plugins.codesonar.unit.models.analysis;

import java.util.List;

import org.jenkinsci.plugins.codesonar.models.analysis.Alert;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.junit.Assert;
import org.junit.Test;

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
