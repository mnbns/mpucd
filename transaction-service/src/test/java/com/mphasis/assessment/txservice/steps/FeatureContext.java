package com.mphasis.assessment.txservice.steps;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * One instance per feature file, shared across every scenario in that
 * feature. Cached in {@link TestContext}, keyed by the feature's URI -
 * Cucumber doesn't fire a "feature started" hook, so {@code Hooks}
 * synthesizes this boundary itself by noticing when a scenario's feature
 * URI differs from the previous scenario's.
 */
public final class FeatureContext {

    public final String uri;
    public final String name;

    private final AtomicInteger scenariosRun = new AtomicInteger();

    FeatureContext(String uri, String name) {
        this.uri = uri;
        this.name = name;
    }

    /** How many scenarios from this feature have started so far in this run (including the current one). */
    public int scenariosRunSoFar() {
        return scenariosRun.get();
    }

    void recordScenarioStarted() {
        scenariosRun.incrementAndGet();
    }
}
