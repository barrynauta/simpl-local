package eu.simpl.vocabularymanager.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.error.ProblemException;
import eu.simpl.vocabularymanager.support.TestIdentityProvider;
import eu.simpl.vocabularymanager.web.OutboundHeaders;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/** Token validation and realm key timing (D17, D44, D88, D108, E5). */
class TokenValidationTest {

    private TestIdentityProvider idp;
    private MutableClock clock;
    private RealmKeyProvider keys;
    private TokenValidator validator;

    @BeforeEach
    void setUp() {
        idp = new TestIdentityProvider();
        clock = new MutableClock(Instant.now());
        AppProperties.Security settings = new AppProperties.Security(
                TestIdentityProvider.ISSUER,
                idp.jwksUri(),
                TestIdentityProvider.AUDIENCE,
                Duration.ofSeconds(2),
                Duration.ofSeconds(10),
                Duration.ofSeconds(30));
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        OutboundHeaders headers = new OutboundHeaders(
                beans.getBeanProvider(io.micrometer.tracing.Tracer.class),
                beans.getBeanProvider(io.micrometer.tracing.propagation.Propagator.class));
        keys = new RealmKeyProvider(
                settings,
                RealmKeyProvider.httpClient(Duration.ofSeconds(2)),
                CircuitBreaker.ofDefaults("keys"),
                headers,
                clock);
        validator = new TokenValidator(settings, keys, clock);
    }

    @AfterEach
    void tearDown() {
        idp.close();
    }

    @Test
    void validTokenGivesUserWithEmailAsCreatorAndRealmRoles() {
        AuthenticatedUser user = validator.validate(idp.admin());
        assertThat(user.subject()).isEqualTo("admin-sub");
        assertThat(user.creator()).isEqualTo("ga-admin@example.com");
        assertThat(user.roles()).containsExactly("GA_VOCABULARY_ADMIN");
        assertThat(user.getPrincipal()).isEqualTo("admin-sub");
        assertThat(user.getCredentials()).isEqualTo("");
        assertThat(user.isAuthenticated()).isTrue();
    }

    @Test
    void creatorFallsBackToPreferredUsername() {
        AuthenticatedUser user = validator.validate(idp.token("sub-x", null, List.of()));
        assertThat(user.creator()).isEqualTo("sub-x");
        assertThat(user.roles()).isEmpty();
    }

    @Test
    void rejectsBadTokens() throws Exception {
        assertStatus("x.y.z", 401);
        assertStatus("", 401);
        JWTClaimsSet claims = idp.claims("s", null, List.of()).build();
        SignedJWT hmac = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.HS256).keyID("test-key").build(), claims);
        hmac.sign(new MACSigner(new byte[32]));
        assertStatus(hmac.serialize(), 401);
        SignedJWT noKid = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        noKid.sign(new RSASSASigner(idp.key()));
        assertStatus(noKid.serialize(), 401);
        assertStatus(idp.sign(idp.claims("s", null, List.of()).subject(null).build(), "test-key", idp.key()), 401);
        assertStatus(
                idp.sign(
                        idp.claims("s", null, List.of())
                                .notBeforeTime(Date.from(Instant.now().plusSeconds(600)))
                                .build(),
                        "test-key",
                        idp.key()),
                401);
        assertStatus(
                idp.sign(idp.claims("s", null, List.of()).expirationTime(null).build(), "test-key", idp.key()), 401);
    }

    @Test
    void unknownKeyIdGetsOneFetchPerCooldownThen401() {
        validator.validate(idp.admin());
        int fetches = idp.fetchCount();
        String unknownKid = idp.sign(idp.claims("s", null, List.of()).build(), "other-kid", idp.key());
        clock.advance(Duration.ofSeconds(11));
        assertStatus(unknownKid, 401);
        assertThat(idp.fetchCount()).isEqualTo(fetches + 1);
        assertStatus(unknownKid, 401);
        assertThat(idp.fetchCount()).isEqualTo(fetches + 1);
    }

    @Test
    void cachedKeysWorkWhileTheIdentityProviderIsDownAndUnknownKeysFailClosed() {
        validator.validate(idp.admin());
        idp.setAvailable(false);
        assertThat(validator.validate(idp.viewer()).subject()).isEqualTo("viewer-sub");
        clock.advance(Duration.ofSeconds(11));
        String unknownKid = idp.sign(idp.claims("s", null, List.of()).build(), "other-kid", idp.key());
        assertStatus(unknownKid, 503);
        assertStatus(unknownKid, 503);
        assertThat(keys.lastFetchFailed()).isTrue();
        assertThat(keys.hasKeys()).isTrue();
    }

    @Test
    void identityProviderDownAtFirstTokenGives503() {
        idp.setAvailable(false);
        assertStatus(idp.admin(), 503);
        assertThat(keys.hasKeys()).isFalse();
        idp.setAvailable(true);
        clock.advance(Duration.ofSeconds(11));
        assertThat(validator.validate(idp.admin()).subject()).isEqualTo("admin-sub");
    }

    @Test
    void unreachableIdentityProviderGives503() {
        idp.close();
        assertStatus(idp.admin(), 503);
        assertThat(keys.refresh()).isFalse();
    }

    private void assertStatus(String token, int status) {
        assertThatThrownBy(() -> validator.validate(token))
                .isInstanceOf(ProblemException.class)
                .extracting(e -> ((ProblemException) e).status())
                .isEqualTo(status);
    }

    /** A clock the test moves forward. */
    static final class MutableClock extends Clock {
        private final AtomicReference<Instant> now;

        MutableClock(Instant start) {
            now = new AtomicReference<>(start);
        }

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
}
