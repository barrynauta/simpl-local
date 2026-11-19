package eu.simpl.vocabularymanager.web.routing;

import static eu.simpl.vocabularymanager.web.routing.ApiOperation.Access.PUBLIC;
import static eu.simpl.vocabularymanager.web.routing.ApiOperation.Access.READ;
import static eu.simpl.vocabularymanager.web.routing.ApiOperation.Access.WRITE;
import static eu.simpl.vocabularymanager.web.routing.ApiOperation.RequestBody.JSON;
import static eu.simpl.vocabularymanager.web.routing.ApiOperation.RequestBody.MULTIPART;
import static eu.simpl.vocabularymanager.web.routing.ApiOperation.RequestBody.NONE;

import eu.simpl.vocabularymanager.web.routing.ApiOperation.ResponseKind;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.util.pattern.PathPattern;

/**
 * The operations of the published contract (vocabulary_openapi-tier1.yaml), each with an explicit role rule (D16,
 * D84). Nothing outside this list is served.
 */
@Component
public class OperationCatalogue {

    public static final String API_BASE = "/v1";

    private static final Set<String> LIST_PARAMETERS =
            Set.of("name", "status", "creationDate", "lastUpdated", "sort", "page", "pageSize");
    private static final Set<String> PAGE_PARAMETERS = Set.of("page", "pageSize");
    private static final Set<String> VERSION_LIST_PARAMETERS = Set.of("sort", "page", "pageSize");

    private final List<ApiOperation> operations = List.of(
            op("getHealth", HttpMethod.GET, "/health", PUBLIC, NONE, ResponseKind.JSON, Set.of()),
            op("getVocabularies", HttpMethod.GET, "/vocabularies", READ, NONE, ResponseKind.JSON, LIST_PARAMETERS),
            op("createVocabulary", HttpMethod.POST, "/vocabularies", WRITE, MULTIPART, ResponseKind.JSON, Set.of()),
            op(
                    "searchVocabularies",
                    HttpMethod.POST,
                    "/vocabularies/searches",
                    READ,
                    JSON,
                    ResponseKind.JSON,
                    Set.of()),
            op(
                    "getVocabulary",
                    HttpMethod.GET,
                    "/vocabularies/{vocabularyName}",
                    READ,
                    NONE,
                    ResponseKind.JSON,
                    Set.of()),
            op(
                    "updateVocabularyStatus",
                    HttpMethod.PATCH,
                    "/vocabularies/{vocabularyName}",
                    WRITE,
                    JSON,
                    ResponseKind.JSON,
                    Set.of()),
            op(
                    "getVocabularyVersions",
                    HttpMethod.GET,
                    "/vocabularies/{vocabularyName}/versions",
                    READ,
                    NONE,
                    ResponseKind.JSON,
                    VERSION_LIST_PARAMETERS),
            op(
                    "createVocabularyVersion",
                    HttpMethod.POST,
                    "/vocabularies/{vocabularyName}/versions",
                    WRITE,
                    MULTIPART,
                    ResponseKind.JSON,
                    Set.of()),
            op(
                    "getVocabularyVersion",
                    HttpMethod.GET,
                    "/vocabularies/{vocabularyName}/versions/{version}",
                    READ,
                    NONE,
                    ResponseKind.JSON,
                    Set.of()),
            op(
                    "updateVocabularyVersionStatus",
                    HttpMethod.PATCH,
                    "/vocabularies/{vocabularyName}/versions/{version}",
                    WRITE,
                    JSON,
                    ResponseKind.JSON,
                    Set.of()),
            op(
                    "downloadVocabulary",
                    HttpMethod.GET,
                    "/content/{vocabularyName}",
                    READ,
                    NONE,
                    ResponseKind.TURTLE,
                    Set.of()),
            op(
                    "downloadVocabularyVersion",
                    HttpMethod.GET,
                    "/content/{vocabularyName}/versions/{version}",
                    READ,
                    NONE,
                    ResponseKind.TURTLE,
                    Set.of()),
            op(
                    "getExternalVocabularies",
                    HttpMethod.GET,
                    "/external-vocabularies",
                    READ,
                    NONE,
                    ResponseKind.JSON,
                    PAGE_PARAMETERS),
            op(
                    "createExternalVocabulary",
                    HttpMethod.POST,
                    "/external-vocabularies",
                    WRITE,
                    MULTIPART,
                    ResponseKind.JSON,
                    Set.of()),
            op(
                    "searchExternalVocabularies",
                    HttpMethod.POST,
                    "/external-vocabulary/searches",
                    READ,
                    JSON,
                    ResponseKind.JSON,
                    Set.of()),
            op(
                    "downloadExternalVocabularyContent",
                    HttpMethod.POST,
                    "/external-vocabularies/content",
                    READ,
                    JSON,
                    ResponseKind.TURTLE,
                    Set.of()),
            op(
                    "getExternalVocabularyContentByName",
                    HttpMethod.GET,
                    "/external-vocabularies/{name}/content",
                    READ,
                    NONE,
                    ResponseKind.TURTLE,
                    Set.of()),
            op(
                    "getExternalVocabulary",
                    HttpMethod.GET,
                    "/external-vocabularies/{name}",
                    READ,
                    NONE,
                    ResponseKind.JSON,
                    Set.of()),
            op(
                    "deleteExternalVocabulary",
                    HttpMethod.DELETE,
                    "/external-vocabularies/{name}",
                    WRITE,
                    NONE,
                    ResponseKind.JSON,
                    Set.of()));

