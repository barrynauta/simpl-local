package eu.simpl.vocabulary.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import eu.simpl.vocabulary.support.TestTokens;
import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

/** Realm key cache (D44) and role mapping (D16, D27). */
class SecurityUnitTest {

    static JWKSelector selector(String kid) {
        return new JWKSelector(new JWKMatcher.Builder().keyID(kid).build());
    }

    @Test
    void cachedKeysKeepWorkingWhileIdentityProviderIsDown() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        boolean[] down = {false};
        RealmKeySource source = new RealmKeySource(
                () -> {
                    calls.incrementAndGet();
                    if (down[0]) {
                        throw new IOException("down");
                    }
                    return TestTokens.publicKeys();
                },
                Duration.ofSeconds(10),
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
        assertThat(source.hasKeys()).isFalse();
        assertThat(source.get(selector("test-key"), null)).hasSize(1);
        assertThat(source.hasKeys()).isTrue();
        down[0] = true;
        assertThat(source.get(selector("test-key"), null)).hasSize(1);
        assertThat(source.get(selector("other"), null)).isEmpty();
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void unknownKeyWithIdentityProviderDownFailsClosed() {
        RealmKeySource source = new RealmKeySource(
                () -> {
                    throw new IOException("down");
                },
                Duration.ZERO,
                Clock.systemUTC());
        assertThatThrownBy(() -> source.get(selector("x"), null)).isInstanceOf(IdpUnavailableException.class);
        source.tryRefresh();
        assertThat(source.hasKeys()).isFalse();
    }

    @Test
    void httpKeyFetcherFailsOnUnreachableEndpoint() {
        HttpKeyFetcher fetcher = new HttpKeyFetcher(URI.create("http://127.0.0.1:1/certs"), Duration.ofMillis(500));
        assertThatThrownBy(fetcher::fetch).isInstanceOf(IOException.class);
    }

    @Test
    void rolesComeFromRealmAccess() {
        Jwt jwt = Jwt.withTokenValue("t")
                .header("alg", "none")
                .claim("realm_access", Map.of("roles", List.of("GA_VOCABULARY_ADMIN", 5)))
                .build();
        assertThat(Roles.authorities(jwt)).extracting(a -> a.getAuthority()).containsExactly("GA_VOCABULARY_ADMIN");
        assertThat(Roles.authorities(Jwt.withTokenValue("t")
                        .header("alg", "none")
                        .claim("x", 1)
                        .build()))
                .isEmpty();
        assertThat(Roles.authorities(Jwt.withTokenValue("t")
                        .header("alg", "none")
                        .claim("realm_access", Map.of("roles", "x"))
                        .build()))
                .isEmpty();
        assertThat(Roles.creator()).isEqualTo("unknown");
        assertThat(Roles.userId()).isEqualTo("anonymous");
    }
}
