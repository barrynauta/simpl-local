package eu.simpl.vocabulary.config;

import eu.simpl.vocabulary.problem.ProblemWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * D2, D3, D4: every endpoint except /v1/health needs a valid bearer token. VIEWER may call every GET and the two
 * search POSTs; ADMIN may call everything. Missing or invalid token: 401; missing role: 403; both as Problem
 * Details.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationEntryPoint entryPoint,
                                           AccessDeniedHandler deniedHandler, AppProperties props) throws Exception {
        http.csrf(c -> c.disable())
                .cors(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.GET, "/v1/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/**").hasAnyAuthority(Roles.VIEWER, Roles.ADMIN)
                        .requestMatchers(HttpMethod.HEAD, "/v1/**").hasAnyAuthority(Roles.VIEWER, Roles.ADMIN)
                        .requestMatchers(HttpMethod.POST, "/v1/vocabularies/searches",
                                "/v1/external-vocabulary/searches").hasAnyAuthority(Roles.VIEWER, Roles.ADMIN)
                        .anyRequest().hasAuthority(Roles.ADMIN))
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.jwtAuthenticationConverter(authenticationConverter(props)))
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler))
                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler));
        return http.build();
    }

    /** Validates signatures with the issuer's JWKS and accepts the configured issuer(s). */
    @Bean
    public JwtDecoder jwtDecoder(AppProperties props) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(props.security().jwkSetUri()).build();
        List<String> issuers = props.security().issuerList();
        OAuth2TokenValidator<Jwt> issuerValidator = jwt -> {
            String iss = jwt.getClaimAsString("iss");
            return iss != null && issuers.contains(iss)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token",
                    "The token issuer '" + iss + "' is not accepted.", null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), issuerValidator));
        return decoder;
    }

    /** Authorities are the values of the top-level {@code roles} claim (D6). */
    static JwtAuthenticationConverter authenticationConverter(AppProperties props) {
        String claim = props.security().rolesClaim();
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            Object value = jwt.getClaims().get(claim);
            if (value instanceof Collection<?> roles) {
                roles.forEach(r -> authorities.add(new SimpleGrantedAuthority(String.valueOf(r))));
            } else if (value instanceof String role) {
                authorities.add(new SimpleGrantedAuthority(role));
            }
            return authorities;
        });
        converter.setPrincipalClaimName("sub");
        return converter;
    }

    @Bean
    public AuthenticationEntryPoint problemEntryPoint(ProblemWriter writer) {
        return (request, response, ex) -> {
            String header = request.getHeader("Authorization");
            boolean hasToken = header != null && header.regionMatches(true, 0, "Bearer ", 0, 7);
            if (hasToken) {
                response.setHeader("WWW-Authenticate",
                        "Bearer realm=\"vocabulary-manager\", error=\"invalid_token\"");
                writer.write(response, 401, "urn:problem-type:belgif:invalidAccessToken",
                        "https://www.belgif.be/specification/rest/api-guide/problems/invalidAccessToken.html",
                        "Invalid Access Token", "The Bearer access token found in the Authorization HTTP header is "
                                + "invalid.", List.of(), List.of());
            } else {
                response.setHeader("WWW-Authenticate", "Bearer realm=\"vocabulary-manager\"");
                writer.write(response, 401, "urn:problem-type:belgif:noAccessToken",
                        "https://www.belgif.be/specification/rest/api-guide/problems/noAccessToken.html",
                        "No Access Token", "No Bearer access token found in Authorization HTTP header.",
                        List.of(), List.of());
            }
        };
    }

    @Bean
    public AccessDeniedHandler problemDeniedHandler(ProblemWriter writer) {
        return (request, response, ex) -> writer.write(response, 403, "urn:problem-type:belgif:missingPermission",
                "https://www.belgif.be/specification/rest/api-guide/problems/missingPermission.html",
                "Missing Permission", "The access token does not grant the role required for this operation ("
                        + Roles.VIEWER + " for read operations, " + Roles.ADMIN + " for write operations).",
                List.of(), List.of());
    }
}
