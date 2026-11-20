package eu.simpl.vocabularymanager.request;

import eu.simpl.vocabularymanager.error.Issue;
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
 * A multipart/form-data request read through the servlet container (D15, D52, D70, D76, D110). The container caps
 * the whole request at 6 MB; the file part is capped at 5 MB here. Unknown, repeated and nameless parts become issues.
 */
public final class MultipartForm {

    public static final long FILE_LIMIT = 5L * 1024 * 1024;
    public static final int MAX_PARTS = 64;

    private final Map<String, List<byte[]>> parts = new LinkedHashMap<>();

    private MultipartForm() {}

    /**
     * Reads every part.
     *
     * @throws eu.simpl.vocabularymanager.error.ProblemException 413 when the request or the file part is too large,
     *     400 when the multipart body is malformed or has too many parts
     */
    public static MultipartForm read(HttpServletRequest request, String fileField) {
        Collection<Part> received;
        try {
            received = request.getParts();
        } catch (IOException | ServletException | IllegalStateException e) {
            throw classify(e);
        }
        if (received.size() > MAX_PARTS) {
            throw Problems.badRequest(
                    Issue.schemaViolation("body", "body", "A request may have at most " + MAX_PARTS + " parts.", null));
        }
        for (Part part : received) {
            if (fileField.equals(part.getName()) && part.getSize() > FILE_LIMIT) {
                throw Problems.uploadTooLarge();
            }
        }
        MultipartForm form = new MultipartForm();
        for (Part part : received) {
            String name = part.getName() == null ? "" : part.getName();
            try (InputStream in = part.getInputStream()) {
                form.parts.computeIfAbsent(name, key -> new ArrayList<>()).add(in.readAllBytes());
            } catch (IOException e) {
                throw Problems.badRequest(Issue.schemaViolation("body", name, "The part could not be read.", null));
            }
        }
        return form;
    }

    private static RuntimeException classify(Exception e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            String type = cause.getClass().getSimpleName();
            if (type.contains("SizeLimitExceeded") || type.contains("SizeException")) {
                return Problems.uploadTooLarge();
            }
            if (type.contains("FileCountLimitExceeded")) {
                return Problems.badRequest(Issue.schemaViolation(
                        "body", "body", "A request may have at most " + MAX_PARTS + " parts.", null));
            }
        }
        return Problems.badRequest(
                Issue.schemaViolation("body", "body", "The multipart request body could not be read.", null));
    }

    /** Adds an issue for every part name not in the allowed set and for every repeated single-value part. */
    public void checkNames(Set<String> allowed, Set<String> repeatable, List<Issue> issues) {
        parts.forEach((name, values) -> {
            if (!allowed.contains(name)) {
                issues.add(Issue.schemaViolation("body", name, "Field '" + name + "' is not supported.", null));
            } else if (values.size() > 1 && !repeatable.contains(name)) {
                issues.add(Issue.schemaViolation("body", name, "Field '" + name + "' may be given only once.", null));
            }
        });
    }

    public boolean has(String name) {
        return parts.containsKey(name);
    }

    public byte[] bytes(String name) {
        List<byte[]> values = parts.get(name);
        return values == null ? null : values.getFirst();
    }

    public String text(String name) {
        byte[] value = bytes(name);
        return value == null ? null : new String(value, StandardCharsets.UTF_8);
    }

    public List<String> texts(String name) {
        List<byte[]> values = parts.get(name);
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(value -> new String(value, StandardCharsets.UTF_8))
                .toList();
    }
}
