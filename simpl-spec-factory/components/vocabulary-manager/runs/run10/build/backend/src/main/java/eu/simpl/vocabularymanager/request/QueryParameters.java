package eu.simpl.vocabularymanager.request;

import eu.simpl.vocabularymanager.error.Issue;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The query parameters of a request, parsed from the raw query string in request order. Unknown, repeated and
 * undecodable parameters become issues (D21, D52).
 */
public final class QueryParameters {

    private final Map<String, String> values = new LinkedHashMap<>();
    private final List<Issue> issues = new ArrayList<>();

    private QueryParameters() {}

    /** Parses the raw query and records issues for parameters not in the allowed set. */
    public static QueryParameters parse(String rawQuery, Set<String> allowed) {
        QueryParameters parameters = new QueryParameters();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return parameters;
        }
        List<String> seen = new ArrayList<>();
        for (String pair : rawQuery.split("&", -1)) {
            int equals = pair.indexOf('=');
            String rawName = equals < 0 ? pair : pair.substring(0, equals);
            String rawValue = equals < 0 ? "" : pair.substring(equals + 1);
            String name = decode(rawName);
            String value = decode(rawValue);
            if (name == null || value == null) {
                parameters.issues.add(Issue.schemaViolation(
                        "query", name == null ? rawName : name, "Query parameter is not correctly encoded.", null));
                continue;
            }
            if (!allowed.contains(name)) {
                parameters.issues.add(Issue.schemaViolation(
                        "query", name, "Query parameter '" + name + "' is not supported.", value));
                continue;
            }
            if (seen.contains(name)) {
                parameters.issues.add(Issue.schemaViolation(
                        "query", name, "Query parameter '" + name + "' may be given only once.", value));
                continue;
            }
            seen.add(name);
            parameters.values.put(name, value);
        }
        return parameters;
    }

    public String get(String name) {
        return values.get(name);
    }

    public boolean has(String name) {
        return values.containsKey(name);
    }

    /** All accepted parameters in request order. */
    public Map<String, String> inOrder() {
        return values;
    }

    public List<Issue> issues() {
        return issues;
    }

    private static String decode(String text) {
        try {
            return URLDecoder.decode(text, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
