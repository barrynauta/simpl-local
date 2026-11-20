package eu.simpl.vocabularymanager.web;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Every operation of the decided contract with its explicit access rule (D16, D84), request body kind and response
 * media type. This table is the single source for routing (405), authorisation, the Accept check (D63) and the
 * Content-Type check (D15).
 */
public enum ApiOperation {
    GET_HEALTH("getHealth", "GET", "/v1/health", Access.PUBLIC, BodyKind.NONE, Produces.JSON),
    GET_VOCABULARIES("getVocabularies", "GET", "/v1/vocabularies", Access.READ, BodyKind.NONE, Produces.JSON),
    CREATE_VOCABULARY("createVocabulary", "POST", "/v1/vocabularies", Access.WRITE, BodyKind.MULTIPART, Produces.JSON),
    SEARCH_VOCABULARIES(
            "searchVocabularies", "POST", "/v1/vocabularies/searches", Access.READ, BodyKind.JSON, Produces.JSON),
    GET_VOCABULARY(
            "getVocabulary", "GET", "/v1/vocabularies/{vocabularyName}", Access.READ, BodyKind.NONE, Produces.JSON),
    UPDATE_VOCABULARY_STATUS(
            "updateVocabularyStatus",
            "PATCH",
            "/v1/vocabularies/{vocabularyName}",
            Access.WRITE,
            BodyKind.JSON,
            Produces.JSON),
    GET_VOCABULARY_VERSIONS(
            "getVocabularyVersions",
            "GET",
            "/v1/vocabularies/{vocabularyName}/versions",
            Access.READ,
            BodyKind.NONE,
            Produces.JSON),
    CREATE_VOCABULARY_VERSION(
            "createVocabularyVersion",
            "POST",
            "/v1/vocabularies/{vocabularyName}/versions",
            Access.WRITE,
            BodyKind.MULTIPART,
            Produces.JSON),
    GET_VOCABULARY_VERSION(
            "getVocabularyVersion",
            "GET",
            "/v1/vocabularies/{vocabularyName}/versions/{version}",
            Access.READ,
            BodyKind.NONE,
            Produces.JSON),
    UPDATE_VOCABULARY_VERSION_STATUS(
            "updateVocabularyVersionStatus",
            "PATCH",
            "/v1/vocabularies/{vocabularyName}/versions/{version}",
            Access.WRITE,
            BodyKind.JSON,
            Produces.JSON),
    DOWNLOAD_VOCABULARY(
            "downloadVocabulary", "GET", "/v1/content/{vocabularyName}", Access.READ, BodyKind.NONE, Produces.TURTLE),
    DOWNLOAD_VOCABULARY_VERSION(
            "downloadVocabularyVersion",
            "GET",
            "/v1/content/{vocabularyName}/versions/{version}",
            Access.READ,
            BodyKind.NONE,
            Produces.TURTLE),
    GET_EXTERNAL_VOCABULARIES(
            "getExternalVocabularies", "GET", "/v1/external-vocabularies", Access.READ, BodyKind.NONE, Produces.JSON),
    CREATE_EXTERNAL_VOCABULARY(
            "createExternalVocabulary",
            "POST",
            "/v1/external-vocabularies",
            Access.WRITE,
            BodyKind.MULTIPART,
            Produces.JSON),
    SEARCH_EXTERNAL_VOCABULARIES(
            "searchExternalVocabularies",
            "POST",
            "/v1/external-vocabulary/searches",
            Access.READ,
            BodyKind.JSON,
            Produces.JSON),
    DOWNLOAD_EXTERNAL_VOCABULARY_CONTENT(
            "downloadExternalVocabularyContent",
            "POST",
            "/v1/external-vocabularies/content",
            Access.READ,
            BodyKind.JSON,
            Produces.TURTLE),
    GET_EXTERNAL_VOCABULARY_CONTENT_BY_NAME(
            "getExternalVocabularyContentByName",
            "GET",
            "/v1/external-vocabularies/{name}/content",
            Access.READ,
            BodyKind.NONE,
            Produces.TURTLE),
    GET_EXTERNAL_VOCABULARY(
            "getExternalVocabulary",
            "GET",
            "/v1/external-vocabularies/{name}",
            Access.READ,
            BodyKind.NONE,
            Produces.JSON),
    DELETE_EXTERNAL_VOCABULARY(
            "deleteExternalVocabulary",
            "DELETE",
            "/v1/external-vocabularies/{name}",
            Access.WRITE,
            BodyKind.NONE,
            Produces.JSON);

