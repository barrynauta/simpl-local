package eu.simpl.vocabularymanager.web.routing;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.web.util.UriUtils;

/**
 * The query parameters of a request, decoded, in request order. Parsed from the raw query string so a multipart body
 * is never read for it.
 */
public final class QueryParameters {

    private final Map<String, List<String>> values;

    private QueryParameters(Map<String, List<String>> values) {
        this.values = values;
    }

    /**
     * @param queryString the raw query string, or null
     * @return the parameters
     * @throws IllegalArgumentException when the percent-encoding is malformed
     */
    public static QueryParameters parse(String queryString) {
        Map<String, List<String>> values = new LinkedHashMap<>();
        if (queryString == null || queryString.isEmpty()) {
            return new QueryParameters(values);
        }
        for (String pair : queryString.split("&", -1)) {
            if (pair.isEmpty()) {
                continue;
            }
            int equals = pair.indexOf('=');
            String rawName = equals < 0 ? pair : pair.substring(0, equals);
            String rawValue = equals < 0 ? "" : pair.substring(equals + 1);
            String name = UriUtils.decode(rawName.replace('+', ' '), StandardCharsets.UTF_8);
            String value = UriUtils.decode(rawValue.replace('+', ' '), StandardCharsets.UTF_8);
            values.computeIfAbsent(name, key -> new ArrayList<>()).add(value);
        }
        return new QueryParameters(values);
    }

    /**
     * @return the parameter names, in request order
     */
    public Set<String> names() {
        return values.keySet();
    }

    /**
     * @param name a parameter name
     * @return its values, empty when absent
     */
    public List<String> values(String name) {
        return values.getOrDefault(name, List.of());
    }

    /**
     * @param name a parameter name
     * @return its first value, or null when absent
     */
    public String first(String name) {
        List<String> list = values.get(name);
        return list == null || list.isEmpty() ? null : list.get(0);
    }
}
