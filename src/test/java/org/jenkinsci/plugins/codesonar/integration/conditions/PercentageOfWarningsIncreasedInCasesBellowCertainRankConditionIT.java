package org.jenkinsci.plugins.codesonar.integration.conditions;

/**
 *
 * @author Andrius
 */
public class PercentageOfWarningsIncreasedInCasesBellowCertainRankConditionIT {

    /*@Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }*/

    /*@Test
    public void buildDoesNoContainBuildAction_ReturnsResultSuccess() throws Exception {
        // arrange
        final Result EXPECTED_RESULT = Result.SUCCESS;
        
        final int RANK_OF_WARNINGS = 30;
        final float WARNING_PERCENTAGE = 50.0f;
        PercentageOfWariningsIncreasedInCasesBellowCertainRank condition = new PercentageOfWariningsIncreasedInCasesBellowCertainRank(RANK_OF_WARNINGS, WARNING_PERCENTAGE);
        
        List<Condition> conditions = new ArrayList<>();
        conditions.add(condition);
        
        final String VALID_HUB_ADDRESS = "10.10.10.10";
        final String VALID_PROJECT_NAME = "projectName";

        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        project.getPublishersList().add(new CodeSonarPublisher(conditions, VALID_HUB_ADDRESS, VALID_PROJECT_NAME));
        
        project.scheduleBuild(new Cause.UserIdCause());
        
        RunList<FreeStyleBuild> builds = project.getBuilds();
        
        for(FreeStyleBuild b : builds) {
             String console = jenkinsRule.createWebClient().getPage(b, "console").asText();
             System.out.println(console);
        }
        
        // act
        // assert
        fail("not implemented");
    }

    @Test
    public void buildActionDoesNotContainAnalysis_ReturnsResultSuccess() {
        // arrange
        // act
        // assert
        fail("not implemented");
    }

    @Test
    public void calculatedWarningPercentageAboveTheAllowedLimit_ReturnsWarrantedResult() {
        // arrange
        // act
        // assert
        fail("not implemented");
    }

    @Test
    public void calculatedWarningPercentageBellowTheAllowedLimit_ReturnsResultSuccess() {
        // arrange
        // act
        // assert
        fail("not implemented");
    }

    @Test
    public void calculatedWarningPercentageAboveTheAllowedLimitRunningOnSlave_ReturnsWarrantedResult() {
        // arrange
        // act
        // assert
        fail("not implemented");
    }

    @Test
    public void calculatedWarningPercentageBellowTheAllowedLimitRunningOnSlave_ReturnsResultSuccess() {
        // arrange
        // act
        // assert
        fail("not implemented");
    }*/
}
