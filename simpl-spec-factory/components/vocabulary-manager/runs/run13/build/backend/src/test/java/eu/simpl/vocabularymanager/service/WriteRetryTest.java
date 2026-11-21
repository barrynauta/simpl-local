package eu.simpl.vocabularymanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.simpl.vocabularymanager.error.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class WriteRetryTest {

    @Test
    void retriesUntilTheConditionHolds() {
        AtomicInteger attempts = new AtomicInteger();
        String result = new WriteRetry(6, Duration.ofSeconds(30), Clock.systemUTC())
                .run(() -> attempts.incrementAndGet() < 3 ? null : "done");
        assertThat(result).isEqualTo("done");
        assertThat(attempts).hasValue(3);
    }

    @Test
    void givesUpWith503() {
        AtomicInteger attempts = new AtomicInteger();
        assertThatThrownBy(() -> new WriteRetry(3, Duration.ofSeconds(30), Clock.systemUTC()).run(() -> {
                    attempts.incrementAndGet();
                    return null;
                }))
                .isInstanceOf(ApiException.class)
                .satisfies(e ->
                        assertThat(((ApiException) e).problemType().status()).isEqualTo(503));
        assertThat(attempts).hasValue(3);
    }

    @Test
    void stopsWhenTheBudgetIsSpent() {
        AtomicInteger attempts = new AtomicInteger();
        assertThatThrownBy(() -> new WriteRetry(6, Duration.ofMillis(10), Clock.systemUTC()).run(() -> {
                    attempts.incrementAndGet();
                    return null;
                }))
                .isInstanceOf(ApiException.class);
        assertThat(attempts.get()).isLessThan(6);
    }
}
