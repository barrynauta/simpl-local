package eu.simpl.vocabularymanager.web;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Every operation of the decided contract (docs/openapi-decided.yaml) with an explicit role rule. Default deny: a
 * request that matches no route never reaches a handler with a role rule (D84); RouteTableTest checks that every
 * handler has a route.
 */
@Component
public class RouteTable {

    /** Role for write operations (D16). */
    public static final String ADMIN = "GA_VOCABULARY_ADMIN";

    /** Role for read operations (D16). */
    public static final String VIEWER = "GA_VOCABULARY_VIEWER";

    private static final Set<String> READ = Set.of(ADMIN, VIEWER);
    private static final Set<String> WRITE = Set.of(ADMIN);

    /** HTTP methods the server knows; others are 405 before anything else (D67). */
    public static final Set<String> KNOWN_METHODS =
            Set.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "TRACE");

    private static final List<Route> ROUTES = List.of(
            new Route("GET", "/v1/health", "getHealth", Set.of()),
            new Route("GET", "/v1/vocabularies", "getVocabularies", READ),
            new Route("POST", "/v1/vocabularies", "createVocabulary", WRITE),
            new Route("POST", "/v1/vocabularies/searches", "searchVocabularies", READ),
            new Route("GET", "/v1/vocabularies/{vocabularyName}", "getVocabulary", READ),
            new Route("PATCH", "/v1/vocabularies/{vocabularyName}", "updateVocabularyStatus", WRITE),
            new Route("GET", "/v1/vocabularies/{vocabularyName}/versions", "getVocabularyVersions", READ),
            new Route("POST", "/v1/vocabularies/{vocabularyName}/versions", "createVocabularyVersion", WRITE),
            new Route("GET", "/v1/vocabularies/{vocabularyName}/versions/{version}", "getVocabularyVersion", READ),
            new Route(
                    "PATCH",
                    "/v1/vocabularies/{vocabularyName}/versions/{version}",
                    "updateVocabularyVersionStatus",
                    WRITE),
            new Route("GET", "/v1/content/{vocabularyName}", "downloadVocabulary", READ),
            new Route("GET", "/v1/content/{vocabularyName}/versions/{version}", "downloadVocabularyVersion", READ),
            new Route("GET", "/v1/external-vocabularies", "getExternalVocabularies", READ),
            new Route("POST", "/v1/external-vocabularies", "createExternalVocabulary", WRITE),
            new Route("POST", "/v1/external-vocabulary/searches", "searchExternalVocabularies", READ),
            new Route("POST", "/v1/external-vocabularies/content", "downloadExternalVocabularyContent", READ),
            new Route("GET", "/v1/external-vocabularies/{name}/content", "getExternalVocabularyContentByName", READ),
            new Route("GET", "/v1/external-vocabularies/{name}", "getExternalVocabulary", READ),
            new Route("DELETE", "/v1/external-vocabularies/{name}", "deleteExternalVocabulary", WRITE));

    /** @return all routes */
    public List<Route> routes() {
        return ROUTES;
    }

    /**
     * The routes of the most specific template that fits the path (D77), whatever the method.
     *
     * @param path request path
     * @return routes of one template, empty when no template fits
     */
    public List<Route> forPath(String path) {
        if (!path.startsWith("/")) {
            return List.of();
        }
        String[] segments = path.substring(1).split("/", -1);
        List<Route> matching = new ArrayList<>();
        for (Route route : ROUTES) {
            if (route.matches(segments)) {
                matching.add(route);
            }
        }
        if (matching.isEmpty()) {
            return List.of();
        }
        int best = matching.stream().mapToInt(Route::fixedSegments).max().orElse(0);
        List<Route> bestRoutes =
                matching.stream().filter(r -> r.fixedSegments() == best).toList();
        String template = bestRoutes.get(0).template();
        return bestRoutes.stream().filter(r -> r.template().equals(template)).toList();
    }

    /**
     * @param method request method
     * @param path request path
     * @return the route of this request
     */
    public Optional<Route> find(String method, String path) {
        return forPath(path).stream().filter(r -> r.method().equals(method)).findFirst();
    }

    /** The Allow header value of a set of routes. */
    public static String allow(List<Route> routes) {
        return routes.stream().map(Route::method).collect(Collectors.toCollection(TreeSet::new)).stream()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining(", "));
    }
}
