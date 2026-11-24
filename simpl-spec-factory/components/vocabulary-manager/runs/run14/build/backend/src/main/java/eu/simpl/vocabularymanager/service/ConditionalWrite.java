package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.store.StoreClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Retry of conditional store writes (D82, D157, D71): when the condition failed, the caller reads again and
 * retries, at most 5 times, with backoff 50, 100, 200, 400 and 800 ms and up to 50 % jitter, inside a 30 s budget;
 * then 503.
 */
@Component
public class ConditionalWrite {

    private static final List<Long> BACKOFF_MS = List.of(50L, 100L, 200L, 400L, 800L);
    private static final Duration BUDGET = Duration.ofSeconds(30);

    private final Clock clock;

    public ConditionalWrite(Clock clock) {
        this.clock = clock;
    }

    /** One attempt: returns a result, or null when the condition failed and the write must be retried. */
    @FunctionalInterface
    public interface Attempt<T> {
        T run();
    }

    /**
     * Runs attempts until one succeeds.
     *
     * @param attempt the attempt, which reads the current state itself
     * @param <T> the result type
     * @return the result of the successful attempt
     * @throws ApiException 503 when every retry failed or the budget ran out
     */
    public <T> T run(Attempt<T> attempt) {
        Instant deadline = clock.instant().plus(BUDGET);
        for (int retry = 0; retry <= BACKOFF_MS.size(); retry++) {
            T result = attempt.run();
            if (result != null) {
                return result;
            }
            if (retry == BACKOFF_MS.size()) {
                break;
            }
            long base = BACKOFF_MS.get(retry);
            long wait = base + ThreadLocalRandom.current().nextLong(base / 2 + 1);
            if (clock.instant().plusMillis(wait).isAfter(deadline)) {
                break;
            }
            pause(wait);
        }
        throw ApiException.unavailable(StoreClient.STORE_DOWN, null);
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw ApiException.unavailable(StoreClient.STORE_DOWN, e);
        }
    }
}
