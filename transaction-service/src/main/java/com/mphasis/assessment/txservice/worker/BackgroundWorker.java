package com.mphasis.assessment.txservice.worker;

import com.mphasis.assessment.txservice.model.TransactionRequest;
import com.mphasis.assessment.txservice.service.TransactionProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * US-12 / L2-FR-13 / DD-12: the "background processing component" named in
 * HLD-01/LLD-01 - a {@link BlockingQueue} fed by the ingestion loop (the
 * producer) and drained by a single dedicated consumer thread (a 1-thread
 * {@link ExecutorService}) that calls {@link TransactionProcessor#process}.
 * Before this, the ingestion loop called the processor inline, sharing one
 * thread and call stack with no architectural separation at all.
 *
 * <p>Deliberately a single consumer, not a pool: DD-02's out-of-order /
 * sequence-buffering logic depends on exactly one global cursor
 * ({@code SequenceBuffer.lastResolvedSequence}) advanced in strict arrival
 * order, and the idempotency check-then-set is already a named Known Gap -
 * a pool of concurrent consumers would race on both (AC-12.3).
 *
 * <p>{@link #submit} enqueues and returns immediately - the ingesting
 * thread is never blocked per-request by processing (AC-12.1/AC-12.2).
 * {@link #awaitDrained} is the signal the ingesting thread must wait on
 * before it is safe to compute a final {@code ProcessingSummary}: it only
 * returns once every request submitted so far has actually finished being
 * processed, closing the race between "ingestion done" and "processing
 * done" that an inline loop never had to worry about.
 */
public class BackgroundWorker {

    private static final Logger log = LoggerFactory.getLogger(BackgroundWorker.class);

    /** Fallback only for callers (e.g. tests) that don't source a name from config; see the two-arg constructor. */
    private static final String DEFAULT_THREAD_NAME = "background-worker";

    private final BlockingQueue<TransactionRequest> queue = new LinkedBlockingQueue<>();
    private final ExecutorService executor;
    private final TransactionProcessor processor;
    private final String threadName;

    private final AtomicLong submittedCount = new AtomicLong();
    private final AtomicLong processedCount = new AtomicLong();
    private final Object drainLock = new Object();

    /** Test/verification support only (AC-12.3): every thread name observed draining the queue. */
    private final Set<String> consumerThreadNames = ConcurrentHashMap.newKeySet();

    private volatile boolean started = false;

    public BackgroundWorker(TransactionProcessor processor) {
        this(processor, DEFAULT_THREAD_NAME);
    }

    /** US-12 / L2-FR-13 / DD-12: threadName is sourced from {@code TransactionServiceProperties}, not hardcoded. */
    public BackgroundWorker(TransactionProcessor processor, String threadName) {
        this.processor = processor;
        this.threadName = threadName;
        this.executor = Executors.newSingleThreadExecutor(this::newDaemonThread);
    }

    /**
     * AC-12.1 / AC-12.2: hands the request to the queue and returns
     * immediately - it is not processed inline on the caller's thread.
     */
    public void submit(TransactionRequest request) {
        submittedCount.incrementAndGet();
        queue.add(request);
    }

    /**
     * Starts the single consumer thread. Safe to call more than once -
     * only the first call has any effect.
     */
    public synchronized void start() {
        if (started) {
            return;
        }
        started = true;
        executor.submit(this::drainLoop);
    }

    private void drainLoop() {
        while (true) {
            TransactionRequest request;
            try {
                request = queue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            consumerThreadNames.add(Thread.currentThread().getName());
            try {
                processor.process(request);
            } catch (RuntimeException e) {
                log.error("background worker: unhandled exception processing requestId={}",
                        request.getRequestId(), e);
            } finally {
                synchronized (drainLock) {
                    processedCount.incrementAndGet();
                    drainLock.notifyAll();
                }
            }
        }
    }

    /**
     * AC-12.2: blocks until every request submitted so far has been fully
     * processed by the consumer thread. Only meaningful once the producer
     * has stopped submitting new requests for this batch.
     */
    public void awaitDrained() {
        synchronized (drainLock) {
            while (processedCount.get() < submittedCount.get()) {
                try {
                    drainLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    /** AC-12.3 verification support: how many distinct threads have ever drained the queue (should be 1). */
    public int consumerThreadCount() {
        return consumerThreadNames.size();
    }

    private Thread newDaemonThread(Runnable task) {
        Thread thread = new Thread(task, threadName);
        thread.setDaemon(true);
        return thread;
    }
}
