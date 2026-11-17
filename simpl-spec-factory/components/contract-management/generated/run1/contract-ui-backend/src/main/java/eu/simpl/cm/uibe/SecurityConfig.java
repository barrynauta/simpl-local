package eu.simpl.cm.uibe;

import eu.simpl.cm.common.web.Problems;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * D66, D69: machine operations use the API key (ApiKeyFilter), user operations need a Bearer JWT of this
 * party's realm. Missing or invalid token, or another issuer: 401.
 */
@Configuration
public class SecurityConfig {
    @Bean
    JwtDecoder jwtDecoder(UibeSettings settings) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(settings.oidcJwksUri).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(settings.oidcIssuer)));
        return decoder;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, ApiKeyFilter apiKeyFilter) throws Exception {
        http.csrf(c -> c.disable())
                .cors(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(c -> c.disable())
                .securityContext(c -> c.requireExplicitSave(true))
                .addFilterBefore(apiKeyFilter, BasicAuthenticationFilter.class)
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/health", "/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/contractOffers/*").permitAll()
                        .requestMatchers("/admin/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o
                        .jwt(j -> {
                        })
                        .authenticationEntryPoint((req, res, ex) -> Problems.write(res, 401, Problems.UNAUTHORIZED,
                                "A valid bearer token is required", req.getRequestURI()))
                        .accessDeniedHandler((req, res, ex) -> Problems.write(res, 403, Problems.FORBIDDEN,
                                "Access denied", req.getRequestURI())))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> Problems.write(res, 401, Problems.UNAUTHORIZED,
                                "A valid bearer token is required", req.getRequestURI()))
                        .accessDeniedHandler((req, res, ex) -> Problems.write(res, 403, Problems.FORBIDDEN,
                                "Access denied", req.getRequestURI())));
        return http.build();
    }
}
