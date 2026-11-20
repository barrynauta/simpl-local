package eu.simpl.vocabularymanager.security;

import eu.simpl.vocabularymanager.error.ProblemException;
import eu.simpl.vocabularymanager.error.ProblemWriter;
import eu.simpl.vocabularymanager.error.Problems;
import eu.simpl.vocabularymanager.web.ApiOperation;
import eu.simpl.vocabularymanager.web.RoutingFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Validates the bearer token (D17) and puts the caller in the security context. Without an Authorization header the
 * request stays anonymous and the authorisation rules decide (401 for every secured operation). A token on the
 * public health operation is ignored. Failures are written as problem+json: 401, or 503 when the realm keys cannot
 * be fetched (D44).
 */
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(TokenAuthenticationFilter.class);
    private static final String PREFIX = "bearer ";

    private final TokenValidator validator;

    public TokenAuthenticationFilter(TokenValidator validator) {
        this.validator = validator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        ApiOperation operation = RoutingFilter.operationOf(request);
        List<String> headers = Collections.list(request.getHeaders("Authorization"));
        if (headers.isEmpty() || (operation != null && operation.access() == ApiOperation.Access.PUBLIC)) {
            chain.doFilter(request, response);
            return;
        }
        try {
            String header = headers.getFirst();
            if (headers.size() > 1
                    || header.length() <= PREFIX.length()
                    || !header.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
                throw Problems.unauthorized("The access token is missing or malformed.");
            }
            AuthenticatedUser user =
                    validator.validate(header.substring(PREFIX.length()).trim());
            SecurityContextHolder.getContext().setAuthentication(user);
        } catch (ProblemException problem) {
            LOG.warn("Authentication failed: status={} path={}", problem.status(), request.getRequestURI());
            ProblemWriter.write(response, problem);
            return;
        }
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
