package eu.simpl.vocabularymanager.request;

import eu.simpl.vocabularymanager.error.Issue;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Field and parameter rules of the decided contract (D19, D22, D37, D49, D56, D73, D91, D112). Each method adds an
 * issue to the list when the value breaks the rule and returns the parsed value or null.
 */
public final class FieldRules {

    public static final Pattern NAME = Pattern.compile("^[A-Z][a-zA-Z0-9]*$");
    public static final int NAME_MIN = 3;
    public static final int NAME_MAX = 64;
    public static final Pattern NAME_FILTER = Pattern.compile("^[A-Za-z0-9]+$");
    public static final int TEXT_MAX = 1000;
    public static final int PAGE_SIZE_MAX = 100;
    public static final int PAGE_SIZE_DEFAULT = 20;
    public static final int URI_MAX = 4096;
    public static final int ALIAS_MAX = 20;
    private static final Pattern DIGITS = Pattern.compile("^[0-9]+$");

    private FieldRules() {}

    public static boolean isValidName(String value) {
        return value != null
                && value.length() >= NAME_MIN
                && value.length() <= NAME_MAX
                && NAME.matcher(value).matches();
    }

    /** A vocabulary or external vocabulary name (D37). */
    public static String name(String value, String in, String field, List<Issue> issues) {
        if (value == null) {
            issues.add(Issue.schemaViolation(in, field, "'" + field + "' is required.", null));
            return null;
        }
        if (!isValidName(value)) {
            issues.add(Issue.schemaViolation(
                    in,
                    field,
                    "'" + field + "' must be 3 to 64 characters, start with an upper-case letter and contain only"
                            + " letters and digits.",
                    value));
            return null;
        }
        return value;
    }

    /** A text field: trimmed, 1 to 1000 characters when required, at most 1000 otherwise (D37, D56). */
    public static String text(String value, String field, boolean required, List<Issue> issues) {
        if (value == null) {
            if (required) {
                issues.add(Issue.schemaViolation("body", field, "'" + field + "' is required.", null));
            }
            return null;
        }
        String trimmed = value.strip();
        if (required && trimmed.isEmpty()) {
            issues.add(Issue.schemaViolation("body", field, "'" + field + "' must not be empty.", null));
            return null;
        }
        if (trimmed.length() > TEXT_MAX) {
            issues.add(Issue.schemaViolation(
                    "body", field, "'" + field + "' must be at most " + TEXT_MAX + " characters.", null));
            return null;
        }
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * A positive 32-bit integer in a path or query, written as digits only (D49, D91, D112).
     *
     * @return the value, or null after adding an issue
     */
    public static Integer positiveInt(String value, String in, String field, int max, List<Issue> issues) {
        if (value == null || !DIGITS.matcher(value).matches() || value.length() > 10) {
            issues.add(Issue.schemaViolation(in, field, "'" + field + "' must be a whole number.", value));
            return null;
        }
        long parsed = Long.parseLong(value);
        if (parsed < 1 || parsed > max) {
            issues.add(Issue.schemaViolation(in, field, "'" + field + "' must be between 1 and " + max + ".", value));
            return null;
        }
        return (int) parsed;
    }

    /** An absolute URI as RFC 3986 requires, with a scheme (D73). */
    public static boolean isAbsoluteUri(String value) {
        if (value == null || value.isEmpty() || value.length() > URI_MAX) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c <= ' ' || c >= 0x7f) {
                return false;
            }
        }
        try {
            URI uri = new URI(value);
            return uri.isAbsolute();
        } catch (URISyntaxException e) {
            return false;
        }
    }

    public static String uri(String value, String in, String field, boolean required, List<Issue> issues) {
        if (value == null) {
            if (required) {
                issues.add(Issue.schemaViolation(in, field, "'" + field + "' is required.", null));
            }
            return null;
        }
        if (!isAbsoluteUri(value)) {
            issues.add(Issue.schemaViolation(in, field, "'" + field + "' must be an absolute URI.", value));
            return null;
        }
        return value;
    }
}
