package eu.simpl.cm.common;

/**
 * An error that becomes an RFC 9457 problem response (D11).
 */
public class ApiException extends RuntimeException {

    private final int status;
    private final String type;
    private final String title;

    public ApiException(int status, String type, String title, String detail) {
        super(detail);
        this.status = status;
        this.type = type;
        this.title = title;
    }

    public int getStatus() {
        return status;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return getMessage();
    }

    public static ApiException unauthorized(String detail) {
        return new ApiException(401, ProblemTypes.UNAUTHORIZED, "Unauthorized", detail);
    }

    public static ApiException forbidden(String detail) {
        return new ApiException(403, ProblemTypes.FORBIDDEN, "Forbidden", detail);
    }

    public static ApiException notFound(String detail) {
        return new ApiException(404, ProblemTypes.NOT_FOUND, "Not Found", detail);
    }

    public static ApiException argumentTypeMismatch(String detail) {
        return new ApiException(400, ProblemTypes.ARGUMENT_TYPE_MISMATCH, "Bad Request", detail);
    }

    public static ApiException validation(String detail) {
        return new ApiException(400, ProblemTypes.VALIDATION_FAILURE, "Bad Request", detail);
    }

    public static ApiException wrongParty(String detail) {
        return new ApiException(400, ProblemTypes.WRONG_PARTY, "Bad Request", detail);
    }

    public static ApiException offerNotPending(String detail) {
        return new ApiException(400, ProblemTypes.OFFER_NOT_PENDING, "Bad Request", detail);
    }

    public static ApiException alreadyExists(String detail) {
        return new ApiException(400, ProblemTypes.ALREADY_EXISTS, "Bad Request", detail);
    }

    /** Invalid transition: 400 on OpenAPI operations, 409 on added operations (D37, D40). */
    public static ApiException invalidTransition(int status, String detail) {
        return new ApiException(status, ProblemTypes.INVALID_TRANSITION, status == 409 ? "Conflict" : "Bad Request", detail);
    }

    public static ApiException invalidDocument(int status, String detail) {
        String title = switch (status) {
            case 413 -> "Payload Too Large";
            case 415 -> "Unsupported Media Type";
            default -> "Bad Request";
        };
        return new ApiException(status, ProblemTypes.INVALID_DOCUMENT, title, detail);
    }

    public static ApiException internal(String detail) {
        return new ApiException(500, ProblemTypes.INTERNAL_SERVER_ERROR, "Server Error", detail);
    }
}
