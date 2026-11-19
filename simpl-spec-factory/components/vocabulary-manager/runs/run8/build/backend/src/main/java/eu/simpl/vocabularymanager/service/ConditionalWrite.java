package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.store.StoreUnavailableException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Optimistic concurrency of D82: an attempt reads, checks and writes with a store-side condition; when the condition
 * fails it runs again, at most {@code maxRetries} times within the write budget of D71, then 503 (D44). The service
 * holds no lock state, so several backend instances may share one store.
 */
@Component
public class ConditionalWrite {

    private static final Logger LOG = LoggerFactory.getLogger(ConditionalWrite.class);

    private final VocabularyManagerProperties.Writes settings;
    private final Clock clock;

    /**
     * @param properties component settings
     * @param clock clock for the budget
     */
    public ConditionalWrite(VocabularyManagerProperties properties, Clock clock) {
        this.settings = properties.writes();
        this.clock = clock;
    }

    /**
     * @param attempt one read-check-write cycle; empty when the store condition failed
     * @param <T> result type
     * @return the result of the first attempt whose condition held
     * @throws StoreUnavailableException when the retries or the budget run out
     */
    public <T> T run(Supplier<Optional<T>> attempt) {
        Instant deadline = clock.instant().plus(settings.budget());
        for (int round = 0; round <= settings.maxRetries(); round++) {
            Optional<T> result = attempt.get();
            if (result.isPresent()) {
                return result.get();
            }
            LOG.info("Store condition failed, round {} of {}", round + 1, settings.maxRetries() + 1);
            if (clock.instant().isAfter(deadline)) {
                break;
            }
            pause(round);
        }
        throw new StoreUnavailableException("Conditional write did not succeed within the retry budget", null);
    }

    private static void pause(int round) {
        long millis = ThreadLocalRandom.current().nextLong(10, 40L * (round + 1));
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StoreUnavailableException("Interrupted between write attempts", e);
        }
    }
}