    private final Map<String, List<ApiOperation>> operationsByPath = new LinkedHashMap<>();
    private final List<PathPattern> patternsBySpecificity;

    /** Builds the lookup tables. */
    public OperationCatalogue() {
        for (ApiOperation operation : operations) {
            operationsByPath
                    .computeIfAbsent(operation.path(), key -> new ArrayList<>())
                    .add(operation);
        }
        List<PathPattern> patterns = new ArrayList<>();
        for (String path : operationsByPath.keySet()) {
            patterns.add(operationsByPath.get(path).get(0).pattern());
        }
        patterns.sort(PathPattern.SPECIFICITY_COMPARATOR);
        this.patternsBySpecificity = List.copyOf(patterns);
    }

    private static ApiOperation op(
            String id,
            HttpMethod method,
            String path,
            ApiOperation.Access access,
            ApiOperation.RequestBody body,
            ResponseKind response,
            Set<String> query) {
        return new ApiOperation(id, method, API_BASE + path, access, body, response, query);
    }

    /**
     * @return every operation of the contract
     */
    public List<ApiOperation> operations() {
        return operations;
    }

    /**
     * Finds the path template a raw request path belongs to. A fixed segment wins over a path parameter (D77).
     *
     * @param rawPath the request URI path, still percent-encoded
     * @return the matching path template
     */
    public Optional<String> matchPath(String rawPath) {
        PathContainer container = PathContainer.parsePath(rawPath);
        return patternsBySpecificity.stream()
                .filter(pattern -> pattern.matches(container))
                .findFirst()
                .map(PathPattern::getPatternString);
    }

    /**
     * @param path a path template returned by {@link #matchPath(String)}
     * @return the methods declared on it
     */
    public Set<String> methodsOf(String path) {
        Set<String> methods = new LinkedHashSet<>();
        for (ApiOperation operation : operationsByPath.getOrDefault(path, List.of())) {
            methods.add(operation.method().name());
        }
        return methods;
    }

    /**
     * @param path a path template
     * @param method the request method
     * @return the operation, when the template declares the method
     */
    public Optional<ApiOperation> find(String path, String method) {
        return operationsByPath.getOrDefault(path, List.of()).stream()
                .filter(operation -> operation.method().name().equals(method))
                .findFirst();
    }

    /**
     * @param rawPath the request URI path, still percent-encoded
     * @param method the request method
     * @return the operation the request addresses, if any
     */
    public Optional<ApiOperation> resolve(String rawPath, String method) {
        return matchPath(rawPath).flatMap(path -> find(path, method));
    }
}
