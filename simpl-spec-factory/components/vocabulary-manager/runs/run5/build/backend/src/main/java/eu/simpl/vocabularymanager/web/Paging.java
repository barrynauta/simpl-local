package eu.simpl.vocabularymanager.web;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * page and pageSize (D22): page at least 1, default 1; pageSize 1 to 100, default 20.
 *
 * @param page page number
 * @param pageSize page size
 */
public record Paging(int page, int pageSize) {

    private static final Pattern DIGITS = Pattern.compile("^[0-9]{1,9}$");

    /**
     * Reads page and pageSize from the query, adding an issue for each invalid value.
     *
     * @param params query parameters
     * @param issues receives issues
     * @return the paging, with defaults for invalid or absent values
     */
    public static Paging read(Map<String, List<String>> params, List<Issue> issues) {
        int page = number(params, "page", 1, 1, Integer.MAX_VALUE, issues);
        int pageSize = number(params, "pageSize", 20, 1, 100, issues);
        return new Paging(page, pageSize);
    }

    private static int number(
            Map<String, List<String>> params, String name, int fallback, int min, int max, List<Issue> issues) {
        List<String> values = params.get(name);
        if (values == null || values.size() != 1) {
            return fallback;
        }
        String value = values.get(0);
        if (!DIGITS.matcher(value).matches()) {
            issues.add(
                    Issue.schemaViolation("query", name, "Query parameter '" + name + "' must be an integer.", value));
            return fallback;
        }
        int number = Integer.parseInt(value);
        if (number < min || number > max) {
            issues.add(Issue.schemaViolation(
                    "query",
                    name,
                    "Query parameter '" + name + "' must be between " + min + " and " + max + ".",
                    value));
            return fallback;
        }
        return number;
    }
}
