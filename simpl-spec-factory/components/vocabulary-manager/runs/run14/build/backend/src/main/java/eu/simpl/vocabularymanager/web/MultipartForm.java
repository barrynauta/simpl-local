package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.error.ProblemType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads a multipart/form-data body with the checks of D15, D52, D76, D121, D169 and D195, in this order: request
 * size (413), file part size (413 with an issue), number of parts (400 body), unknown fields (400 per name),
 * repeated single-valued fields (400). The servlet container parses the parts (no own multipart parser, D121).
 */
public final class MultipartForm {

    public static final String FILE_FIELD = "vocabularyFile";
    public static final long FILE_LIMIT = 5L * 1024 * 1024;
    public static final int MAX_PARTS = 64;
    public static final String FILE_TOO_LARGE = "Uploaded vocabulary file is too large. The maximum allowed upload"
            + " size is 5 MB. Please upload a smaller Turtle file.";

    private final Collection<Part> parts;
    private final Map<String, List<byte[]>> values = new LinkedHashMap<>();

    private MultipartForm(Collection<Part> parts) {
        this.parts = parts;
    }

    /**
     * Reads the parts and checks the file part size (D169: size comes before shape).
     *
     * @param request the request
     * @return the form
     * @throws ApiException 413 for a request over 6 MB or a file part over 5 MB, 400 for an unreadable body
     */
    public static MultipartForm read(HttpServletRequest request) {
        Collection<Part> parts = parts(request);
        for (Part part : parts) {
            if (FILE_FIELD.equals(part.getName()) && part.getSize() > FILE_LIMIT) {
                throw new ApiException(
                        ProblemType.PAYLOAD_TOO_LARGE,
                        FILE_TOO_LARGE,
                        List.of(Issue.invalidInput("body", FILE_FIELD, FILE_TOO_LARGE, null)));
            }
        }
        return new MultipartForm(parts);
    }

    /**
     * Checks the shape: number of parts, unknown fields, repeated single-valued fields.
     *
     * @param singleFields fields that may appear once
     * @param multiFields fields that may repeat
     * @throws ApiException 400 as described
     */
    public void checkShape(Set<String> singleFields, Set<String> multiFields) {
        if (parts.size() > MAX_PARTS) {
            throw ApiException.badRequest(
                    "body", "body", "The request has more than " + MAX_PARTS + " multipart parts.");
        }
        Set<String> unknown = new LinkedHashSet<>();
        for (Part part : parts) {
            String name = part.getName();
            if (name == null || name.isEmpty()) {
                continue;
            }
            if (!singleFields.contains(name) && !multiFields.contains(name)) {
                unknown.add(name);
                continue;
            }
            values.computeIfAbsent(name, key -> new ArrayList<>()).add(bytes(part));
        }
        if (!unknown.isEmpty()) {
            List<Issue> issues = new ArrayList<>();
            for (String name : unknown) {
                issues.add(Issue.schemaViolation("body", name, "Field '" + name + "' is not supported."));
            }
            throw ApiException.badRequest(issues);
        }
        List<Issue> repeated = new ArrayList<>();
        for (Map.Entry<String, List<byte[]>> entry : values.entrySet()) {
            if (singleFields.contains(entry.getKey()) && entry.getValue().size() > 1) {
                repeated.add(Issue.schemaViolation(
                        "body", entry.getKey(), "Field '" + entry.getKey() + "' is given more than once."));
            }
        }
        if (!repeated.isEmpty()) {
            throw ApiException.badRequest(repeated);
        }
    }

    /** The UTF-8 text of a single field, or null when absent. */
    public String text(String name) {
        List<byte[]> list = values.get(name);
        return list == null ? null : new String(list.get(0), StandardCharsets.UTF_8);
    }

    /** The UTF-8 texts of a repeatable field, in request order. */
    public List<String> texts(String name) {
        List<byte[]> list = values.get(name);
        if (list == null) {
            return List.of();
        }
        return list.stream()
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                .toList();
    }

    /** The bytes of a single field, or null when absent. */
    public byte[] bytes(String name) {
        List<byte[]> list = values.get(name);
        return list == null ? null : list.get(0);
    }

    private static Collection<Part> parts(HttpServletRequest request) {
        try {
            return request.getParts();
        } catch (IOException | ServletException | RuntimeException e) {
            throw classify(e);
        }
    }

    private static ApiException classify(Exception exception) {
        Throwable cause = exception;
        while (cause != null) {
            String type = cause.getClass().getSimpleName();
            if (type.contains("FileCountLimitExceeded")) {
                return ApiException.badRequest(
                        "body", "body", "The request has more than " + MAX_PARTS + " multipart parts.");
            }
            if (type.contains("SizeLimitExceeded") || type.contains("MaxUploadSizeExceeded")) {
                return new ApiException(
                        ProblemType.PAYLOAD_TOO_LARGE,
                        "The request is larger than the maximum allowed size of 6 MB.",
                        List.of());
            }
            cause = cause.getCause();
        }
        return new ApiException(ProblemType.BAD_REQUEST, "The multipart body could not be read.", List.of());
    }

    private static byte[] bytes(Part part) {
        try (InputStream input = part.getInputStream()) {
            return input.readAllBytes();
        } catch (IOException e) {
            throw new ApiException(ProblemType.BAD_REQUEST, "The multipart body could not be read.", List.of());
        }
    }
}
