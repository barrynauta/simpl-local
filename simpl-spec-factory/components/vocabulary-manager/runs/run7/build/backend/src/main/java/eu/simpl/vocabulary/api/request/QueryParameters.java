package eu.simpl.vocabulary.api.request;

import eu.simpl.vocabulary.api.problem.Issue;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The query string of a request, parsed in order. Unknown, repeated and malformed parameters become
 * issues (D21, D52); the paging links keep the parameters in request order (D78).
 */
public final class QueryParameters {

    /**
     * One parameter.
     *
     * @param name decoded name
     * @param value decoded value
     */
    public record Parameter(String name, String value) {}

    private final List<Parameter> parameters;
    private final List<Issue> malformed;

    private QueryParameters(List<Parameter> parameters, List<Issue> malformed) {
        this.parameters = List.copyOf(parameters);
        this.malformed = List.copyOf(malformed);
    }

    /**
     * Parses a raw query string.
     *
     * @param rawQuery the query string, may be null
     * @return the parsed parameters
     */
    public static QueryParameters parse(String rawQuery) {
        List<Parameter> parameters = new ArrayList<>();
        List<Issue> malformed = new ArrayList<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return new QueryParameters(parameters, malformed);
        }
        for (String pair : rawQuery.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int equals = pair.indexOf('=');
            String rawName = equals < 0 ? pair : pair.substring(0, equals);
            String rawValue = equals < 0 ? "" : pair.substring(equals + 1);
            Optional<String> name = decode(rawName);
            Optional<String> value = decode(rawValue);
            if (name.isEmpty()) {
                malformed.add(
                        Issue.schemaViolation(Issue.IN_QUERY, rawName, "Query parameter name is not well encoded."));
            } else if (value.isEmpty()) {
                malformed.add(Issue.schemaViolation(
                        Issue.IN_QUERY, name.get(), "Query parameter '" + name.get() + "' is not well encoded."));
            } else {
                parameters.add(new Parameter(name.get(), value.get()));
            }
        }
        return new QueryParameters(parameters, malformed);
    }

    private static Optional<String> decode(String raw) {
        try {
            return Optional.of(URLDecoder.decode(raw, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Issues for an operation that takes no query parameters (POST, PATCH, DELETE and some GETs).
     *
     * @return one issue per parameter name
     */
    public List<Issue> requireNone() {
        return allowOnly(Set.of());
    }

    /**
     * Issues for unknown, repeated and malformed parameters.
     *
     * @param allowed the names the operation defines
     * @return the issues, empty when the query is acceptable
     */
    public List<Issue> allowOnly(Set<String> allowed) {
        List<Issue> issues = new ArrayList<>(malformed);
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Parameter parameter : parameters) {
            counts.merge(parameter.name(), 1, Integer::sum);
        }
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            String name = entry.getKey();
            if (!allowed.contains(name)) {
                issues.add(Issue.schemaViolation(
                        Issue.IN_QUERY, name, "Query parameter '" + name + "' is not supported."));
            } else if (entry.getValue() > 1) {
                issues.add(Issue.schemaViolation(
                        Issue.IN_QUERY, name, "Query parameter '" + name + "' is given more than once."));
            }
        }
        return issues;
    }

    /**
     * The value of a parameter.
     *
     * @param name the name
     * @return the first value, if present
     */
    public Optional<String> get(String name) {
        return parameters.stream()
                .filter(p -> p.name().equals(name))
                .map(Parameter::value)
                .findFirst();
    }

    /** The distinct names in request order. */
    public Set<String> names() {
        Set<String> names = new LinkedHashSet<>();
        parameters.forEach(p -> names.add(p.name()));
        return names;
    }

    public List<Parameter> parameters() {
        return parameters;
    }
}
