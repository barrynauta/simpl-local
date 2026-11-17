package eu.simpl.vocabulary.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/**
 * Millisecond timestamps that never repeat, so sorting by creationDate or lastUpdated is deterministic even when
 * two vocabularies are created in the same millisecond.
 */
@Component
public class MonotonicClock {

    private Instant last = Instant.EPOCH;

    public synchronized Instant now() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        if (!now.isAfter(last)) {
            now = last.plusMillis(1);
        }
        last = now;
        return now;
    }
}
