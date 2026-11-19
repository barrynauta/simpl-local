package eu.simpl.vocabularymanager.security;

import eu.simpl.vocabularymanager.web.problem.ProblemWriter;
import eu.simpl.vocabularymanager.web.problem.Problems;
import eu.simpl.vocabularymanager.web.trace.TraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads the bearer token and validates it (D17, D44). No token: the request continues anonymous and the
 * authorisation rules decide (401 or the public health operation). A bad token: 401. Keys needed but not obtainable:
 * 503. Runs before role and body checks.
 */
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger("security");
    private static final String BEARER = "bearer ";
    private static final String HEALTH_PATH = "/v1/health";

    private final TokenValidator validator;
    private final ProblemWriter problemWriter;

    /**
     * @param validator validates tokens
     * @param problemWriter writes 401 and 503 bodies
     */
    public TokenAuthenticationFilter(TokenValidator validator, ProblemWriter problemWriter) {
        this.validator = validator;
        this.problemWriter = problemWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        List<String> headers = Collections.list(request.getHeaders("Authorization"));
        if (headers.isEmpty() || HEALTH_PATH.equals(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }
        String header = headers.get(0);
        if (headers.size() > 1
                || header.length() <= BEARER.length()
                || !header.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            reject(response, "The Authorization header is not a single bearer token.");
            return;
        }
        String token = header.substring(BEARER.length()).trim();
        try {
            AuthenticatedUser user = validator.validate(token);
            MDC.put(TraceContext.MDC_USER_ID, user.subject());
            List<SimpleGrantedAuthority> authorities =
                    user.roles().stream().map(SimpleGrantedAuthority::new).toList();
            var authentication = UsernamePasswordAuthenticationToken.authenticated(user, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (TokenValidator.InvalidTokenException e) {
            LOG.warn("Rejected bearer token: {}", e.getMessage());
            reject(response, "The bearer token is not valid.");
            return;
        } catch (IdentityProviderUnavailableException e) {
            LOG.error("Identity provider not available while validating a token", e);
            problemWriter.write(
                    response,
                    Problems.serviceUnavailableProblem("The identity provider is not available. Retry later."),
                    Map.of("Retry-After", Problems.RETRY_AFTER_SECONDS));
            return;
        }
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void reject(HttpServletResponse response, String detail) throws IOException {
        problemWriter.write(
                response,
                Problems.unauthorized(detail),
                Map.of("WWW-Authenticate", "Bearer error=\"invalid_token\""));
    }
}
