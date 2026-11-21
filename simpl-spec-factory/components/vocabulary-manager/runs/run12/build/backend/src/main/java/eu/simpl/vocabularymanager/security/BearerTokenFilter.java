package eu.simpl.vocabularymanager.security;

import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.ProblemType;
import eu.simpl.vocabularymanager.error.ProblemWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads the bearer token, verifies it ({@link TokenVerifier}) and sets the authentication with the
 * realm roles as authorities. A present but invalid token gives 401 at once; no key set gives 503
 * (D17, D44). GET /v1/health takes no token (D17).
 */
public class BearerTokenFilter extends OncePerRequestFilter {

    private static final org.slf4j.Logger SECURITY_LOG = org.slf4j.LoggerFactory.getLogger("security");
    public static final String UNAUTHORIZED_DETAIL = "A valid bearer token is required.";

    private final TokenVerifier verifier;
    private final ProblemWriter problems;

    public BearerTokenFilter(TokenVerifier verifier, ProblemWriter problems) {
        this.verifier = verifier;
        this.problems = problems;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "/v1/health".equals(request.getRequestURI()) && "GET".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null) {
            chain.doFilter(request, response);
            return;
        }
        if (!header.regionMatches(true, 0, "Bearer ", 0, 7) || header.length() <= 7) {
            unauthorized(response);
            return;
        }
        try {
            TokenVerifier.Principal p = verifier.verify(header.substring(7).trim());
            List<SimpleGrantedAuthority> authorities = p.roles().stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .toList();
            UsernamePasswordAuthenticationToken auth =
                    UsernamePasswordAuthenticationToken.authenticated(p, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
            MDC.put("userId", p.subject());
            chain.doFilter(request, response);
        } catch (TokenVerifier.TokenException e) {
            SECURITY_LOG.warn("token rejected: {}", e.getMessage());
            if (e.unavailable()) {
                problems.write(response, ApiException.unavailable(ApiException.IDP_UNAVAILABLE));
            } else {
                unauthorized(response);
            }
        }
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        problems.write(
                response,
                new ApiException(
                        ProblemType.UNAUTHORIZED,
                        UNAUTHORIZED_DETAIL,
                        List.of(),
                        Map.of("WWW-Authenticate", "Bearer"),
                        false));
    }
}
