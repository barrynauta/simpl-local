package eu.simpl.vocabularymanager.problem;

import java.util.List;
import java.util.Map;

/**
 * Factory for every problem the API returns. Types follow decision D48: belgif types for 400, 404, 500 and
 * 503; simpl types for 401, 403, 405, 406, 409, 413 and 415.
 */
public final class Problems {

    private static final String BELGIF_PROBLEMS = "https://www.belgif.be/specification/rest/api-guide/problems/";

    /** Maximum upload size text of the UploadTooLargeResponse (decision D15). */
    public static final String TOO_LARGE_DETAIL = "Uploaded vocabulary file is too large. The maximum allowed upload"
            + " size is 5 MB. Please upload a smaller Turtle file.";

    public static final String STORE_DOWN_DETAIL = "The vocabulary store is not available. Retry later.";
    public static final String IDP_DOWN_DETAIL = "The identity provider is not available. Retry later.";

    private Problems() {}

    /**
     * 400 with input validation issues.
     *
     * @param issues the issues, at least one
     * @return the exception
     */
    public static ProblemException badRequest(List<Issue> issues) {
        return new ProblemException(Problem.of(
                "urn:problem-type:belgif:badRequest",
                BELGIF_PROBLEMS + "badRequest.html",
                "Bad Request",
                400,
                "The input message is incorrect",
                issues));
    }

    /**
     * 400 with one issue.
     *
     * @param issue the issue
     * @return the exception
     */
    public static ProblemException badRequest(Issue issue) {
        return badRequest(List.of(issue));
    }

    /**
     * 401 for a missing or invalid token (decision D17).
     *
     * @return the problem
     */
    public static Problem unauthorized() {
        return Problem.of(
                "urn:problem-type:simpl:unauthorized",
                null,
                "Unauthorized",
                401,
                "A valid bearer token is required to access this resource.",
                null);
    }

    /**
     * 403 for a valid token without the needed role (decision D17).
     *
     * @return the problem
     */
    public static Problem forbidden() {
        return Problem.of(
                "urn:problem-type:simpl:forbidden",
                null,
                "Forbidden",
                403,
                "You do not have the role needed for this operation.",
                null);
    }

    /**
     * 404 for an unknown vocabulary (VocabularyNotFoundResponse).
     *
     * @param name the vocabulary name
     * @return the exception
     */
    public static ProblemException vocabularyNotFound(String name) {
        String detail = "Vocabulary with name '" + name + "' was not found.";
        return notFound(detail, Issue.plain(Issue.IN_PATH, "vocabularyName", detail));
    }

    /**
     * 404 for an unknown version of a known vocabulary (decision D55).
     *
     * @param name the vocabulary name
     * @param version the version number as given
     * @return the exception
     */
    public static ProblemException versionNotFound(String name, String version) {
        String detail = "Version " + version + " of vocabulary '" + name + "' was not found.";
        return notFound(detail, Issue.plain(Issue.IN_PATH, "version", detail));
    }

    /**
     * 404 for an unknown external vocabulary name (decision D41).
     *
     * @param name the external vocabulary name
     * @return the exception
     */
    public static ProblemException externalNotFound(String name) {
        String detail = "External vocabulary with name '" + name + "' was not found.";
        return notFound(detail, Issue.plain(Issue.IN_PATH, "name", detail));
    }

    /**
     * 404 for an unknown external namespace (ExternalVocabularyNotFoundResponse).
     *
     * @param namespaceUri the namespace URI as given
     * @return the exception
     */
    public static ProblemException externalNamespaceNotFound(String namespaceUri) {
        String detail = "External vocabulary with namespaceUri '" + namespaceUri + "' was not found.";
        return notFound(detail, Issue.plain(Issue.IN_BODY, "namespaceUri", detail));
    }

