package eu.simpl.vocabularymanager.error;

import java.util.List;
import java.util.Map;

/** Factory methods for the problems whose texts the decisions fix. */
public final class Problems {

    public static final String BAD_REQUEST_DETAIL = "The input message is incorrect";
    public static final String STORE_UNAVAILABLE = "The vocabulary store is not available. Retry later.";
    public static final String IDP_UNAVAILABLE = "The identity provider is not available. Retry later.";
    public static final String TOO_MANY_UPLOADS = "Too many uploads in progress. Retry later.";
    public static final String FILE_TOO_LARGE =
            "Uploaded vocabulary file is too large. The maximum allowed upload size is"
                    + " 5 MB. Please upload a smaller Turtle file.";
    public static final String JSON_TOO_LARGE = "The request body is larger than 1 MB.";
    public static final String RETRY_AFTER_SECONDS = "5";

    private Problems() {}

    public static ApiException badRequest(List<ProblemIssue> issues) {
        return new ApiException(ProblemType.BAD_REQUEST, BAD_REQUEST_DETAIL, issues);
    }

    public static ApiException badRequest(ProblemIssue issue) {
        return badRequest(List.of(issue));
    }

    public static ApiException vocabularyNotFound(String name) {
        String detail = "Vocabulary with name '" + name + "' was not found.";
        return new ApiException(
                ProblemType.NOT_FOUND, detail, List.of(ProblemIssue.reference("path", "vocabularyName", detail)));
    }

    public static ApiException versionNotFound(String name, int version) {
        String detail = "Version " + version + " of vocabulary '" + name + "' was not found.";
        return new ApiException(
                ProblemType.NOT_FOUND, detail, List.of(ProblemIssue.reference("path", "version", detail)));
    }

    public static ApiException externalNotFound(String name) {
        String detail = "External vocabulary with name '" + name + "' was not found.";
        return new ApiException(ProblemType.NOT_FOUND, detail, List.of(ProblemIssue.reference("path", "name", detail)));
    }

    public static ApiException externalNamespaceNotFound(String uri) {
        String detail = "External vocabulary with namespaceUri '" + uri + "' was not found.";
        return new ApiException(
                ProblemType.NOT_FOUND, detail, List.of(ProblemIssue.reference("body", "namespaceUri", detail)));
    }

    public static ApiException conflict(String detail) {
        return new ApiException(ProblemType.CONFLICT, detail);
    }

    public static ApiException fileTooLarge() {
        return new ApiException(
                ProblemType.PAYLOAD_TOO_LARGE,
                FILE_TOO_LARGE,
                List.of(ProblemIssue.invalidInput("body", "vocabularyFile", FILE_TOO_LARGE, null)));
    }

    public static ApiException jsonTooLarge() {
        return new ApiException(ProblemType.PAYLOAD_TOO_LARGE, JSON_TOO_LARGE);
    }

    public static ApiException storeUnavailable() {
        return serviceUnavailable(STORE_UNAVAILABLE);
    }

    public static ApiException identityProviderUnavailable() {
        return serviceUnavailable(IDP_UNAVAILABLE);
    }

    public static ApiException tooManyUploads() {
        return serviceUnavailable(TOO_MANY_UPLOADS);
    }

    private static ApiException serviceUnavailable(String detail) {
        return new ApiException(
                ProblemType.SERVICE_UNAVAILABLE, detail, List.of(), Map.of("Retry-After", RETRY_AFTER_SECONDS), false);
    }

    public static ApiException unsupportedMediaType() {
        return new ApiException(ProblemType.UNSUPPORTED_MEDIA_TYPE, "Request content type is not supported.");
    }

    public static ApiException internal() {
        return new ApiException(ProblemType.INTERNAL_SERVER_ERROR, "Internal Server Error");
    }
}
