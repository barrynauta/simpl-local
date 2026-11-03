package eu.simpl.cm.uibe;

import eu.simpl.cm.common.web.Problems;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** D66: machine operations need x-api-key equal to the UIBE key. */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {
    private final UibeSettings settings;

    public ApiKeyFilter(UibeSettings settings) {
        this.settings = settings;
    }

    static boolean isMachine(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.startsWith("/admin/")) {
            return true;
        }
        return "POST".equals(request.getMethod()) && path.matches("/contractOffers/[^/]+");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isMachine(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = request.getHeader("x-api-key");
        if (key == null || !MessageDigest.isEqual(key.getBytes(StandardCharsets.UTF_8), settings.apiKey.getBytes(StandardCharsets.UTF_8))) {
            Problems.write(response, 401, Problems.UNAUTHORIZED, "Invalid API Key", request.getRequestURI());
            return;
        }
        chain.doFilter(request, response);
    }
}
