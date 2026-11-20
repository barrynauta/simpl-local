package eu.simpl.vocabularymanager.web;

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
 * Reads or creates X-Correlation-Id, returns it, and writes one access log line per request (D86, D96, MSV-09).
 * The W3C trace context is handled by Micrometer Tracing, which puts traceId in the log context.
 */
public class CorrelationFilter extends OncePerRequestFilter {

    /** Header name of the correlation id. */
    public static final String HEADER = "X-Correlation-Id";

    private static final Logger LOG = LoggerFactory.getLogger("access");
    private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        String correlationId = incoming != null && SAFE.matcher(incoming).matches()
                ? incoming
                : UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        response.setHeader(HEADER, correlationId);
        response.setHeader("Vary", "Accept");
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long millis = (System.nanoTime() - start) / 1_000_000;
            LOG.info(
                    "{} {} -> {} in {} ms", request.getMethod(), request.getRequestURI(), response.getStatus(), millis);
            MDC.remove("correlationId");
            MDC.remove("operationId");
            MDC.remove("vocabularyName");
            MDC.remove("version");
        }
    }
}
