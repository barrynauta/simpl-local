package eu.simpl.cm.manager.web;

import eu.simpl.cm.common.ProblemTypes;
import eu.simpl.cm.common.ProblemWriter;
import eu.simpl.cm.manager.CmProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * D65 and D84 step 1: every CM request, /health included, needs X-Api-Key equal to the configured key.
 * Missing or wrong: 401 with a problem body, before any other check. CM never reads Authorization.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiKeyFilter extends OncePerRequestFilter {

    private final byte[] key;

    public ApiKeyFilter(CmProperties props) {
        this.key = props.apiKey().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String given = request.getHeader("X-Api-Key");
        if (given == null || !MessageDigest.isEqual(key, given.getBytes(StandardCharsets.UTF_8))) {
            ProblemWriter.write(request, response, 401, ProblemTypes.UNAUTHORIZED, "Unauthorized",
                    given == null ? "Missing API Key" : "Invalid API Key");
            return;
        }
        chain.doFilter(request, response);
    }
}
