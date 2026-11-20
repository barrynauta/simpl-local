package eu.simpl.vocabularymanager.config;

import eu.simpl.vocabularymanager.error.ProblemWriter;
import eu.simpl.vocabularymanager.error.Problems;
import eu.simpl.vocabularymanager.security.AuthenticatedUser;
import eu.simpl.vocabularymanager.security.OperationAuthorizationManager;
import eu.simpl.vocabularymanager.security.RealmKeyProvider;
import eu.simpl.vocabularymanager.security.TokenAuthenticationFilter;
import eu.simpl.vocabularymanager.security.TokenValidator;
import eu.simpl.vocabularymanager.web.MediaTypeFilter;
import eu.simpl.vocabularymanager.web.OutboundHeaders;
import eu.simpl.vocabularymanager.web.RoutingFilter;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.servlet.DispatcherType;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.session.DisableEncodeUrlFilter;

/** Spring Security wiring: firewall, routing (405), token validation, default-deny rules, 401 and 403 bodies. */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    RealmKeyProvider realmKeyProvider(
            AppProperties properties, CircuitBreakerRegistry breakers, OutboundHeaders outboundHeaders, Clock clock) {
        AppProperties.Security settings = properties.security();
        return new RealmKeyProvider(
                settings,
                RealmKeyProvider.httpClient(settings.keyFetchTimeout()),
                breakers.circuitBreaker("realmKeys"),
                outboundHeaders,
                clock);
    }

    @Bean
    TokenValidator tokenValidator(AppProperties properties, RealmKeyProvider keys, Clock clock) {
        return new TokenValidator(properties.security(), keys, clock);
    }

    /**
     * The management port (health probes and Prometheus metrics, D67, D86) is a separate listener that is published
     * nowhere and sits on the internal networks of the deployment (D98); it carries no API operation.
     */
    @Bean
    @Order(0)
    SecurityFilterChain managementSecurity(HttpSecurity http, @Value("${management.server.port:-1}") int managementPort)
            throws Exception {
        http.securityMatcher(request -> managementPort > 0 && request.getLocalPort() == managementPort)
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rules -> rules.requestMatchers(
                                request -> request.getRequestURI().startsWith("/actuator/"))
                        .permitAll()
                        .anyRequest()
                        .denyAll());
        return http.build();
    }

    @Bean
    @Order(1)
    SecurityFilterChain apiSecurity(HttpSecurity http, TokenValidator validator) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .requestCache(cache -> cache.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new RoutingFilter(), DisableEncodeUrlFilter.class)
                .addFilterBefore(new TokenAuthenticationFilter(validator), AnonymousAuthenticationFilter.class)
                .addFilterAfter(new MediaTypeFilter(), AuthorizationFilter.class)
                .authorizeHttpRequests(rules -> rules.dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()
                        .anyRequest()
                        .access(new OperationAuthorizationManager()))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(
                                (request, response, exception) -> ProblemWriter.write(
                                        response, Problems.unauthorized("The access token is missing or not valid.")))
                        .accessDeniedHandler((request, response, exception) -> {
                            LOG.warn(
                                    "Access denied: subject={} path={}",
                                    subjectOf(SecurityContextHolder.getContext().getAuthentication()),
                                    request.getRequestURI());
                            ProblemWriter.write(response, Problems.forbidden());
                        }));
        return http.build();
    }

    @Bean
    HttpFirewall httpFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        // Unknown methods are answered with 405 by the routing filter (D67), not rejected by the firewall.
        firewall.setUnsafeAllowAnyHttpMethod(true);
        return firewall;
    }

    @Bean
    RequestRejectedHandler requestRejectedHandler() {
        return (request, response, exception) -> {
            LOG.warn("Request rejected by the firewall: {}", exception.getMessage());
            ProblemWriter.write(response, Problems.firewallRejected());
        };
    }

    private static String subjectOf(Authentication authentication) {
        return authentication instanceof AuthenticatedUser user ? user.subject() : "anonymous";
    }
}
