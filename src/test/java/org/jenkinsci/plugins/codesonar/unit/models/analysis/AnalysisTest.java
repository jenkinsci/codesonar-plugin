package org.jenkinsci.plugins.codesonar.unit.models.analysis;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.jenkinsci.plugins.codesonar.models.analysis.Alert;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;

import org.junit.jupiter.api.Test;

/**
 *
 * @author Andrius
 */
class AnalysisTest {

    @Test
    void returnsEmptyListWhenRedAlertsDoNotExist() {
        Analysis analysis = new Analysis();
        analysis.setAlerts(null);
        
        List<Alert> redAlerts = analysis.getRedAlerts();
        
        assertNotNull(redAlerts);
    }

    @Test
    void returnsEmptyListWhenYellowAlertsDoNotExist() {
        Analysis analysis = new Analysis();
        analysis.setAlerts(null);
        
        List<Alert> YellowAlerts = analysis.getYellowAlerts();
        
        assertNotNull(YellowAlerts);
    }
}
