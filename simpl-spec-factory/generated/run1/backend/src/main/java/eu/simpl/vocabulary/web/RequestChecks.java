package eu.simpl.vocabulary.web;

import eu.simpl.vocabulary.problem.ApiException;
import eu.simpl.vocabulary.problem.ProblemIssue;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.MultiValueMap;

/** Request validation helpers that produce Problem issues in the OpenAPI format. */
public final class RequestChecks {

    public static final Pattern NAME = Pattern.compile("^[A-Z][a-zA-Z0-9]*$");
    public static final String NAME_RULE = "must be PascalCase alphanumeric (pattern ^[A-Z][a-zA-Z0-9]*$), "
            + "3 to 64 characters long";

    private RequestChecks() {}

    public static boolean validName(String value) {
        return value != null && value.length() >= 3 && value.length() <= 64 && NAME.matcher(value).matches();
    }

    /** Validates a vocabulary name path parameter; throws 400 if invalid. */
    public static void requireName(String value, String paramName) {
        if (!validName(value)) {
            throw ApiException.badRequest(ProblemIssue.schemaViolation("path", paramName, value,
                    "Path parameter '" + paramName + "' " + NAME_RULE + "."));
        }
    }

    /** Parses a version path parameter (integer, minimum 1); throws 400 if invalid. */
    public static int requireVersion(String value) {
        try {
            int v = Integer.parseInt(value);
            if (v >= 1) {
                return v;
            }
        } catch (NumberFormatException ignored) {
            // fall through
        }
        throw ApiException.badRequest(ProblemIssue.schemaViolation("path", "version", value,
                "Path parameter 'version' must be an integer greater than or equal to 1."));
    }

    /** Rejects query parameters that the operation does not define (SIMPL-18164: unsupported filter or sort). */
    public static void onlyAllowed(MultiValueMap<String, String> query, Set<String> allowed, List<ProblemIssue> out) {
        for (String key : query.keySet()) {
            if (!allowed.contains(key)) {
                out.add(ProblemIssue.schemaViolation("query", key, query.getFirst(key),
                        "Query parameter '" + key + "' is not supported. Supported parameters: "
                                + String.join(", ", allowed.stream().sorted().toList()) + "."));
            }
        }
    }

    /** Paging parameters after validation. */
    public record Paging(int page, int pageSize) {}

    public static Paging paging(MultiValueMap<String, String> query, int maxPageSize, List<ProblemIssue> out) {
        int page = intParam(query, "page", 1, 1, Integer.MAX_VALUE, out);
        int pageSize = intParam(query, "pageSize", 20, 1, maxPageSize, out);
        return new Paging(page, pageSize);
    }

    private static int intParam(MultiValueMap<String, String> query, String name, int def, int min, int max,
                                List<ProblemIssue> out) {
        String raw = query.getFirst(name);
        if (raw == null) {
            return def;
        }
        try {
            int v = Integer.parseInt(raw.trim());
            if (v >= min && v <= max) {
                return v;
            }
        } catch (NumberFormatException ignored) {
            // fall through
        }
        String range = max == Integer.MAX_VALUE ? "greater than or equal to " + min : "between " + min + " and " + max;
        out.add(ProblemIssue.schemaViolation("query", name, raw,
                "Query parameter '" + name + "' must be an integer " + range + "."));
        return def;
    }

    public static void throwIfAny(List<ProblemIssue> issues) {
        if (!issues.isEmpty()) {
            throw ApiException.badRequest(issues);
        }
    }

    /** Applies paging to a sorted list and builds the CollectionPage links. */
    public static <S, T> Dtos.PageResponse<T> page(List<S> all, Paging paging, String path,
                                                    Map<String, String> extraParams, Function<S, T> mapper) {
        int total = all.size();
        int totalPages = (int) Math.ceil(total / (double) paging.pageSize());
        int from = Math.min((paging.page() - 1) * paging.pageSize(), total);
        int to = Math.min(from + paging.pageSize(), total);
        List<T> items = new ArrayList<>();
        for (S s : all.subList(from, to)) {
            items.add(mapper.apply(s));
        }
        Function<Integer, String> link = p -> link(path, p, paging.pageSize(), extraParams);
        return new Dtos.PageResponse<>(paging.page(), totalPages, total, paging.pageSize(),
                link.apply(paging.page()), link.apply(1),
                paging.page() > 1 ? link.apply(Math.min(paging.page() - 1, Math.max(totalPages, 1))) : null,
                paging.page() < totalPages ? link.apply(paging.page() + 1) : null,
                totalPages > 0 ? link.apply(totalPages) : null,
                items);
    }

    private static String link(String path, int page, int pageSize, Map<String, String> extra) {
        StringBuilder sb = new StringBuilder(path).append("?page=").append(page).append("&pageSize=").append(pageSize);
        Map<String, String> ordered = new LinkedHashMap<>(extra);
        ordered.forEach((k, v) -> {
            if (v != null) {
                sb.append('&').append(k).append('=').append(URLEncoder.encode(v, StandardCharsets.UTF_8));
            }
        });
        return sb.toString();
    }

    public static boolean isAbsoluteUri(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            URI uri = new URI(value.trim());
            return uri.isAbsolute();
        } catch (Exception e) {
            return false;
        }
    }

    /** Name of the authenticated user, stored as createdBy: preferred_username, else email, else subject. */
    public static String user(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwt) {
            for (String claim : List.of("preferred_username", "email", "sub")) {
                String v = jwt.getToken().getClaimAsString(claim);
                if (v != null && !v.isBlank()) {
                    return v;
                }
            }
        }
        return auth == null ? "unknown" : auth.getName();
    }
}
