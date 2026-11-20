package eu.simpl.vocabularymanager.api;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;

/**
 * Request checks in the decided order (D51, D63): Accept (406), Content-Type (415), then the request shape (400).
 * The checks follow docs/openapi-decided.yaml; ContractConformanceTest compares the constants with that file.
 */
public final class RequestChecks {

    /** Vocabulary and external vocabulary name pattern (D37). */
    public static final Pattern NAME = Pattern.compile("^[A-Z][a-zA-Z0-9]*$");

    /** Minimum name length (D37). */
    public static final int NAME_MIN = 3;

    /** Maximum name length (D37). */
    public static final int NAME_MAX = 64;

    /** Largest integer of D91. */
    public static final long INT32_MAX = Integer.MAX_VALUE;

    private static final List<MediaType> JSON_ACCEPTABLE =
            List.of(MediaType.APPLICATION_JSON, MediaType.APPLICATION_PROBLEM_JSON);
    private static final MediaType TURTLE = MediaType.parseMediaType("text/turtle");

    private RequestChecks() {}

    /** 406 unless the client accepts JSON (D63). */
    public static void acceptJson(HttpServletRequest request) {
        accept(request, JSON_ACCEPTABLE);
    }

    /** 406 unless the client accepts Turtle (D63). */
    public static void acceptTurtle(HttpServletRequest request) {
        accept(request, List.of(TURTLE));
    }

    private static void accept(HttpServletRequest request, List<MediaType> offered) {
        Enumeration<String> values = request.getHeaders("Accept");
        List<String> headers = new ArrayList<>();
        while (values != null && values.hasMoreElements()) {
            headers.add(values.nextElement());
        }
        String joined = String.join(",", headers).trim();
        if (joined.isEmpty()) {
            return;
        }
        List<MediaType> accepted;
        try {
            accepted = MediaType.parseMediaTypes(joined);
        } catch (IllegalArgumentException ex) {
            throw new ProblemException(Problems.notAcceptable());
        }
        for (MediaType range : accepted) {
            if (range.getQualityValue() <= 0.0) {
                continue;
            }
            for (MediaType candidate : offered) {
                if (range.includes(candidate)) {
                    return;
                }
            }
        }
        throw new ProblemException(Problems.notAcceptable());
    }

    /** 415 unless the request body is multipart/form-data (D15). */
    public static void contentTypeMultipart(HttpServletRequest request) {
        contentType(request, MediaType.MULTIPART_FORM_DATA);
    }

    /** 415 unless the request body is application/json (D15). */
    public static void contentTypeJson(HttpServletRequest request) {
        contentType(request, MediaType.APPLICATION_JSON);
    }

    private static void contentType(HttpServletRequest request, MediaType expected) {
        String header = request.getContentType();
        if (header == null || header.isBlank()) {
            throw new ProblemException(Problems.unsupportedMediaType());
        }
        try {
            MediaType actual = MediaType.parseMediaType(header);
            if (!actual.getType().equalsIgnoreCase(expected.getType())
                    || !actual.getSubtype().equalsIgnoreCase(expected.getSubtype())) {
                throw new ProblemException(Problems.unsupportedMediaType());
            }
        } catch (InvalidMediaTypeException ex) {
            throw new ProblemException(Problems.unsupportedMediaType());
        }
    }

    /**
     * Parses the query string: a repeated, malformed or unknown parameter gives 400 (D21, D52).
     *
     * @param request the request
     * @param allowed the parameters the operation declares
     * @return the parameters in request order
     */
    public static Map<String, String> query(HttpServletRequest request, Set<String> allowed) {
        List<Issue> issues = new ArrayList<>();
        Map<String, String> parameters = query(request, allowed, issues);
        if (!issues.isEmpty()) {
            throw new ProblemException(Problems.badRequest(issues));
        }
        return parameters;
    }

    /**
     * Parses the query string and collects issues instead of throwing.
     *
     * @param request the request
     * @param allowed the parameters the operation declares
     * @param issues receives one issue per bad parameter
     * @return the valid parameters in request order
     */
    public static Map<String, String> query(HttpServletRequest request, Set<String> allowed, List<Issue> issues) {
        Map<String, String> parameters = new LinkedHashMap<>();
        String raw = request.getQueryString();
        if (raw == null || raw.isEmpty()) {
            return parameters;
        }
        for (String pair : raw.split("&", -1)) {
            if (pair.isEmpty()) {
                continue;
            }
            int equals = pair.indexOf('=');
            String rawName = equals < 0 ? pair : pair.substring(0, equals);
            String rawValue = equals < 0 ? "" : pair.substring(equals + 1);
            String name;
            String value;
            try {
                name = URLDecoder.decode(rawName, StandardCharsets.UTF_8);
                value = URLDecoder.decode(rawValue, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException ex) {
                issues.add(Issue.schemaViolation("query", rawName, "Query parameter is not correctly encoded.", null));
                continue;
            }
            if (!allowed.contains(name)) {
                issues.add(
                        Issue.schemaViolation("query", name, "Query parameter '" + name + "' is not allowed.", value));
            } else if (parameters.containsKey(name)) {
                issues.add(Issue.schemaViolation(
                        "query", name, "Query parameter '" + name + "' must not be repeated.", value));
            } else {
                parameters.put(name, value);
            }
        }
        return parameters;
    }

    /** Checks a name in a path (D49). */
    public static String pathName(String value, String parameter) {
        Issue issue = nameIssue(value, "path", parameter);
        if (issue != null) {
            throw Problems.badRequest(issue);
        }
        return value;
    }

    /** Checks a version number in a path (D49, D91). */
    public static int pathVersion(String value) {
        Integer version = positiveInt(value);
        if (version == null) {
            throw Problems.badRequest(Issue.schemaViolation(
                    "path", "version", "Path parameter 'version' must be an integer from 1 to 2147483647.", value));
        }
        return version;
    }

    /** A positive 32-bit integer written in decimal, or null. */
    public static Integer positiveInt(String value) {
        if (value == null || !value.matches("[0-9]{1,10}")) {
            return null;
        }
        long parsed = Long.parseLong(value);
        if (parsed < 1 || parsed > INT32_MAX) {
            return null;
        }
        return (int) parsed;
    }

    /** The issue for a name that breaks D37, or null. */
    public static Issue nameIssue(String value, String in, String parameter) {
        if (value == null
                || value.length() < NAME_MIN
                || value.length() > NAME_MAX
                || !NAME.matcher(value).matches()) {
            return Issue.schemaViolation(
                    in,
                    parameter,
                    "'" + parameter + "' must be 3 to 64 characters, start with an upper-case letter and contain only"
                            + " letters and digits.",
                    value);
        }
        return null;
    }
}
