package eu.simpl.schemamanager.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Builds problem+json bodies (RFC 9457 shape of the OpenAPI) with the D63 type URNs.
 * Bodies are written as bytes with an explicit content type, so an error is never
 * turned into a second error by content negotiation.
 */
@Component
public class Problems {

    public static final String NOT_ACCEPTABLE_DETAIL =
            "Server cannot produce a response matching the list of acceptable values defined in the request's Accept header.";
    public static final String UNSUPPORTED_DETAIL =
            "Server refused to accept the request because the message content format is not supported.";
    public static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;

    private final ObjectMapper mapper;

    public Problems(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public static String typeName(int status) {
        return switch (status) {
            case 400 -> "badRequest";
            case 401 -> "unauthorized";
            case 403 -> "forbidden";
            case 404 -> "notFound";
            case 405 -> "methodNotAllowed";
            case 406 -> "notAcceptable";
            case 409 -> "conflict";
            case 413 -> "payloadTooLarge";
            case 415 -> "unsupportedMediaType";
            default -> "internalServerError";
        };
    }

    public static String title(int status) {
        return switch (status) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Resource Not Found";
            case 405 -> "Method Not Allowed";
            case 406 -> "Not Acceptable";
            case 409 -> "Conflict";
            case 413 -> "Payload Too Large";
            case 415 -> "Unsupported Media Type";
            default -> "Internal Server Error";
        };
    }

    public static String href(int status) {
        return switch (status) {
            case 400 -> "https://www.belgif.be/specification/rest/api-guide/problems/badRequest.html";
            case 500 -> "https://www.belgif.be/specification/rest/api-guide/problems/internalServerError.html";
            default -> "https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Status/" + status;
        };
    }

    public Map<String, Object> body(int status, String detail, List<Issue> issues) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "urn:problem-type:simpl:" + typeName(status));
        body.put("href", href(status));
        body.put("title", title(status));
        body.put("status", status);
        body.put("detail", detail);
        body.put("instance", "urn:uuid:" + UUID.randomUUID());
        if (issues != null) {
            body.put("issues", issues);
        }
        return body;
    }

    public byte[] bytes(int status, String detail, List<Issue> issues) {
        try {
            return mapper.writeValueAsBytes(body(status, detail, issues));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public ResponseEntity<byte[]> response(HttpStatus status, String detail, List<Issue> issues) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(PROBLEM_JSON);
        headers.add(HttpHeaders.VARY, HttpHeaders.ACCEPT);
        return new ResponseEntity<>(bytes(status.value(), detail, issues), headers, status);
    }

    public ResponseEntity<byte[]> response(ApiException e) {
        return response(e.status(), e.detail(), e.issues());
    }

    /** Used by the security filter chain, which runs outside Spring MVC. */
    public void write(HttpServletResponse response, int status, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType(PROBLEM_JSON.toString());
        response.getOutputStream().write(bytes(status, detail, null));
    }
}
