package eu.simpl.vocabularymanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.problem.ProblemException;
import eu.simpl.vocabularymanager.support.TestProperties;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;

/** Decision D43: waiting for the lock counts towards the timeout; when it runs out the request gets 503. */
class VocabularyLocksTest {

    @Test
    void lockTimeoutGives503() throws Exception {
        AppProperties base = TestProperties.properties();
        AppProperties props = new AppProperties(
                base.contentBaseUrl(),
                new AppProperties.Store(
                        base.store().datasetUrl(),
                        base.store().connectTimeout(),
                        base.store().responseTimeout(),
                        base.store().healthTimeout(),
                        Duration.ofMillis(100)),
                base.identity(),
                base.limits());
        VocabularyLocks locks = new VocabularyLocks(props);
        CountDownLatch held = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread holder = new Thread(() -> locks.withVocabulary("Health", () -> {
            held.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        }));
        holder.start();
        held.await();
        assertThatThrownBy(() -> locks.withVocabulary("HEALTH", () -> 1))
                .isInstanceOfSatisfying(
                        ProblemException.class, e -> assertThat(e.status()).isEqualTo(503));
        assertThat(locks.withExternalRegistry(() -> 2)).isEqualTo(2);
        release.countDown();
        holder.join();
        assertThat(locks.withVocabulary("Health", () -> 3)).isEqualTo(3);
    }
}
