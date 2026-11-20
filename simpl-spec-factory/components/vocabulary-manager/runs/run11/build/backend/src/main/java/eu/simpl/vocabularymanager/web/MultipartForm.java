package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.Issue;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The parts of a multipart/form-data request, read through the Servlet container's parser (D121:
 * no own multipart parser). Size limits are the container's (D15, D76); a part with an empty name
 * is ignored (D121).
 */
public final class MultipartForm {

    public static final String TOO_LARGE =
            "Uploaded vocabulary file is too large. The maximum allowed upload size is 5 MB. Please upload a smaller Turtle file.";

    private final Map<String, List<byte[]>> parts;

    private MultipartForm(Map<String, List<byte[]>> parts) {
        this.parts = parts;
    }

    /**
     * Reads every part of the request.
     *
     * @throws ApiException 413 above the size limits, 400 when the body is not a readable form
     */
    public static MultipartForm read(HttpServletRequest request) {
        Map<String, List<byte[]>> parts = new LinkedHashMap<>();
        try {
            for (Part part : request.getParts()) {
                String name = part.getName();
                if (name == null || name.isEmpty()) {
                    continue;
                }
                try (InputStream in = part.getInputStream()) {
                    parts.computeIfAbsent(name, k -> new ArrayList<>()).add(in.readAllBytes());
                }
            }
        } catch (IllegalStateException | IOException | ServletException ex) {
            throw translate(ex);
        }
        return new MultipartForm(parts);
    }

    public static ApiException translate(Exception ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            String type = t.getClass().getSimpleName();
            if (type.contains("SizeLimitExceeded") || type.contains("MaxUploadSize")) {
                return ApiException.tooLarge(ContentField.FILE, TOO_LARGE);
            }
            if (type.contains("FileCountLimitExceeded")) {
                return ApiException.badRequest("body", "", "The form has more than 64 parts.");
            }
        }
        return ApiException.badRequest("body", "", "The body is not a readable multipart/form-data form.");
    }

    /** Names of the parts in the request. */
    public Set<String> names() {
        return parts.keySet();
    }

    /** Field names of the request that the operation does not define (D52). */
    public List<Issue> unknownFields(Set<String> allowed) {
        List<Issue> issues = new ArrayList<>();
        for (String name : parts.keySet()) {
            if (!allowed.contains(name)) {
                issues.add(Issue.schema("body", name, "Form field '" + name + "' is not supported."));
            }
        }
        return issues;
    }

    /** Fields given more than once that may appear only once. */
    public List<Issue> repeatedFields(Set<String> single) {
        List<Issue> issues = new ArrayList<>();
        for (Map.Entry<String, List<byte[]>> entry : parts.entrySet()) {
            if (single.contains(entry.getKey()) && entry.getValue().size() > 1) {
                issues.add(Issue.schema(
                        "body", entry.getKey(), "Form field '" + entry.getKey() + "' is given more than once."));
            }
        }
        return issues;
    }

    public byte[] bytes(String name) {
        List<byte[]> values = parts.get(name);
        return values == null ? null : values.get(0);
    }

    public String text(String name) {
        byte[] value = bytes(name);
        return value == null ? null : new String(value, StandardCharsets.UTF_8);
    }

    public List<String> texts(String name) {
        List<byte[]> values = parts.getOrDefault(name, List.of());
        return values.stream().map(v -> new String(v, StandardCharsets.UTF_8)).toList();
    }

    /** Field names used in issues. */
    public static final class ContentField {
        public static final String FILE = "vocabularyFile";

        private ContentField() {}
    }
}
