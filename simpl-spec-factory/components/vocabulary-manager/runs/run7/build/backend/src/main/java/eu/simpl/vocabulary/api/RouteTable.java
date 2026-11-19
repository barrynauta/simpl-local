package eu.simpl.vocabulary.api;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The operations of the contract (vocabulary_openapi-tier1.yaml) as one table: path template, methods,
 * role, request and response media kinds. Filters use it for 405 (D67, D77), roles (D16), 406 (D63),
 * 415 (D15) and 413 (D76). Fixed segments win over path parameters (D77).
 */
public final class RouteTable {

    /** API prefix of every operation (E2). */
    public static final String API_PREFIX = "/v1";

    /** Access level of an operation (D16). */
    public enum Access {
        PUBLIC,
        READ,
        ADMIN
    }

    /** Request body kind. */
    public enum BodyKind {
        NONE,
        JSON,
        MULTIPART
    }

    /** Response body kind. */
    public enum ResponseKind {
        JSON,
        TURTLE,
        NONE
    }

    /**
     * One operation.
     *
     * @param method HTTP method
     * @param template path template below the prefix
     * @param access required access
     * @param body request body kind
     * @param response response kind
     */
    public record Operation(String method, String template, Access access, BodyKind body, ResponseKind response) {}

    /**
     * A matched path.
     *
     * @param template the template that matched
     * @param operations operations on the template, by method
     */
    public record PathMatch(String template, Map<String, Operation> operations) {

        public Optional<Operation> operation(String method) {
            return Optional.ofNullable(operations.get(method));
        }

        public String allowHeader() {
            return String.join(", ", operations.keySet());
        }
    }

    /** Methods the server knows; any other method gives 405 before anything else (D67). */
    public static final Set<String> KNOWN_METHODS =
            Set.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "TRACE", "CONNECT");

    private static final List<Operation> OPERATIONS = List.of(
            new Operation("GET", "/health", Access.PUBLIC, BodyKind.NONE, ResponseKind.JSON),
            new Operation("GET", "/vocabularies", Access.READ, BodyKind.NONE, ResponseKind.JSON),
            new Operation("POST", "/vocabularies", Access.ADMIN, BodyKind.MULTIPART, ResponseKind.JSON),
            new Operation("POST", "/vocabularies/searches", Access.READ, BodyKind.JSON, ResponseKind.JSON),
            new Operation("GET", "/vocabularies/{vocabularyName}", Access.READ, BodyKind.NONE, ResponseKind.JSON),
            new Operation("PATCH", "/vocabularies/{vocabularyName}", Access.ADMIN, BodyKind.JSON, ResponseKind.JSON),
            new Operation(
                    "GET", "/vocabularies/{vocabularyName}/versions", Access.READ, BodyKind.NONE, ResponseKind.JSON),
            new Operation(
                    "POST",
                    "/vocabularies/{vocabularyName}/versions",
                    Access.ADMIN,
                    BodyKind.MULTIPART,
                    ResponseKind.JSON),
            new Operation(
                    "GET",
                    "/vocabularies/{vocabularyName}/versions/{version}",
                    Access.READ,
                    BodyKind.NONE,
                    ResponseKind.JSON),
            new Operation(
                    "PATCH",
                    "/vocabularies/{vocabularyName}/versions/{version}",
                    Access.ADMIN,
                    BodyKind.JSON,
                    ResponseKind.JSON),
            new Operation("GET", "/content/{vocabularyName}", Access.READ, BodyKind.NONE, ResponseKind.TURTLE),
            new Operation(
                    "GET",
                    "/content/{vocabularyName}/versions/{version}",
                    Access.READ,
                    BodyKind.NONE,
                    ResponseKind.TURTLE),
            new Operation("GET", "/external-vocabularies", Access.READ, BodyKind.NONE, ResponseKind.JSON),
            new Operation("POST", "/external-vocabularies", Access.ADMIN, BodyKind.MULTIPART, ResponseKind.JSON),
            new Operation("POST", "/external-vocabulary/searches", Access.READ, BodyKind.JSON, ResponseKind.JSON),
            new Operation("POST", "/external-vocabularies/content", Access.READ, BodyKind.JSON, ResponseKind.TURTLE),
            new Operation(
                    "GET", "/external-vocabularies/{name}/content", Access.READ, BodyKind.NONE, ResponseKind.TURTLE),
            new Operation("GET", "/external-vocabularies/{name}", Access.READ, BodyKind.NONE, ResponseKind.JSON),
            new Operation("DELETE", "/external-vocabularies/{name}", Access.ADMIN, BodyKind.NONE, ResponseKind.NONE));

    private static final Map<String, Map<String, Operation>> BY_TEMPLATE = byTemplate();

    private RouteTable() {}

    private static Map<String, Map<String, Operation>> byTemplate() {
        Map<String, Map<String, Operation>> map = new LinkedHashMap<>();
        for (Operation operation : OPERATIONS) {
            map.computeIfAbsent(operation.template(), key -> new LinkedHashMap<>())
                    .put(operation.method(), operation);
        }
        return map;
    }

    /**
     * Finds the template that matches a raw request path (with the /v1 prefix).
     *
     * @param rawPath the request URI path, not decoded
     * @return the match, empty when no operation has this path
     */
    public static Optional<PathMatch> match(String rawPath) {
        if (rawPath == null || !rawPath.startsWith(API_PREFIX + "/")) {
            return Optional.empty();
        }
        List<String> segments = segments(rawPath.substring(API_PREFIX.length()));
        if (segments == null) {
            return Optional.empty();
        }
        PathMatch best = null;
        int bestFixed = -1;
        for (Map.Entry<String, Map<String, Operation>> entry : BY_TEMPLATE.entrySet()) {
            int fixed = fixedMatches(entry.getKey(), segments);
            if (fixed > bestFixed) {
                bestFixed = fixed;
                best = new PathMatch(entry.getKey(), entry.getValue());
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * Returns the number of fixed segments that matched, or -1 when the template does not match.
     */
    private static int fixedMatches(String template, List<String> segments) {
        String[] parts = template.substring(1).split("/");
        if (parts.length != segments.size()) {
            return -1;
        }
        int fixed = 0;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            String segment = segments.get(i);
            if (part.startsWith("{")) {
                if (segment.isEmpty()) {
                    return -1;
                }
            } else if (part.equals(segment)) {
                fixed++;
            } else {
                return -1;
            }
        }
        return fixed;
    }

    private static List<String> segments(String path) {
        if (!path.startsWith("/")) {
            return null;
        }
        String[] raw = path.substring(1).split("/", -1);
        List<String> result = new ArrayList<>(raw.length);
        for (String segment : raw) {
            try {
                result.add(URLDecoder.decode(segment.replace("+", "%2B"), StandardCharsets.UTF_8));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return result;
    }

    /** All operations, for documentation and tests. */
    public static List<Operation> operations() {
        return OPERATIONS;
    }
}
