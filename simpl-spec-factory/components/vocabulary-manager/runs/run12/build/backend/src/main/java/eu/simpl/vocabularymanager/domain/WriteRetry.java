package eu.simpl.vocabularymanager.domain;

import eu.simpl.vocabularymanager.error.ApiException;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Retry of a conditional write whose condition failed (D82, D157): at most 5 retries after 50, 100,
 * 200, 400 and 800 ms, each with up to 50 % random jitter, inside the write budget (D71); then 503.
 */
public final class WriteRetry {

    static final long[] BACKOFF_MS = {50, 100, 200, 400, 800};

    private final long deadlineNanos;
    private int attempt;

    public WriteRetry(Duration budget) {
        this.deadlineNanos = System.nanoTime() + budget.toNanos();
    }

    /** Waits before the next attempt, or throws 503 when no attempt is left. */
    public void beforeRetry() {
        if (attempt >= BACKOFF_MS.length) {
            throw ApiException.unavailable(ApiException.STORE_UNAVAILABLE);
        }
        long base = BACKOFF_MS[attempt++];
        long wait = base + ThreadLocalRandom.current().nextLong(base / 2 + 1);
        if (System.nanoTime() + wait * 1_000_000L > deadlineNanos) {
            throw ApiException.unavailable(ApiException.STORE_UNAVAILABLE);
        }
        try {
            Thread.sleep(wait);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw ApiException.unavailable(ApiException.STORE_UNAVAILABLE);
        }
    }

    int attempts() {
        return attempt;
    }
}
