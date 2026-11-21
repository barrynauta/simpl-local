package eu.simpl.vocabularymanager.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.support.TestKeys;
import eu.simpl.vocabularymanager.web.TraceHeaders;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class RealmKeyProviderTest {

    /** A clock the test moves by hand. */
    static final class MovableClock extends Clock {
        final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-21T10:00:00Z"));

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now.get();
        }

        void advance(Duration duration) {
            now.set(now.get().plus(duration));
        }
    }

    private static RealmKeyProvider provider(TestKeys keys, Clock clock) {
        return new RealmKeyProvider(
                new VocabularyManagerProperties.Identity(
                        TestKeys.ISSUER,
                        TestKeys.AUDIENCE,
                        keys.jwksUri(),
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(10)),
                clock,
                CircuitBreaker.ofDefaults("idp"),
                new TraceHeaders(null, null));
    }

    @Test
    void lazyFetchCachedKeysAndCooldownGives401() throws Exception {
        TestKeys keys = new TestKeys("k1");
        MovableClock clock = new MovableClock();
        try {
            RealmKeyProvider provider = provider(keys, clock);
            assertThat(provider.state()).isEqualTo(RealmKeyProvider.KeyState.NOT_FETCHED);
            assertThat(provider.keyFor("k1").getKeyID()).isEqualTo("k1");
            assertThat(provider.state()).isEqualTo(RealmKeyProvider.KeyState.LOADED);
            assertThat(keys.fetches()).isEqualTo(1);
            assertThatThrownBy(() -> provider.keyFor("unknown"))
                    .isInstanceOf(RealmKeyProvider.UnknownKeyException.class);
            assertThatThrownBy(() -> provider.keyFor("unknown"))
                    .isInstanceOf(RealmKeyProvider.UnknownKeyException.class);
            assertThat(keys.fetches())
                    .as("no refetch inside the 10 s window (D88)")
                    .isEqualTo(1);
            clock.advance(Duration.ofSeconds(11));
            assertThatThrownBy(() -> provider.keyFor("unknown"))
                    .isInstanceOf(RealmKeyProvider.UnknownKeyException.class);
            assertThat(keys.fetches()).isEqualTo(2);
            keys.setAvailable(false);
            assertThat(provider.keyFor("k1")).isNotNull();
            clock.advance(Duration.ofSeconds(11));
            assertThatThrownBy(() -> provider.keyFor("unknown"))
                    .isInstanceOf(RealmKeyProvider.KeysUnavailableException.class);
            assertThatThrownBy(() -> provider.keyFor("unknown2"))
                    .isInstanceOf(RealmKeyProvider.KeysUnavailableException.class);
            assertThat(provider.keyFor("k1")).isNotNull();
            assertThat(provider.state()).isEqualTo(RealmKeyProvider.KeyState.LOADED);
        } finally {
            keys.stop();
        }
    }

    @Test
    void failedFirstFetchIsReportedForHealth() {
        TestKeys keys = new TestKeys("k2");
        keys.setAvailable(false);
        try {
            RealmKeyProvider provider = provider(keys, new MovableClock());
            assertThatThrownBy(() -> provider.keyFor("k2"))
                    .isInstanceOf(RealmKeyProvider.KeysUnavailableException.class);
            assertThat(provider.state()).isEqualTo(RealmKeyProvider.KeyState.FETCH_FAILED);
        } finally {
            keys.stop();
        }
    }
}
