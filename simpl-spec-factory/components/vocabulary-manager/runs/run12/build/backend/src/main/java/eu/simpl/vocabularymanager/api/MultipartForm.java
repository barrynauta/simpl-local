package eu.simpl.vocabularymanager.api;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads a multipart/form-data request through the servlet container (no own multipart parser,
 * D121). The file part is limited to 5 MB (D15, D76); parts with an empty name are ignored (D121);
 * unknown part names (D52) and repeated single-value parts give 400.
 */
public final class MultipartForm {

    public static final long FILE_LIMIT = 5L * 1024L * 1024L;
    public static final String FILE = "vocabularyFile";

    private final Map<String, List<String>> fields;
    private final byte[] file;

    private MultipartForm(Map<String, List<String>> fields, byte[] file) {
        this.fields = fields;
        this.file = file;
    }

    /**
     * Reads every part. 413 when the file part is above the limit; shape problems are returned by
     * {@link #shapeIssues} so the caller can order its checks (D51).
     */
    public static MultipartForm read(HttpServletRequest request) {
        java.util.Collection<Part> parts;
        try {
            parts = request.getParts();
        } catch (IllegalStateException e) {
            if (sizeLimit(e)) {
                throw ApiException.fileTooLarge();
            }
            throw ApiException.badRequest(Issue.schemaViolation(
                    "body", "body", "The multipart request has too many parts or is malformed.", null));
        } catch (IOException | ServletException e) {
            throw new ApiException(ProblemType.BAD_REQUEST, "The multipart request could not be read.");
        }
        Map<String, List<String>> fields = new LinkedHashMap<>();
        byte[] file = null;
        boolean fileSeen = false;
        for (Part part : parts) {
            String name = part.getName();
            if (name == null || name.isEmpty()) {
                continue;
            }
            if (FILE.equals(name)) {
                if (part.getSize() > FILE_LIMIT) {
                    throw ApiException.fileTooLarge();
                }
                if (!fileSeen) {
                    file = readAll(part);
                    fileSeen = true;
                    fields.computeIfAbsent(name, k -> new ArrayList<>()).add("");
                } else {
                    fields.get(name).add("");
                }
                continue;
            }
            if (part.getSize() > 64 * 1024) {
                fields.computeIfAbsent(name, k -> new ArrayList<>()).add(null);
                continue;
            }
            fields.computeIfAbsent(name, k -> new ArrayList<>()).add(new String(readAll(part), StandardCharsets.UTF_8));
        }
        return new MultipartForm(fields, file);
    }

    /** True when the container refused the request for its size (413), not for its shape. */
    static boolean sizeLimit(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t.getClass().getSimpleName().contains("SizeLimitExceeded")
                    || t.getClass().getSimpleName().contains("SizeException")) {
                return true;
            }
        }
        return false;
    }

    private static byte[] readAll(Part part) {
        try (InputStream in = part.getInputStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new ApiException(ProblemType.BAD_REQUEST, "The multipart request could not be read.");
        }
    }

    /** Unknown parts (D52) and repeated parts that take one value. */
    public List<Issue> shapeIssues(Set<String> allowed, Set<String> multiValued) {
        List<Issue> issues = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : fields.entrySet()) {
            String name = e.getKey();
            if (!allowed.contains(name)) {
                issues.add(Issue.schemaViolation("body", name, "Form field '" + name + "' is not supported.", null));
            } else if (!multiValued.contains(name) && e.getValue().size() > 1) {
                issues.add(
                        Issue.schemaViolation("body", name, "Form field '" + name + "' may be given only once.", null));
            }
        }
        return issues;
    }

    public boolean has(String name) {
        return fields.containsKey(name);
    }

    /** The single value of a text field, or null. A value over 64 KB is returned as null. */
    public String text(String name) {
        List<String> v = fields.get(name);
        return v == null || v.isEmpty() ? null : v.get(0);
    }

    public boolean oversized(String name) {
        List<String> v = fields.get(name);
        return v != null && !v.isEmpty() && v.get(0) == null;
    }

    public List<String> texts(String name) {
        List<String> v = fields.get(name);
        return v == null ? List.of() : v;
    }

    /** The uploaded file bytes, or null when the part is missing. */
    public byte[] file() {
        return file;
    }
}
