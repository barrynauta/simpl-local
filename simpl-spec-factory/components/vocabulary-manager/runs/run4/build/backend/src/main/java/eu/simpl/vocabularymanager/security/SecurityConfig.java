package eu.simpl.vocabularymanager.security;

import eu.simpl.vocabularymanager.error.AccessErrorCode;
import eu.simpl.vocabularymanager.error.ProblemWriter;
import jakarta.servlet.DispatcherType;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

/** Access rules of decision D16, with the 401 and 403 problem bodies of decision D17. */
@Configuration
public class SecurityConfig {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityConfig.class);
    private static final String ADMIN = Roles.ADMIN;
    private static final String VIEWER = Roles.VIEWER;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, TokenValidator tokenValidator, ProblemWriter problemWriter) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h ->
                        h.contentSecurityPolicy(c -> c.policyDirectives("default-src 'none'; frame-ancestors 'none'")))
                .addFilterBefore(
                        new BearerTokenFilter(tokenValidator, problemWriter), AnonymousAuthenticationFilter.class)
                .exceptionHandling(e -> e.authenticationEntryPoint((request, response, ex) -> problemWriter.write(
                                response, AccessErrorCode.UNAUTHORIZED, "A valid bearer token is required.", List.of()))
                        .accessDeniedHandler((request, response, ex) -> {
                            LOG.warn(
                                    "Access denied: method={} path={} subject={}",
                                    request.getMethod(),
                                    request.getServletPath(),
                                    subjectOf(SecurityContextHolder.getContext().getAuthentication()));
                            problemWriter.write(
                                    response,
                                    AccessErrorCode.FORBIDDEN,
                                    "The caller does not have the role required for this operation.",
                                    List.of());
                        }))
                .authorizeHttpRequests(a -> a.dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/health")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/**")
                        .hasAnyRole(ADMIN, VIEWER)
                        .requestMatchers(
                                HttpMethod.POST,
                                "/vocabularies/searches",
                                "/external-vocabulary/searches",
                                "/external-vocabularies/content")
                        .hasAnyRole(ADMIN, VIEWER)
                        .requestMatchers(HttpMethod.POST, "/vocabularies", "/vocabularies/*/versions")
                        .hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/vocabularies/*", "/vocabularies/*/versions/*")
                        .hasRole(ADMIN)
                        .requestMatchers(HttpMethod.POST, "/external-vocabularies")
                        .hasRole(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/external-vocabularies/*")
                        .hasRole(ADMIN)
                        .anyRequest()
                        .hasAnyRole(ADMIN, VIEWER));
        return http.build();
    }

    private static String subjectOf(Authentication authentication) {
        return authentication instanceof AuthenticatedUser user ? user.subject() : "anonymous";
    }
}
