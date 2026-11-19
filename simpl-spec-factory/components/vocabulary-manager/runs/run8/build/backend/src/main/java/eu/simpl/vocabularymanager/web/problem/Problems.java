package eu.simpl.vocabularymanager.web.problem;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Factory of every problem the API returns. Problem types follow D48: belgif types for 400, 404, 500 and 503, simpl
 * types for 401, 403, 405, 406, 409, 413 and 415.
 */
public final class Problems {

    public static final String BAD_REQUEST_DETAIL = "The input message is incorrect";
    public static final String RETRY_AFTER_SECONDS = "5";
    private static final String BELGIF_PROBLEMS = "https://www.belgif.be/specification/rest/api-guide/problems/";

    private Problems() {}

    private static String instance() {
        return "urn:uuid:" + UUID.randomUUID();
    }

    /**
     * 400 with input issues (schema violations or content validation errors).
     *
     * @param issues the issues, at least one
     * @return the exception to throw
     */
    public static ApiException badRequest(List<ProblemIssue> issues) {
        return new ApiException(badRequestProblem(issues));
    }

    /**
     * 400 with a single schema violation.
     *
     * @param in location kind
     * @param name input name
     * @param detail explanation
     * @return the exception to throw
     */
    public static ApiException schemaViolation(String in, String name, String detail) {
        return badRequest(List.of(ProblemIssue.schemaViolation(in, name, detail)));
    }

    /**
     * @param issues the issues, or null for a request the firewall refused
     * @return the problem body of a 400
     */
    public static Problem badRequestProblem(List<ProblemIssue> issues) {
        return new Problem(
                "urn:problem-type:belgif:badRequest",
                BELGIF_PROBLEMS + "badRequest.html",
                "Bad Request",
                400,
                BAD_REQUEST_DETAIL,
                instance(),
                issues);
    }

    /**
     * 401 for a missing or invalid token (D17).
     *
     * @param detail explanation
     * @return the problem
     */
    public static Problem unauthorized(String detail) {
        return new Problem("urn:problem-type:simpl:unauthorized", null, "Unauthorized", 401, detail, instance(), null);
    }

    /**
     * 403 for a valid token without the needed role (D17).
     *
     * @return the problem
     */
    public static Problem forbidden() {
        return new Problem(
                "urn:problem-type:simpl:forbidden",
                null,
                "Forbidden",
                403,
                "You do not have the role this operation needs.",
                instance(),
                null);
    }

    /**
     * 404 for a path that matches no operation (D55, D67).
     *
     * @return the problem
     */
    public static Problem unknownPath() {
        return new Problem(
                "urn:problem-type:belgif:resourceNotFound",
                BELGIF_PROBLEMS + "resourceNotFound.html",
                "Resource Not Found",
                404,
                "The requested resource does not exist.",
                instance(),
                null);
    }

    /**
     * 404 with one issue (VocabularyNotFoundResponse, ExternalVocabularyNotFoundResponse).
     *
     * @param detail the decided text
     * @param in issue location kind
     * @param name issue input name
     * @return the exception to throw
     */
    public static ApiException notFound(String detail, String in, String name) {
        return new ApiException(new Problem(
                "urn:problem-type:belgif:resourceNotFound",
                BELGIF_PROBLEMS + "resourceNotFound.html",
                "Resource Not Found",
                404,
                detail,
                instance(),
                List.of(ProblemIssue.plain(in, name, detail))));
    }

    /**
     * 404 for an unknown vocabulary (D66).
     *
     * @param name the vocabulary name
     * @return the exception to throw
     */
    public static ApiException vocabularyNotFound(String name) {
        return notFound("Vocabulary with name '" + name + "' was not found.", "path", "vocabularyName");
    }

    /**
     * 404 for an unknown version (D55).
     *
     * @param name the vocabulary name
     * @param version the version number
     * @return the exception to throw
     */
    public static ApiException versionNotFound(String name, long version) {
        return notFound("Version " + version + " of vocabulary '" + name + "' was not found.", "path", "version");
    }

    /**
     * 404 for an unknown external vocabulary name (D41).
     *
     * @param name the external vocabulary name
     * @return the exception to throw
     */
    public static ApiException externalVocabularyNotFound(String name) {
        return notFound("External vocabulary with name '" + name + "' was not found.", "path", "name");
    }

    /**
     * 405 for a method the path does not offer (D67, D77).
     *
     * @param method the request method
     * @return the problem
     */
    public static Problem methodNotAllowed(String method) {
        return new Problem(
                "urn:problem-type:simpl:methodNotAllowed",
                null,
                "Method Not Allowed",
                405,
                "Method " + sanitize(method) + " is not allowed on this resource.",
                instance(),
                null);
    }

    /**
     * 406 (D63).
     *
     * @return the exception to throw
     */
    public static ApiException notAcceptable() {
        return new ApiException(new Problem(
                "urn:problem-type:simpl:notAcceptable",
                null,
                "Not Acceptable",
                406,
                "Requested media type cannot be served.",
                instance(),
                null));
    }

    /**
     * 409 (D4, D15, D30, D46, D55).
     *
     * @param detail the decided text
     * @return the exception to throw
     */
    public static ApiException conflict(String detail) {
        return new ApiException(
                new Problem("urn:problem-type:simpl:conflict", null, "Conflict", 409, detail, instance(), null));
    }

    /**
     * 413 (D15, D76).
     *
     * @param name the input name the limit applies to
     * @return the exception to throw
     */
    public static ApiException payloadTooLarge(String name) {
        String detail = "Uploaded vocabulary file is too large. The maximum allowed upload size is 5 MB. "
                + "Please upload a smaller Turtle file.";
        return new ApiException(new Problem(
                "urn:problem-type:simpl:payloadTooLarge",
                null,
                "Payload Too Large",
                413,
                detail,
                instance(),
                List.of(ProblemIssue.invalidInput("body", name, detail, null))));
    }

    /**
     * 415 (D15).
     *
     * @return the exception to throw
     */
    public static ApiException unsupportedMediaType() {
        return new ApiException(new Problem(
                "urn:problem-type:simpl:unsupportedMediaType",
                null,
                "Unsupported Media Type",
                415,
                "Request content type is not supported.",
                instance(),
                null));
    }

    /**
     * 500 without internal details (BLG-err-sanit).
     *
     * @return the problem
     */
    public static Problem internalServerError() {
        return new Problem(
                "urn:problem-type:belgif:internalServerError",
                BELGIF_PROBLEMS + "internalServerError.html",
                "Internal Server Error",
                500,
                "Internal Server Error",
                instance(),
                null);
    }

    /**
     * 503 with Retry-After (D44).
     *
     * @param detail the decided text for the store or the identity provider
     * @param cause the cause
     * @return the exception to throw
     */
    public static ApiException serviceUnavailable(String detail, Throwable cause) {
        return new ApiException(serviceUnavailableProblem(detail), Map.of("Retry-After", RETRY_AFTER_SECONDS), cause);
    }

    /**
     * @param detail the decided text
     * @return the problem body of a 503
     */
    public static Problem serviceUnavailableProblem(String detail) {
        return new Problem(
                "urn:problem-type:belgif:serviceUnavailable",
                BELGIF_PROBLEMS + "serviceUnavailable.html",
                "Service Unavailable",
                503,
                detail,
                instance(),
                null);
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.replaceAll("[^A-Za-z0-9_-]", "");
        return cleaned.length() > 20 ? cleaned.substring(0, 20) : cleaned;
    }
}
