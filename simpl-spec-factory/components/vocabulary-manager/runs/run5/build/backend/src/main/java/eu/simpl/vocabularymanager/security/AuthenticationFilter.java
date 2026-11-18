package eu.simpl.vocabularymanager.security;

import eu.simpl.vocabularymanager.web.ApiException;
import eu.simpl.vocabularymanager.web.ProblemWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Checks the bearer token and the role before anything else (D17, D51): no or invalid token gives 401, a missing
 * role 403, an unreachable identity provider for an unknown key 503 (D44). Fails closed.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuthenticationFilter extends OncePerRequestFilter {

    /** Request attribute that holds the {@link AuthenticatedUser}. */
    public static final String USER_ATTRIBUTE = AuthenticationFilter.class.getName() + ".user";

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationFilter.class);
    private static final String BEARER = "bearer ";

    private final TokenValidator validator;
    private final ProblemWriter problems;

    public AuthenticationFilter(TokenValidator validator, ProblemWriter problems) {
        this.validator = validator;
        this.problems = problems;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Set<Role> required = AccessRules.required(request.getMethod(), request.getRequestURI());
        if (required == null) {
            chain.doFilter(request, response);
            return;
        }
        String header = request.getHeader("Authorization");
        if (header == null
                || header.length() <= BEARER.length()
                || !header.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            LOG.warn("Access denied: no bearer token for {} {}", request.getMethod(), request.getRequestURI());
            problems.write(response, ApiException.unauthorized("A valid bearer token is required."));
            return;
        }
        AuthenticatedUser user;
        try {
            user = validator.validate(header.substring(BEARER.length()).trim());
        } catch (InvalidTokenException e) {
            LOG.warn("Access denied: {} for {} {}", e.getMessage(), request.getMethod(), request.getRequestURI());
            problems.write(response, ApiException.unauthorized("A valid bearer token is required."));
            return;
        } catch (IdentityProviderUnavailableException e) {
            LOG.error("Identity provider not available while checking a token", e);
            problems.write(
                    response, ApiException.serviceUnavailable("The identity provider is not available. Retry later."));
            return;
        }
        if (!user.hasAnyRole(required)) {
            LOG.warn(
                    "Access denied: user {} lacks role {} for {} {}",
                    user.subject(),
                    required,
                    request.getMethod(),
                    request.getRequestURI());
            problems.write(response, ApiException.forbidden());
            return;
        }
        request.setAttribute(USER_ATTRIBUTE, user);
        chain.doFilter(request, response);
    }
}
