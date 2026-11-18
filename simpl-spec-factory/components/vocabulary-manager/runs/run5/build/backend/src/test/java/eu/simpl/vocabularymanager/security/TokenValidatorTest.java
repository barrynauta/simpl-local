package eu.simpl.vocabularymanager.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import eu.simpl.vocabularymanager.support.TestProperties;
import eu.simpl.vocabularymanager.support.TestTokens;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Token checks of D17, D27, D44 and OWT-A07. */
class TokenValidatorTest {

    private final TestTokens tokens = new TestTokens("key-1");
    private final AtomicInteger fetches = new AtomicInteger();
    private volatile boolean identityProviderUp = true;
    private volatile JWKSet published = tokens.publicKeys();
    private RealmKeys keys;
    private TokenValidator validator;

    @BeforeEach
    void setUp() {
        KeySource source = () -> {
            fetches.incrementAndGet();
            if (!identityProviderUp) {
                throw new IdentityProviderUnavailableException("down", null);
            }
            return published;
        };
        keys = new RealmKeys(source, Clock.systemUTC(), TestProperties.create());
        validator = new TokenValidator(keys, Clock.systemUTC(), TestProperties.create());
    }

    @Test
    void validAdminToken() {
        AuthenticatedUser user = validator.validate(tokens.admin());
        assertThat(user.roles()).containsExactly(Role.GA_VOCABULARY_ADMIN);
        assertThat(user.creator()).isEqualTo("ga-admin@example.com");
        assertThat(user.hasAnyRole(AccessRules.WRITE)).isTrue();
    }

    @Test
    void creatorFallsBackToPreferredUsername() {
        String token = tokens.sign(
                tokens.claims("someone", null, List.of("GA_VOCABULARY_VIEWER")).build());
        AuthenticatedUser user = validator.validate(token);
        assertThat(user.creator()).isEqualTo("someone");
        assertThat(user.hasAnyRole(AccessRules.WRITE)).isFalse();
        assertThat(user.hasAnyRole(AccessRules.READ)).isTrue();
    }

    @Test
    void userWithoutRoleIsAuthenticatedWithNoRoles() {
        assertThat(validator.validate(tokens.noRole()).roles()).isEmpty();
    }

    @Test
    void rejectsGarbage() {
        assertThatThrownBy(() -> validator.validate("not-a-jwt")).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void rejectsExpiredToken() {
        JWTClaimsSet claims = tokens.claims("u", "u@e", List.of())
                .expirationTime(Date.from(Instant.now().minusSeconds(120)))
                .build();
        assertThatThrownBy(() -> validator.validate(tokens.sign(claims))).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void rejectsOtherIssuerAndAudience() {
        JWTClaimsSet issuer =
                tokens.claims("u", "u@e", List.of()).issuer("http://evil").build();
        assertThatThrownBy(() -> validator.validate(tokens.sign(issuer))).isInstanceOf(InvalidTokenException.class);
        JWTClaimsSet audience =
                tokens.claims("u", "u@e", List.of()).audience("other").build();
        assertThatThrownBy(() -> validator.validate(tokens.sign(audience))).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void rejectsTokenSignedByAnotherKeyWithTheSameId() {
        TestTokens forger = new TestTokens("key-1");
        assertThatThrownBy(() -> validator.validate(forger.admin())).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void cachedKeysKeepWorkingWhileIdentityProviderIsDown() {
        keys.loadAtStart();
        identityProviderUp = false;
        assertThat(validator.validate(tokens.viewer()).roles()).containsExactly(Role.GA_VOCABULARY_VIEWER);
    }

    @Test
    void unknownKeyWithIdentityProviderDownFailsClosed() {
        identityProviderUp = false;
        keys.loadAtStart();
        assertThat(keys.hasKeys()).isFalse();
        assertThatThrownBy(() -> validator.validate(tokens.admin()))
                .isInstanceOf(IdentityProviderUnavailableException.class);
    }

    @Test
    void unknownKeyIdTriggersOneRefetchThenStaysUnknown() {
        keys.loadAtStart();
        int before = fetches.get();
        TestTokens rotated = new TestTokens("key-2");
        assertThatThrownBy(() -> validator.validate(rotated.admin())).isInstanceOf(InvalidTokenException.class);
        assertThat(fetches.get()).isEqualTo(before);
        assertThat(keys.find(null)).isEmpty();
    }

    @Test
    void rotatedKeyIsFetchedWhenTheCacheIsOld() {
        TestTokens rotated = new TestTokens("key-2");
        published = rotated.publicKeys();
        assertThat(validator.validate(rotated.admin()).roles()).contains(Role.GA_VOCABULARY_ADMIN);
        assertThat(fetches.get()).isEqualTo(1);
    }

    @Test
    void accessRules() {
        assertThat(AccessRules.required("GET", "/v1/health")).isNull();
        assertThat(AccessRules.required("GET", "/other")).isNull();
        assertThat(AccessRules.required("GET", "/v1/vocabularies")).isEqualTo(AccessRules.READ);
        assertThat(AccessRules.required("POST", "/v1/vocabularies/searches")).isEqualTo(AccessRules.READ);
        assertThat(AccessRules.required("POST", "/v1/external-vocabularies/content"))
                .isEqualTo(AccessRules.READ);
        assertThat(AccessRules.required("POST", "/v1/vocabularies")).isEqualTo(AccessRules.WRITE);
        assertThat(AccessRules.required("DELETE", "/v1/external-vocabularies/X"))
                .isEqualTo(AccessRules.WRITE);
    }
}
