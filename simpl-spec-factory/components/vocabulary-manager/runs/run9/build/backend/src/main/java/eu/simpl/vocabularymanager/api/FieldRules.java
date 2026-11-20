package eu.simpl.vocabularymanager.api;

import java.util.List;

/** Field rules of D37 and D56 for multipart forms. */
public final class FieldRules {

    /** Maximum description and changelog length after trimming (D37). */
    public static final int TEXT_MAX = 1000;

    private FieldRules() {}

    /**
     * A free-text field: trimmed, 1 to 1000 characters when required, at most 1000 when optional.
     *
     * @param form the form
     * @param name field name
     * @param required whether it must be present and non-empty
     * @param issues receives the issues
     * @return the trimmed value, or null when absent or empty and optional
     */
    public static String text(MultipartForm form, String name, boolean required, List<Issue> issues) {
        FormPart part = form.single(name, issues);
        if (part == null) {
            if (required && !form.has(name)) {
                issues.add(Issue.schemaViolation("body", name, "Form field '" + name + "' is required.", null));
            }
            return null;
        }
        String value = part.text().strip();
        int length = value.codePointCount(0, value.length());
        if (required && length < 1) {
            issues.add(Issue.schemaViolation(
                    "body", name, "Form field '" + name + "' must have 1 to 1000 characters after trimming.", null));
            return null;
        }
        if (length > TEXT_MAX) {
            issues.add(Issue.schemaViolation(
                    "body", name, "Form field '" + name + "' must have at most 1000 characters after trimming.", null));
            return null;
        }
        return value.isEmpty() ? null : value;
    }

    /** The name field of D37. */
    public static String name(MultipartForm form, List<Issue> issues) {
        FormPart part = form.single("name", issues);
        if (part == null) {
            if (!form.has("name")) {
                issues.add(Issue.schemaViolation("body", "name", "Form field 'name' is required.", null));
            }
            return null;
        }
        String value = part.text();
        Issue issue = RequestChecks.nameIssue(value, "body", "name");
        if (issue != null) {
            issues.add(issue);
            return null;
        }
        return value;
    }

    /** The vocabularyFile part: required, once; 0 bytes passes (D70). */
    public static byte[] file(MultipartForm form, List<Issue> issues) {
        FormPart part = form.single("vocabularyFile", issues);
        if (part == null) {
            if (!form.has("vocabularyFile")) {
                issues.add(Issue.schemaViolation(
                        "body", "vocabularyFile", "Form field 'vocabularyFile' is required.", null));
            }
            return null;
        }
        return part.bytes();
    }
}
