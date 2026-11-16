package eu.simpl.cm.common;

/** Problem type URIs of D11. */
public final class ProblemTypes {
    private static final String P = "urn:problem-type:simpl:";
    private static final String CM = "urn:problem-type:simpl:contract-management:";

    public static final String UNAUTHORIZED = P + "unauthorized";
    public static final String FORBIDDEN = P + "forbidden";
    public static final String NOT_FOUND = P + "notFound";
    public static final String METHOD_NOT_SUPPORTED = P + "methodNotSupported";
    public static final String NOT_ACCEPTABLE = P + "notAcceptable";
    public static final String UNSUPPORTED_MEDIA_TYPE = P + "unsupportedMediaType";
    public static final String ARGUMENT_TYPE_MISMATCH = P + "argumentTypeMismatch";
    public static final String VALIDATION_FAILURE = P + "validationFailure";
    public static final String INTERNAL_SERVER_ERROR = P + "internalServerError";
    public static final String SERVICE_UNAVAILABLE = P + "serviceUnavailable";

    public static final String WRONG_PARTY = CM + "wrongParty";
    public static final String INVALID_TRANSITION = CM + "invalidTransition";
    public static final String ALREADY_EXISTS = CM + "alreadyExists";
    public static final String OFFER_NOT_PENDING = CM + "offerNotPending";
    public static final String INVALID_DOCUMENT = CM + "invalidDocument";

    private ProblemTypes() {
    }
}
