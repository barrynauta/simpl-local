package eu.simpl.vocabularymanager.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads and checks the parts of a request that Spring would otherwise check in its own order, so the order of D51
 * holds: 415 media type, 413 size, then 400 request shape.
 */
@Component
public class RequestInspector {

    private static final int MAX_JSON_BYTES = 1024 * 1024;

    private final ObjectMapper mapper;

    public RequestInspector(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /** Query parameters in order of appearance, from the raw query string. */
    public Map<String, List<String>> query(HttpServletRequest request) {
        Map<String, List<String>> params = new LinkedHashMap<>();
        String raw = request.getQueryString();
        if (raw == null || raw.isEmpty()) {
            return params;
        }
        for (String pair : raw.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String name = decode(eq < 0 ? pair : pair.substring(0, eq));
            String value = eq < 0 ? "" : decode(pair.substring(eq + 1));
            params.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        }
        return params;
    }

    private static String decode(String text) {
        try {
            return URLDecoder.decode(text, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return text;
        }
    }

    /** Issues for unknown and repeated query parameters (D21, D52). */
    public List<Issue> queryIssues(Map<String, List<String>> params, Set<String> allowed) {
        List<Issue> issues = new ArrayList<>();
        params.forEach((name, values) -> {
            if (!allowed.contains(name)) {
                issues.add(Issue.schemaViolation(
                        "query", name, "Query parameter '" + name + "' is not supported.", values.get(0)));
            } else if (values.size() > 1) {
                issues.add(Issue.schemaViolation(
                        "query", name, "Query parameter '" + name + "' must be given only once.", null));
            }
        });
        return issues;
    }

    /** 406 when the Accept header excludes the media type the operation produces. */
    public void requireAcceptable(HttpServletRequest request, MediaType produced) {
        String accept = request.getHeader("Accept");
        if (accept == null || accept.isBlank()) {
            return;
        }
        try {
            for (MediaType type : MediaType.parseMediaTypes(accept)) {
                if (type.isCompatibleWith(produced)
                        || type.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                                && produced.equals(MediaType.APPLICATION_JSON)) {
                    return;
                }
            }
        } catch (InvalidMediaTypeException e) {
            return;
        }
        throw ApiException.notAcceptable();
    }

    /** 415 unless the body is multipart/form-data (D15). */
    public void requireMultipart(HttpServletRequest request) {
        if (!hasType(request, MediaType.MULTIPART_FORM_DATA)) {
            throw ApiException.unsupportedMediaType("multipart/form-data");
        }
    }

    /** 415 unless the body is application/json (D15). */
    public void requireJson(HttpServletRequest request) {
        if (!hasType(request, MediaType.APPLICATION_JSON)) {
            throw ApiException.unsupportedMediaType("application/json");
        }
    }

    private static boolean hasType(HttpServletRequest request, MediaType expected) {
        String contentType = request.getContentType();
        if (contentType == null) {
            return false;
        }
        try {
            MediaType type = MediaType.parseMediaType(contentType);
            return type.getType().equalsIgnoreCase(expected.getType())
                    && type.getSubtype().equalsIgnoreCase(expected.getSubtype());
        } catch (InvalidMediaTypeException e) {
            return false;
        }
    }

    /**
     * Reads all multipart parts, then gives 413 when the file part is larger than the limit (D15).
     *
     * @param request the request, already checked for multipart/form-data
     * @param fileField name of the file field
     * @param limit largest accepted file size in bytes
     * @return the parts in order
     */
    public List<FormPart> multipart(HttpServletRequest request, String fileField, long limit) {
        Collection<Part> parts;
        try {
            parts = request.getParts();
        } catch (MaxUploadSizeExceededException e) {
            throw ApiException.payloadTooLarge(fileField);
        } catch (IllegalStateException e) {
            if (isSizeProblem(e)) {
                throw ApiException.payloadTooLarge(fileField);
            }
            throw malformedMultipart();
        } catch (MultipartException | IOException | ServletException e) {
            if (isSizeProblem(e)) {
                throw ApiException.payloadTooLarge(fileField);
            }
            throw malformedMultipart();
        }
        List<FormPart> result = new ArrayList<>();
        for (Part part : parts) {
            if (fileField.equals(part.getName()) && part.getSize() > limit) {
                throw ApiException.payloadTooLarge(fileField);
            }
        }
        for (Part part : parts) {
            try (InputStream in = part.getInputStream()) {
                result.add(new FormPart(part.getName(), part.getSubmittedFileName(), in.readAllBytes()));
            } catch (IOException e) {
                throw malformedMultipart();
            }
        }
        return result;
    }

    private static boolean isSizeProblem(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof MaxUploadSizeExceededException) {
                return true;
            }
            String name = t.getClass().getSimpleName();
            if (name.contains("SizeLimitExceeded") || name.contains("SizeException")) {
                return true;
            }
        }
        return false;
    }

    private static ApiException malformedMultipart() {
        return ApiException.badRequest(
                Issue.schemaViolation("body", "body", "The multipart/form-data body could not be read.", null));
    }

    /**
     * Reads a JSON object body; a body that is not a JSON object gives 400.
     *
     * @param request the request, already checked for application/json
     * @return the JSON object
     */
    public JsonNode jsonObject(HttpServletRequest request) {
        byte[] body;
        try (InputStream in = request.getInputStream()) {
            body = in.readNBytes(MAX_JSON_BYTES + 1);
        } catch (IOException e) {
            throw invalidJson();
        }
        if (body.length > MAX_JSON_BYTES) {
            throw new ApiException(
                    new Problem(
                            "urn:problem-type:simpl:payloadTooLarge",
                            null,
                            "Payload Too Large",
                            413,
                            "The request body is larger than 1 MB.",
                            "urn:uuid:" + java.util.UUID.randomUUID(),
                            null),
                    Map.of());
        }
        try {
            JsonNode node = mapper.readTree(body);
            if (node == null || !node.isObject()) {
                throw invalidJson();
            }
            return node;
        } catch (JacksonException e) {
            throw invalidJson();
        }
    }

    private static ApiException invalidJson() {
        return ApiException.badRequest(
                Issue.schemaViolation("body", "body", "The request body must be a JSON object.", null));
    }
}
