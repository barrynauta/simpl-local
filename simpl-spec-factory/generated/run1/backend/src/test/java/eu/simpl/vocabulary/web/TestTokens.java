package eu.simpl.vocabulary.web;

import java.time.Instant;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/** A fake decoder: the token value names the test user. Everything after it runs the real security chain. */
@TestConfiguration
public class TestTokens {

    public static final String ADMIN = "Bearer admin";
    public static final String VIEWER = "Bearer viewer";
    public static final String NOROLE = "Bearer norole";
    public static final String BAD = "Bearer bad";

    @Bean
    @Primary
    public JwtDecoder testJwtDecoder() {
        return token -> {
            List<String> roles = switch (token) {
                case "admin" -> List.of("GA_VOCABULARY_ADMIN", "offline_access");
                case "viewer" -> List.of("GA_VOCABULARY_VIEWER");
                case "norole" -> List.of("offline_access");
                default -> throw new BadJwtException("invalid token");
            };
            return Jwt.withTokenValue(token).header("alg", "RS256").subject(token)
                    .claim("preferred_username", "ga-" + token).claim("roles", roles)
                    .issuer("http://localhost:8183/realms/simpl")
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
        };
    }
}
