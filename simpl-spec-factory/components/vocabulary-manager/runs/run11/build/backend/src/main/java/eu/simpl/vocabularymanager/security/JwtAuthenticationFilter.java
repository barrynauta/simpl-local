package eu.simpl.vocabularymanager.security;

import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.ProblemType;
import eu.simpl.vocabularymanager.error.ProblemWriter;
import eu.simpl.vocabularymanager.web.ApiRoutes;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads the bearer token and sets the authenticated user (D17). Invalid tokens get 401 at once;
 * when the realm keys cannot be fetched the request fails closed with 503 (D44). GET /health is
 * outside the access rules and is not checked.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String IDP_UNAVAILABLE = "The identity provider is not available. Retry later.";
    private static final Logger LOG = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER = "bearer ";

    private final TokenVerifier verifier;
    private final ProblemWriter problems;

    public JwtAuthenticationFilter(TokenVerifier verifier, ProblemWriter problems) {
        this.verifier = verifier;
        this.problems = problems;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return (ApiRoutes.BASE + "/health").equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null) {
            chain.doFilter(request, response);
            return;
        }
        if (!header.regionMatches(true, 0, BEARER, 0, BEARER.length())
                || header.substring(BEARER.length()).isBlank()) {
            reject(response, "authorization header is not a bearer token");
            return;
        }
        TokenVerifier.Result result =
                verifier.verify(header.substring(BEARER.length()).trim());
        switch (result) {
            case TokenVerifier.Valid valid -> {
                AuthenticatedUser user = valid.user();
                List<SimpleGrantedAuthority> authorities = user.roles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();
                SecurityContextHolder.getContext()
                        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, authorities));
                MDC.put("userId", user.subject());
                chain.doFilter(request, response);
            }
            case TokenVerifier.KeysUnavailable unavailable -> {
                LOG.warn("Token not checked: realm keys unavailable");
                problems.write(response, ApiException.unavailable(IDP_UNAVAILABLE));
            }
            case TokenVerifier.Invalid invalid -> reject(response, invalid.reason());
        }
    }

    private void reject(HttpServletResponse response, String reason) throws IOException {
        LOG.warn("Authentication failed: {}", reason);
        problems.write(
                response, new ApiException(ProblemType.UNAUTHORIZED, "The bearer token is missing or not valid."));
    }
}
