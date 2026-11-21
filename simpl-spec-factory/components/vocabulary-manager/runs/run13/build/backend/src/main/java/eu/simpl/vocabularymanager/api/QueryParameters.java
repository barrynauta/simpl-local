package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.error.ProblemIssue;
import eu.simpl.vocabularymanager.error.Problems;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

/**
 * The query parameters of a request in request order, read with Spring's URI parser. Unknown and repeated
 * parameters are rejected (D21, D52, D138, D149).
 */
public final class QueryParameters {

    private final Map<String, String> values;

    private QueryParameters(Map<String, String> values) {
        this.values = values;
    }

    /**
     * Reads the query string.
     *
     * @param allowed the parameter names of the operation; any other name is an issue
     * @param issues collects one issue per unknown or repeated parameter
     */
    public static QueryParameters read(HttpServletRequest request, Set<String> allowed, List<ProblemIssue> issues) {
        Map<String, String> values = new LinkedHashMap<>();
        String raw = request.getQueryString();
        if (raw == null || raw.isEmpty()) {
            return new QueryParameters(values);
        }
        MultiValueMap<String, String> parsed;
        try {
            parsed = UriComponentsBuilder.newInstance().query(raw).build().getQueryParams();
        } catch (IllegalArgumentException e) {
            throw Problems.badRequest(ProblemIssue.schemaViolation("query", "query", "Malformed query string.", null));
        }
        for (Map.Entry<String, List<String>> entry : parsed.entrySet()) {
            String name = decode(entry.getKey(), issues);
            if (name == null) {
                continue;
            }
            List<String> raws = entry.getValue();
            if (!allowed.contains(name)) {
                issues.add(ProblemIssue.schemaViolation(
                        "query", name, "Query parameter '" + name + "' is not supported.", null));
            } else if (raws.size() > 1 || values.containsKey(name)) {
                issues.add(ProblemIssue.schemaViolation(
                        "query", name, "Query parameter '" + name + "' must be given only once.", null));
                values.remove(name);
            } else {
                String value = raws.get(0) == null ? "" : decode(raws.get(0), issues);
                if (value != null) {
                    values.put(name, value);
                }
            }
        }
        return new QueryParameters(values);
    }

    private static String decode(String text, List<ProblemIssue> issues) {
        try {
            return UriUtils.decode(text, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            issues.add(ProblemIssue.schemaViolation("query", "query", "Malformed query string.", null));
            return null;
        }
    }

    public String get(String name) {
        return values.get(name);
    }

    /** The parameters other than page and pageSize, in request order, for paging links (D78, D110). */
    public Map<String, String> others() {
        Map<String, String> others = new LinkedHashMap<>(values);
        others.remove("page");
        others.remove("pageSize");
        return others;
    }

    public List<String> names() {
        return new ArrayList<>(values.keySet());
    }
}
