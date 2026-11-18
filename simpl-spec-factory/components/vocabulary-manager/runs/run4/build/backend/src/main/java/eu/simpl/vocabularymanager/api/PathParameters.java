package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.error.Problems;
import java.util.regex.Pattern;

/** Checks of path parameters against the OpenAPI schemas. */
public final class PathParameters {

    public static final Pattern NAME = Pattern.compile("^[A-Z][a-zA-Z0-9]*$");
    private static final Pattern DIGITS = Pattern.compile("^\\d{1,9}$");

    private PathParameters() {}

    /** True when the value is a valid vocabulary name: 3 to 64 characters, ^[A-Z][a-zA-Z0-9]*$ (decision D37). */
    public static boolean isValidName(String value) {
        return value != null
                && value.length() >= 3
                && value.length() <= 64
                && NAME.matcher(value).matches();
    }

    /**
     * Checks a name in the path.
     *
     * @throws eu.simpl.vocabularymanager.error.VocabularyManagerException 400
     */
    public static String name(String parameter, String value) {
        if (!isValidName(value)) {
            throw Problems.badRequest(Issue.schemaViolation(
                    "path",
                    parameter,
                    value,
                    "Path parameter '" + parameter + "' must have 3 to 64 characters and match ^[A-Z][a-zA-Z0-9]*$."));
        }
        return value;
    }

    /**
     * Checks a version number in the path: an integer of at least 1.
     *
     * @throws eu.simpl.vocabularymanager.error.VocabularyManagerException 400
     */
    public static int version(String value) {
        if (value == null || !DIGITS.matcher(value).matches() || Integer.parseInt(value) < 1) {
            throw Problems.badRequest(Issue.schemaViolation(
                    "path", "version", value, "Path parameter 'version' must be an integer of at least 1."));
        }
        return Integer.parseInt(value);
    }
}