    /**
     * 404 for a path that is not part of the API (decision D55).
     *
     * @return the problem
     */
    public static Problem unknownPath() {
        return Problem.of(
                "urn:problem-type:belgif:resourceNotFound",
                BELGIF_PROBLEMS + "resourceNotFound.html",
                "Resource Not Found",
                404,
                "No resource exists at this path.",
                null);
    }

    private static ProblemException notFound(String detail, Issue issue) {
        return new ProblemException(Problem.of(
                "urn:problem-type:belgif:resourceNotFound",
                BELGIF_PROBLEMS + "resourceNotFound.html",
                "Resource Not Found",
                404,
                detail,
                List.of(issue)));
    }

    /**
     * 405 for a method the path does not declare (decision D55).
     *
     * @param allow the methods the path declares
     * @return the exception
     */
    public static ProblemException methodNotAllowed(String allow) {
        return new ProblemException(
                Problem.of(
                        "urn:problem-type:simpl:methodNotAllowed",
                        null,
                        "Method Not Allowed",
                        405,
                        "The method is not allowed on this resource.",
                        null),
                Map.of("Allow", allow));
    }

    /**
     * 406 when the requested response media type cannot be served.
     *
     * @return the exception
     */
    public static ProblemException notAcceptable() {
        return new ProblemException(Problem.of(
                "urn:problem-type:simpl:notAcceptable",
                null,
                "Not Acceptable",
                406,
                "Requested media type cannot be served.",
                null));
    }

    /**
     * 409 conflict (decisions D4, D15, D30, D46, D55).
     *
     * @param detail the decided detail text
     * @return the exception
     */
    public static ProblemException conflict(String detail) {
        return new ProblemException(Problem.of("urn:problem-type:simpl:conflict", null, "Conflict", 409, detail, null));
    }

    /**
     * 413 for an upload above the limit (decision D15).
     *
     * @param field the form field that is too large
     * @return the exception
     */
    public static ProblemException payloadTooLarge(String field) {
        return new ProblemException(Problem.of(
                "urn:problem-type:simpl:payloadTooLarge",
                null,
                "Payload Too Large",
                413,
                TOO_LARGE_DETAIL,
                List.of(Issue.invalidInput(field, TOO_LARGE_DETAIL, null))));
    }

    /**
     * 413 for a JSON body above the limit (NFR-04).
     *
     * @return the exception
     */
    public static ProblemException bodyTooLarge() {
        return new ProblemException(Problem.of(
                "urn:problem-type:simpl:payloadTooLarge",
                null,
                "Payload Too Large",
                413,
                "The request body is too large.",
                null));
    }

    /**
     * 415 for an unsupported request media type (decision D15).
     *
     * @return the exception
     */
    public static ProblemException unsupportedMediaType() {
        return new ProblemException(Problem.of(
                "urn:problem-type:simpl:unsupportedMediaType",
                null,
                "Unsupported Media Type",
                415,
                "Request content type is not supported.",
                null));
    }

    /**
     * 500 with a body that reveals no internals.
     *
     * @return the problem
     */
    public static Problem internalError() {
        return Problem.of(
                "urn:problem-type:belgif:internalServerError",
                BELGIF_PROBLEMS + "internalServerError.html",
                "Internal Server Error",
                500,
                "Internal Server Error",
                null);
    }

    /**
     * 503 when a dependency is down (decision D44).
     *
     * @param detail which dependency, one of the two decided texts
     * @return the exception
     */
    public static ProblemException serviceUnavailable(String detail) {
        return new ProblemException(serviceUnavailableProblem(detail), Map.of("Retry-After", "5"));
    }

    /**
     * 503 body (decision D44).
     *
     * @param detail the decided detail text
     * @return the problem
     */
    public static Problem serviceUnavailableProblem(String detail) {
        return Problem.of(
                "urn:problem-type:belgif:serviceUnavailable",
                BELGIF_PROBLEMS + "serviceUnavailable.html",
                "Service Unavailable",
                503,
                detail,
                null);
    }
}
