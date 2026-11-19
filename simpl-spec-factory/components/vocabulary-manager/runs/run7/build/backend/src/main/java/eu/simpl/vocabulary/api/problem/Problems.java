package eu.simpl.vocabulary.api.problem;

import java.util.List;
import java.util.Map;

/** Factory for every problem response the API returns (D17, D44, D48, D55, D63, D66, D67). */
public final class Problems {

    private static final String BELGIF_PROBLEMS = "https://www.belgif.be/specification/rest/api-guide/problems/";

    /** Retry-After value of a 503 (D44). */
    public static final String RETRY_AFTER_SECONDS = "5";

    public static final String STORE_UNAVAILABLE = "The vocabulary store is not available. Retry later.";
    public static final String IDP_UNAVAILABLE = "The identity provider is not available. Retry later.";
    public static final String TOO_LARGE_DETAIL =
            "Uploaded vocabulary file is too large. The maximum allowed upload size is 5 MB. "
                    + "Please upload a smaller Turtle file.";

    private Problems() {}

    public static ApiException badRequest(List<Issue> issues) {
        return new ApiException(
                400,
                "urn:problem-type:belgif:badRequest",
                BELGIF_PROBLEMS + "badRequest.html",
                "Bad Request",
                "The input message is incorrect",
                issues,
                null);
    }

    public static ApiException badRequest(Issue issue) {
        return badRequest(List.of(issue));
    }

    /** A request the firewall refuses: 400 without issues (D67). */
    public static ApiException rejectedRequest() {
        return badRequest(List.of());
    }

    public static ApiException notFound(String detail, Issue issue) {
        return new ApiException(
                404,
                "urn:problem-type:belgif:resourceNotFound",
                BELGIF_PROBLEMS + "resourceNotFound.html",
                "Resource Not Found",
                detail,
                issue == null ? List.of() : List.of(issue),
                null);
    }

    public static ApiException vocabularyNotFound(String name) {
        String detail = "Vocabulary with name '" + name + "' was not found.";
        return notFound(detail, Issue.plain(Issue.IN_PATH, "vocabularyName", detail));
    }

    public static ApiException versionNotFound(String name, long version) {
        String detail = "Version " + version + " of vocabulary '" + name + "' was not found.";
        return notFound(detail, Issue.plain(Issue.IN_PATH, "version", detail));
    }

    public static ApiException externalNotFound(String name) {
        String detail = "External vocabulary with name '" + name + "' was not found.";
        return notFound(detail, Issue.plain(Issue.IN_PATH, "name", detail));
    }

    public static ApiException externalNamespaceNotFound(String uri) {
        String detail = "External vocabulary with namespaceUri '" + uri + "' was not found.";
        return notFound(detail, Issue.plain(Issue.IN_BODY, "namespaceUri", detail));
    }

    public static ApiException pathNotFound() {
        return notFound("The requested resource does not exist.", null);
    }

    public static ApiException conflict(String detail) {
        return simpl(409, "conflict", "Conflict", detail, null);
    }

    public static ApiException payloadTooLarge() {
        return simpl(
                413,
                "payloadTooLarge",
                "Payload Too Large",
                TOO_LARGE_DETAIL,
                List.of(Issue.invalidInput("vocabularyFile", TOO_LARGE_DETAIL, null)));
    }

    public static ApiException unsupportedMediaType() {
        return simpl(
                415, "unsupportedMediaType", "Unsupported Media Type", "Request content type is not supported.", null);
    }

    public static ApiException notAcceptable() {
        return simpl(406, "notAcceptable", "Not Acceptable", "Requested media type cannot be served.", null);
    }

    public static ApiException unauthorized() {
        return simpl(401, "unauthorized", "Unauthorized", "A valid bearer token is required.", null);
    }

    public static ApiException forbidden() {
        return simpl(403, "forbidden", "Forbidden", "You are not allowed to perform this operation.", null);
    }

    public static ApiException methodNotAllowed(String allow) {
        return new ApiException(
                405,
                "urn:problem-type:simpl:methodNotAllowed",
                null,
                "Method Not Allowed",
                "The method is not allowed on this resource.",
                null,
                allow == null ? null : Map.of("Allow", allow));
    }

    public static ApiException serviceUnavailable(String detail) {
        return new ApiException(
                503,
                "urn:problem-type:belgif:serviceUnavailable",
                BELGIF_PROBLEMS + "serviceUnavailable.html",
                "Service Unavailable",
                detail,
                null,
                Map.of("Retry-After", RETRY_AFTER_SECONDS));
    }

    public static ApiException storeUnavailable() {
        return serviceUnavailable(STORE_UNAVAILABLE);
    }

    public static ApiException internalError() {
        return new ApiException(
                500,
                "urn:problem-type:belgif:internalServerError",
                BELGIF_PROBLEMS + "internalServerError.html",
                "Internal Server Error",
                "Internal Server Error",
                null,
                null);
    }

    private static ApiException simpl(int status, String code, String title, String detail, List<Issue> issues) {
        return new ApiException(status, "urn:problem-type:simpl:" + code, null, title, detail, issues, null);
    }
}
