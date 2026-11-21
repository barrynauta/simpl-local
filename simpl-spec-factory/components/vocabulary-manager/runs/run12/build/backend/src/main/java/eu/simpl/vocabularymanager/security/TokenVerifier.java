package eu.simpl.vocabularymanager.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import eu.simpl.vocabularymanager.config.AppProperties;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Bearer token checks of decisions D17, D44, D88, D108, D129 and D132: signature with the cached
 * realm keys (Nimbus JWKSet and JWSVerifier), claims with Nimbus DefaultJWTClaimsVerifier (issuer,
 * audience, expiry, not-before). Own code only for the timing of the key fetch: at most one fetch
 * per 10 s, 2 s timeout, no retry inside a request, fail closed with 503 when no keys can be
 * fetched.
 */
@Component
public class TokenVerifier {

    private static final Logger LOG = LoggerFactory.getLogger(TokenVerifier.class);
    private static final Set<JWSAlgorithm> ALLOWED = Set.of(
            JWSAlgorithm.RS256,
            JWSAlgorithm.RS384,
            JWSAlgorithm.RS512,
            JWSAlgorithm.PS256,
            JWSAlgorithm.ES256,
            JWSAlgorithm.ES384);

    /** Outcome of a failed check: 401 or 503. */
    public static final class TokenException extends Exception {
        private static final long serialVersionUID = 1L;
        private final boolean unavailable;

        public TokenException(String message, boolean unavailable) {
            super(message);
            this.unavailable = unavailable;
        }

        public boolean unavailable() {
            return unavailable;
        }
    }

    /**
     * The checked identity.
     *
     * @param subject token subject (sub), the user id in logs (D80)
     * @param creator email, else preferred_username (D27)
     * @param roles realm roles
     */
    public record Principal(String subject, String creator, Set<String> roles) {}

    private final AppProperties.Auth cfg;
    private final HttpClient http;
    private final Clock clock;
    private final CircuitBreaker breaker;
    private final DefaultJWSVerifierFactory verifierFactory = new DefaultJWSVerifierFactory();
    private final DefaultJWTClaimsVerifier<com.nimbusds.jose.proc.SecurityContext> claimsVerifier;

    private volatile JWKSet keys;
    private volatile Instant lastFetchAttempt;
    private volatile boolean lastFetchFailed;

    @org.springframework.beans.factory.annotation.Autowired
    public TokenVerifier(AppProperties props, MeterRegistry meters) {
        this(props, meters, Clock.systemUTC());
    }

    TokenVerifier(AppProperties props, MeterRegistry meters, Clock clock) {
        this.cfg = props.auth();
        this.clock = clock;
        this.http = HttpClient.newBuilder()
                .connectTimeout(cfg.fetchTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        CircuitBreakerConfig cbc = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(20)
                .minimumNumberOfCalls(10)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(cbc);
        this.breaker = registry.circuitBreaker("identityProviderKeys");
        TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry).bindTo(meters);
        JWTClaimsSet exact = new JWTClaimsSet.Builder().issuer(cfg.issuer()).build();
        this.claimsVerifier = new DefaultJWTClaimsVerifier<>(cfg.audience(), exact, Set.of("sub", "exp", "iss"));
    }

