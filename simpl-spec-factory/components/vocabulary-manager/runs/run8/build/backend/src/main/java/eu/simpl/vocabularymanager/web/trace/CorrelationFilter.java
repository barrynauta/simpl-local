package eu.simpl.vocabularymanager.web.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * First filter of every request: reads or creates traceparent and X-Correlation-Id, returns X-Correlation-Id, puts both
 * in the MDC and writes one access log line per request (D86, MSV-09, CODE-20).
 */
public class CorrelationFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger("access");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = TraceContext.traceIdFrom(request.getHeader(TraceContext.TRACEPARENT));
        String spanId = TraceContext.randomHex(8);
        String correlationId = TraceContext.correlationIdFrom(request.getHeader(TraceContext.CORRELATION_ID));
        MDC.put(TraceContext.MDC_TRACE_ID, traceId);
        MDC.put(TraceContext.MDC_SPAN_ID, spanId);
        MDC.put(TraceContext.MDC_CORRELATION_ID, correlationId);
        response.setHeader(TraceContext.CORRELATION_ID, correlationId);
        response.setHeader(TraceContext.TRACEPARENT, "00-" + traceId + "-" + spanId + "-01");
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long millis = (System.nanoTime() - start) / 1_000_000;
            LOG.info(
                    "{} {} -> {} in {} ms",
                    safeMethod(request.getMethod()),
                    safePath(request.getRequestURI()),
                    response.getStatus(),
                    millis);
            MDC.clear();
        }
    }

    private static String safeMethod(String method) {
        return method == null ? "" : method.replaceAll("[^A-Za-z]", "?");
    }

    private static String safePath(String path) {
        if (path == null) {
            return "";
        }
        String cleaned = path.replaceAll("[^A-Za-z0-9/%._~-]", "?");
        return cleaned.length() > 200 ? cleaned.substring(0, 200) + "..." : cleaned;
    }
}
