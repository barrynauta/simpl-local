package eu.simpl.vocabularymanager.unit;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.config.TraceHeaders;
import eu.simpl.vocabularymanager.it.TestEnvironment;
import eu.simpl.vocabularymanager.security.RealmKeys;
import eu.simpl.vocabularymanager.security.TokenVerifier;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Realm key fetch timing (D44, D88, D108, D129) and token checks (D17, D132). */
class RealmKeysTest {

    private final MutableClock clock = new MutableClock();

    @AfterEach
    void idpBackUp() {
        TestEnvironment.idpUp = true;
    }

    private RealmKeys keys(String url) {
        AppProperties properties = new AppProperties(
                "https://authority.example/vocabulary-manager",
                new AppProperties.Security(
                        TestEnvironment.ISSUER,
                        TestEnvironment.AUDIENCE,
                        url,
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(10)),
                null,
                null);
        return new RealmKeys(
                properties,
                HttpClient.newHttpClient(),
                CircuitBreaker.ofDefaults("idp"),
                clock,
                new TraceHeaders(Tracer.NOOP, Propagator.NOOP));
    }

    @Test
    void fetchesLazilyAndAnswersUnknownKeysWithinTheWindowWith401() {
        RealmKeys keys = keys(TestEnvironment.jwksUrl());
        assertThat(keys.loaded()).isFalse();
        assertThat(keys.lookup("test-key")).isInstanceOf(RealmKeys.Found.class);
        assertThat(keys.loaded()).isTrue();
        assertThat(keys.lookup("unknown")).isInstanceOf(RealmKeys.Unknown.class);
        clock.advance(Duration.ofSeconds(11));
        assertThat(keys.lookup("unknown")).isInstanceOf(RealmKeys.Unknown.class);
    }

    @Test
    void failedFetchGives503InsideTheWindowAndCachedKeysKeepWorking() {
        RealmKeys keys = keys(TestEnvironment.jwksUrl());
        assertThat(keys.lookup("test-key")).isInstanceOf(RealmKeys.Found.class);
        TestEnvironment.idpUp = false;
        clock.advance(Duration.ofSeconds(11));
        assertThat(keys.lookup("other")).isInstanceOf(RealmKeys.Unavailable.class);
        assertThat(keys.lastFetchFailed()).isTrue();
        assertThat(keys.lookup("other")).isInstanceOf(RealmKeys.Unavailable.class);
        assertThat(keys.lookup("test-key")).isInstanceOf(RealmKeys.Found.class);
    }

    @Test
    void unreachableProviderFailsClosed() {
        RealmKeys keys = keys("http://127.0.0.1:1/certs");
        assertThat(keys.lookup("test-key")).isInstanceOf(RealmKeys.Unavailable.class);
        assertThat(keys.loaded()).isFalse();
        TokenVerifier verifier = new TokenVerifier(
                new AppProperties(
                        "x",
                        new AppProperties.Security(
                                TestEnvironment.ISSUER,
                                TestEnvironment.AUDIENCE,
                                "x",
                                Duration.ofSeconds(1),
                                Duration.ofSeconds(10)),
                        null,
                        null),
                keys);
        assertThat(verifier.verify(TestEnvironment.admin())).isInstanceOf(TokenVerifier.KeysUnavailable.class);
    }

    @Test
    void verifierAcceptsValidTokensAndRejectsOthers() {
        RealmKeys keys = keys(TestEnvironment.jwksUrl());
        TokenVerifier verifier = new TokenVerifier(
                new AppProperties(
                        "x",
                        new AppProperties.Security(
                                TestEnvironment.ISSUER,
                                TestEnvironment.AUDIENCE,
                                "x",
                                Duration.ofSeconds(1),
                                Duration.ofSeconds(10)),
                        null,
                        null),
                keys);
        TokenVerifier.Result valid = verifier.verify(TestEnvironment.token("u", List.of("GA_VOCABULARY_VIEWER")));
        assertThat(valid).isInstanceOf(TokenVerifier.Valid.class);
        assertThat(((TokenVerifier.Valid) valid).user().creator()).isEqualTo("u@example.com");
        assertThat(((TokenVerifier.Valid) valid).user().toString()).doesNotContain("example.com");
        assertThat(verifier.verify("a.b.c")).isInstanceOf(TokenVerifier.Invalid.class);
        assertThat(verifier.verify("eyJhbGciOiJub25lIn0.eyJzdWIiOiJ4In0.")).isInstanceOf(TokenVerifier.Invalid.class);
    }

    static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-20T10:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
