package eu.simpl.vocabularymanager.security;

import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.ProblemType;
import eu.simpl.vocabularymanager.error.ProblemWriter;
import eu.simpl.vocabularymanager.web.ApiRoutes;
import jakarta.servlet.DispatcherType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.firewall.RequestRejectedHandler;

/**
 * Access rules (D16, D17, D84): one explicit rule per operation from the route table, everything
 * else needs an authenticated user and ends in 404 (D67). Stateless bearer tokens, no sessions,
 * no CSRF (no cookies), no login pages (OWT-A02).
 */
@Configuration
public class SecurityConfig {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityConfig.class);

    /**
     * The management port (D67, D98, D120) serves only health and Prometheus metrics, reachable inside
     * the deployment only; it is not published on the host and the proxy never routes to it.
     */
    @Bean
    @Order(1)
    SecurityFilterChain management(HttpSecurity http, @Value("${management.server.port:0}") int managementPort)
            throws Exception {
        http.securityMatcher(request -> managementPort > 0 && request.getLocalPort() == managementPort)
                .csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers(
                                HttpMethod.GET, "/actuator/health", "/actuator/health/**", "/actuator/prometheus")
                        .permitAll()
                        .anyRequest()
                        .denyAll());
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain api(HttpSecurity http, TokenVerifier verifier, ProblemWriter problems) throws Exception {
        http.csrf(c -> c.disable())
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .logout(l -> l.disable())
                .requestCache(r -> r.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new JwtAuthenticationFilter(verifier, problems), AnonymousAuthenticationFilter.class)
                .exceptionHandling(e -> e.authenticationEntryPoint((request, response, ex) -> problems.write(
                                response,
                                new ApiException(
                                        ProblemType.UNAUTHORIZED, "The bearer token is missing or not valid.")))
                        .accessDeniedHandler((request, response, ex) -> {
                            LOG.warn("Access denied to {}", MDC.get("operationId"));
                            problems.write(
                                    response,
                                    new ApiException(
                                            ProblemType.FORBIDDEN,
                                            "The user does not have the role needed for this operation."));
                        }))
                .authorizeHttpRequests(auth -> {
                    auth.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
                    for (ApiRoutes.Route route : ApiRoutes.all()) {
                        var matcher = auth.requestMatchers(
                                HttpMethod.valueOf(route.method()), ApiRoutes.BASE + route.template());
                        if (route.access() == ApiRoutes.Access.PUBLIC) {
                            matcher.permitAll();
                        } else {
                            matcher.hasAnyRole(route.roles().toArray(String[]::new));
                        }
                    }
                    auth.anyRequest().authenticated();
                });
        return http.build();
    }

    /** Requests the firewall refuses (encoded slash, control characters) get 400 (D67). */
    @Bean
    RequestRejectedHandler requestRejectedHandler(ProblemWriter problems) {
        return (request, response, ex) -> {
            LOG.warn("Request rejected by the firewall: {}", ex.getMessage());
            problems.write(response, new ApiException(ProblemType.BAD_REQUEST, "The request URL is not allowed."));
        };
    }

    @Bean
    WebSecurityCustomizer firewallHandler(RequestRejectedHandler handler) {
        return web -> web.requestRejectedHandler(handler);
    }
}
