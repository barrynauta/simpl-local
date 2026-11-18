package eu.simpl.vocabularymanager.security;

import eu.simpl.vocabularymanager.error.ProblemWriter;
import eu.simpl.vocabularymanager.error.VocabularyManagerException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates the bearer token before any role or body check (decision D17). No header: the request continues as
 * anonymous and the access rules answer 401. An invalid token: 401. Keys not available: 503 (decision D44).
 */
public class BearerTokenFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(BearerTokenFilter.class);
    private static final String PREFIX = "bearer ";

    private final TokenValidator validator;
    private final ProblemWriter problemWriter;

    public BearerTokenFilter(TokenValidator validator, ProblemWriter problemWriter) {
        this.validator = validator;
        this.problemWriter = problemWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "/health".equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || header.isBlank()) {
            chain.doFilter(request, response);
            return;
        }
        try {
            if (!header.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
                throw new VocabularyManagerException(
                        eu.simpl.vocabularymanager.error.AccessErrorCode.UNAUTHORIZED,
                        "Authorization header is not a bearer token.");
            }
            AuthenticatedUser user =
                    validator.validate(header.substring(PREFIX.length()).trim());
            SecurityContextHolder.getContext().setAuthentication(user);
        } catch (VocabularyManagerException e) {
            LOG.warn(
                    "Authentication failed: method={} path={} status={} reason={}",
                    request.getMethod(),
                    request.getServletPath(),
                    e.code().status(),
                    e.detail());
            SecurityContextHolder.clearContext();
            problemWriter.write(response, e.code(), e.detail(), e.issues());
            return;
        }
        chain.doFilter(request, response);
    }
}
