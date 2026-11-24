package eu.simpl.vocabularymanager.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.ProblemType;
import eu.simpl.vocabularymanager.support.TestIdentityProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Key fetch timing and token checks of D44, D88, D108, D129, D132 (identity provider cases are unit tested, D44). */
class RealmKeyCacheTest {

    private static final TestIdentityProvider IDP = TestIdentityProvider.shared();

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-23T10:00:00Z"));

        void advance(Duration duration) {
            now.updateAndGet(instant -> instant.plus(duration));
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
            return now.get();
        }
    }

    private static AppProperties properties(String jwks) {
        return new AppProperties(
                "https://authority.example/vocabulary-manager",
                4,
                null,
                new AppProperties.Identity(TestIdentityProvider.ISSUER, TestIdentityProvider.AUDIENCE, jwks, 2000));
    }

    @Test
    void refetchAtMostOncePerTenSeconds() {
        MutableClock clock = new MutableClock();
        RealmKeyCache cache = new RealmKeyCache(properties(IDP.jwksUrl()), CircuitBreakerRegistry.ofDefaults(), clock);
        assertThat(cache.hasKeys()).isFalse();
        int before = IDP.fetches();
        assertThat(cache.lookup("key-1")).isEqualTo(RealmKeyCache.Lookup.FOUND);
        assertThat(cache.lookup("key-1")).isEqualTo(RealmKeyCache.Lookup.FOUND);
        assertThat(cache.lookup(null)).isEqualTo(RealmKeyCache.Lookup.FOUND);
        assertThat(cache.lookup("other")).isEqualTo(RealmKeyCache.Lookup.NOT_FOUND);
        assertThat(IDP.fetches() - before).isEqualTo(1);
        clock.advance(Duration.ofSeconds(11));
        assertThat(cache.lookup("other")).isEqualTo(RealmKeyCache.Lookup.NOT_FOUND);
        assertThat(IDP.fetches() - before).isEqualTo(2);
    }

    @Test
    void identityProviderDownFailsClosed() {
        MutableClock clock = new MutableClock();
        RealmKeyCache cache = new RealmKeyCache(properties(IDP.jwksUrl()), CircuitBreakerRegistry.ofDefaults(), clock);
        assertThat(cache.lookup("key-1")).isEqualTo(RealmKeyCache.Lookup.FOUND);
        IDP.setDown(true);
        try {
            clock.advance(Duration.ofSeconds(11));
            assertThat(cache.lookup("key-1")).isEqualTo(RealmKeyCache.Lookup.FOUND);
            assertThat(cache.lookup("rotated")).isEqualTo(RealmKeyCache.Lookup.UNAVAILABLE);
            assertThat(cache.lastFetchFailed()).isTrue();
            assertThat(cache.lookup("rotated")).isEqualTo(RealmKeyCache.Lookup.UNAVAILABLE);
        } finally {
            IDP.setDown(false);
        }
        RealmKeyCache unreachable =
                new RealmKeyCache(properties("http://127.0.0.1:9/certs"), CircuitBreakerRegistry.ofDefaults(), clock);
        assertThat(unreachable.lookup("key-1")).isEqualTo(RealmKeyCache.Lookup.UNAVAILABLE);
        JwtAuthenticator authenticator = new JwtAuthenticator(properties("http://127.0.0.1:9/certs"), unreachable);
        String token = IDP.token("ga-admin", "GA_VOCABULARY_ADMIN");
        assertThatThrownBy(() -> authenticator.authenticate("Bearer " + token))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.type()).isEqualTo(ProblemType.SERVICE_UNAVAILABLE);
                    assertThat(e.detail()).isEqualTo("The identity provider is not available. Retry later.");
                });
    }

    @Test
    void authenticatorReadsCreatorAndRoles() {
        RealmKeyCache cache =
                new RealmKeyCache(properties(IDP.jwksUrl()), CircuitBreakerRegistry.ofDefaults(), Clock.systemUTC());
        JwtAuthenticator authenticator = new JwtAuthenticator(properties(IDP.jwksUrl()), cache);
        AuthenticatedUser user = authenticator.authenticate("bearer " + IDP.token("ga-viewer", "GA_VOCABULARY_VIEWER"));
        assertThat(user.creator()).isEqualTo("ga-viewer@example.com");
        assertThat(user.subject()).isEqualTo("sub-ga-viewer");
        assertThat(user.canRead()).isTrue();
        assertThat(user.canWrite()).isFalse();
        assertThat(user.toString()).doesNotContain("example.com");
        for (String header : new String[] {null, "Bearer", "Bearer   ", "Token x", "Bearer a.b.c"}) {
            assertThatThrownBy(() -> authenticator.authenticate(header)).isInstanceOf(ApiException.class);
        }
    }
}
