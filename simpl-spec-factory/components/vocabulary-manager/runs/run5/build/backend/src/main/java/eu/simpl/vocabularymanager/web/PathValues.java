package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.domain.VocabularyNames;
import java.util.ArrayList;
import java.util.List;

/** Checks path values (D49): names by the rules of D37, versions as positive integers. */
public final class PathValues {

    private PathValues() {}

    /** Adds an issue when the name breaks its pattern. */
    public static void checkName(String parameter, String value, List<Issue> issues) {
        if (!VocabularyNames.isValidName(value)) {
            issues.add(Issue.schemaViolation(
                    "path",
                    parameter,
                    "Path parameter '" + parameter + "' must have 3 to 64 characters and match ^[A-Z][a-zA-Z0-9]*$.",
                    value));
        }
    }

    /** Adds an issue when the version is not a positive integer; returns the number or -1. */
    public static int checkVersion(String value, List<Issue> issues) {
        if (!VocabularyNames.isValidVersion(value)) {
            issues.add(Issue.schemaViolation(
                    "path", "version", "Path parameter 'version' must be a positive integer.", value));
            return -1;
        }
        return Integer.parseInt(value);
    }

    /** Fails with 400 when there are issues. */
    public static void failOn(List<Issue> issues) {
        if (!issues.isEmpty()) {
            throw ApiException.badRequest(new ArrayList<>(issues));
        }
    }
}
