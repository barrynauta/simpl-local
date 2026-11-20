package eu.simpl.vocabularymanager.error;

import java.util.List;
import java.util.Map;

/**
 * Factories for every problem the component returns. Types follow D48: belgif for 400, 404, 500 and 503, simpl for
 * 401, 403, 405, 406, 409, 413 and 415.
 */
public final class Problems {

    public static final String UPLOAD_TOO_LARGE =
            "Uploaded vocabulary file is too large. The maximum allowed upload size is 5 MB. "
                    + "Please upload a smaller Turtle file.";
    public static final String STORE_UNAVAILABLE = "The vocabulary store is not available. Retry later.";
    public static final String IDP_UNAVAILABLE = "The identity provider is not available. Retry later.";
    public static final String TOO_MANY_UPLOADS = "Too many uploads in progress. Retry later.";

    private static final String BELGIF_PROBLEMS = "https://www.belgif.be/specification/rest/api-guide/problems/";

    private Problems() {}

    public static ProblemException badRequest(List<Issue> issues) {
        return new ProblemException(
                400,
                "urn:problem-type:belgif:badRequest",
                BELGIF_PROBLEMS + "badRequest.html",
                "Bad Request",
                "The input message is incorrect",
                issues,
                null);
    }

    public static ProblemException badRequest(Issue issue) {
        return badRequest(List.of(issue));
    }

    public static ProblemException unauthorized(String detail) {
        return new ProblemException(
                401,
                "urn:problem-type:simpl:unauthorized",
                null,
                "Unauthorized",
                detail,
                null,
                Map.of("WWW-Authenticate", "Bearer"));
    }

    public static ProblemException forbidden() {
        return new ProblemException(
                403,
                "urn:problem-type:simpl:forbidden",
                null,
                "Forbidden",
                "You are not allowed to perform this operation.",
                null,
                null);
    }

    public static ProblemException notFound(String detail, Issue issue) {
        return new ProblemException(
                404,
                "urn:problem-type:belgif:resourceNotFound",
                BELGIF_PROBLEMS + "resourceNotFound.html",
                "Resource Not Found",
                detail,
                issue == null ? null : List.of(issue),
                null);
    }

    public static ProblemException vocabularyNotFound(String name) {
        String detail = "Vocabulary with name '" + name + "' was not found.";
        return notFound(detail, Issue.notFound("path", "vocabularyName", detail));
    }

    public static ProblemException versionNotFound(String name, int version) {
        String detail = "Version " + version + " of vocabulary '" + name + "' was not found.";
        return notFound(detail, Issue.notFound("path", "version", detail));
    }

    public static ProblemException externalNotFound(String name) {
        String detail = "External vocabulary with name '" + name + "' was not found.";
        return notFound(detail, Issue.notFound("path", "name", detail));
    }

    public static ProblemException externalNamespaceNotFound(String uri) {
        String detail = "External vocabulary with namespaceUri '" + uri + "' was not found.";
        return notFound(detail, Issue.notFound("body", "namespaceUri", detail));
    }

    public static ProblemException pathNotFound() {
        return notFound("The requested resource does not exist.", null);
    }

    public static ProblemException methodNotAllowed(String allow) {
        return new ProblemException(
                405,
                "urn:problem-type:simpl:methodNotAllowed",
                null,
                "Method Not Allowed",
                "The method is not allowed on this resource.",
                null,
                allow == null ? null : Map.of("Allow", allow));
    }

    public static ProblemException notAcceptable() {
        return new ProblemException(
                406,
                "urn:problem-type:simpl:notAcceptable",
                null,
                "Not Acceptable",
                "Requested media type cannot be served.",
                null,
                null);
    }

    public static ProblemException conflict(String detail) {
        return new ProblemException(409, "urn:problem-type:simpl:conflict", null, "Conflict", detail, null, null);
    }

    public static ProblemException payloadTooLarge(String detail, String name) {
        return new ProblemException(
                413,
                "urn:problem-type:simpl:payloadTooLarge",
                null,
                "Payload Too Large",
                detail,
                List.of(Issue.invalidInput(name, detail, null)),
                null);
    }

    public static ProblemException uploadTooLarge() {
        return payloadTooLarge(UPLOAD_TOO_LARGE, "vocabularyFile");
    }

    public static ProblemException unsupportedMediaType() {
        return new ProblemException(
                415,
                "urn:problem-type:simpl:unsupportedMediaType",
                null,
                "Unsupported Media Type",
                "Request content type is not supported.",
                null,
                null);
    }

    public static ProblemException internalError() {
        return new ProblemException(
                500,
                "urn:problem-type:belgif:internalServerError",
                BELGIF_PROBLEMS + "internalServerError.html",
                "Internal Server Error",
                "Internal Server Error",
                null,
                null);
    }

    public static ProblemException serviceUnavailable(String detail) {
        return new ProblemException(
                503,
                "urn:problem-type:belgif:serviceUnavailable",
                BELGIF_PROBLEMS + "serviceUnavailable.html",
                "Service Unavailable",
                detail,
                null,
                Map.of("Retry-After", "5"));
    }

    public static ProblemException firewallRejected() {
        return new ProblemException(
                400,
                "urn:problem-type:belgif:badRequest",
                BELGIF_PROBLEMS + "badRequest.html",
                "Bad Request",
                "The request was rejected.",
                null,
                null);
    }
}
