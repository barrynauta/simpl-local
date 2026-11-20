package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.api.ProblemException.Problem;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The problem responses of the component. Types follow D48: belgif for 400, 404, 500 and 503; simpl for 401, 403,
 * 405, 406, 409, 413 and 415.
 */
public final class Problems {

    private static final String PROBLEMS = "https://www.belgif.be/specification/rest/api-guide/problems/";
    private static final String BAD_REQUEST_DETAIL = "The input message is incorrect";
    private static final String TOO_LARGE_DETAIL = "Uploaded vocabulary file is too large. The maximum allowed upload"
            + " size is 5 MB. Please upload a smaller Turtle file.";

    private Problems() {}

    /** 400 with issues (D21, D37, D49, D52). */
    public static Problem badRequest(List<Issue> issues) {
        return new Problem(
                400,
                "urn:problem-type:belgif:badRequest",
                PROBLEMS + "badRequest.html",
                "Bad Request",
                BAD_REQUEST_DETAIL,
                issues,
                Map.of());
    }

    /** 400 with one issue. */
    public static ProblemException badRequest(Issue issue) {
        return new ProblemException(badRequest(List.of(issue)));
    }

    /** 400 of the request firewall: no issues (D67). */
    public static Problem rejectedRequest() {
        return badRequest(List.of());
    }

    /** 401 (D17). */
    public static Problem unauthorized() {
        return simple(401, "urn:problem-type:simpl:unauthorized", "Unauthorized", "A valid bearer token is required.");
    }

    /** 403 (D17). */
    public static Problem forbidden() {
        return simple(
                403, "urn:problem-type:simpl:forbidden", "Forbidden", "You are not authorized to perform this action.");
    }

    /** 404 for a path that matches no operation (D55, D67). */
    public static Problem unknownResource() {
        return new Problem(
                404,
                "urn:problem-type:belgif:resourceNotFound",
                PROBLEMS + "resourceNotFound.html",
                "Resource Not Found",
                "The requested resource does not exist.",
                List.of(),
                Map.of());
    }

    /** 404 with the decided detail and one issue (D41, D55, D66). */
    public static Problem notFound(String detail, String in, String name, Object value) {
        return new Problem(
                404,
                "urn:problem-type:belgif:resourceNotFound",
                PROBLEMS + "resourceNotFound.html",
                "Resource Not Found",
                detail,
                List.of(Issue.notFound(in, name, detail, value)),
                Map.of());
    }

    /** 405 (D67, D77). */
    public static Problem methodNotAllowed(String allow) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (allow != null && !allow.isEmpty()) {
            headers.put("Allow", allow);
        }
        return new Problem(
                405,
                "urn:problem-type:simpl:methodNotAllowed",
                null,
                "Method Not Allowed",
                "The request method is not supported by this resource.",
                List.of(),
                headers);
    }

    /** 406 (D63). */
    public static Problem notAcceptable() {
        return simple(
                406,
                "urn:problem-type:simpl:notAcceptable",
                "Not Acceptable",
                "Requested media type cannot be served.");
    }

    /** 409 (D4, D15, D46, D55). */
    public static Problem conflict(String detail) {
        return simple(409, "urn:problem-type:simpl:conflict", "Conflict", detail);
    }

    /** 413 (D15, D76). */
    public static Problem payloadTooLarge() {
        return new Problem(
                413,
                "urn:problem-type:simpl:payloadTooLarge",
                null,
                "Payload Too Large",
                TOO_LARGE_DETAIL,
                List.of(Issue.invalidInput("vocabularyFile", TOO_LARGE_DETAIL, null)),
                Map.of());
    }

    /** 413 for a JSON body over its limit. */
    public static Problem jsonTooLarge() {
        return simple(
                413, "urn:problem-type:simpl:payloadTooLarge", "Payload Too Large", "The request body is too large.");
    }

    /** 415 (D15). */
    public static Problem unsupportedMediaType() {
        return simple(
                415,
                "urn:problem-type:simpl:unsupportedMediaType",
                "Unsupported Media Type",
                "Request content type is not supported.");
    }

    /** 500 without internals (BLG-err-sanit). */
    public static Problem internalError() {
        return new Problem(
                500,
                "urn:problem-type:belgif:internalServerError",
                PROBLEMS + "internalServerError.html",
                "Internal Server Error",
                "Internal Server Error",
                List.of(),
                Map.of());
    }

    /** 503 with Retry-After 5 (D44, D92). */
    public static Problem serviceUnavailable(String detail) {
        return new Problem(
                503,
                "urn:problem-type:belgif:serviceUnavailable",
                PROBLEMS + "serviceUnavailable.html",
                "Service Unavailable",
                detail,
                List.of(),
                Map.of("Retry-After", "5"));
    }

    /** Any other status, used by the error page for errors raised outside the API code. */
    public static Problem generic(int status, String title) {
        if (status == 404) {
            return unknownResource();
        }
        if (status == 405) {
            return methodNotAllowed(null);
        }
        if (status >= 500) {
            return internalError();
        }
        if (status == 400) {
            return rejectedRequest();
        }
        return simple(status, "urn:problem-type:simpl:httpError", title, title);
    }

    private static Problem simple(int status, String type, String title, String detail) {
        return new Problem(status, type, null, title, detail, List.of(), Map.of());
    }

    /** Wire form of a problem, with a fresh instance URN. */
    public static Map<String, Object> toMap(Problem problem) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", problem.type());
        if (problem.href() != null) {
            map.put("href", problem.href());
        }
        map.put("title", problem.title());
        map.put("status", problem.status());
        map.put("detail", problem.detail());
        map.put("instance", "urn:uuid:" + UUID.randomUUID());
        if (!problem.issues().isEmpty()) {
            map.put("issues", problem.issues().stream().map(Issue::toMap).toList());
        }
        return map;
    }
}
