package eu.simpl.vocabularymanager.security;

import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.web.problem.ProblemWriter;
import eu.simpl.vocabularymanager.web.problem.Problems;
import eu.simpl.vocabularymanager.web.routing.ApiOperation;
import eu.simpl.vocabularymanager.web.routing.OperationCatalogue;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import java.net.http.HttpClient;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.firewall.RequestRejectedHandler;

/**
 * Authorisation by default deny (D16, D17, D84): every request is decided by the role rule of the contract operation
 * it addresses. A request that addresses no operation is denied: 401 without a token, 404 with one.
 */
@Configuration
public class SecurityConfig {

    private static final Logger SECURITY_LOG = LoggerFactory.getLogger("security");

    /**
     * @param properties component settings
     * @param httpClient shared HTTP client
     * @param clock clock
     * @return the realm keys, loaded at start
     */
    @Bean
    public RealmKeySource realmKeySource(VocabularyManagerProperties properties, HttpClient httpClient, Clock clock) {
        RealmKeySource source = new RealmKeySource(httpClient, properties.identity(), clock);
        source.loadAtStart();
        return source;
    }

    /**
     * @param keySource the realm keys
     * @param properties component settings
     * @return the token validator
     */
    @Bean
    public TokenValidator tokenValidator(RealmKeySource keySource, VocabularyManagerProperties properties) {
        return new TokenValidator(
                keySource,
                properties.identity().issuerUri(),
                properties.identity().audience());
    }

    /**
     * The management port (health probes, Prometheus metrics) is internal to the deployment and never published (D67,
     * D86). Its requests skip the token rules of the API.
     *
     * @param http the security builder
     * @param managementPort the management port
     * @return the filter chain of the management port
     * @throws Exception when the configuration fails
     */
    @Bean
    @Order(1)
    public SecurityFilterChain managementFilterChain(
            HttpSecurity http, @Value("${management.server.port:8081}") int managementPort) throws Exception {
        http.securityMatcher(request -> request.getLocalPort() == managementPort)
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rules -> rules.anyRequest().permitAll());
        return http.build();
    }

    /**
     * @param http the security builder
     * @param validator validates tokens
     * @param catalogue the operations and their role rules
     * @param problemWriter writes problem bodies
     * @return the filter chain
     * @throws Exception when the configuration fails
     */
    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, TokenValidator validator, OperationCatalogue catalogue, ProblemWriter problemWriter)
            throws Exception {
        http.csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .requestCache(cache -> cache.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(
                        new TokenAuthenticationFilter(validator, problemWriter), AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(rules -> rules.dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()
                        .anyRequest()
                        .access((authentication, context) -> decide(catalogue, authentication, context)))
                .exceptionHandling(handling -> handling.authenticationEntryPoint((request, response, exception) -> {
                            SECURITY_LOG.warn("Access without a token refused");
                            problemWriter.write(
                                    response,
                                    Problems.unauthorized("Authentication is required."),
                                    Map.of("WWW-Authenticate", "Bearer"));
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            if (operationOf(catalogue, request).isEmpty()) {
                                problemWriter.write(response, Problems.unknownPath());
                            } else {
                                SECURITY_LOG.warn("Access refused: missing role for {}", request.getMethod());
                                problemWriter.write(response, Problems.forbidden());
                            }
                        }))
                .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'none'; frame-ancestors 'none'"))
                        .cacheControl(cache -> {}));
        return http.build();
    }

    /**
     * Firewall refusals (encoded slash, control characters) become a Belgif 400 without issues (D67).
     *
     * @param problemWriter writes the body
     * @return the handler
     */
    @Bean
    public RequestRejectedHandler requestRejectedHandler(ProblemWriter problemWriter) {
        return (request, response, exception) -> {
            SECURITY_LOG.warn("Request refused by the firewall");
            problemWriter.write(response, Problems.badRequestProblem(null));
        };
    }

    /**
     * The role rule of D16, applied per contract operation; anything else is denied (D84).
     *
     * @param catalogue the operations
     * @param authentication the caller
     * @param context the request
     * @return the decision
     */
    static AuthorizationResult decide(
            OperationCatalogue catalogue,
            Supplier<? extends Authentication> authentication,
            RequestAuthorizationContext context) {
        Optional<ApiOperation> operation = operationOf(catalogue, context.getRequest());
        if (operation.isEmpty()) {
            return new AuthorizationDecision(false);
        }
        if (operation.get().access() == ApiOperation.Access.PUBLIC) {
            return new AuthorizationDecision(true);
        }
        Authentication current = authentication.get();
        if (current == null || !(current.getPrincipal() instanceof AuthenticatedUser user)) {
            return new AuthorizationDecision(false);
        }
        boolean allowed = operation.get().access() == ApiOperation.Access.READ ? user.canRead() : user.canWrite();
        return new AuthorizationDecision(allowed);
    }

    private static Optional<ApiOperation> operationOf(OperationCatalogue catalogue, HttpServletRequest request) {
        return catalogue.resolve(request.getRequestURI(), request.getMethod());
    }

    /**
     * @return the system clock in UTC
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
