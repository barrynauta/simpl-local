package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.error.ProblemType;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The query string of a request, parsed in request order. Unknown parameters (D21, D52) and
 * repeated parameters (D52, D138) give 400; bad encoding gives 400 badRequest (D158).
 */
public final class QueryParams {

    private final Map<String, String> values;

    private QueryParams(Map<String, String> values) {
        this.values = values;
    }

    /** Parses and checks the names against {@code allowed}. */
    public static QueryParams parse(String rawQuery, Set<String> allowed) {
        Map<String, String> values = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return new QueryParams(values);
        }
        List<Issue> issues = new ArrayList<>();
        List<String> repeated = new ArrayList<>();
        // RFC 3986: '+' in a query is a plus sign, not a space (date-time offsets, D20).
        for (String pair : rawQuery.replace("+", "%2B").split("&", -1)) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String name;
            String value;
            try {
                name = URLDecoder.decode(eq < 0 ? pair : pair.substring(0, eq), StandardCharsets.UTF_8);
                value = eq < 0 ? "" : URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                throw new ApiException(ProblemType.BAD_REQUEST, "The query string is not correctly encoded.");
            }
            if (values.containsKey(name)) {
                if (!repeated.contains(name)) {
                    repeated.add(name);
                }
                continue;
            }
            values.put(name, value);
        }
        for (String name : values.keySet()) {
            if (!allowed.contains(name)) {
                issues.add(
                        Issue.schemaViolation("query", name, "Query parameter '" + name + "' is not supported.", null));
            }
        }
        for (String name : repeated) {
            if (allowed.contains(name)) {
                issues.add(Issue.schemaViolation(
                        "query", name, "Query parameter '" + name + "' may be given only once.", null));
            }
        }
        if (!issues.isEmpty()) {
            throw ApiException.badRequest(issues);
        }
        return new QueryParams(values);
    }

    public String get(String name) {
        return values.get(name);
    }

    /** The parameters in request order. */
    public Map<String, String> all() {
        return values;
    }

    /**
     * A positive 32-bit integer written as digits only (D22, D91, D112).
     *
     * @return the value or {@code dflt} when absent
     */
    public int positiveInt(String name, int dflt, int max, List<Issue> issues) {
        String v = values.get(name);
        if (v == null) {
            return dflt;
        }
        Integer parsed = Validation.positiveInt(v);
        if (parsed == null || parsed > max) {
            issues.add(Issue.schemaViolation(
                    "query", name, "Query parameter '" + name + "' must be an integer from 1 to " + max + ".", v));
            return dflt;
        }
        return parsed;
    }
}
