package eu.simpl.catalogue.common;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Raised anywhere in a service to answer with an RFC 9457 Problem.
 * Either built from a problem type, or carrying a ready Problem body (pass-through of a downstream error).
 */
public class ApiException extends RuntimeException {
    private final int status;
    private final ProblemTypes type;
    private final ObjectNode body;

    public ApiException(ProblemTypes type, String detail) {
        super(detail);
        this.type = type;
        this.status = type.status;
        this.body = null;
    }

    public ApiException(ProblemTypes type, int status, String detail) {
        super(detail);
        this.type = type;
        this.status = status;
        this.body = null;
    }

    /** Pass a downstream Problem body through unchanged, with the given status. */
    public ApiException(int status, ObjectNode body) {
        super(body.path("detail").asText(""));
        this.type = null;
        this.status = status;
        this.body = body;
    }

    public int status() { return status; }
    public ProblemTypes type() { return type; }
    public ObjectNode body() { return body; }

    public String typeUri() {
        if (type != null) return type.uri;
        return body == null ? null : body.path("type").asText(null);
    }

    public static ApiException badRequest(String detail) { return new ApiException(ProblemTypes.BAD_REQUEST, detail); }
    public static ApiException invalidInput(String detail) { return new ApiException(ProblemTypes.INVALID_INPUT, detail); }
    public static ApiException forbidden(String detail) { return new ApiException(ProblemTypes.FORBIDDEN, detail); }
}
