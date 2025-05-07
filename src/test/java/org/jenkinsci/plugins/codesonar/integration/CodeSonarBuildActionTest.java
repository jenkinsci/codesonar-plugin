package org.jenkinsci.plugins.codesonar.integration;

import java.net.URI;
import java.util.List;

import org.javatuples.Pair;
import org.jenkinsci.plugins.codesonar.CodeSonarBuildAction;
import org.jenkinsci.plugins.codesonar.models.CodeSonarBuildActionDTO;
import org.jenkinsci.plugins.codesonar.models.Metric;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.jenkinsci.plugins.codesonar.models.metrics.Metrics;
import org.jenkinsci.plugins.codesonar.models.procedures.Procedures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.FreeStyleProject;
import hudson.model.Run;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Created by mads on 4/9/18.
 */
@WithJenkins
class CodeSonarBuildActionTest {

    private JenkinsRule jr;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        jr = rule;
    }

    @Test
    void testJEP200() throws Exception {
         FreeStyleProject fp = jr.createFreeStyleProject("JEP-200");
         Run<?,?> r = jr.buildAndAssertSuccess(fp);
         Long analysisId = 1L;
         Analysis a = new Analysis();
         Analysis a2 = new Analysis();
         Metric m = new Metric("fuzzyness", "42");
         List<Metric> mmms = List.of(m);
         Metrics ms = new Metrics();
         Procedures p = new Procedures();
         Pair<String, String> pp = new Pair<>("hello","there");

         CodeSonarBuildActionDTO dto = new CodeSonarBuildActionDTO(analysisId, URI.create("http://localhost"));
         dto.setConditionNamesAndResults(List.of(pp));

         r.addAction(new CodeSonarBuildAction(dto, r, "dummy"));
         r.save();
    }
}