    /** Who may call an operation (D16). */
    public enum Access {
        PUBLIC,
        READ,
        WRITE
    }

    /** The kind of request body an operation takes. */
    public enum BodyKind {
        NONE,
        JSON,
        MULTIPART
    }

    /** The media type of the success response. */
    public enum Produces {
        JSON,
        TURTLE
    }

    public static final String ROLE_VIEWER = "GA_VOCABULARY_VIEWER";
    public static final String ROLE_ADMIN = "GA_VOCABULARY_ADMIN";

    private final String operationId;
    private final String method;
    private final String pathTemplate;
    private final Access access;
    private final BodyKind bodyKind;
    private final Produces produces;
    private final String[] segments;

    ApiOperation(
            String operationId,
            String method,
            String pathTemplate,
            Access access,
            BodyKind bodyKind,
            Produces produces) {
        this.operationId = operationId;
        this.method = method;
        this.pathTemplate = pathTemplate;
        this.access = access;
        this.bodyKind = bodyKind;
        this.produces = produces;
        this.segments = pathTemplate.substring(1).split("/");
    }

    public String operationId() {
        return operationId;
    }

    public String method() {
        return method;
    }

    public String pathTemplate() {
        return pathTemplate;
    }

    public Access access() {
        return access;
    }

    public BodyKind bodyKind() {
        return bodyKind;
    }

    public Produces produces() {
        return produces;
    }

    /** Roles that may call the operation; empty for a public operation. */
    public Set<String> allowedRoles() {
        return switch (access) {
            case PUBLIC -> Set.of();
            case READ -> Set.of(ROLE_VIEWER, ROLE_ADMIN);
            case WRITE -> Set.of(ROLE_ADMIN);
        };
    }

    /** Number of fixed (non-parameter) segments that match; -1 when the path does not match. */
    int matchScore(String[] pathSegments) {
        if (pathSegments.length != segments.length) {
            return -1;
        }
        int fixed = 0;
        for (int i = 0; i < segments.length; i++) {
            String template = segments[i];
            if (template.startsWith("{")) {
                if (pathSegments[i].isEmpty()) {
                    return -1;
                }
            } else if (template.equals(pathSegments[i])) {
                fixed++;
            } else {
                return -1;
            }
        }
        return fixed;
    }

    /** The result of routing a request: the matched path template and the operation for the method, if any. */
    public record Route(String pathTemplate, Optional<ApiOperation> operation, String allowedMethods) {}

    /**
     * Routes a raw request path and method. A fixed segment wins over a path parameter (D77). Returns empty when no
     * path template matches.
     */
    public static Optional<Route> route(String method, String rawPath) {
        if (rawPath == null || !rawPath.startsWith("/")) {
            return Optional.empty();
        }
        String[] pathSegments = rawPath.substring(1).split("/", -1);
        int best = -1;
        String bestTemplate = null;
        for (ApiOperation operation : values()) {
            int score = operation.matchScore(pathSegments);
            if (score > best) {
                best = score;
                bestTemplate = operation.pathTemplate;
            }
        }
        if (bestTemplate == null) {
            return Optional.empty();
        }
        String template = bestTemplate;
        List<ApiOperation> onPath = Arrays.stream(values())
                .filter(op -> op.pathTemplate.equals(template))
                .toList();
        String allowed =
                onPath.stream().map(ApiOperation::method).collect(Collectors.toCollection(TreeSet::new)).stream()
                        .collect(Collectors.joining(", "));
        Optional<ApiOperation> operation =
                onPath.stream().filter(op -> op.method.equals(method)).findFirst();
        return Optional.of(new Route(template, operation, allowed));
    }
}
