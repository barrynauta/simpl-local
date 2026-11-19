package eu.simpl.vocabularymanager.security;

import com.nimbusds.jose.KeySourceException;
import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.problem.Problem;
import eu.simpl.vocabularymanager.problem.ProblemWriter;
import eu.simpl.vocabularymanager.problem.Problems;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.firewall.RequestRejectedHandler;

/**
 * Access rules of decisions D16 and D17: the service validates the bearer token (signature, issuer, audience,
 * expiry, OWT-A07) and the realm roles itself; 401 and 403 come before any body validation.
 */
@Configuration
public class SecurityConfig {

    /** Realm role for read operations. */
    public static final String VIEWER = "GA_VOCABULARY_VIEWER";

    /** Realm role for read and write operations. */
    public static final String ADMIN = "GA_VOCABULARY_ADMIN";

    private static final Logger SECURITY_LOG = LoggerFactory.getLogger("security");

    /**
     * The realm key source (decision D44).
     *
     * @param properties the configuration
     * @param clock the clock
     * @return the key source
     */
    @Bean
    public RealmKeySource realmKeySource(AppProperties properties, Clock clock) {
        return new RealmKeySource(properties.identity(), clock);
    }

    /**
     * The JWT decoder: Nimbus with the realm key source, RS256, issuer and audience checks.
     *
     * @param keySource the key source
     * @param properties the configuration
     * @return the decoder
     */
    @Bean
    public JwtDecoder jwtDecoder(RealmKeySource keySource, AppProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSource(keySource).build();
        String audience = properties.identity().audience();
        OAuth2TokenValidator<Jwt> audienceValidator =
                new JwtClaimValidator<Collection<String>>("aud", aud -> aud != null && aud.contains(audience));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.identity().issuer()), audienceValidator));
        return decoder;
    }

    /**
     * Reads the realm roles from realm_access.roles (decision E1).
     *
     * @return the converter
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(SecurityConfig::realmRoles);
        return converter;
    }

    static Collection<GrantedAuthority> realmRoles(Jwt jwt) {
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (!(realmAccess instanceof Map<?, ?> map) || !(map.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream()
                .filter(String.class::isInstance)
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority((String) role))
                .toList();
    }

    /**
     * Health and metrics on the management port (not published outside the container network, NFR-07, NFR-08):
     * open to the platform probes and the metrics scraper, read-only, no API data.
     *
     * @param http the security builder
     * @param managementPort the management port
     * @return the chain
     * @throws Exception when the chain cannot be built
     */
    @Bean
    @org.springframework.core.annotation.Order(0)
    public SecurityFilterChain managementFilterChain(
            HttpSecurity http,
            @org.springframework.beans.factory.annotation.Value("${management.server.port:8081}") int managementPort)
            throws Exception {
        http.securityMatcher(request -> request.getLocalPort() == managementPort)
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers(
                                HttpMethod.GET, "/actuator/health/**", "/actuator/health", "/actuator/prometheus")
                        .permitAll()
                        .anyRequest()
                        .denyAll());
        return http.build();
    }

    /**
     * The filter chain with the role rules per operation (decision D16).
     *
     * @param http the security builder
     * @param problems the problem writer
     * @param converter the role converter
     * @return the chain
     * @throws Exception when the chain cannot be built
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, ProblemWriter problems, JwtAuthenticationConverter converter) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .requestCache(cache -> cache.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()
                        .requestMatchers("/v1/health")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/v1/vocabularies/searches",
                                "/v1/external-vocabulary/searches",
                                "/v1/external-vocabularies/content")
                        .hasAnyAuthority(VIEWER, ADMIN)
                        .requestMatchers(HttpMethod.GET, "/v1/**")
                        .hasAnyAuthority(VIEWER, ADMIN)
                        .requestMatchers(HttpMethod.POST, "/v1/**")
                        .hasAuthority(ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/v1/**")
                        .hasAuthority(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/v1/**")
                        .hasAuthority(ADMIN)
                        .anyRequest()
                        .hasAnyAuthority(VIEWER, ADMIN))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                                (request, response, e) -> unauthorized(problems, request, response, e))
                        .accessDeniedHandler((request, response, e) -> forbidden(problems, request, response)))
                .oauth2ResourceServer(oauth -> oauth.authenticationEntryPoint(
                                (request, response, e) -> unauthorized(problems, request, response, e))
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                        .withObjectPostProcessor(new ObjectPostProcessor<BearerTokenAuthenticationFilter>() {
                            @Override
                            public <O extends BearerTokenAuthenticationFilter> O postProcess(O filter) {
                                filter.setAuthenticationFailureHandler(
                                        (request, response, e) -> tokenFailure(problems, request, response, e));
                                return filter;
                            }
                        }));
        return http.build();
    }

    /** Methods the firewall accepts; any other method (for example QUERY) is not declared on any path. */
    private static final java.util.Set<String> FIREWALL_METHODS =
            java.util.Set.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

    /**
     * Writes the problem for requests the firewall rejects: 405 (or 404 outside the API) for a method no path
     * declares, 400 for anything else (encoded slashes, control characters).
     *
     * @param problems the problem writer
     * @return the handler
     */
    @Bean
    public RequestRejectedHandler requestRejectedHandler(ProblemWriter problems) {
        return (request, response, e) -> {
            SECURITY_LOG.warn("Request rejected by the firewall: {} {}", request.getMethod(), e.getMessage());
            if (!FIREWALL_METHODS.contains(request.getMethod())) {
                java.util.Optional<eu.simpl.vocabularymanager.web.RouteTable.Route> route =
                        eu.simpl.vocabularymanager.web.RouteTable.find(path(request));
                if (route.isPresent()) {
                    eu.simpl.vocabularymanager.problem.ProblemException notAllowed =
                            Problems.methodNotAllowed(route.get().allow());
                    problems.write(response, notAllowed.problem(), notAllowed.headers());
                } else {
                    problems.write(response, Problems.unknownPath(), Map.of());
                }
                return;
            }
            Problem problem = Problems.badRequest(List.of()).problem();
            problems.write(response, problem, Map.of());
        };
    }

    private static void tokenFailure(
            ProblemWriter problems, HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
            throws IOException {
        if (e instanceof AuthenticationServiceException && causedByKeySource(e)) {
            SECURITY_LOG.warn(
                    "Token rejected, identity provider unavailable: {} {}", request.getMethod(), path(request));
            problems.write(
                    response, Problems.serviceUnavailableProblem(Problems.IDP_DOWN_DETAIL), Map.of("Retry-After", "5"));
            return;
        }
        unauthorized(problems, request, response, e);
    }

    private static boolean causedByKeySource(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof KeySourceException) {
                return true;
            }
        }
        return false;
    }

    private static void unauthorized(
            ProblemWriter problems, HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
            throws IOException {
        SECURITY_LOG.warn(
                "Authentication failed: {} {} ({})",
                request.getMethod(),
                path(request),
                e.getClass().getSimpleName());
        problems.write(response, Problems.unauthorized(), Map.of("WWW-Authenticate", "Bearer"));
    }

    private static void forbidden(ProblemWriter problems, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String user = request.getUserPrincipal() == null
                ? "unknown"
                : request.getUserPrincipal().getName();
        SECURITY_LOG.warn("Access denied: {} {} for subject {}", request.getMethod(), path(request), user);
        problems.write(response, Problems.forbidden(), Map.of());
    }

    private static String path(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.length() > 200 ? uri.substring(0, 200) : uri.replaceAll("[\\r\\n]", "_");
    }
}
