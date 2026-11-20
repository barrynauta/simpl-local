package eu.simpl.vocabularymanager.web;

import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Headers forwarded on calls to the store and the identity provider (D86): the W3C trace context from Micrometer
 * Tracing and the X-Correlation-Id of the request.
 */
@Component
public class OutboundHeaders {

    private final ObjectProvider<Tracer> tracer;
    private final ObjectProvider<Propagator> propagator;

    public OutboundHeaders(ObjectProvider<Tracer> tracer, ObjectProvider<Propagator> propagator) {
        this.tracer = tracer;
        this.propagator = propagator;
    }

    public Map<String, String> current() {
        Map<String, String> headers = new LinkedHashMap<>();
        Tracer activeTracer = tracer.getIfAvailable();
        Propagator activePropagator = propagator.getIfAvailable();
        if (activeTracer != null && activePropagator != null) {
            TraceContext context = activeTracer.currentTraceContext().context();
            if (context != null) {
                activePropagator.inject(context, headers, Map::put);
            }
        }
        String correlationId = MDC.get(CorrelationFilter.MDC_KEY);
        if (correlationId != null) {
            headers.put(CorrelationFilter.HEADER, correlationId);
        }
        return headers;
    }
}
