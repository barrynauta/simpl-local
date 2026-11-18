package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.error.Problems;
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
 * The parts of a multipart/form-data request. Only the fields of the operation are accepted (OWA-API3); the file part
 * may be larger than the upload limit, the limit is checked by the caller.
 */
public final class MultipartForm {

    private final Map<String, List<String>> fields;
    private final Map<String, List<byte[]>> files;

    private MultipartForm(Map<String, List<String>> fields, Map<String, List<byte[]>> files) {
        this.fields = fields;
        this.files = files;
    }

    /**
     * Reads the parts.
     *
     * @param fileField the name of the file part; it may also arrive as a plain field
     * @param allowed every accepted part name
     * @param forbiddenDetail detail per part name that is explicitly refused (for example 'version')
     * @throws eu.simpl.vocabularymanager.error.VocabularyManagerException 400 for unknown or refused parts
     */
    public static MultipartForm read(
            HttpServletRequest request, String fileField, Set<String> allowed, Map<String, String> forbiddenDetail)
            throws IOException, ServletException {
        Map<String, List<String>> fields = new LinkedHashMap<>();
        Map<String, List<byte[]>> files = new LinkedHashMap<>();
        List<Issue> issues = new ArrayList<>();
        for (Part part : request.getParts()) {
            String name = part.getName();
            if (forbiddenDetail.containsKey(name)) {
                issues.add(Issue.schemaViolation("body", name, null, forbiddenDetail.get(name)));
                continue;
            }
            if (!allowed.contains(name)) {
                issues.add(Issue.schemaViolation("body", name, null, "Form field '" + name + "' is not supported."));
                continue;
            }
            byte[] bytes;
            try (InputStream in = part.getInputStream()) {
                bytes = in.readAllBytes();
            }
            if (name.equals(fileField)) {
                files.computeIfAbsent(name, k -> new ArrayList<>()).add(bytes);
            } else {
                fields.computeIfAbsent(name, k -> new ArrayList<>()).add(new String(bytes, StandardCharsets.UTF_8));
            }
        }
        if (!issues.isEmpty()) {
            throw Problems.badRequest(issues);
        }
        return new MultipartForm(fields, files);
    }

    /** All values of a text field. */
    public List<String> values(String name) {
        return fields.getOrDefault(name, List.of());
    }

    /** The single value of a text field, or null. */
    public String value(String name) {
        List<String> list = values(name);
        return list.isEmpty() ? null : list.get(0);
    }

    /** Number of parts with this name. */
    public int count(String name) {
        return values(name).size() + files.getOrDefault(name, List.of()).size();
    }

    /** The file bytes, or null when the part is missing. */
    public byte[] file(String name) {
        List<byte[]> list = files.get(name);
        return list == null || list.isEmpty() ? null : list.get(0);
    }
}
