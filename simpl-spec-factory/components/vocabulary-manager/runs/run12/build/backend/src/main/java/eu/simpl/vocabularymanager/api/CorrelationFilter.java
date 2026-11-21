package eu.simpl.vocabularymanager.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads or creates X-Correlation-Id, returns it, puts it in the MDC for every log line, and writes
 * one access log line per request (D86, D96). The W3C trace context is handled by Micrometer
 * Tracing, which runs before this filter. No email or token is logged (D80).
 */
public class CorrelationFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9._:-]{1,64}$");
    private static final Logger ACCESS = LoggerFactory.getLogger("access");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id = request.getHeader(HEADER);
        if (id == null || !VALID.matcher(id).matches()) {
            id = UUID.randomUUID().toString();
        }
        MDC.put("correlationId", id);
        response.setHeader(HEADER, id);
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long ms = (System.nanoTime() - start) / 1_000_000;
            ACCESS.info(
                    "{} {} -> {} in {} ms",
                    request.getMethod(),
                    sanitize(request.getRequestURI()),
                    response.getStatus(),
                    ms);
            MDC.remove("correlationId");
            MDC.remove("operationId");
            MDC.remove("vocabularyName");
            MDC.remove("version");
            MDC.remove("userId");
        }
    }

    private static String sanitize(String uri) {
        if (uri == null) {
            return "";
        }
        String s = uri.length() > 200 ? uri.substring(0, 200) : uri;
        return s.replaceAll("[\\r\\n\\t]", "_");
    }
}
