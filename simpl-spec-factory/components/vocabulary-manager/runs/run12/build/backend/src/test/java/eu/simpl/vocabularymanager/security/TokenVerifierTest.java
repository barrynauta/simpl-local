package eu.simpl.vocabularymanager.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.it.TestIdp;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Token checks and key fetch timing (D17, D44, D88, D108, D129, D132). */
class TokenVerifierTest {

    TestIdp idp;
    AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-20T10:00:00Z"));

    @BeforeEach
    void start() throws Exception {
        idp = new TestIdp();
    }

    @AfterEach
    void stop() {
        idp.close();
    }

    TokenVerifier verifier(String jwks) {
        AppProperties props = new AppProperties(
                null,
                null,
                new AppProperties.Auth(
                        TestIdp.ISSUER, TestIdp.AUDIENCE, jwks, Duration.ofSeconds(2), Duration.ofSeconds(10)),
                null);
        Clock clock = new Clock() {
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
        };
        return new TokenVerifier(props, new SimpleMeterRegistry(), clock);
    }

    @Test
    void validTokenGivesPrincipal() throws Exception {
        TokenVerifier v = verifier(idp.jwksUri());
        TokenVerifier.Principal p = v.verify(idp.token("ga-admin", "GA_VOCABULARY_ADMIN"));
        assertThat(p.creator()).isEqualTo("ga-admin@example.com");
        assertThat(p.subject()).isEqualTo("sub-ga-admin");
        assertThat(p.roles()).containsExactly("GA_VOCABULARY_ADMIN");
        assertThat(v.degraded()).isFalse();
        v.verify(idp.token("ga-viewer"));
        assertThat(idp.fetches.get()).isEqualTo(1);
    }

    @Test
    void badTokensAre401() throws Exception {
        TokenVerifier v = verifier(idp.jwksUri());
        assertThatThrownBy(() -> v.verify("abc")).isInstanceOf(TokenVerifier.TokenException.class);
        String wrongAud =
                idp.token("u", TestIdp.ISSUER, "x", idp.key(), Instant.now().plusSeconds(60));
        assertThatThrownBy(() -> v.verify(wrongAud))
                .satisfies(e -> assertThat(((TokenVerifier.TokenException) e).unavailable())
                        .isFalse());
        String wrongIss = idp.token(
                "u", "http://other", TestIdp.AUDIENCE, idp.key(), Instant.now().plusSeconds(60));
        assertThatThrownBy(() -> v.verify(wrongIss)).isInstanceOf(TokenVerifier.TokenException.class);
        String expired = idp.token(
                "u", TestIdp.ISSUER, TestIdp.AUDIENCE, idp.key(), Instant.now().minusSeconds(600));
        assertThatThrownBy(() -> v.verify(expired)).isInstanceOf(TokenVerifier.TokenException.class);
        RSAKey other = new RSAKeyGenerator(2048).keyID("test-key").generate();
        String forged = idp.token(
                "u", TestIdp.ISSUER, TestIdp.AUDIENCE, other, Instant.now().plusSeconds(60));
        assertThatThrownBy(() -> v.verify(forged)).isInstanceOf(TokenVerifier.TokenException.class);
    }

    @Test
    void unknownKeyIdWithinWindowIs401ThenRefetch() throws Exception {
        TokenVerifier v = verifier(idp.jwksUri());
        v.verify(idp.token("u"));
        RSAKey rotated = new RSAKeyGenerator(2048).keyID("rotated").generate();
        String t = idp.token(
                "u", TestIdp.ISSUER, TestIdp.AUDIENCE, rotated, Instant.now().plusSeconds(60));
        assertThatThrownBy(() -> v.verify(t))
                .satisfies(e -> assertThat(((TokenVerifier.TokenException) e).unavailable())
                        .isFalse());
        assertThat(idp.fetches.get()).isEqualTo(1);
        now.set(now.get().plusSeconds(11));
        assertThatThrownBy(() -> v.verify(t))
                .satisfies(e -> assertThat(((TokenVerifier.TokenException) e).unavailable())
                        .isFalse());
        assertThat(idp.fetches.get()).isEqualTo(2);
    }

    @Test
    void keysUnavailableFailClosedWith503() throws Exception {
        idp.failing.set(true);
        TokenVerifier v = verifier(idp.jwksUri());
        String t = idp.token("u");
        assertThatThrownBy(() -> v.verify(t))
                .satisfies(e -> assertThat(((TokenVerifier.TokenException) e).unavailable())
                        .isTrue());
        assertThat(v.degraded()).isTrue();
        assertThatThrownBy(() -> v.verify(t))
                .satisfies(e -> assertThat(((TokenVerifier.TokenException) e).unavailable())
                        .isTrue());
        assertThat(idp.fetches.get()).isEqualTo(1);
        idp.failing.set(false);
        now.set(now.get().plusSeconds(11));
        assertThat(v.verify(t).subject()).isEqualTo("sub-u");
        assertThat(v.degraded()).isFalse();
        TokenVerifier unreachable = verifier("http://127.0.0.1:9/certs");
        assertThatThrownBy(() -> unreachable.verify(t))
                .satisfies(e -> assertThat(((TokenVerifier.TokenException) e).unavailable())
                        .isTrue());
    }

    @Test
    void cachedKeysKeepWorkingWhileProviderIsDown() throws Exception {
        TokenVerifier v = verifier(idp.jwksUri());
        v.verify(idp.token("u"));
        idp.failing.set(true);
        now.set(now.get().plusSeconds(60));
        assertThat(v.verify(idp.token("u")).subject()).isEqualTo("sub-u");
    }
}
