package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.error.Problems;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Retry policy of the conditional writes (D82, D157): waits 50, 100, 200, 400 and 800 ms, each with up to
 * 50 % random jitter, within the write budget of D71; after that the request gets 503.
 */
public class WriteRetry {

    private static final long[] BACKOFF_MILLIS = {50, 100, 200, 400, 800};

    private final int maxAttempts;
    private final Duration budget;
    private final Clock clock;

    public WriteRetry(int maxAttempts, Duration budget, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.budget = budget;
        this.clock = clock;
    }

    /** One attempt of a conditional write. */
    @FunctionalInterface
    public interface Attempt<T> {
        /** Returns the result, or null when the store condition failed and the write must be tried again. */
        T run();
    }

    public <T> T run(Attempt<T> attempt) {
        Instant deadline = clock.instant().plus(budget);
        for (int number = 0; number < maxAttempts; number++) {
            T result = attempt.run();
            if (result != null) {
                return result;
            }
            if (number == maxAttempts - 1) {
                break;
            }
            long base = BACKOFF_MILLIS[Math.min(number, BACKOFF_MILLIS.length - 1)];
            long wait = base + ThreadLocalRandom.current().nextLong(base / 2 + 1);
            if (clock.instant().plusMillis(wait).isAfter(deadline)) {
                break;
            }
            sleep(wait);
        }
        throw Problems.storeUnavailable();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw Problems.storeUnavailable();
        }
    }
}
