package eu.simpl.vocabularymanager.web;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * The route table: one row per operation of the decided contract (openapi-decided.yaml). It is the
 * single source for the method check (405), the role rules (D16, D84), the media type checks (406,
 * 415) and the allowed query parameters (D21, D52). An own test compares it with the contract and
 * with the Spring MVC mappings.
 */
public final class ApiRoutes {

    public static final String BASE = "/v1";
    public static final String VIEWER = "GA_VOCABULARY_VIEWER";
    public static final String ADMIN = "GA_VOCABULARY_ADMIN";

    /** Which media type the response of an operation has. */
    public enum Produces {
        JSON,
        TURTLE,
        NONE
    }

    /** Which request body an operation takes. */
    public enum Consumes {
        NONE,
        JSON,
        MULTIPART
    }

    /** Who may call the operation. */
    public enum Access {
        PUBLIC,
        READ,
        WRITE
    }

    /** One operation. */
    public record Route(
            String operationId,
            String method,
            String template,
            Access access,
            Produces produces,
            Consumes consumes,
            Set<String> queryParameters) {

        public Pattern pattern() {
            return PATTERNS.computeIfAbsent(template, t -> Pattern.compile(t.replaceAll("\\{[^/]+}", "[^/]+")));
        }

        public boolean matches(String path) {
            return pattern().matcher(path).matches();
        }

        /** The roles that may call the operation (empty for public operations). */
        public List<String> roles() {
            return switch (access) {
                case PUBLIC -> List.of();
                case READ -> List.of(VIEWER, ADMIN);
                case WRITE -> List.of(ADMIN);
            };
        }
    }

    private static final Map<String, Pattern> PATTERNS = new ConcurrentHashMap<>();

    private static final Set<String> PAGING = Set.of("page", "pageSize");

    private static final List<Route> ROUTES = List.of(
            new Route("getHealth", "GET", "/health", Access.PUBLIC, Produces.JSON, Consumes.NONE, Set.of()),
            new Route(
                    "getVocabularies",
                    "GET",
                    "/vocabularies",
                    Access.READ,
                    Produces.JSON,
                    Consumes.NONE,
                    Set.of("name", "status", "creationDate", "lastUpdated", "sort", "page", "pageSize")),
            new Route(
                    "createVocabulary",
                    "POST",
                    "/vocabularies",
                    Access.WRITE,
                    Produces.JSON,
                    Consumes.MULTIPART,
                    Set.of()),
            new Route(
                    "searchVocabularies",
                    "POST",
                    "/vocabularies/searches",
                    Access.READ,
                    Produces.JSON,
                    Consumes.JSON,
                    Set.of()),
            new Route(
                    "getVocabulary",
                    "GET",
                    "/vocabularies/{vocabularyName}",
                    Access.READ,
                    Produces.JSON,
                    Consumes.NONE,
                    Set.of()),
            new Route(
                    "updateVocabularyStatus",
                    "PATCH",
                    "/vocabularies/{vocabularyName}",
                    Access.WRITE,
                    Produces.JSON,
                    Consumes.JSON,
                    Set.of()),
            new Route(
                    "getVocabularyVersions",
                    "GET",
                    "/vocabularies/{vocabularyName}/versions",
                    Access.READ,
                    Produces.JSON,
                    Consumes.NONE,
                    Set.of("sort", "page", "pageSize")),
            new Route(
                    "createVocabularyVersion",
                    "POST",
                    "/vocabularies/{vocabularyName}/versions",
                    Access.WRITE,
                    Produces.JSON,
                    Consumes.MULTIPART,
                    Set.of()),
            new Route(
                    "getVocabularyVersion",
                    "GET",
                    "/vocabularies/{vocabularyName}/versions/{version}",
                    Access.READ,
                    Produces.JSON,
                    Consumes.NONE,
                    Set.of()),
            new Route(
                    "updateVocabularyVersionStatus",
                    "PATCH",
                    "/vocabularies/{vocabularyName}/versions/{version}",
                    Access.WRITE,
                    Produces.JSON,
                    Consumes.JSON,
                    Set.of()),
            new Route(
                    "downloadVocabulary",
                    "GET",
                    "/content/{vocabularyName}",
                    Access.READ,
                    Produces.TURTLE,
                    Consumes.NONE,
                    Set.of()),
            new Route(
                    "downloadVocabularyVersion",
                    "GET",
                    "/content/{vocabularyName}/versions/{version}",
                    Access.READ,
                    Produces.TURTLE,
                    Consumes.NONE,
                    Set.of()),
            new Route(
                    "getExternalVocabularies",
                    "GET",
                    "/external-vocabularies",
                    Access.READ,
                    Produces.JSON,
                    Consumes.NONE,
                    PAGING),
            new Route(
                    "createExternalVocabulary",
                    "POST",
                    "/external-vocabularies",
                    Access.WRITE,
                    Produces.JSON,
                    Consumes.MULTIPART,
                    Set.of()),
            new Route(
                    "searchExternalVocabularies",
                    "POST",
                    "/external-vocabulary/searches",
                    Access.READ,
                    Produces.JSON,
                    Consumes.JSON,
                    Set.of()),
            new Route(
                    "downloadExternalVocabularyContent",
                    "POST",
                    "/external-vocabularies/content",
                    Access.READ,
                    Produces.TURTLE,
                    Consumes.JSON,
                    Set.of()),
            new Route(
                    "getExternalVocabularyContentByName",
                    "GET",
                    "/external-vocabularies/{name}/content",
                    Access.READ,
                    Produces.TURTLE,
                    Consumes.NONE,
                    Set.of()),
            new Route(
                    "getExternalVocabulary",
                    "GET",
                    "/external-vocabularies/{name}",
                    Access.READ,
                    Produces.JSON,
                    Consumes.NONE,
                    Set.of()),
            new Route(
                    "deleteExternalVocabulary",
                    "DELETE",
                    "/external-vocabularies/{name}",
                    Access.WRITE,
                    Produces.NONE,
                    Consumes.NONE,
                    Set.of()));

    private ApiRoutes() {}

    public static List<Route> all() {
        return ROUTES;
    }

    /** Strips the API base; returns empty when the path is outside the API. */
    public static Optional<String> relative(String requestUri) {
        if (requestUri == null || !requestUri.startsWith(BASE + "/")) {
            return Optional.empty();
        }
        return Optional.of(requestUri.substring(BASE.length()));
    }

    /**
     * All routes whose template matches the path. Fixed segments win over parameters (D77): when a
     * route with fewer parameters matches, the parameter routes of the same shape are left out.
     */
    public static List<Route> matching(String path) {
        List<Route> found = new ArrayList<>();
        for (Route route : ROUTES) {
            if (route.matches(path)) {
                found.add(route);
            }
        }
        int fewestParameters =
                found.stream().mapToInt(r -> parameterCount(r.template())).min().orElse(0);
        found.removeIf(r -> parameterCount(r.template()) > fewestParameters);
        return found;
    }

    /** The route for a method and path, if declared. */
    public static Optional<Route> find(String method, String path) {
        return matching(path).stream().filter(r -> r.method().equals(method)).findFirst();
    }

    /** The Allow header value for a path. */
    public static String allow(List<Route> routes) {
        Set<String> methods = new LinkedHashSet<>();
        routes.forEach(r -> methods.add(r.method()));
        return String.join(", ", methods);
    }

    private static int parameterCount(String template) {
        return (int) template.chars().filter(c -> c == '{').count();
    }
}
