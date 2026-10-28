package eu.simpl.schemamanager.security;

import eu.simpl.schemamanager.config.Settings;
import eu.simpl.schemamanager.web.Problems;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * OAuth2 resource server (D59): issuer {KEYCLOAK_URL}/realms/simpl, roles from the
 * top-level "roles" claim, access matrix of D5. Authorisation runs in the filter chain,
 * so it comes before any validation or existence check.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    public static final String ADMIN = "GA_SCHEMA_ADMIN";
    public static final String VIEWER = "GA_SCHEMA_VIEWER";

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, Problems problems, Settings settings) throws Exception {
        http
                .csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(c -> c.configurationSource(corsSource(settings)))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/schemas", "/schemas/**").hasAnyRole(VIEWER, ADMIN)
                        .requestMatchers(HttpMethod.HEAD, "/schemas", "/schemas/**").hasAnyRole(VIEWER, ADMIN)
                        .requestMatchers(HttpMethod.POST, "/schemas", "/schemas/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/schemas/**").hasRole(ADMIN)
                        .requestMatchers("/schemas", "/schemas/**").authenticated()
                        .requestMatchers("/webhooks", "/webhooks/**").authenticated()
                        // Out-of-scope paths (/vocabularies, quality profiles) fall through to 404 (D62).
                        .anyRequest().permitAll())
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.jwtAuthenticationConverter(authenticationConverter()))
                        .authenticationEntryPoint((req, res, ex) ->
                                problems.write(res, 401, "Invalid or expired token"))
                        .accessDeniedHandler((req, res, ex) ->
                                problems.write(res, 403, "Access to the requested resource is forbidden.")))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) ->
                                problems.write(res, 401, "Invalid or expired token"))
                        .accessDeniedHandler((req, res, ex) ->
                                problems.write(res, 403, "Access to the requested resource is forbidden.")));
        return http.build();
    }

    @Bean
    public RequestRejectedHandler requestRejectedHandler(Problems problems) {
        return (req, res, ex) -> problems.write(res, 400, "Request rejected: " + ex.getMessage());
    }

    @Bean
    public JwtDecoder jwtDecoder(Settings settings) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(settings.jwksUrl()).build();
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(settings.issuer()));
        decoder.setJwtValidator(validator);
        return decoder;
    }

    /** Realm roles from the top-level "roles" array claim become ROLE_ authorities. */
    static JwtAuthenticationConverter authenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(SecurityConfig::roles);
        converter.setPrincipalClaimName("sub");
        return converter;
    }

    public static Collection<GrantedAuthority> roles(Jwt jwt) {
        List<GrantedAuthority> out = new ArrayList<>();
        Object claim = jwt.getClaims().get("roles");
        if (claim instanceof Collection<?> values) {
            for (Object v : values) {
                if (v != null) {
                    out.add(new SimpleGrantedAuthority("ROLE_" + v));
                }
            }
        }
        return out;
    }

    private static CorsConfigurationSource corsSource(Settings settings) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(settings.corsOrigins());
        cfg.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("Location", "Content-Location"));
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
