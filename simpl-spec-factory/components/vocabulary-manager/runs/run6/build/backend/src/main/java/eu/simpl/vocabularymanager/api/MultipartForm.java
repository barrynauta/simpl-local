package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.problem.Issue;
import eu.simpl.vocabularymanager.problem.Problems;
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
import org.springframework.web.multipart.MultipartException;

/**
 * The parts of a multipart/form-data request, read through the Servlet API (the container parses the multipart
 * format, REUSE-02). Checks size (413) before shape (400), as decision D51 orders them.
 */
public final class MultipartForm {

    private static final int MAX_TEXT_PART_BYTES = 64 * 1024;

    private final Map<String, List<Part>> parts;

    private MultipartForm(Map<String, List<Part>> parts) {
        this.parts = parts;
    }

    /**
     * Reads the parts of the request.
     *
     * @param request the request, already checked to be multipart/form-data
     * @return the form
     * @throws eu.simpl.vocabularymanager.problem.ProblemException 400 when the body is not valid multipart
     */
    public static MultipartForm read(HttpServletRequest request) {
        Map<String, List<Part>> byName = new LinkedHashMap<>();
        try {
            for (Part part : request.getParts()) {
                byName.computeIfAbsent(part.getName(), n -> new ArrayList<>()).add(part);
            }
        } catch (IOException | ServletException | IllegalStateException | MultipartException e) {
            throw Problems.badRequest(
                    Issue.schemaViolation(Issue.IN_BODY, "body", "The multipart/form-data body cannot be read."));
        }
        return new MultipartForm(byName);
    }

    /**
     * Checks that the file field is not larger than the limit (413, decision D15).
     *
     * @param field the file field
     * @param maxBytes the limit
     * @throws eu.simpl.vocabularymanager.problem.ProblemException 413
     */
    public void checkFileSize(String field, long maxBytes) {
        for (Part part : parts.getOrDefault(field, List.of())) {
            if (part.getSize() > maxBytes) {
                throw Problems.payloadTooLarge(field);
            }
        }
    }

    /**
     * Adds issues for unknown fields (decision D52) and for fields given more than once.
     *
     * @param allowed the fields the operation defines
     * @param repeatable the fields that may repeat
     * @param issues the issue list
     */
    public void checkNames(Set<String> allowed, Set<String> repeatable, List<Issue> issues) {
        parts.forEach((name, list) -> {
            if (!allowed.contains(name)) {
                issues.add(Issue.unknownInput(Issue.IN_BODY, name));
            } else if (list.size() > 1 && !repeatable.contains(name)) {
                issues.add(Issue.schemaViolation(
                        Issue.IN_BODY, name, "Form field '" + name + "' is given more than once."));
            }
        });
    }

    /**
     * Tells whether a field is present.
     *
     * @param name the field
     * @return true when at least one part has this name
     */
    public boolean has(String name) {
        return parts.containsKey(name);
    }

    /**
     * Reads a text field.
     *
     * @param name the field
     * @param issues the issue list, for a field that is too long
     * @return the value, or null when absent or unreadable
     */
    public String text(String name, List<Issue> issues) {
        List<Part> list = parts.get(name);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return textOf(list.get(0), name, issues);
    }

    /**
     * Reads every value of a repeatable text field.
     *
     * @param name the field
     * @param issues the issue list
     * @return the values in order, empty when absent
     */
    public List<String> texts(String name, List<Issue> issues) {
        List<String> values = new ArrayList<>();
        for (Part part : parts.getOrDefault(name, List.of())) {
            String value = textOf(part, name, issues);
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    /**
     * Reads the bytes of the file field (a file part or a text part).
     *
     * @param name the field
     * @param issues the issue list, for a missing field
     * @return the bytes, or null when absent
     */
    public byte[] file(String name, List<Issue> issues) {
        List<Part> list = parts.get(name);
        if (list == null || list.isEmpty()) {
            issues.add(Issue.schemaViolation(Issue.IN_BODY, name, "Field '" + name + "' is required."));
            return null;
        }
        try (InputStream in = list.get(0).getInputStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            issues.add(Issue.schemaViolation(Issue.IN_BODY, name, "Field '" + name + "' cannot be read."));
            return null;
        }
    }

    private static String textOf(Part part, String name, List<Issue> issues) {
        if (part.getSize() > MAX_TEXT_PART_BYTES) {
            issues.add(Issue.schemaViolation(Issue.IN_BODY, name, "Field '" + name + "' is too long."));
            return null;
        }
        try (InputStream in = part.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            issues.add(Issue.schemaViolation(Issue.IN_BODY, name, "Field '" + name + "' cannot be read."));
            return null;
        }
    }
}
