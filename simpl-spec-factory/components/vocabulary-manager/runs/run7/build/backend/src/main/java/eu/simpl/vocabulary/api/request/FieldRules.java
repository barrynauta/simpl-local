package eu.simpl.vocabulary.api.request;

import eu.simpl.vocabulary.api.problem.Issue;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.regex.Pattern;

/** Field and path rules of D37, D49, D56 and D73. */
public final class FieldRules {

    /** Vocabulary and external vocabulary name pattern (D37). */
    public static final Pattern NAME = Pattern.compile("^[A-Z][a-zA-Z0-9]*$");

    public static final int NAME_MIN = 3;
    public static final int NAME_MAX = 64;
    public static final int TEXT_MAX = 1000;
    public static final int MAX_ALIASES = 20;

    private static final Pattern POSITIVE_DIGITS = Pattern.compile("^[0-9]{1,100}$");

    private FieldRules() {}

    /**
     * Tells whether the value is a valid name (D37).
     *
     * @param value the value
     * @return true when valid
     */
    public static boolean isValidName(String value) {
        return value != null
                && value.length() >= NAME_MIN
                && value.length() <= NAME_MAX
                && NAME.matcher(value).matches();
    }

    /**
     * Checks a name.
     *
     * @param in where it is
     * @param field field name
     * @param value the value
     * @return an issue when invalid
     */
    public static Optional<Issue> checkName(String in, String field, String value) {
        if (isValidName(value)) {
            return Optional.empty();
        }
        return Optional.of(
                Issue.schemaViolation(
                        in,
                        field,
                        "'" + field
                                + "' must be 3 to 64 characters, start with an upper-case letter and contain only letters and digits."));
    }

    /**
     * Checks a required text of 1 to 1000 characters after trimming (D37).
     *
     * @param field field name
     * @param value the value, null when missing
     * @return an issue when invalid
     */
    public static Optional<Issue> checkRequiredText(String field, String value) {
        if (value == null) {
            return Optional.of(required(field));
        }
        int length = trimmedLength(value);
        if (length < 1 || length > TEXT_MAX) {
            return Optional.of(Issue.schemaViolation(
                    Issue.IN_BODY, field, "'" + field + "' must be 1 to 1000 characters after trimming."));
        }
        return Optional.empty();
    }

    /**
     * Checks an optional text of at most 1000 characters after trimming (D56).
     *
     * @param field field name
     * @param value the value, null when missing
     * @return an issue when invalid
     */
    public static Optional<Issue> checkOptionalText(String field, String value) {
        if (value != null && trimmedLength(value) > TEXT_MAX) {
            return Optional.of(Issue.schemaViolation(
                    Issue.IN_BODY, field, "'" + field + "' must be at most 1000 characters after trimming."));
        }
        return Optional.empty();
    }

    /**
     * Number of characters (code points) after trimming.
     *
     * @param value the text
     * @return the length
     */
    public static int trimmedLength(String value) {
        String trimmed = value.strip();
        return trimmed.codePointCount(0, trimmed.length());
    }

    /**
     * Issue for a missing required field.
     *
     * @param field the field
     * @return the issue
     */
    public static Issue required(String field) {
        return Issue.schemaViolation(Issue.IN_BODY, field, "'" + field + "' is required.");
    }

    /**
     * Tells whether the value is an absolute URI with a scheme (RFC 3986, D73).
     *
     * @param value the value
     * @return true when absolute
     */
    public static boolean isAbsoluteUri(String value) {
        if (value == null || value.isEmpty() || value.length() > 2048) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c <= 0x20 || c == 0x7f || c == '<' || c == '>' || c == '"' || c == '{' || c == '}' || c == '|'
                    || c == '\\' || c == '^' || c == '`') {
                return false;
            }
        }
        try {
            URI uri = new URI(value);
            return uri.isAbsolute() && uri.getScheme() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Normalises a namespace for comparison: removes one trailing '#' or '/' (D30).
     *
     * @param uri the URI
     * @return the comparison key
     */
    public static String normaliseNamespace(String uri) {
        if (uri.endsWith("#") || uri.endsWith("/")) {
            return uri.substring(0, uri.length() - 1);
        }
        return uri;
    }

    /**
     * Parses a version path value (D49): a positive integer. Values beyond the integer range are valid but
     * cannot exist, so they are returned as {@link Long#MAX_VALUE}.
     *
     * @param value the raw value
     * @return the version, empty when it is not a positive integer
     */
    public static Optional<Long> parseVersion(String value) {
        if (value == null || !POSITIVE_DIGITS.matcher(value).matches()) {
            return Optional.empty();
        }
        BigInteger number = new BigInteger(value);
        if (number.signum() <= 0) {
            return Optional.empty();
        }
        if (number.bitLength() > 62) {
            return Optional.of(Long.MAX_VALUE);
        }
        return Optional.of(number.longValue());
    }
}
