package eu.simpl.vocabularymanager.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.error.VocabularyManagerException;
import eu.simpl.vocabularymanager.support.TestTokens;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

class TokenValidatorTest {

    private RealmKeys keys(String url) {
        VocabularyManagerProperties properties = new VocabularyManagerProperties(
                null,
                0,
                null,
                new VocabularyManagerProperties.Security(
                        url, TestTokens.ISSUER, TestTokens.AUDIENCE, Duration.ofSeconds(2)));
        return new RealmKeys(properties, Clock.systemUTC());
    }

    private TokenValidator validator(RealmKeys keys) {
        VocabularyManagerProperties properties = new VocabularyManagerProperties(
                null,
                0,
                null,
                new VocabularyManagerProperties.Security(
                        TestTokens.jwksUrl(), TestTokens.ISSUER, TestTokens.AUDIENCE, Duration.ofSeconds(2)));
        return new TokenValidator(keys, properties, Clock.systemUTC());
    }

    @AfterEach
    void reset() {
        TestTokens.JWKS_DOWN.set(false);
    }

    private static int status(Throwable e) {
        return ((VocabularyManagerException) e).code().status();
    }

    @Test
    void validTokenGivesUserAndRoles() {
        AuthenticatedUser user = validator(keys(TestTokens.jwksUrl())).validate(TestTokens.admin());
        assertThat(user.getName()).isEqualTo("ga-admin@example.com");
        assertThat(user.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_GA_VOCABULARY_ADMIN");
    }

    @Test
    void creatorFallsBackToPreferredUsername() {
        String token = TestTokens.sign(
                TestTokens.KEY,
                TestTokens.claims("x@example.com", List.of())
                        .claim("email", null)
                        .claim("preferred_username", "jdoe")
                        .build());
        assertThat(validator(keys(TestTokens.jwksUrl())).validate(token).getName())
                .isEqualTo("jdoe");
    }

    @Test
    void rejectsExpiredWrongIssuerWrongAudienceAndGarbage() {
        TokenValidator validator = validator(keys(TestTokens.jwksUrl()));
        Instant past = Instant.now().minusSeconds(3600);
        String expired = TestTokens.sign(
                TestTokens.KEY,
                TestTokens.claims("a@example.com", List.of())
                        .expirationTime(Date.from(past))
                        .build());
        String issuer = TestTokens.sign(
                TestTokens.KEY,
                TestTokens.claims("a@example.com", List.of())
                        .issuer("http://evil")
                        .build());
        String audience = TestTokens.sign(
                TestTokens.KEY,
                TestTokens.claims("a@example.com", List.of()).audience("other").build());
        String future = TestTokens.sign(
                TestTokens.KEY,
                TestTokens.claims("a@example.com", List.of())
                        .notBeforeTime(Date.from(Instant.now().plusSeconds(3600)))
                        .build());
        for (String token : List.of(expired, issuer, audience, future, "not-a-token")) {
            assertThatThrownBy(() -> validator.validate(token))
                    .satisfies(e -> assertThat(status(e)).isEqualTo(401));
        }
    }

    @Test
    void rejectsSymmetricAlgorithmAndForeignKey() throws Exception {
        TokenValidator validator = validator(keys(TestTokens.jwksUrl()));
        SignedJWT hmac = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.HS256).keyID("test-key-1").build(),
                new JWTClaimsSet.Builder().issuer(TestTokens.ISSUER).build());
        hmac.sign(new MACSigner(new byte[32]));
        assertThatThrownBy(() -> validator.validate(hmac.serialize()))
                .satisfies(e -> assertThat(status(e)).isEqualTo(401));
        String foreign = TestTokens.sign(
                TestTokens.OTHER_KEY,
                TestTokens.claims("a@example.com", List.of()).build());
        assertThatThrownBy(() -> validator.validate(foreign))
                .satisfies(e -> assertThat(status(e)).isEqualTo(401));
    }

    @Test
    void tamperedSignatureIsRejected() {
        String token = TestTokens.admin();
        String tampered = token.substring(0, token.length() - 4) + (token.endsWith("AAAA") ? "BBBB" : "AAAA");
        assertThatThrownBy(() -> validator(keys(TestTokens.jwksUrl())).validate(tampered))
                .satisfies(e -> assertThat(status(e)).isEqualTo(401));
    }

    @Test
    void cachedKeysKeepWorkingWhileTheIdentityProviderIsDown() {
        RealmKeys keys = keys(TestTokens.jwksUrl());
        assertThat(keys.refreshQuietly()).isTrue();
        TestTokens.JWKS_DOWN.set(true);
        assertThat(validator(keys).validate(TestTokens.viewer()).getName()).isEqualTo("ga-viewer@example.com");
    }

    @Test
    void unknownKeyIdWithIdentityProviderDownFailsClosedWith503() {
        TestTokens.JWKS_DOWN.set(true);
        RealmKeys keys = keys(TestTokens.jwksUrl());
        assertThat(keys.hasKeys()).isFalse();
        assertThatThrownBy(() -> validator(keys).validate(TestTokens.admin()))
                .satisfies(e -> assertThat(status(e)).isEqualTo(503));
    }

    @Test
    void unreachableKeyEndpointFailsClosedWith503() {
        RealmKeys keys = keys("http://localhost:1/certs");
        keys.loadAtStart();
        assertThatThrownBy(() -> validator(keys).validate(TestTokens.admin()))
                .satisfies(e -> assertThat(status(e)).isEqualTo(503));
    }

    @Test
    void unknownKeyIdIsFetchedOnce() {
        RealmKeys keys = keys(TestTokens.jwksUrl());
        int before = TestTokens.JWKS_CALLS.get();
        assertThat(keys.key("test-key-1")).isNotNull();
        assertThat(keys.key("test-key-1")).isNotNull();
        assertThat(keys.key("unknown")).isNull();
        assertThat(TestTokens.JWKS_CALLS.get() - before).isEqualTo(1);
    }
}