    /** Verifies the token and returns the principal. */
    public Principal verify(String token) throws TokenException {
        SignedJWT jwt;
        try {
            jwt = SignedJWT.parse(token);
        } catch (ParseException | RuntimeException e) {
            throw new TokenException("malformed token", false);
        }
        JWSAlgorithm alg = jwt.getHeader().getAlgorithm();
        if (!ALLOWED.contains(alg)) {
            throw new TokenException("unsupported algorithm", false);
        }
        String kid = jwt.getHeader().getKeyID();
        JWK jwk = keyFor(kid);
        try {
            Key key = jwk.getKeyType().getValue().equals("EC")
                    ? jwk.toECKey().toECPublicKey()
                    : jwk.toRSAKey().toRSAPublicKey();
            JWSVerifier verifier = verifierFactory.createJWSVerifier(jwt.getHeader(), key);
            if (!jwt.verify(verifier)) {
                throw new TokenException("bad signature", false);
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            claimsVerifier.verify(claims, null);
            return new Principal(claims.getSubject(), creator(claims), roles(claims));
        } catch (JOSEException | ParseException | BadJWTException | RuntimeException e) {
            throw new TokenException("invalid token: " + e.getClass().getSimpleName(), false);
        }
    }

    private JWK keyFor(String kid) throws TokenException {
        JWKSet current = keys;
        if (current != null && kid != null && current.getKeyByKeyId(kid) != null) {
            return current.getKeyByKeyId(kid);
        }
        return keyForSlow(kid);
    }

    /** Unknown key id: one thread at a time, so a fetch in progress is waited for (D88, D129). */
    private synchronized JWK keyForSlow(String kid) throws TokenException {
        JWKSet current = keys;
        if (current != null && kid != null && current.getKeyByKeyId(kid) != null) {
            return current.getKeyByKeyId(kid);
        }
        Instant now = clock.instant();
        Instant last = lastFetchAttempt;
        boolean mayFetch = last == null || !now.isBefore(last.plus(cfg.refetchInterval()));
        if (mayFetch) {
            fetch(now);
            current = keys;
            if (lastFetchFailed) {
                throw new TokenException("realm keys could not be fetched", true);
            }
            if (kid != null && current != null && current.getKeyByKeyId(kid) != null) {
                return current.getKeyByKeyId(kid);
            }
            throw new TokenException("unknown key id", false);
        }
        if (lastFetchFailed) {
            throw new TokenException("realm keys not available", true);
        }
        throw new TokenException("unknown key id", false);
    }

    private void fetch(Instant now) {
        lastFetchAttempt = now;
        try {
            String body = breaker.executeCallable(this::download);
            keys = JWKSet.parse(body);
            lastFetchFailed = false;
            LOG.info("realm keys loaded: {} keys", keys.getKeys().size());
        } catch (CallNotPermittedException e) {
            lastFetchFailed = true;
            LOG.warn("realm key fetch skipped: circuit breaker open");
        } catch (Exception e) {
            lastFetchFailed = true;
            LOG.warn("realm key fetch failed: {}", e.getClass().getSimpleName());
        }
    }

    private String download() throws IOException, InterruptedException {
        HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(cfg.jwksUri()))
                .timeout(cfg.fetchTimeout())
                .GET();
        String correlationId = MDC.get("correlationId");
        if (correlationId != null) {
            rb.header("X-Correlation-Id", correlationId);
        }
        String traceId = MDC.get("traceId");
        String spanId = MDC.get("spanId");
        if (traceId != null && spanId != null && traceId.length() == 32 && spanId.length() == 16) {
            rb.header("traceparent", "00-" + traceId + "-" + spanId + "-01");
        }
        HttpResponse<String> resp = http.send(rb.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200) {
            throw new IOException("status " + resp.statusCode());
        }
        return resp.body();
    }

    /** Key state for GET /health (D108, D134): never fetches. */
    public boolean degraded() {
        return keys == null && lastFetchFailed;
    }

    private static String creator(JWTClaimsSet c) throws ParseException {
        String email = c.getStringClaim("email");
        if (email != null && !email.isBlank()) {
            return email;
        }
        String user = c.getStringClaim("preferred_username");
        return user != null ? user : c.getSubject();
    }

    @SuppressWarnings("unchecked")
    private static Set<String> roles(JWTClaimsSet c) {
        Set<String> out = new HashSet<>();
        Object ra = c.getClaim("realm_access");
        if (ra instanceof Map<?, ?> m && m.get("roles") instanceof List<?> l) {
            for (Object o : new ArrayList<>((List<Object>) l)) {
                if (o instanceof String s) {
                    out.add(s);
                }
            }
        }
        return out;
    }
}
