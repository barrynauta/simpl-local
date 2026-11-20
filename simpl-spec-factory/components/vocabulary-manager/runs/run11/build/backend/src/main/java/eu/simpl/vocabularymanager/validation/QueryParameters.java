package eu.simpl.vocabularymanager.validation;

import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.Issue;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The query string of a request, read strictly (D21, D52, D138): unknown parameters, repeated
 * parameters and parameters on operations that take none give 400. Keeps the request order for the
 * paging links (D110).
 */
public final class QueryParameters {

    private final Map<String, String> values;

    private QueryParameters(Map<String, String> values) {
        this.values = values;
    }

    /**
     * Parses and checks the raw query string.
     *
     * @param allowed the parameters the operation declares
     * @throws ApiException 400 with one issue per unknown or repeated parameter
     */
    public static QueryParameters parse(String rawQuery, Set<String> allowed) {
        Map<String, String> values = new LinkedHashMap<>();
        List<Issue> issues = new ArrayList<>();
        List<String> repeatedReported = new ArrayList<>();
        if (rawQuery != null && !rawQuery.isEmpty()) {
            for (String pair : rawQuery.split("&", -1)) {
                if (pair.isEmpty()) {
                    continue;
                }
                int eq = pair.indexOf('=');
                String name = decode(eq < 0 ? pair : pair.substring(0, eq), issues);
                String value = eq < 0 ? "" : decode(pair.substring(eq + 1), issues);
                if (name == null || value == null) {
                    continue;
                }
                if (!allowed.contains(name)) {
                    if (!repeatedReported.contains(name)) {
                        issues.add(Issue.schema("query", name, "Query parameter '" + name + "' is not supported."));
                        repeatedReported.add(name);
                    }
                } else if (values.containsKey(name)) {
                    if (!repeatedReported.contains(name)) {
                        issues.add(
                                Issue.schema("query", name, "Query parameter '" + name + "' is given more than once."));
                        repeatedReported.add(name);
                    }
                } else {
                    values.put(name, value);
                }
            }
        }
        if (!issues.isEmpty()) {
            throw ApiException.badRequest(issues);
        }
        return new QueryParameters(values);
    }

    public String get(String name) {
        return values.get(name);
    }

    /** The parameters in request order. */
    public Map<String, String> inOrder() {
        return values;
    }

    private static String decode(String raw, List<Issue> issues) {
        try {
            return URLDecoder.decode(raw, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            issues.add(Issue.schema("query", "", "The query string is not correctly encoded."));
            return null;
        }
    }
}
