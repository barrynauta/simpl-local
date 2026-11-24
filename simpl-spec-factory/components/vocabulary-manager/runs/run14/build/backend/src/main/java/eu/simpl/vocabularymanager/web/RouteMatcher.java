package eu.simpl.vocabularymanager.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Matches a raw request path against the operation table. A fixed segment wins over a path parameter (D77), so
 * /vocabularies/searches is never read as a vocabulary name.
 */
@Component
public class RouteMatcher {

    /**
     * The result of a match.
     *
     * @param template the declared path template, or null for an unknown path
     * @param operation the operation for the method, or null when the path does not declare the method
     * @param allowedMethods the methods the path declares
     * @param pathValues raw (still percent-encoded) path parameter values
     */
    public record Match(
            String template, Operation operation, Set<String> allowedMethods, Map<String, String> pathValues) {

        public boolean pathDeclared() {
            return template != null;
        }
    }

    /**
     * Matches the raw path.
     *
     * @param method the HTTP method
     * @param rawPath the raw request URI path, without query
     * @return the match, never null
     */
    public Match match(String method, String rawPath) {
        if (!rawPath.startsWith("/") || rawPath.endsWith("/")) {
            return new Match(null, null, Set.of(), Map.of());
        }
        String[] segments = rawPath.substring(1).split("/", -1);
        String bestTemplate = null;
        int bestScore = -1;
        for (Operation operation : Operation.values()) {
            int score = score(operation.segments(), segments);
            if (score > bestScore) {
                bestScore = score;
                bestTemplate = operation.path();
            }
        }
        if (bestTemplate == null) {
            return new Match(null, null, Set.of(), Map.of());
        }
        Set<String> allowed = new LinkedHashSet<>();
        Operation chosen = null;
        Map<String, String> values = new LinkedHashMap<>();
        for (Operation operation : Operation.values()) {
            if (operation.path().equals(bestTemplate)) {
                allowed.add(operation.method());
                if (operation.method().equals(method)) {
                    chosen = operation;
                }
                if (values.isEmpty()) {
                    values.putAll(extract(operation.segments(), segments));
                }
            }
        }
        return new Match(bestTemplate, chosen, allowed, values);
    }

    /** All declared templates with their methods, used by the Allow header and the build tests. */
    public Map<String, List<String>> declaredPaths() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Operation operation : Operation.values()) {
            result.computeIfAbsent(operation.path(), key -> new ArrayList<>()).add(operation.method());
        }
        return result;
    }

    private static int score(List<String> template, String[] segments) {
        if (template.size() != segments.length) {
            return -1;
        }
        int score = 0;
        for (int i = 0; i < segments.length; i++) {
            String expected = template.get(i);
            boolean parameter = expected.startsWith("{");
            if (segments[i].isEmpty()) {
                return -1;
            }
            if (!parameter && !expected.equals(segments[i])) {
                return -1;
            }
            // Fixed segments weigh more the earlier they are, so a fixed segment always beats a parameter.
            score = score * 2 + (parameter ? 0 : 1);
        }
        return score;
    }

    private static Map<String, String> extract(List<String> template, String[] segments) {
        Map<String, String> values = new LinkedHashMap<>();
        for (int i = 0; i < segments.length; i++) {
            String expected = template.get(i);
            if (expected.startsWith("{")) {
                values.put(expected.substring(1, expected.length() - 1), segments[i]);
            }
        }
        return values;
    }
}
