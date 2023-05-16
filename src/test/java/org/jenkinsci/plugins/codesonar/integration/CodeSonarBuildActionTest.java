package org.jenkinsci.plugins.codesonar.integration;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.javatuples.Pair;
import org.jenkinsci.plugins.codesonar.CodeSonarBuildAction;
import org.jenkinsci.plugins.codesonar.models.CodeSonarBuildActionDTO;
import org.jenkinsci.plugins.codesonar.models.Metric;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.jenkinsci.plugins.codesonar.models.metrics.Metrics;
import org.jenkinsci.plugins.codesonar.models.procedures.Procedures;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.FreeStyleProject;
import hudson.model.Run;

/**
 * Created by mads on 4/9/18.
 */
public class CodeSonarBuildActionTest {
    @Rule
    public JenkinsRule jr = new JenkinsRule();

    @Test
    public void testJEP200() throws Exception {
         FreeStyleProject fp = jr.createFreeStyleProject("JEP-200");
         Run<?,?> r = jr.buildAndAssertSuccess(fp);
         Long analysisId = Long.valueOf(1L);
         Analysis a = new Analysis();
         Analysis a2 = new Analysis();
         Metric m = new Metric("fuzzyness", "42");
         List<Metric> mmms = Arrays.asList(m);
         Metrics ms = new Metrics();
         Procedures p = new Procedures();
         Pair<String, String> pp = new Pair<>("hello","there");

         CodeSonarBuildActionDTO dto = new CodeSonarBuildActionDTO(CodeSonarBuildActionDTO.VERSION_FAT, analysisId, URI.create("http://localhost"));
         dto.setConditionNamesAndResults(Arrays.asList(pp));

         r.addAction(new CodeSonarBuildAction(dto, r, "dummy"));
         r.save();
    }
}
