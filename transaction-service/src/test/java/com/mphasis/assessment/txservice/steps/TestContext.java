package com.mphasis.assessment.txservice.steps;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One instance for the entire Cucumber run (the whole JVM/suite) - the top
 * of the Test -> Feature -> Scenario -> Step hierarchy. Cucumber has no
 * native "whole run" scope beyond {@code @BeforeAll}/{@code @AfterAll}, and
 * PicoContainer rebuilds its object graph every scenario, so this can't be
 * a constructor-injected, per-scenario object like {@link ScenarioContext} -
 * it's a plain singleton that any scenario, from any feature, reaches
 * through {@link #instance()}.
 */
public final class TestContext {

    private static final TestContext INSTANCE = new TestContext();

    private final Map<String, FeatureContext> featuresByUri = new ConcurrentHashMap<>();

    private TestContext() {
    }

    public static TestContext instance() {
        return INSTANCE;
    }

    /**
     * One {@link FeatureContext} per feature file, created the first time a
     * scenario from it runs and reused by every later scenario in that same
     * feature. {@code uri} is the feature file's own identity (from
     * Cucumber's {@code Scenario#getUri()}) - there is no separate
     * "feature started" hook to create this from, so {@code Hooks}
     * synthesizes the boundary here instead.
     */
    public FeatureContext featureFor(String uri, String name) {
        return featuresByUri.computeIfAbsent(uri, u -> new FeatureContext(u, name));
    }
}
