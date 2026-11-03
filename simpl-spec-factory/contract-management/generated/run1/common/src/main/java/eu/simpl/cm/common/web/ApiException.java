package eu.simpl.cm.common.web;

/** An error that maps to one RFC 9457 problem response. */
public class ApiException extends RuntimeException {
    private final int status;
    private final String type;
    private final transient Object body;

    public ApiException(int status, String type, String detail) {
        this(status, type, detail, null);
    }

    /** {@code body}, when set, is sent as is (used to pass through a problem from a downstream service). */
    public ApiException(int status, String type, String detail, Object body) {
        super(detail);
        this.status = status;
        this.type = type;
        this.body = body;
    }

    public int getStatus() {
        return status;
    }

    public String getType() {
        return type;
    }

    public Object getBody() {
        return body;
    }

    public static ApiException validation(String detail) {
        return new ApiException(400, Problems.VALIDATION_FAILURE, detail);
    }

    public static ApiException typeMismatch(String detail) {
        return new ApiException(400, Problems.ARGUMENT_TYPE_MISMATCH, detail);
    }

    public static ApiException notFound(String detail) {
        return new ApiException(404, Problems.NOT_FOUND, detail);
    }

    public static ApiException cm(int status, String cmType, String detail) {
        return new ApiException(status, Problems.CM_PREFIX + cmType, detail);
    }
}
