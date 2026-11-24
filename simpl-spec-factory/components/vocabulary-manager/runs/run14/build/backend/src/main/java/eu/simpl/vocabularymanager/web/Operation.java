package eu.simpl.vocabularymanager.web;

import java.util.List;

/**
 * The operation table (D84, D206): every operation of the decided OpenAPI with its method, path, access rule,
 * request body kind and response kind. The gatekeeper filter routes and checks access with this table only; a
 * request that matches no row never reaches a handler. A build test compares the table with Spring MVC and with
 * openapi-decided.yaml.
 */
public enum Operation {
    GET_HEALTH("getHealth", "GET", "/v1/health", Access.PUBLIC, Body.NONE, Reply.JSON),
    GET_VOCABULARIES("getVocabularies", "GET", "/v1/vocabularies", Access.VIEWER, Body.NONE, Reply.JSON),
    CREATE_VOCABULARY("createVocabulary", "POST", "/v1/vocabularies", Access.ADMIN, Body.MULTIPART, Reply.JSON),
    SEARCH_VOCABULARIES(
            "searchVocabularies", "POST", "/v1/vocabularies/searches", Access.VIEWER, Body.JSON, Reply.JSON),
    GET_VOCABULARY("getVocabulary", "GET", "/v1/vocabularies/{vocabularyName}", Access.VIEWER, Body.NONE, Reply.JSON),
    UPDATE_VOCABULARY_STATUS(
            "updateVocabularyStatus",
            "PATCH",
            "/v1/vocabularies/{vocabularyName}",
            Access.ADMIN,
            Body.JSON,
            Reply.JSON),
    GET_VOCABULARY_VERSIONS(
            "getVocabularyVersions",
            "GET",
            "/v1/vocabularies/{vocabularyName}/versions",
            Access.VIEWER,
            Body.NONE,
            Reply.JSON),
    CREATE_VOCABULARY_VERSION(
            "createVocabularyVersion",
            "POST",
            "/v1/vocabularies/{vocabularyName}/versions",
            Access.ADMIN,
            Body.MULTIPART,
            Reply.JSON),
    GET_VOCABULARY_VERSION(
            "getVocabularyVersion",
            "GET",
            "/v1/vocabularies/{vocabularyName}/versions/{version}",
            Access.VIEWER,
            Body.NONE,
            Reply.JSON),
    UPDATE_VOCABULARY_VERSION_STATUS(
            "updateVocabularyVersionStatus",
            "PATCH",
            "/v1/vocabularies/{vocabularyName}/versions/{version}",
            Access.ADMIN,
            Body.JSON,
            Reply.JSON),
    DOWNLOAD_VOCABULARY(
            "downloadVocabulary", "GET", "/v1/content/{vocabularyName}", Access.VIEWER, Body.NONE, Reply.TURTLE),
    DOWNLOAD_VOCABULARY_VERSION(
            "downloadVocabularyVersion",
            "GET",
            "/v1/content/{vocabularyName}/versions/{version}",
            Access.VIEWER,
            Body.NONE,
            Reply.TURTLE),
    GET_EXTERNAL_VOCABULARIES(
            "getExternalVocabularies", "GET", "/v1/external-vocabularies", Access.VIEWER, Body.NONE, Reply.JSON),
    CREATE_EXTERNAL_VOCABULARY(
            "createExternalVocabulary", "POST", "/v1/external-vocabularies", Access.ADMIN, Body.MULTIPART, Reply.JSON),
    SEARCH_EXTERNAL_VOCABULARIES(
            "searchExternalVocabularies",
            "POST",
            "/v1/external-vocabulary/searches",
            Access.VIEWER,
            Body.JSON,
            Reply.JSON),
    DOWNLOAD_EXTERNAL_VOCABULARY_CONTENT(
            "downloadExternalVocabularyContent",
            "POST",
            "/v1/external-vocabularies/content",
            Access.VIEWER,
            Body.JSON,
            Reply.TURTLE),
    GET_EXTERNAL_VOCABULARY_CONTENT_BY_NAME(
            "getExternalVocabularyContentByName",
            "GET",
            "/v1/external-vocabularies/{name}/content",
            Access.VIEWER,
            Body.NONE,
            Reply.TURTLE),
    GET_EXTERNAL_VOCABULARY(
            "getExternalVocabulary", "GET", "/v1/external-vocabularies/{name}", Access.VIEWER, Body.NONE, Reply.JSON),
    DELETE_EXTERNAL_VOCABULARY(
            "deleteExternalVocabulary",
            "DELETE",
            "/v1/external-vocabularies/{name}",
            Access.ADMIN,
            Body.NONE,
            Reply.NONE);

    /** Who may call an operation (D16). */
    public enum Access {
        PUBLIC,
        VIEWER,
        ADMIN
    }

    /** Kind of request body. */
    public enum Body {
        NONE,
        JSON,
        MULTIPART
    }

    /** Kind of success response. */
    public enum Reply {
        JSON,
        TURTLE,
        NONE
    }

    private final String operationId;
    private final String method;
    private final String path;
    private final Access access;
    private final Body body;
    private final Reply reply;
    private final List<String> segments;

    Operation(String operationId, String method, String path, Access access, Body body, Reply reply) {
        this.operationId = operationId;
        this.method = method;
        this.path = path;
        this.access = access;
        this.body = body;
        this.reply = reply;
        this.segments = List.of(path.substring(1).split("/"));
    }

    public String operationId() {
        return operationId;
    }

    public String method() {
        return method;
    }

    public String path() {
        return path;
    }

    public Access access() {
        return access;
    }

    public Body body() {
        return body;
    }

    public Reply reply() {
        return reply;
    }

    List<String> segments() {
        return segments;
    }

    /** True when this operation changes data (D51 order of checks for writes). */
    public boolean isWrite() {
        return !"GET".equals(method) && access == Access.ADMIN;
    }
}
