package eu.simpl.vocabularymanager.web;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Every operation of the decided contract with its access rule (D16, D84). The request gate routes on this
 * table: a known path with an undeclared method gets 405, an unknown path never reaches a handler without a
 * valid token, and each handler of the application must match exactly one entry (checked at start).
 */
public final class OperationCatalog {

    public static final String API_BASE = "/v1";

    /** Who may call an operation. */
    public enum Access {
        PUBLIC,
        READ,
        WRITE
    }

    /** What the operation answers on success, for the Accept check of D63. */
    public enum Produces {
        JSON,
        TURTLE,
        NONE
    }

    /** One declared operation. */
    public record Operation(String method, String template, String operationId, Access access, Produces produces) {

        Pattern regex() {
            String quoted = template.replaceAll("\\{[^/]+}", "[^/]+");
            return Pattern.compile("^" + quoted + "$");
        }

        boolean literal() {
            return !template.contains("{");
        }
    }

    /** Result of routing a request path. */
    public record Route(Optional<Operation> operation, Set<String> allowedMethods, boolean pathKnown) {}

    private static final List<Operation> OPERATIONS = List.of(
            op("GET", "/health", "getHealth", Access.PUBLIC, Produces.JSON),
            op("GET", "/vocabularies", "getVocabularies", Access.READ, Produces.JSON),
            op("POST", "/vocabularies", "createVocabulary", Access.WRITE, Produces.JSON),
            op("POST", "/vocabularies/searches", "searchVocabularies", Access.READ, Produces.JSON),
            op("GET", "/vocabularies/{vocabularyName}", "getVocabulary", Access.READ, Produces.JSON),
            op("PATCH", "/vocabularies/{vocabularyName}", "updateVocabularyStatus", Access.WRITE, Produces.JSON),
            op("GET", "/vocabularies/{vocabularyName}/versions", "getVocabularyVersions", Access.READ, Produces.JSON),
            op(
                    "POST",
                    "/vocabularies/{vocabularyName}/versions",
                    "createVocabularyVersion",
                    Access.WRITE,
                    Produces.JSON),
            op(
                    "GET",
                    "/vocabularies/{vocabularyName}/versions/{version}",
                    "getVocabularyVersion",
                    Access.READ,
                    Produces.JSON),
            op(
                    "PATCH",
                    "/vocabularies/{vocabularyName}/versions/{version}",
                    "updateVocabularyVersionStatus",
                    Access.WRITE,
                    Produces.JSON),
            op("GET", "/content/{vocabularyName}", "downloadVocabulary", Access.READ, Produces.TURTLE),
            op(
                    "GET",
                    "/content/{vocabularyName}/versions/{version}",
                    "downloadVocabularyVersion",
                    Access.READ,
                    Produces.TURTLE),
            op("GET", "/external-vocabularies", "getExternalVocabularies", Access.READ, Produces.JSON),
            op("POST", "/external-vocabularies", "createExternalVocabulary", Access.WRITE, Produces.JSON),
            op("POST", "/external-vocabulary/searches", "searchExternalVocabularies", Access.READ, Produces.JSON),
            op(
                    "POST",
                    "/external-vocabularies/content",
                    "downloadExternalVocabularyContent",
                    Access.READ,
                    Produces.TURTLE),
            op(
                    "GET",
                    "/external-vocabularies/{name}/content",
                    "getExternalVocabularyContentByName",
                    Access.READ,
                    Produces.TURTLE),
            op("GET", "/external-vocabularies/{name}", "getExternalVocabulary", Access.READ, Produces.JSON),
            op("DELETE", "/external-vocabularies/{name}", "deleteExternalVocabulary", Access.WRITE, Produces.NONE));

    /** HTTP methods the server knows; any other method is refused with 405 before routing (D67). */
    public static final Set<String> KNOWN_METHODS =
            Set.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "TRACE", "CONNECT");

    private static final java.util.Map<Operation, Pattern> PATTERNS =
            OPERATIONS.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(op -> op, Operation::regex));

    private OperationCatalog() {}

    private static Operation op(String method, String path, String id, Access access, Produces produces) {
        return new Operation(method, API_BASE + path, id, access, produces);
    }

    public static List<Operation> operations() {
        return OPERATIONS;
    }

    /**
     * Routes a decoded request path. A literal path wins over a template (D77): when a literal template
     * matches, only its methods count.
     */
    public static Route route(String method, String path) {
        List<Operation> literalMatches = new ArrayList<>();
        List<Operation> templateMatches = new ArrayList<>();
        for (Operation operation : OPERATIONS) {
            if (PATTERNS.get(operation).matcher(path).matches()) {
                (operation.literal() ? literalMatches : templateMatches).add(operation);
            }
        }
        List<Operation> candidates = literalMatches.isEmpty() ? templateMatches : literalMatches;
        if (candidates.isEmpty()) {
            return new Route(Optional.empty(), Set.of(), false);
        }
        Set<String> allowed = new LinkedHashSet<>();
        Operation chosen = null;
        for (Operation operation : candidates) {
            allowed.add(operation.method());
            if (operation.method().equals(method)) {
                chosen = operation;
            }
        }
        return new Route(Optional.ofNullable(chosen), allowed, true);
    }

    /** Finds the operation by method and template, used by the start-up rule check. */
    public static Optional<Operation> byTemplate(String method, String template) {
        return OPERATIONS.stream()
                .filter(op -> op.method().equals(method) && op.template().equals(template))
                .findFirst();
    }
}
