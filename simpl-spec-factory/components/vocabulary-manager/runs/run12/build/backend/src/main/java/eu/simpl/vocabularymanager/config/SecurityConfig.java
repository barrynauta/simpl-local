package eu.simpl.vocabularymanager.config;

import eu.simpl.vocabularymanager.api.Routes;
import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.ProblemType;
import eu.simpl.vocabularymanager.error.ProblemWriter;
import eu.simpl.vocabularymanager.security.BearerTokenFilter;
import eu.simpl.vocabularymanager.security.TokenVerifier;
import jakarta.servlet.DispatcherType;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.security.web.firewall.StrictHttpFirewall;

/**
 * Authorisation by default deny (D16, D84): one rule per declared operation from {@link
 * Routes#ALL}; any other request needs a valid token and then gets 404 from the framework (D67).
 * The firewall refuses encoded slashes and control characters with 400 problem+json (D67).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final org.slf4j.Logger SECURITY_LOG = org.slf4j.LoggerFactory.getLogger("security");
    public static final String ADMIN = "GA_VOCABULARY_ADMIN";
    public static final String VIEWER = "GA_VOCABULARY_VIEWER";

    /**
     * The management port (health, metrics) is on an internal network only and not proxied (D67, D98,
     * D120); its requests bypass the API rules.
     */
    @Bean
    @org.springframework.core.annotation.Order(1)
    SecurityFilterChain management(
            HttpSecurity http,
            @org.springframework.beans.factory.annotation.Value("${management.server.port:9090}") int managementPort)
            throws Exception {
        http.securityMatcher(req -> req.getLocalPort() == managementPort)
                .csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @org.springframework.core.annotation.Order(2)
    SecurityFilterChain api(HttpSecurity http, TokenVerifier verifier, ProblemWriter problems) throws Exception {
        http.csrf(c -> c.disable())
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .logout(l -> l.disable())
                .requestCache(r -> r.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new BearerTokenFilter(verifier, problems), AuthorizationFilter.class)
                .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> problems.write(
                                res,
                                new ApiException(
                                        ProblemType.UNAUTHORIZED,
                                        BearerTokenFilter.UNAUTHORIZED_DETAIL,
                                        List.of(),
                                        Map.of("WWW-Authenticate", "Bearer"),
                                        false)))
                        .accessDeniedHandler((req, res, ex) -> {
                            SECURITY_LOG.warn("access denied: {} {}", req.getMethod(), req.getRequestURI());
                            problems.write(
                                    res,
                                    new ApiException(
                                            ProblemType.FORBIDDEN,
                                            "The user does not have the role required for this" + " operation."));
                        }))
                .authorizeHttpRequests(a -> {
                    a.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
                    for (Routes.Route r : Routes.ALL) {
                        HttpMethod m = HttpMethod.valueOf(r.method());
                        switch (r.access()) {
                            case PUBLIC -> a.requestMatchers(m, r.pattern()).permitAll();
                            case READ -> a.requestMatchers(m, r.pattern()).hasAnyRole(ADMIN, VIEWER);
                            case WRITE -> a.requestMatchers(m, r.pattern()).hasRole(ADMIN);
                        }
                    }
                    a.anyRequest().authenticated();
                });
        return http.build();
    }

    @Bean
    WebSecurityCustomizer firewall(ProblemWriter problems) {
        StrictHttpFirewall fw = new StrictHttpFirewall();
        fw.setUnsafeAllowAnyHttpMethod(true);
        RequestRejectedHandler handler = (req, res, ex) -> problems.write(
                res, new ApiException(ProblemType.BAD_REQUEST, "The request was rejected by the server."));
        return web -> web.httpFirewall(fw).requestRejectedHandler(handler);
    }
}
