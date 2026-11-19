package eu.simpl.vocabulary.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import eu.simpl.vocabulary.api.RouteTable;
import eu.simpl.vocabulary.api.filter.EntryFilter;
import eu.simpl.vocabulary.api.problem.ProblemWriter;
import eu.simpl.vocabulary.api.problem.Problems;
import eu.simpl.vocabulary.config.AppProperties;
import jakarta.servlet.DispatcherType;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.security.web.firewall.StrictHttpFirewall;

/**
 * Token validation and access rules (D16, D17, D40, D44, E1, E5). Spring Security and Nimbus JOSE + JWT do
 * the cryptography (REUSE-01); the role per operation comes from the {@link RouteTable}.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public RealmKeySource realmKeySource(AppProperties properties) {
        AppProperties.Security security = properties.security();
        RealmKeySource source = new RealmKeySource(
                new HttpKeyFetcher(URI.create(security.jwksUri()), security.keyFetchTimeout()),
                Duration.ofSeconds(10),
                Clock.systemUTC());
        Thread.ofVirtual().name("realm-key-loader").start(source::tryRefresh);
        return source;
    }

    @Bean
    public JwtDecoder jwtDecoder(RealmKeySource keySource, AppProperties properties) {
        DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(new JWSVerificationKeySelector<>(
                Set.of(
                        JWSAlgorithm.RS256,
                        JWSAlgorithm.RS384,
                        JWSAlgorithm.RS512,
                        JWSAlgorithm.PS256,
                        JWSAlgorithm.ES256),
                keySource));
        processor.setJWTClaimsSetVerifier((claims, context) -> {
            // Claims are checked by the Spring validators below.
        });
        NimbusJwtDecoder decoder = new NimbusJwtDecoder(processor);
        decoder.setJwtValidator(tokenValidator(properties.security()));
        return decoder;
    }

    /**
     * Issuer, expiry and audience checks (E5, OWT-A07).
     *
     * @param security token settings
     * @return the validator
     */
    static OAuth2TokenValidator<Jwt> tokenValidator(AppProperties.Security security) {
        String audience = security.audience();
        OAuth2TokenValidator<Jwt> audienceValidator =
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD, aud -> aud != null && aud.contains(audience));
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(security.issuerUri()), audienceValidator);
    }

    @Bean
    public HttpFirewall httpFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setUnsafeAllowAnyHttpMethod(true);
        return firewall;
    }

    @Bean
    public RequestRejectedHandler requestRejectedHandler(ProblemWriter problemWriter) {
        return (request, response, exception) -> problemWriter.write(response, Problems.rejectedRequest());
    }

    @Bean
    public AuthenticationEntryPoint problemEntryPoint(ProblemWriter problemWriter) {
        return (request, response, exception) -> {
            if (EntryFilter.causedBy(exception, IdpUnavailableException.class)) {
                LOG.warn("Token could not be checked: identity provider unavailable");
                problemWriter.write(response, Problems.serviceUnavailable(Problems.IDP_UNAVAILABLE));
                return;
            }
            LOG.warn(
                    "Authentication failed: {} {} ({})",
                    request.getMethod(),
                    request.getRequestURI(),
                    exception.getClass().getSimpleName());
            response.setHeader("WWW-Authenticate", "Bearer");
            problemWriter.write(response, Problems.unauthorized());
        };
    }

    @Bean
    @Order(1)
    public SecurityFilterChain managementChain(
            HttpSecurity http, @Value("${management.server.port:8081}") int managementPort) throws Exception {
        http.securityMatcher(request -> request.getLocalPort() == managementPort)
                .authorizeHttpRequests(rules -> rules.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiChain(
            HttpSecurity http, JwtDecoder jwtDecoder, AuthenticationEntryPoint problemEntryPoint) throws Exception {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(Roles::authorities);
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .authorizeHttpRequests(rules -> rules.dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, RouteTable.API_PREFIX + "/health")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(oauth -> oauth.bearerTokenResolver(bearerTokenResolver())
                        .authenticationEntryPoint(problemEntryPoint)
                        .jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(converter)))
                .exceptionHandling(e -> e.authenticationEntryPoint(problemEntryPoint))
                .headers(headers -> headers.contentSecurityPolicy(
                                csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                        .frameOptions(frame -> frame.deny()));
        return http.build();
    }

    private static BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();
        return request -> {
            if ((RouteTable.API_PREFIX + "/health").equals(request.getRequestURI())) {
                return null;
            }
            return delegate.resolve(request);
        };
    }
}
