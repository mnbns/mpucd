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
 * Surefire's default include pattern actually picks it up. The
 * ExtentCucumberAdapter plugin writes an HTML report per run (path and
 * theme configured in src/test/resources/extent.properties).
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.mphasis.assessment.txservice.steps")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME,
        value = "pretty, summary, com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:")
class RunCucumberTest {
}
