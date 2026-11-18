package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.domain.VocabularyNames;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Checks of multipart form fields (D7, D37, D52, D56). Collects every issue before the caller fails. */
public final class FormFields {

    /** Longest description and changelog after trimming (D37). */
    public static final int MAX_TEXT = 1000;

    private final Map<String, List<FormPart>> byName = new LinkedHashMap<>();
    private final List<Issue> issues = new ArrayList<>();

    /**
     * Groups the parts and reports unknown fields and repeated single-value fields.
     *
     * @param parts the parts
     * @param allowed allowed field names
     * @param repeatable fields that may appear more than once
     */
    public FormFields(List<FormPart> parts, Set<String> allowed, Set<String> repeatable) {
        Set<String> unknown = new LinkedHashSet<>();
        for (FormPart part : parts) {
            if (!allowed.contains(part.name())) {
                unknown.add(part.name());
            } else {
                byName.computeIfAbsent(part.name(), k -> new ArrayList<>()).add(part);
            }
        }
        for (String name : unknown) {
            String detail = "version".equals(name)
                    ? "The version number is assigned automatically; do not send a 'version' field."
                    : "Form field '" + name + "' is not supported.";
            issues.add(Issue.schemaViolation("body", name, detail, null));
        }
        byName.forEach((name, list) -> {
            if (list.size() > 1 && !repeatable.contains(name)) {
                issues.add(Issue.schemaViolation(
                        "body", name, "Form field '" + name + "' must be given only once.", null));
            }
        });
    }

    public List<Issue> issues() {
        return issues;
    }

    /** Adds an issue found by the caller. */
    public void addIssue(Issue issue) {
        issues.add(issue);
    }

    /** The first part of a field, or null. */
    public FormPart part(String name) {
        List<FormPart> list = byName.get(name);
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    /** All parts of a field. */
    public List<FormPart> parts(String name) {
        return byName.getOrDefault(name, List.of());
    }

    /** The file content of a required file field, or null with an issue. */
    public byte[] requiredFile(String name) {
        FormPart part = part(name);
        if (part == null) {
            issues.add(Issue.schemaViolation("body", name, "Form field '" + name + "' is required.", null));
            return null;
        }
        return part.bytes();
    }

    /** A required vocabulary name (D37), or null with an issue. */
    public String requiredName(String field) {
        FormPart part = part(field);
        if (part == null) {
            issues.add(Issue.schemaViolation("body", field, "Form field '" + field + "' is required.", null));
            return null;
        }
        String value = part.text();
        if (!VocabularyNames.isValidName(value)) {
            issues.add(Issue.schemaViolation(
                    "body",
                    field,
                    "Name must have 3 to 64 characters, start with an upper-case letter and contain only letters and"
                            + " digits.",
                    value));
            return null;
        }
        return value;
    }

    /** A required text of 1 to 1000 characters after trimming (D37), trimmed, or null with an issue. */
    public String requiredText(String field) {
        FormPart part = part(field);
        if (part == null) {
            issues.add(Issue.schemaViolation("body", field, "Form field '" + field + "' is required.", null));
            return null;
        }
        return checkedText(field, part.text().trim(), true);
    }

    /** An optional text of at most 1000 characters after trimming (D56), trimmed; null when absent or blank. */
    public String optionalText(String field) {
        FormPart part = part(field);
        if (part == null) {
            return null;
        }
        return checkedText(field, part.text().trim(), false);
    }

    private String checkedText(String field, String trimmed, boolean required) {
        if (trimmed.isEmpty()) {
            if (required) {
                issues.add(Issue.schemaViolation("body", field, "Form field '" + field + "' must not be empty.", null));
            }
            return null;
        }
        if (trimmed.length() > MAX_TEXT) {
            issues.add(Issue.schemaViolation(
                    "body", field, "Form field '" + field + "' must have at most " + MAX_TEXT + " characters.", null));
            return null;
        }
        return trimmed;
    }
}
