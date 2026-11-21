package eu.simpl.vocabularymanager.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * The declared operations of the decided contract (docs/openapi-decided.yaml) with their access
 * rule (D16, D84) and media types (D15, D63). The security configuration, the method check (D67,
 * D139), the negotiation check (D63, D133) and the start-up check that every handler has a rule all
 * read this one table.
 */
public final class Routes {

    /** Who may call an operation (D16). */
    public enum Access {
        PUBLIC,
        READ,
        WRITE
    }

    /** Body media type of an operation. */
    public enum Body {
        NONE,
        JSON,
        MULTIPART
    }

    /**
     * One operation.
     *
     * @param method HTTP method
     * @param pattern path pattern under /v1
     * @param operationId OpenAPI operationId
     * @param access role rule
     * @param body request body media type
     * @param turtle true when the success response is text/turtle
     */
    public record Route(String method, String pattern, String operationId, Access access, Body body, boolean turtle) {

        public PathPattern parsed() {
            return PATTERNS.computeIfAbsent(pattern, PARSER::parse);
        }
    }

    /**
     * A matched request.
     *
     * @param route the operation, or null when the path is declared but not for this method
     * @param allowed the methods the matched path declares
     * @param variables path variables
     */
    public record Match(Route route, Set<String> allowed, Map<String, String> variables) {}

    public static final String V1 = "/v1";
    private static final PathPatternParser PARSER = new PathPatternParser();
    private static final Map<String, PathPattern> PATTERNS = new java.util.concurrent.ConcurrentHashMap<>();

    public static final List<Route> ALL = List.of(
            new Route("GET", V1 + "/health", "getHealth", Access.PUBLIC, Body.NONE, false),
            new Route("GET", V1 + "/vocabularies", "getVocabularies", Access.READ, Body.NONE, false),
            new Route("POST", V1 + "/vocabularies", "createVocabulary", Access.WRITE, Body.MULTIPART, false),
            new Route("POST", V1 + "/vocabularies/searches", "searchVocabularies", Access.READ, Body.JSON, false),
            new Route("GET", V1 + "/vocabularies/{vocabularyName}", "getVocabulary", Access.READ, Body.NONE, false),
            new Route(
                    "PATCH",
                    V1 + "/vocabularies/{vocabularyName}",
                    "updateVocabularyStatus",
                    Access.WRITE,
                    Body.JSON,
                    false),
            new Route(
                    "GET",
                    V1 + "/vocabularies/{vocabularyName}/versions",
                    "getVocabularyVersions",
                    Access.READ,
                    Body.NONE,
                    false),
            new Route(
                    "POST",
                    V1 + "/vocabularies/{vocabularyName}/versions",
                    "createVocabularyVersion",
                    Access.WRITE,
                    Body.MULTIPART,
                    false),
            new Route(
                    "GET",
                    V1 + "/vocabularies/{vocabularyName}/versions/{version}",
                    "getVocabularyVersion",
                    Access.READ,
                    Body.NONE,
                    false),
            new Route(
                    "PATCH",
                    V1 + "/vocabularies/{vocabularyName}/versions/{version}",
                    "updateVocabularyVersionStatus",
                    Access.WRITE,
                    Body.JSON,
                    false),
            new Route("GET", V1 + "/content/{vocabularyName}", "downloadVocabulary", Access.READ, Body.NONE, true),
            new Route(
                    "GET",
                    V1 + "/content/{vocabularyName}/versions/{version}",
                    "downloadVocabularyVersion",
                    Access.READ,
                    Body.NONE,
                    true),
            new Route("GET", V1 + "/external-vocabularies", "getExternalVocabularies", Access.READ, Body.NONE, false),
            new Route(
                    "POST",
                    V1 + "/external-vocabularies",
                    "createExternalVocabulary",
                    Access.WRITE,
                    Body.MULTIPART,
                    false),
            new Route(
                    "POST",
                    V1 + "/external-vocabulary/searches",
                    "searchExternalVocabularies",
                    Access.READ,
                    Body.JSON,
                    false),
            new Route(
                    "POST",
                    V1 + "/external-vocabularies/content",
                    "downloadExternalVocabularyContent",
                    Access.READ,
                    Body.JSON,
                    true),
            new Route(
                    "GET",
                    V1 + "/external-vocabularies/{name}/content",
                    "getExternalVocabularyContentByName",
                    Access.READ,
                    Body.NONE,
                    true),
            new Route(
                    "GET",
                    V1 + "/external-vocabularies/{name}",
                    "getExternalVocabulary",
                    Access.READ,
                    Body.NONE,
                    false),
            new Route(
                    "DELETE",
                    V1 + "/external-vocabularies/{name}",
                    "deleteExternalVocabulary",
                    Access.WRITE,
                    Body.NONE,
                    false));

    private Routes() {}

    /**
     * Finds the operation for a raw request path. A fixed segment wins over a path parameter (D77):
     * among the matching patterns the most specific one decides.
     */
    public static Optional<Match> match(String method, String rawPath) {
        PathContainer path = PathContainer.parsePath(rawPath);
        List<PathPattern> matching = new ArrayList<>();
        for (Route r : ALL) {
            PathPattern p = r.parsed();
            if (p.matches(path)
                    && matching.stream().noneMatch(x -> x.getPatternString().equals(p.getPatternString()))) {
                matching.add(p);
            }
        }
        if (matching.isEmpty()) {
            return Optional.empty();
        }
        matching.sort(Comparator.comparing(p -> p, PathPattern.SPECIFICITY_COMPARATOR));
        PathPattern best = matching.get(0);
        Set<String> allowed = new LinkedHashSet<>();
        Route found = null;
        for (Route r : ALL) {
            if (r.pattern().equals(best.getPatternString())) {
                allowed.add(r.method());
                if (r.method().equals(method)) {
                    found = r;
                }
            }
        }
        PathPattern.PathMatchInfo info = best.matchAndExtract(path);
        Map<String, String> vars = info == null ? Map.of() : info.getUriVariables();
        return Optional.of(new Match(found, allowed, vars));
    }
}
