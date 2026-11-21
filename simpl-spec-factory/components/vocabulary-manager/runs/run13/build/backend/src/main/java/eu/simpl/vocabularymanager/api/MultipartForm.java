package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.ProblemIssue;
import eu.simpl.vocabularymanager.error.ProblemType;
import eu.simpl.vocabularymanager.error.Problems;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The parts of a multipart/form-data request, parsed by the servlet container (no own multipart parser,
 * D121). Size limits (D15, D76) are the container's multipart configuration; above them the answer is 413.
 */
public final class MultipartForm {

    public static final int MAX_PARTS = 64;
    public static final int FILE_LIMIT = 5 * 1024 * 1024;

    private final Map<String, List<byte[]>> parts;

    private MultipartForm(Map<String, List<byte[]>> parts) {
        this.parts = parts;
    }

    /**
     * Reads every part. Parts with an empty name are ignored (D121).
     *
     * @throws ApiException 413 above a size limit, 400 for a malformed body or more than 64 parts
     */
    public static MultipartForm read(HttpServletRequest request) {
        Collection<Part> received;
        try {
            received = request.getParts();
        } catch (IllegalStateException | ServletException | IOException e) {
            throw translate(e);
        }
        if (received.size() > MAX_PARTS) {
            throw Problems.badRequest(
                    ProblemIssue.schemaViolation("body", "body", "At most 64 form parts are allowed.", null));
        }
        Map<String, List<byte[]>> parts = new LinkedHashMap<>();
        for (Part part : received) {
            String name = part.getName();
            if (name == null || name.isEmpty()) {
                continue;
            }
            if (part.getSize() > FILE_LIMIT) {
                throw Problems.fileTooLarge();
            }
            try (InputStream in = part.getInputStream()) {
                parts.computeIfAbsent(name, key -> new ArrayList<>()).add(in.readAllBytes());
            } catch (IOException e) {
                throw translate(e);
            }
        }
        return new MultipartForm(parts);
    }

    private static ApiException translate(Exception error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            String type = cause.getClass().getSimpleName();
            if (type.contains("SizeLimitExceeded") || type.contains("SizeException")) {
                return Problems.fileTooLarge();
            }
            if (type.contains("FileCountLimitExceeded")) {
                return Problems.badRequest(
                        ProblemIssue.schemaViolation("body", "body", "At most 64 form parts are allowed.", null));
            }
        }
        return new ApiException(ProblemType.BAD_REQUEST, "The multipart request body could not be read.");
    }

    /** Issues for every part name the operation does not define (D52), and for repeated single fields. */
    public void checkNames(Set<String> allowed, Set<String> repeatable, List<ProblemIssue> issues) {
        for (Map.Entry<String, List<byte[]>> entry : parts.entrySet()) {
            String name = entry.getKey();
            if (!allowed.contains(name)) {
                issues.add(ProblemIssue.schemaViolation(
                        "body", name, "Form field '" + name + "' is not supported.", null));
            } else if (entry.getValue().size() > 1 && !repeatable.contains(name)) {
                issues.add(ProblemIssue.schemaViolation(
                        "body", name, "Form field '" + name + "' must be given only once.", null));
            }
        }
    }

    public boolean has(String name) {
        return parts.containsKey(name);
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
        return values.stream()
                .map(value -> new String(value, StandardCharsets.UTF_8))
                .toList();
    }
}
