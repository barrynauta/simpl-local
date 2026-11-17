package eu.simpl.cm.uibe;

import eu.simpl.cm.common.ProblemTypes;
import eu.simpl.cm.common.ProblemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * D66 to D69. Machine operations: API key. User operations: bearer JWT from this party's realm,
 * role contract_manager, participant_id equal to the own participant id.
 */
@Configuration
public class SecurityConfig {

    public static final String ROLE_CONTRACT_MANAGER = "ROLE_contract_manager";
    public static final String ROLE_CONSENT_MANAGER = "ROLE_consent_manager";

    static RequestMatcher machineOperations() {
        return new OrRequestMatcher(
                new AntPathRequestMatcher("/contractOffers/*", HttpMethod.POST.name()),
                new AntPathRequestMatcher("/admin/**"));
    }

    @Bean
    @Order(1)
    SecurityFilterChain machineChain(HttpSecurity http, UibeProperties props) throws Exception {
        http.securityMatcher(machineOperations())
                .csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .addFilterBefore(new MachineApiKeyFilter(props.apiKey()), BasicAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain userChain(HttpSecurity http, UibeProperties props, JwtDecoder decoder) throws Exception {
        http.csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(new AntPathRequestMatcher("/health")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/error")).permitAll()
                        .anyRequest().hasAuthority(ROLE_CONTRACT_MANAGER))
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.decoder(decoder).jwtAuthenticationConverter(converter(props.participantId())))
                        .authenticationEntryPoint((req, res, ex) -> ProblemWriter.write(req, res, 401,
                                ProblemTypes.UNAUTHORIZED, "Unauthorized", "Missing or invalid token"))
                        .accessDeniedHandler((req, res, ex) -> ProblemWriter.write(req, res, 403,
                                ProblemTypes.FORBIDDEN, "Forbidden", "Access denied")))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> ProblemWriter.write(req, res, 401,
                                ProblemTypes.UNAUTHORIZED, "Unauthorized", "Missing or invalid token"))
                        .accessDeniedHandler((req, res, ex) -> ProblemWriter.write(req, res, 403,
                                ProblemTypes.FORBIDDEN, "Forbidden", "Access denied")));
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder(UibeProperties props) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(props.oidcJwksUri()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(props.oidcIssuer()));
        return decoder;
    }

    /** Realm roles become ROLE_<name>, but only when participant_id is this party's id (D69). */
    static Converter<Jwt, AbstractAuthenticationToken> converter(String participantId) {
        return jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            if (participantId.equals(jwt.getClaimAsString("participant_id"))) {
                Object realmAccess = jwt.getClaims().get("realm_access");
                if (realmAccess instanceof Map<?, ?> m && m.get("roles") instanceof List<?> roles) {
                    for (Object r : roles) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + r));
                    }
                }
            }
            String name = jwt.getClaimAsString("preferred_username");
            return new JwtAuthenticationToken(jwt, authorities, name != null ? name : jwt.getSubject());
        };
    }
}
