package eu.simpl.cm.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Writes problem bodies straight to the servlet response, so that content negotiation
 * can never turn an error into an empty body (D11: the body is sent for every error status).
 */
public final class ProblemWriter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ProblemWriter() {
    }

    public static Map<String, Object> body(int status, String type, String title, String detail, String instance) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        m.put("title", title);
        m.put("status", status);
        m.put("detail", detail == null ? title : detail);
        if (instance != null) {
            m.put("instance", instance);
        }
        return m;
    }

    public static void write(HttpServletRequest req, HttpServletResponse res, int status, String type, String title,
                             String detail) throws IOException {
        if (res.isCommitted()) {
            return;
        }
        res.resetBuffer();
        res.setStatus(status);
        res.setContentType("application/problem+json");
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String instance = req == null ? null : req.getRequestURI();
        byte[] bytes = MAPPER.writeValueAsBytes(body(status, type, title, detail, instance));
        res.setContentLength(bytes.length);
        res.getOutputStream().write(bytes);
        res.flushBuffer();
    }

    public static void write(HttpServletRequest req, HttpServletResponse res, ApiException e) throws IOException {
        write(req, res, e.getStatus(), e.getType(), e.getTitle(), e.getDetail());
    }
}
