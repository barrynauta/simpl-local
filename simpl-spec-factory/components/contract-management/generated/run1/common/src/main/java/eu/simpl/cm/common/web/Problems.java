package eu.simpl.cm.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Problem type URNs (D11) and a writer that bypasses content negotiation. */
public final class Problems {
    public static final String PREFIX = "urn:problem-type:simpl:";
    public static final String CM_PREFIX = PREFIX + "contract-management:";
    public static final String UNAUTHORIZED = PREFIX + "unauthorized";
    public static final String FORBIDDEN = PREFIX + "forbidden";
    public static final String NOT_FOUND = PREFIX + "notFound";
    public static final String METHOD_NOT_SUPPORTED = PREFIX + "methodNotSupported";
    public static final String NOT_ACCEPTABLE = PREFIX + "notAcceptable";
    public static final String UNSUPPORTED_MEDIA_TYPE = PREFIX + "unsupportedMediaType";
    public static final String ARGUMENT_TYPE_MISMATCH = PREFIX + "argumentTypeMismatch";
    public static final String VALIDATION_FAILURE = PREFIX + "validationFailure";
    public static final String INTERNAL_SERVER_ERROR = PREFIX + "internalServerError";
    public static final String SERVICE_UNAVAILABLE = PREFIX + "serviceUnavailable";
    public static final String WRONG_PARTY = CM_PREFIX + "wrongParty";
    public static final String INVALID_TRANSITION = CM_PREFIX + "invalidTransition";
    public static final String ALREADY_EXISTS = CM_PREFIX + "alreadyExists";
    public static final String OFFER_NOT_PENDING = CM_PREFIX + "offerNotPending";
    public static final String INVALID_DOCUMENT = CM_PREFIX + "invalidDocument";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Problems() {
    }

    public static Map<String, Object> body(int status, String type, String detail, String instance) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        HttpStatus hs = HttpStatus.resolve(status);
        m.put("title", hs != null ? hs.getReasonPhrase() : "Error");
        m.put("status", status);
        m.put("detail", detail);
        if (instance != null) {
            m.put("instance", instance);
        }
        return m;
    }

    public static void write(HttpServletResponse response, int status, Object body) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.resetBuffer();
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        MAPPER.writeValue(response.getOutputStream(), body);
        response.flushBuffer();
    }

    public static void write(HttpServletResponse response, int status, String type, String detail, String instance)
            throws IOException {
        write(response, status, body(status, type, detail, instance));
    }
}
