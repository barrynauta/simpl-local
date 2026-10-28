package eu.simpl.catalogue.common;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Builds Problem bodies and responses. The content type is preset so Accept negotiation never blocks an error. */
public final class Problems {
    public static final String PROBLEM_JSON = "application/problem+json";

    private Problems() {}

    public static ObjectNode body(ProblemTypes type, int status, String detail) {
        ObjectNode n = Json.object();
        n.put("type", type.uri);
        n.put("title", type.title);
        n.put("status", status);
        n.put("detail", detail == null ? type.title : detail);
        n.put("instance", "urn:uuid:" + UUID.randomUUID());
        return n;
    }

    public static ResponseEntity<String> response(ApiException e) {
        ObjectNode body = e.body() != null ? e.body() : body(e.type(), e.status(), e.getMessage());
        return response(e.status(), body);
    }

    public static ResponseEntity<String> response(ProblemTypes type, String detail) {
        return response(type.status, body(type, type.status, detail));
    }

    public static ResponseEntity<String> response(int status, ObjectNode body) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.CONTENT_TYPE, PROBLEM_JSON)
                .body(Json.write(body));
    }

    public static void write(HttpServletResponse res, ProblemTypes type, String detail) throws IOException {
        res.setStatus(type.status);
        res.setContentType(PROBLEM_JSON);
        res.setCharacterEncoding("UTF-8");
        res.getOutputStream().write(Json.write(body(type, type.status, detail)).getBytes(StandardCharsets.UTF_8));
    }
}
