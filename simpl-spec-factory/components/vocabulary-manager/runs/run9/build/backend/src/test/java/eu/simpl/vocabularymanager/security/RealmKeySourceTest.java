package eu.simpl.vocabularymanager.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.support.TestIdentityProvider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;

/** Realm key fetch rules of D44 and D88, and the 401 versus 503 split. */
class RealmKeySourceTest {

    private final AtomicLong now =
            new AtomicLong(Instant.parse("2026-11-19T10:00:00Z").toEpochMilli());
    private final Clock clock = new Clock() {
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
            return Instant.ofEpochMilli(now.get());
        }
    };

    private RealmKeySource source(String uri) {
        return new RealmKeySource(
                new VocabularyManagerProperties.Identity(
                        TestIdentityProvider.ISSUER,
                        uri,
                        "vocabulary-manager-be",
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(10)),
                clock,
                Map::of);
    }

    private static JWKSelector kid(String id) {
        return new JWKSelector(new JWKMatcher.Builder().keyID(id).build());
    }

    @Test
    void unknownKeyIdInsideTheWindowGivesNoKey() throws Exception {
        TestIdentityProvider idp = TestIdentityProvider.get();
        idp.setAvailable(true);
        RealmKeySource keys = source(idp.jwkSetUri());
        assertThat(keys.hasKeys()).isFalse();
        assertThat(keys.get(kid("kid-1"), null)).hasSize(1);
        assertThat(keys.hasKeys()).isTrue();
        assertThat(keys.get(kid("other"), null)).isEmpty();
        now.addAndGet(11_000);
        assertThat(keys.get(kid("other"), null)).isEmpty();
        idp.setAvailable(false);
        assertThat(keys.get(kid("kid-1"), null)).hasSize(1);
        now.addAndGet(11_000);
        assertThatThrownBy(() -> keys.get(kid("other"), null)).isInstanceOf(KeySourceException.class);
        assertThatThrownBy(() -> keys.get(kid("other"), null)).isInstanceOf(KeySourceException.class);
        idp.setAvailable(true);
    }

    @Test
    void unreachableIdentityProviderFailsClosedWith503() {
        RealmKeySource keys = source("http://127.0.0.1:1/certs");
        assertThat(keys.refresh()).isFalse();
        now.addAndGet(11_000);
        JwtDecoder decoder = new SecurityConfig().jwtDecoder(keys, properties());
        AuthenticationManager manager = SecurityConfig.authenticationManager(decoder);
        String token = TestIdentityProvider.get().token("ga-admin", "GA_VOCABULARY_ADMIN");
        assertThatThrownBy(() -> manager.authenticate(new BearerTokenAuthenticationToken(token)))
                .isInstanceOf(IdentityProviderUnavailableException.class);
        assertThatThrownBy(() -> manager.authenticate(new BearerTokenAuthenticationToken("garbage")))
                .isInstanceOf(AuthenticationException.class)
                .isNotInstanceOf(IdentityProviderUnavailableException.class);
    }

    private static VocabularyManagerProperties properties() {
        return new VocabularyManagerProperties(
                "https://a.example",
                null,
                new VocabularyManagerProperties.Identity(
                        TestIdentityProvider.ISSUER,
                        "x",
                        "vocabulary-manager-be",
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(10)),
                null);
    }
}
