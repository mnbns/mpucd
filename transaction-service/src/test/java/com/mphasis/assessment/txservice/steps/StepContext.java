package com.mphasis.assessment.txservice.steps;

/**
 * State scoped to "the current step" within a scenario. Unlike
 * {@link ScenarioContext} (one fresh object per scenario via PicoContainer),
 * Cucumber/PicoContainer don't instantiate a new object per step - this is
 * one mutable holder, reused for the whole scenario and reset by
 * {@code Hooks}' {@code @BeforeStep} hook just before every step runs.
 */
public final class StepContext {

    private int index = -1;
    private long startedAtNanos;

    StepContext() {
    }

    void startNext() {
        index++;
        startedAtNanos = System.nanoTime();
    }

    /** 0-based position of the current step within its scenario. */
    public int index() {
        return index;
    }

    public long elapsedNanos() {
        return System.nanoTime() - startedAtNanos;
    }
}
