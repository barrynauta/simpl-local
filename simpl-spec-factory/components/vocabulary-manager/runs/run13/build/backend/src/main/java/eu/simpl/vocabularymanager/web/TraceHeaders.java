package eu.simpl.vocabularymanager.web;

import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.MDC;

/**
 * The headers forwarded on calls to the store and the identity provider (D86): the W3C trace context from
 * Micrometer Tracing and the X-Correlation-Id of the request.
 */
public class TraceHeaders {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_MDC = "correlationId";

    private final Tracer tracer;
    private final Propagator propagator;

    public TraceHeaders(Tracer tracer, Propagator propagator) {
        this.tracer = tracer;
        this.propagator = propagator;
    }

    /** Headers for an outgoing call, one value per header. */
    public Map<String, List<String>> forOutgoingCall() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        TraceContext context =
                tracer == null ? null : tracer.currentTraceContext().context();
        if (context != null && propagator != null) {
            propagator.inject(context, headers, (carrier, key, value) -> carrier.put(key, List.of(value)));
        }
        String correlationId = MDC.get(CORRELATION_MDC);
        if (correlationId != null) {
            headers.put(CORRELATION_HEADER, List.of(correlationId));
        }
        return headers;
    }
}
