package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.config.TraceHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads or creates X-Correlation-Id, returns it, and puts it and the operationId in the log
 * context (D86, D96, MSV-09). The trace id comes from Micrometer Tracing.
 */
public class CorrelationFilter extends OncePerRequestFilter {

    private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String given = request.getHeader(TraceHeaders.CORRELATION_HEADER);
        String correlationId = given != null && SAFE.matcher(given).matches()
                ? given
                : UUID.randomUUID().toString();
        MDC.put(TraceHeaders.CORRELATION_MDC, correlationId);
        ApiRoutes.relative(request.getRequestURI())
                .flatMap(path -> ApiRoutes.find(request.getMethod(), path))
                .ifPresent(route -> MDC.put("operationId", route.operationId()));
        response.setHeader(TraceHeaders.CORRELATION_HEADER, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(TraceHeaders.CORRELATION_MDC);
            MDC.remove("operationId");
            MDC.remove("userId");
            MDC.remove("vocabularyName");
            MDC.remove("version");
        }
    }
}
