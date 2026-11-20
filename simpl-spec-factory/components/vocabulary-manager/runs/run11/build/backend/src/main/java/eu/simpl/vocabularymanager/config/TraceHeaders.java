package eu.simpl.vocabularymanager.config;

import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import java.net.http.HttpRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Adds the W3C trace context and the correlation id to outgoing calls (D86), through Micrometer
 * Tracing (D94).
 */
@Component
public class TraceHeaders {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_MDC = "correlationId";

    private final Tracer tracer;
    private final Propagator propagator;

    public TraceHeaders(Tracer tracer, Propagator propagator) {
        this.tracer = tracer;
        this.propagator = propagator;
    }

    public HttpRequest.Builder apply(HttpRequest.Builder builder) {
        String correlationId = MDC.get(CORRELATION_MDC);
        if (correlationId != null) {
            builder.header(CORRELATION_HEADER, correlationId);
        }
        TraceContext context = tracer.currentTraceContext().context();
        if (context != null) {
            propagator.inject(context, builder, (b, key, value) -> b.header(key, value));
        }
        return builder;
    }
}
