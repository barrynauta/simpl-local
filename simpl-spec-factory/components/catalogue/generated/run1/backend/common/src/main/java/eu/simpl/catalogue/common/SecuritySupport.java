package eu.simpl.catalogue.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * OAuth2 resource server set-up shared by the three backends (D68).
 * Ordinary paths accept only the own realm; federation paths accept the configured federation realms.
 */
public final class SecuritySupport {
    private SecuritySupport() {}

    public static SecurityFilterChain chain(HttpSecurity http, SecurityProps props, List<String> openPaths) throws Exception {
        AuthenticationManagerResolver<HttpServletRequest> resolver = resolver(props);
        String[] open = openPaths.toArray(String[]::new);
        http.csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(b -> b.disable())
            .formLogin(f -> f.disable())
            .logout(l -> l.disable())
            .requestCache(r -> r.disable())
            .anonymous(a -> {})
            .authorizeHttpRequests(a -> a.requestMatchers(open).permitAll().anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) ->
                    Problems.write(res, ProblemTypes.UNAUTHORIZED, "The request lacks valid authentication credentials."))
                .accessDeniedHandler((req, res, ex) ->
                    Problems.write(res, ProblemTypes.FORBIDDEN, "Forbidden.")))
            .oauth2ResourceServer(o -> o.authenticationManagerResolver(resolver)
                .authenticationEntryPoint((req, res, ex) ->
                    Problems.write(res, ProblemTypes.UNAUTHORIZED, "Invalid or expired token.")));
        return http.build();
    }

    public static AuthenticationManagerResolver<HttpServletRequest> resolver(SecurityProps props) {
        Map<String, AuthenticationManager> managers = new HashMap<>();
        Set<String> realms = new LinkedHashSet<>();
        realms.add(props.getOwnRealm());
        realms.addAll(props.getFederationRealms());
        for (String realm : realms) {
            managers.put(props.issuer(realm), manager(props, realm));
        }
        JwtIssuerAuthenticationManagerResolver own = new JwtIssuerAuthenticationManagerResolver(
                issuer -> issuer.equals(props.issuer(props.getOwnRealm())) ? managers.get(issuer) : null);
        JwtIssuerAuthenticationManagerResolver federation = new JwtIssuerAuthenticationManagerResolver(
                issuer -> props.getFederationRealms().stream().anyMatch(r -> props.issuer(r).equals(issuer))
                        ? managers.get(issuer) : null);
        return request -> {
            String path = request.getRequestURI().substring(request.getContextPath().length());
            boolean fed = props.getFederationPaths().stream().anyMatch(path::startsWith);
            return fed ? federation.resolve(request) : own.resolve(request);
        };
    }

    private static AuthenticationManager manager(SecurityProps props, String realm) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(props.jwks(realm)).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(props.issuer(realm)));
        return new ProviderManager(new JwtAuthenticationProvider(decoder));
    }
}
