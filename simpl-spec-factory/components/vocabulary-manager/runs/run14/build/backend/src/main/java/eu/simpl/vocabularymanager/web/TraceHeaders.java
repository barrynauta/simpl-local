package eu.simpl.vocabularymanager.web;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * The headers forwarded on outgoing calls to the store and the identity provider (D86): the W3C trace context
 * through Micrometer Tracing (D94) and the correlation id.
 */
@Component
public class TraceHeaders {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_MDC = "correlationId";

    private static volatile TraceHeaders instance;

    private final Tracer tracer;
    private final Propagator propagator;

    public TraceHeaders(Tracer tracer, Propagator propagator) {
        this.tracer = tracer;
        this.propagator = propagator;
        instance = this;
    }

    /** The headers for the current request, empty outside a request. */
    public Map<String, String> headers() {
        Map<String, String> headers = new LinkedHashMap<>();
        Span span = tracer.currentSpan();
        if (span != null) {
            propagator.inject(span.context(), headers, Map::put);
        }
        String correlationId = MDC.get(CORRELATION_MDC);
        if (correlationId != null) {
            headers.put(CORRELATION_HEADER, correlationId);
        }
        return headers;
    }

    /** Static access for collaborators created outside the Spring context. */
    public static Map<String, String> current() {
        TraceHeaders current = instance;
        if (current == null) {
            String correlationId = MDC.get(CORRELATION_MDC);
            return correlationId == null ? Map.of() : Map.of(CORRELATION_HEADER, correlationId);
        }
        return current.headers();
    }
}
