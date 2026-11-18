package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.error.Problems;
import eu.simpl.vocabularymanager.service.PageRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The query parameters of one request, read from the query string only (so multipart fields never count). Unknown
 * parameters and invalid values give 400 (decisions D21, D22).
 */
public final class QueryParameters {

    private static final Pattern INTEGER = Pattern.compile("^[+-]?\\d{1,9}$");
    private final Map<String, List<String>> values;

    private QueryParameters(Map<String, List<String>> values) {
        this.values = values;
    }

    /**
     * Reads the query string and rejects parameters that are not allowed or given more than once.
     *
     * @throws eu.simpl.vocabularymanager.error.VocabularyManagerException 400
     */
    public static QueryParameters read(HttpServletRequest request, Set<String> allowed) {
        Map<String, List<String>> values = parse(request.getQueryString());
        List<Issue> issues = new ArrayList<>();
        values.forEach((name, list) -> {
            if (!allowed.contains(name)) {
                issues.add(Issue.schemaViolation(
                        "query", name, list.get(0), "Query parameter '" + name + "' is not supported."));
            } else if (list.size() > 1) {
                issues.add(Issue.schemaViolation(
                        "query", name, list.get(0), "Query parameter '" + name + "' must be given once."));
            }
        });
        if (!issues.isEmpty()) {
            throw Problems.badRequest(issues);
        }
        return new QueryParameters(values);
    }

    private static Map<String, List<String>> parse(String query) {
        Map<String, List<String>> values = new LinkedHashMap<>();
        if (query == null || query.isEmpty()) {
            return values;
        }
        for (String pair : query.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int equals = pair.indexOf('=');
            String name = decode(equals < 0 ? pair : pair.substring(0, equals));
            String value = equals < 0 ? "" : decode(pair.substring(equals + 1));
            values.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        }
        return values;
    }

    private static String decode(String text) {
        try {
            return URLDecoder.decode(text, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return text;
        }
    }

    /** The value, or null when absent. */
    public String get(String name) {
        List<String> list = values.get(name);
        return list == null ? null : list.get(0);
    }

    /** Reads page and pageSize: page from 1, pageSize 1 to 100, defaults 1 and 20 (decision D22). */
    public PageRequest pageRequest(List<Issue> issues) {
        int page = integer("page", 1, 1, Integer.MAX_VALUE, issues);
        int pageSize = integer("pageSize", 20, 1, 100, issues);
        return new PageRequest(page, pageSize);
    }

    private int integer(String name, int fallback, int min, int max, List<Issue> issues) {
        String value = get(name);
        if (value == null) {
            return fallback;
        }
        if (!INTEGER.matcher(value).matches()) {
            issues.add(
                    Issue.schemaViolation("query", name, value, "Query parameter '" + name + "' must be an integer."));
            return fallback;
        }
        int number = Integer.parseInt(value);
        if (number < min || number > max) {
            String range = max == Integer.MAX_VALUE ? "at least " + min : "between " + min + " and " + max;
            issues.add(Issue.schemaViolation(
                    "query", name, value, "Query parameter '" + name + "' must be " + range + "."));
            return fallback;
        }
        return number;
    }
}
