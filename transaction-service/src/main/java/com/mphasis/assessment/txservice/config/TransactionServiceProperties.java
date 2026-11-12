package com.mphasis.assessment.txservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DD-05 / L2-FR-10: binds every runtime-tunable value the service's wiring
 * needs - which input resource to load (US-01), how many times to retry
 * (DD-04), what name the single background-consumer thread runs under
 * (US-12 / DD-12) - from {@code application.properties}, so none of it is
 * a literal at the call site in {@code TransactionServiceApplication}.
 */
@ConfigurationProperties(prefix = "txservice")
public class TransactionServiceProperties {

    private final Input input = new Input();
    private final Retry retry = new Retry();
    private final Worker worker = new Worker();

    public Input getInput() {
        return input;
    }

    public Retry getRetry() {
        return retry;
    }

    public Worker getWorker() {
        return worker;
    }

    public static class Input {
        private String resourcePath;

        public String getResourcePath() {
            return resourcePath;
        }

        public void setResourcePath(String resourcePath) {
            this.resourcePath = resourcePath;
        }
    }

    public static class Retry {
        private int maxAttempts;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }
    }

    public static class Worker {
        private String threadName;

        public String getThreadName() {
            return threadName;
        }

        public void setThreadName(String threadName) {
            this.threadName = threadName;
        }
    }
}
