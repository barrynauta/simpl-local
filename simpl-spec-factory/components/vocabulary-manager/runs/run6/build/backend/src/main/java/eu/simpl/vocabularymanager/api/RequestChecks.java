package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.problem.Issue;
import eu.simpl.vocabularymanager.problem.Problems;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

/**
 * Request checks shared by the operations, in the order of decision D51: media types (406, 415) and request shape
 * (400) for query parameters, path values and field rules.
 */
public final class RequestChecks {

    /** Vocabulary and external vocabulary name pattern (decision D37). */
    public static final Pattern NAME = Pattern.compile("^[A-Z][a-zA-Z0-9]*$");

    private static final Pattern POSITIVE_INTEGER = Pattern.compile("^[0-9]+$");

    /** Longest description and changelog, in characters after trimming (decision D37). */
    public static final int MAX_TEXT = 1000;

    private RequestChecks() {}

    /**
     * Checks the Accept header against the media type the operation produces (406).
     *
     * @param request the request
     * @param produced the media type of a successful response
     * @throws eu.simpl.vocabularymanager.problem.ProblemException 406
     */
    public static void requireAcceptable(HttpServletRequest request, MediaType produced) {
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        if (accept == null || accept.isBlank()) {
            return;
        }
        List<MediaType> accepted;
        try {
            accepted = MediaType.parseMediaTypes(accept);
        } catch (InvalidMediaTypeException e) {
            return;
        }
        boolean ok = accepted.stream().anyMatch(m -> m.isCompatibleWith(produced) && m.getQualityValue() > 0);
        if (!ok) {
            throw Problems.notAcceptable();
        }
    }

    /**
     * Requires a multipart/form-data body (415, decision D15).
     *
     * @param request the request
     * @throws eu.simpl.vocabularymanager.problem.ProblemException 415
     */
    public static void requireMultipart(HttpServletRequest request) {
        if (!hasContentType(request, MediaType.MULTIPART_FORM_DATA)) {
            throw Problems.unsupportedMediaType();
        }
    }

    /**
     * Requires an application/json body (415, decision D15).
     *
     * @param request the request
     * @throws eu.simpl.vocabularymanager.problem.ProblemException 415
     */
    public static void requireJson(HttpServletRequest request) {
        if (!hasContentType(request, MediaType.APPLICATION_JSON)) {
            throw Problems.unsupportedMediaType();
        }
    }

    private static boolean hasContentType(HttpServletRequest request, MediaType wanted) {
        String contentType = request.getContentType();
        if (contentType == null) {
            return false;
        }
        try {
            MediaType given = MediaType.parseMediaType(contentType);
            return given.getType().equalsIgnoreCase(wanted.getType())
                    && given.getSubtype().equalsIgnoreCase(wanted.getSubtype());
        } catch (InvalidMediaTypeException e) {
            return false;
        }
    }

    /**
     * Parses the query string: names and decoded values, in order.
     *
     * @param request the request
     * @param issues where problems with the query string go
     * @return the parameters
     */
    public static MultiValueMap<String, String> queryParameters(HttpServletRequest request, List<Issue> issues) {
        String query = request.getQueryString();
        MultiValueMap<String, String> raw = UriComponentsBuilder.newInstance()
                .query(query == null ? "" : query)
                .build()
                .getQueryParams();
        MultiValueMap<String, String> decoded = new org.springframework.util.LinkedMultiValueMap<>();
        raw.forEach((name, values) -> {
            try {
                String decodedName = UriUtils.decode(name, StandardCharsets.UTF_8);
                for (String value : values) {
                    decoded.add(decodedName, value == null ? "" : UriUtils.decode(value, StandardCharsets.UTF_8));
                }
            } catch (IllegalArgumentException e) {
                issues.add(Issue.schemaViolation(Issue.IN_QUERY, name, "Query parameter is not correctly encoded."));
            }
        });
        return decoded;
    }

    /**
     * Adds an issue for every unknown or repeated query parameter (decisions D21, D52).
     *
     * @param parameters the parsed parameters
     * @param allowed the parameters the operation defines
     * @param issues the issue list
     */
    public static void checkQueryNames(
            MultiValueMap<String, String> parameters, Set<String> allowed, List<Issue> issues) {
        for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
            if (!allowed.contains(entry.getKey())) {
                issues.add(Issue.unknownInput(Issue.IN_QUERY, entry.getKey()));
            } else if (entry.getValue().size() > 1) {
                issues.add(Issue.schemaViolation(
                        Issue.IN_QUERY,
                        entry.getKey(),
                        "Query parameter '" + entry.getKey() + "' is given more than once."));
            }
        }
    }

    /**
     * Rejects any query parameter on POST, PATCH and DELETE (decision D52), throwing at once.
     *
     * @param request the request
     * @throws eu.simpl.vocabularymanager.problem.ProblemException 400
     */
    public static void requireNoQuery(HttpServletRequest request) {
        List<Issue> issues = new ArrayList<>();
        MultiValueMap<String, String> parameters = queryParameters(request, issues);
        checkQueryNames(parameters, Set.of(), issues);
        throwIfAny(issues);
    }

    /**
     * Checks a name path value against the name rules (decision D49).
     *
     * @param parameter the path parameter name
     * @param value the value
     * @param issues the issue list
     */
    public static void checkPathName(String parameter, String value, List<Issue> issues) {
        if (!validName(value)) {
            issues.add(Issue.schemaViolation(
                    Issue.IN_PATH,
                    parameter,
                    "Path parameter '" + parameter + "' must be 3 to 64 characters and match ^[A-Z][a-zA-Z0-9]*$."));
        }
    }

    /**
     * Checks a version path value: a positive integer (decision D49).
     *
     * @param value the value
     * @param issues the issue list
     */
    public static void checkPathVersion(String value, List<Issue> issues) {
        if (!POSITIVE_INTEGER.matcher(value).matches() || value.chars().allMatch(c -> c == '0')) {
            issues.add(Issue.schemaViolation(
                    Issue.IN_PATH, "version", "Path parameter 'version' must be a positive integer."));
        }
    }

    /**
     * Tells whether a name follows the rules of decision D37.
     *
     * @param value the name
     * @return true when valid
     */
    public static boolean validName(String value) {
        return value != null
                && value.length() >= 3
                && value.length() <= 64
                && NAME.matcher(value).matches();
    }

    /**
     * Checks a required text field and returns it trimmed (decision D37).
     *
     * @param field the field name
     * @param value the raw value, may be null
     * @param issues the issue list
     * @return the trimmed value, or null when invalid
     */
    public static String requiredText(String field, String value, List<Issue> issues) {
        if (value == null) {
            issues.add(Issue.schemaViolation(Issue.IN_BODY, field, "Field '" + field + "' is required."));
            return null;
        }
        String trimmed = value.strip();
        int length = trimmed.codePointCount(0, trimmed.length());
        if (length < 1 || length > MAX_TEXT) {
            issues.add(Issue.schemaViolation(
                    Issue.IN_BODY, field, "Field '" + field + "' must be 1 to " + MAX_TEXT + " characters."));
            return null;
        }
        return trimmed;
    }

    /**
     * Throws a 400 when issues were found.
     *
     * @param issues the issues
     * @throws eu.simpl.vocabularymanager.problem.ProblemException 400
     */
    public static void throwIfAny(List<Issue> issues) {
        if (!issues.isEmpty()) {
            throw Problems.badRequest(issues);
        }
    }
}
