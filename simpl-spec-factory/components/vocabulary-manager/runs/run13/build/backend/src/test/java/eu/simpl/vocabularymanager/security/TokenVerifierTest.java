package eu.simpl.vocabularymanager.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.support.TestKeys;
import eu.simpl.vocabularymanager.web.TraceHeaders;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

class TokenVerifierTest {

    private static final TestKeys KEYS = new TestKeys("kid-1");

    private static TokenVerifier verifier(TestKeys keys) {
        VocabularyManagerProperties.Identity identity = new VocabularyManagerProperties.Identity(
                TestKeys.ISSUER, TestKeys.AUDIENCE, keys.jwksUri(), Duration.ofSeconds(2), Duration.ofSeconds(10));
        RealmKeyProvider provider = new RealmKeyProvider(
                identity, Clock.systemUTC(), CircuitBreaker.ofDefaults("t"), new TraceHeaders(null, null));
        return new TokenVerifier(identity, provider);
    }

    @AfterAll
    static void stop() {
        KEYS.stop();
    }

    @Test
    void acceptsAValidTokenAndReadsRolesAndCreator() throws Exception {
        AuthenticatedUser user = verifier(KEYS).verify(KEYS.token("ga-admin", List.of("GA_VOCABULARY_ADMIN", "x")));
        assertThat(user.creator()).isEqualTo("ga-admin@example.com");
        assertThat(user.subject()).isEqualTo("sub-ga-admin");
        assertThat(user.canWrite()).isTrue();
        assertThat(user.canRead()).isTrue();
    }

    @Test
    void viewerAndNoRole() throws Exception {
        TokenVerifier verifier = verifier(KEYS);
        AuthenticatedUser viewer = verifier.verify(KEYS.token("v", List.of("GA_VOCABULARY_VIEWER")));
        assertThat(viewer.canRead()).isTrue();
        assertThat(viewer.canWrite()).isFalse();
        AuthenticatedUser none = verifier.verify(KEYS.token("n", List.of()));
        assertThat(none.canRead()).isFalse();
    }

    @Test
    void rejectsWrongIssuerAudienceAndExpiredTokens() {
        TokenVerifier verifier = verifier(KEYS);
        Instant later = Instant.now().plusSeconds(300);
        assertThatThrownBy(() ->
                        verifier.verify(KEYS.token("a", List.of(), "http://other/realms/x", TestKeys.AUDIENCE, later)))
                .isInstanceOf(TokenVerifier.InvalidTokenException.class);
        assertThatThrownBy(() -> verifier.verify(KEYS.token("a", List.of(), TestKeys.ISSUER, "account", later)))
                .isInstanceOf(TokenVerifier.InvalidTokenException.class);
        assertThatThrownBy(() -> verifier.verify(KEYS.token(
                        "a",
                        List.of(),
                        TestKeys.ISSUER,
                        TestKeys.AUDIENCE,
                        Instant.now().minusSeconds(600))))
                .isInstanceOf(TokenVerifier.InvalidTokenException.class);
        assertThatThrownBy(() -> verifier.verify("garbage")).isInstanceOf(TokenVerifier.InvalidTokenException.class);
    }

    @Test
    void rejectsForgedSignatureAndSymmetricAlgorithms() throws Exception {
        TokenVerifier verifier = verifier(KEYS);
        RSAKey forger = new RSAKeyGenerator(2048).keyID("kid-1").generate();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("x")
                .issuer(TestKeys.ISSUER)
                .audience(TestKeys.AUDIENCE)
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plusSeconds(60)))
                .build();
        assertThatThrownBy(() -> verifier.verify(TestKeys.sign(claims, forger)))
                .isInstanceOf(TokenVerifier.InvalidTokenException.class);
        SignedJWT hmac = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.HS256).keyID("kid-1").build(), claims);
        hmac.sign(new MACSigner("0123456789abcdef0123456789abcdef0123456789abcdef"));
        assertThatThrownBy(() -> verifier.verify(hmac.serialize()))
                .isInstanceOf(TokenVerifier.InvalidTokenException.class);
        RSAKey otherKid = new RSAKeyGenerator(2048).keyID("unknown-kid").generate();
        assertThatThrownBy(() -> verifier.verify(TestKeys.sign(claims, otherKid)))
                .isInstanceOf(TokenVerifier.InvalidTokenException.class);
    }

    @Test
    void failsClosedWith503WhenKeysCannotBeFetched() {
        TestKeys down = new TestKeys("kid-down");
        down.setAvailable(false);
        try {
            TokenVerifier verifier = verifier(down);
            String token = down.token("a", List.of("GA_VOCABULARY_ADMIN"));
            assertThatThrownBy(() -> verifier.verify(token))
                    .isInstanceOf(ApiException.class)
                    .satisfies(e -> assertThat(((ApiException) e).problemType().status())
                            .isEqualTo(503));
        } finally {
            down.stop();
        }
    }
}
