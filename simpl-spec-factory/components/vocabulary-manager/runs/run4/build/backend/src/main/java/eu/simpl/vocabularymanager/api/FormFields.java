package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.error.Issue;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/** Field rules of decision D37, shared by the create forms. */
final class FormFields {

    static final int MAX_TEXT = 1000;

    private FormFields() {}

    /** A required text field of 1 to 1000 characters after trimming; returns the trimmed value. */
    static String requiredText(MultipartForm form, String field, List<Issue> issues) {
        if (form.count(field) == 0) {
            issues.add(Issue.schemaViolation("body", field, null, "Field '" + field + "' is required."));
            return null;
        }
        return checkedText(form, field, issues, true);
    }

    /** An optional text field of at most 1000 characters after trimming; blank counts as absent. */
    static String optionalText(MultipartForm form, String field, List<Issue> issues) {
        if (form.count(field) == 0) {
            return null;
        }
        String value = checkedText(form, field, issues, false);
        return value == null || value.isEmpty() ? null : value;
    }

    private static String checkedText(MultipartForm form, String field, List<Issue> issues, boolean required) {
        if (form.count(field) > 1) {
            issues.add(Issue.schemaViolation("body", field, null, "Field '" + field + "' must be given once."));
            return null;
        }
        String value = form.value(field);
        String trimmed = value == null ? "" : value.trim();
        if ((required && trimmed.isEmpty()) || trimmed.length() > MAX_TEXT) {
            issues.add(Issue.schemaViolation(
                    "body",
                    field,
                    trimmed.length() > 100 ? trimmed.substring(0, 100) + "..." : trimmed,
                    "Field '" + field + "' must have 1 to " + MAX_TEXT + " characters after trimming."));
            return null;
        }
        return trimmed;
    }

    /** A required name of 3 to 64 characters matching ^[A-Z][a-zA-Z0-9]*$ (decision D37). */
    static String name(MultipartForm form, List<Issue> issues) {
        if (form.count("name") == 0) {
            issues.add(Issue.schemaViolation("body", "name", null, "Field 'name' is required."));
            return null;
        }
        if (form.count("name") > 1) {
            issues.add(Issue.schemaViolation("body", "name", null, "Field 'name' must be given once."));
            return null;
        }
        String value = form.value("name");
        if (!PathParameters.isValidName(value)) {
            issues.add(Issue.schemaViolation(
                    "body", "name", value, "Field 'name' must have 3 to 64 characters and match ^[A-Z][a-zA-Z0-9]*$."));
            return null;
        }
        return value;
    }

    /** The required file part, given once. */
    static byte[] file(MultipartForm form, String field, List<Issue> issues) {
        if (form.count(field) == 0) {
            issues.add(Issue.schemaViolation("body", field, null, "Field '" + field + "' is required."));
            return null;
        }
        if (form.count(field) > 1) {
            issues.add(Issue.schemaViolation("body", field, null, "Field '" + field + "' must be given once."));
            return null;
        }
        return form.file(field);
    }

    /** True when the value is an absolute URI. */
    static boolean isAbsoluteUri(String value) {
        if (value == null || value.isBlank() || value.chars().anyMatch(Character::isWhitespace)) {
            return false;
        }
        try {
            return new URI(value).isAbsolute();
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
