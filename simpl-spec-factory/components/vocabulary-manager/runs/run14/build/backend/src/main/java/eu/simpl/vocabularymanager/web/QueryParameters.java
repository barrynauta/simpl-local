package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.error.ProblemType;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Strict query string of one request (D21, D52, D138, D168). The servlet container decodes '+' as a space, which
 * D168 forbids, so the raw query string is parsed here. Values keep their raw form for paging links (D78).
 */
public final class QueryParameters {

    private final Map<String, String> values = new LinkedHashMap<>();
    private final Map<String, String> rawValues = new LinkedHashMap<>();
    private final List<String> order = new ArrayList<>();

    private QueryParameters() {}

    /**
     * Parses the raw query string.
     *
     * @param rawQuery the raw query string, may be null
     * @return the parameters
     * @throws ApiException 400 when a parameter is repeated or badly encoded
     */
    public static QueryParameters parse(String rawQuery) {
        QueryParameters parameters = new QueryParameters();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return parameters;
        }
        List<Issue> issues = new ArrayList<>();
        for (String pair : rawQuery.split("&", -1)) {
            if (pair.isEmpty()) {
                continue;
            }
            int equals = pair.indexOf('=');
            String rawName = equals < 0 ? pair : pair.substring(0, equals);
            String rawValue = equals < 0 ? "" : pair.substring(equals + 1);
            String name = decode(rawName);
            String value = decode(rawValue);
            if (name == null || value == null) {
                throw new ApiException(ProblemType.BAD_REQUEST, null, List.of());
            }
            if (parameters.values.containsKey(name)) {
                if (issues.stream().noneMatch(issue -> issue.name().equals(name))) {
                    issues.add(Issue.schemaViolation("query", name, "Query parameter '" + name + "' is repeated."));
                }
                continue;
            }
            parameters.values.put(name, value);
            parameters.rawValues.put(name, rawValue);
            parameters.order.add(name);
        }
        if (!issues.isEmpty()) {
            throw ApiException.badRequest(issues);
        }
        return parameters;
    }

    /**
     * Rejects any parameter not in the allowed set (D21, D52).
     *
     * @param allowed the declared parameters of the operation
     * @throws ApiException 400 with one issue per unknown parameter
     */
    public void requireOnly(Set<String> allowed) {
        List<Issue> issues = new ArrayList<>();
        for (String name : order) {
            if (!allowed.contains(name)) {
                issues.add(Issue.schemaViolation("query", name, "Query parameter '" + name + "' is not supported."));
            }
        }
        if (!issues.isEmpty()) {
            throw ApiException.badRequest(issues);
        }
    }

    public String get(String name) {
        return values.get(name);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    /** Names in request order. */
    public List<String> names() {
        return List.copyOf(order);
    }

    /** The value as the client encoded it. */
    public String raw(String name) {
        return rawValues.get(name);
    }

    /**
     * Percent-decodes one URL component with '+' kept as a plus sign (RFC 3986, D168).
     *
     * @param raw the raw component
     * @return the decoded text, or null when the encoding is invalid or not UTF-8
     */
    public static String decode(String raw) {
        if (raw.indexOf('%') < 0) {
            return raw;
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(raw.length());
        int i = 0;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (c == '%') {
                if (i + 2 >= raw.length()) {
                    return null;
                }
                int high = Character.digit(raw.charAt(i + 1), 16);
                int low = Character.digit(raw.charAt(i + 2), 16);
                if (high < 0 || low < 0) {
                    return null;
                }
                bytes.write(high * 16 + low);
                i += 3;
            } else {
                byte[] encoded = String.valueOf(c).getBytes(StandardCharsets.UTF_8);
                bytes.write(encoded, 0, encoded.length);
                i++;
            }
        }
        try {
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes.toByteArray()))
                    .toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }
}
