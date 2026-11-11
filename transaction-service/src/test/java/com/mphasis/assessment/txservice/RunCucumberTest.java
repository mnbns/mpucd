package com.mphasis.assessment.txservice;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * Runs every .feature file under src/test/resources/features (one per User
 * Story, plus end_to_end_batch.feature) as JUnit 5 tests. Named *Test so
 * Surefire's default include pattern actually picks it up. Three outputs
 * per run: the human-facing ExtentReport HTML (path/theme in
 * src/test/resources/extent.properties), the machine-readable Cucumber
 * JSON (same per-step/per-scenario data, for CI tooling or re-processing),
 * and the console/file log stream (src/test/resources/logback-test.xml).
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.mphasis.assessment.txservice.steps")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME,
        value = "pretty, summary, json:target/cucumber-reports/cucumber.json, "
                + "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:")
class RunCucumberTest {
}
