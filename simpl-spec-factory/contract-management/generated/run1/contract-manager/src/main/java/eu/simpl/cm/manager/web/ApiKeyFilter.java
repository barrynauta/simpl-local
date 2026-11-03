package eu.simpl.cm.manager.web;

import eu.simpl.cm.common.web.Problems;
import eu.simpl.cm.manager.CmSettings;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/** D65: every CM operation except /health needs X-Api-Key equal to the configured key, else 401. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiKeyFilter extends OncePerRequestFilter {
    private final CmSettings settings;

    public ApiKeyFilter(CmSettings settings) {
        this.settings = settings;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals("/health") || path.equals("/health/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = request.getHeader("X-Api-Key");
        if (key == null || !MessageDigest.isEqual(key.getBytes(StandardCharsets.UTF_8), settings.apiKey.getBytes(StandardCharsets.UTF_8))) {
            Problems.write(response, 401, Problems.UNAUTHORIZED, "Invalid API Key", request.getRequestURI());
            return;
        }
        chain.doFilter(request, response);
    }
}
