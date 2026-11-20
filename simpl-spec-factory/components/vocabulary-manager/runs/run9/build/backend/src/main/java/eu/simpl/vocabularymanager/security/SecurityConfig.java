package eu.simpl.vocabularymanager.security;

import com.nimbusds.jose.KeySourceException;
import eu.simpl.vocabularymanager.api.ProblemWriter;
import eu.simpl.vocabularymanager.api.Problems;
import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.domain.ServiceBusyException;
import eu.simpl.vocabularymanager.store.OutboundHeaders;
import eu.simpl.vocabularymanager.web.Route;
import eu.simpl.vocabularymanager.web.RouteTable;
import jakarta.servlet.DispatcherType;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.firewall.RequestRejectedHandler;

/**
 * Token and role checks in the service (D17, D40): JWT from the Keycloak realm (E1, E5) validated with Spring
 * Security's resource server, realm roles per operation from {@link RouteTable} (D16, D84).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityConfig.class);

    /**
     * @param properties component configuration
     * @param clock time source
     * @param outboundHeaders trace headers
     * @return the realm key source
     */
    @Bean
    public RealmKeySource realmKeySource(
            VocabularyManagerProperties properties, Clock clock, OutboundHeaders outboundHeaders) {
        return new RealmKeySource(properties.identity(), clock, outboundHeaders);
    }

    /**
     * @param keySource realm keys
     * @param properties component configuration
     * @return the JWT decoder with issuer, expiry and audience checks
     */
    @Bean
    public JwtDecoder jwtDecoder(RealmKeySource keySource, VocabularyManagerProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSource(keySource).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.identity().issuerUri()),
                new AudienceValidator(properties.identity().audience())));
        return decoder;
    }

    /** Firewall rejections (encoded slash, control characters) as problem+json 400 (D67). */
    @Bean
    public WebSecurityCustomizer firewallProblems() {
        RequestRejectedHandler handler = (request, response, ex) -> {
            LOG.warn("Request rejected by the firewall: {}", ex.getMessage());
            ProblemWriter.write(response, Problems.rejectedRequest());
        };
        return web -> web.requestRejectedHandler(handler);
    }

    /**
     * The management port (D67, D98) serves health and metrics without a token; it is not exposed outside the stack.
     *
     * @param http security builder
     * @param managementPort management port
     * @return the chain of the management port
     * @throws Exception when the chain cannot be built
     */
    @Bean
    @Order(1)
    public SecurityFilterChain managementChain(
            HttpSecurity http, @Value("${management.server.port}") int managementPort) throws Exception {
        http.securityMatcher(request -> request.getLocalPort() == managementPort)
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(a -> a.anyRequest().permitAll());
        return http.build();
    }

    /**
     * The API chain.
     *
     * @param http security builder
     * @param routes route table with the role rules
     * @param decoder JWT decoder
     * @return the API chain
     * @throws Exception when the chain cannot be built
     */
    @Bean
    @Order(2)
    public SecurityFilterChain apiChain(HttpSecurity http, RouteTable routes, JwtDecoder decoder) throws Exception {
        AuthenticationEntryPoint entryPoint = SecurityConfig::authenticationFailed;
        AccessDeniedHandler deniedHandler = (request, response, ex) -> {
            LOG.warn("Access denied for subject {}", CurrentUser.subject());
            ProblemWriter.write(response, Problems.forbidden());
        };
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .anonymous(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> {
                    authorize.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
                    for (Route route : routes.routes()) {
                        HttpMethod method = HttpMethod.valueOf(route.method());
                        if (route.roles().isEmpty()) {
                            authorize.requestMatchers(method, route.pattern()).permitAll();
                        } else {
                            authorize
                                    .requestMatchers(method, route.pattern())
                                    .hasAnyAuthority(route.roles().toArray(String[]::new));
                        }
                    }
                    authorize.anyRequest().authenticated();
                })
                .oauth2ResourceServer(o -> o.jwt(j -> j.authenticationManager(authenticationManager(decoder)))
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler))
                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler));
        return http.build();
    }

    private static void authenticationFailed(
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            AuthenticationException ex)
            throws java.io.IOException {
        if (ex instanceof IdentityProviderUnavailableException) {
            LOG.error("Token could not be checked: realm keys unavailable", ex);
            ProblemWriter.write(response, Problems.serviceUnavailable(ServiceBusyException.IDENTITY_DETAIL));
            return;
        }
        LOG.warn("Authentication failed: {}", ex.getClass().getSimpleName());
        response.setHeader("WWW-Authenticate", "Bearer");
        ProblemWriter.write(response, Problems.unauthorized());
    }

    /** Wraps the JWT provider so a key fetch failure becomes 503 instead of 401 or 500 (D44). */
    static AuthenticationManager authenticationManager(JwtDecoder decoder) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new RealmRoles());
        converter.setPrincipalClaimName("sub");
        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(decoder);
        provider.setJwtAuthenticationConverter(converter);
        return authentication -> {
            try {
                return provider.authenticate(authentication);
            } catch (AuthenticationServiceException ex) {
                if (hasKeySourceCause(ex)) {
                    throw new IdentityProviderUnavailableException("Realm keys unavailable", ex);
                }
                throw ex;
            }
        };
    }

    private static boolean hasKeySourceCause(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof KeySourceException) {
                return true;
            }
            if (cause.getCause() == cause) {
                return false;
            }
        }
        return false;
    }
}
