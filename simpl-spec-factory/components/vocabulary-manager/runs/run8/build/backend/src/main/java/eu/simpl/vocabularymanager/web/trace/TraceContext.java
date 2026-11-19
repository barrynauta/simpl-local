package eu.simpl.vocabularymanager.web.trace;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.MDC;

/**
 * W3C trace context and correlation id of the current request (D86, MSV-09). Values live in the logging MDC, so every
 * log line carries them and outgoing calls can forward them.
 */
public final class TraceContext {

    public static final String TRACEPARENT = "traceparent";
    public static final String CORRELATION_ID = "X-Correlation-Id";
    public static final String MDC_CORRELATION_ID = "correlationId";
    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_SPAN_ID = "spanId";
    public static final String MDC_USER_ID = "userId";
    public static final String MDC_OPERATION = "operationId";

    private static final Pattern TRACEPARENT_PATTERN = Pattern.compile("^00-([0-9a-f]{32})-([0-9a-f]{16})-[0-9a-f]{2}$");
    private static final Pattern CORRELATION_PATTERN = Pattern.compile("^[A-Za-z0-9._:-]{1,64}$");
    private static final String ZERO_TRACE = "0".repeat(32);
    private static final SecureRandom RANDOM = new SecureRandom();

    private TraceContext() {}

    /**
     * @param traceparent the incoming traceparent header, or null
     * @return the trace id to continue, a new one when the header is absent or malformed
     */
    public static String traceIdFrom(String traceparent) {
        if (traceparent != null) {
            var matcher = TRACEPARENT_PATTERN.matcher(traceparent.trim());
            if (matcher.matches() && !ZERO_TRACE.equals(matcher.group(1))) {
                return matcher.group(1);
            }
        }
        return randomHex(16);
    }

    /**
     * @param correlationId the incoming X-Correlation-Id header, or null
     * @return the correlation id to use, a new one when absent or malformed (no log injection)
     */
    public static String correlationIdFrom(String correlationId) {
        if (correlationId != null && CORRELATION_PATTERN.matcher(correlationId).matches()) {
            return correlationId;
        }
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * @param bytes number of random bytes
     * @return lower-case hex
     */
    public static String randomHex(int bytes) {
        byte[] buffer = new byte[bytes];
        RANDOM.nextBytes(buffer);
        return HexFormat.of().formatHex(buffer);
    }

    /**
     * Headers to forward on calls to the store and the identity provider.
     *
     * @return header name to value, empty outside a request
     */
    public static Map<String, String> outgoingHeaders() {
        String traceId = MDC.get(MDC_TRACE_ID);
        String spanId = MDC.get(MDC_SPAN_ID);
        String correlationId = MDC.get(MDC_CORRELATION_ID);
        if (traceId == null || spanId == null || correlationId == null) {
            return Map.of();
        }
        return Map.of(TRACEPARENT, "00-" + traceId + "-" + spanId + "-01", CORRELATION_ID, correlationId);
    }
}
