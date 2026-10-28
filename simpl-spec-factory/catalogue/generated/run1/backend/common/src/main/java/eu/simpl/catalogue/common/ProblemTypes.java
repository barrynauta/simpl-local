package eu.simpl.catalogue.common;

/** Problem type URIs of digest section 6.6, with their titles. */
public enum ProblemTypes {
    BAD_REQUEST("urn:problem-type:simpl:badRequest", "Bad Request", 400),
    INVALID_INPUT("urn:problem-type:simpl:catalogue:invalidInput", "Invalid input", 400),
    UNAUTHORIZED("urn:problem-type:simpl:unauthorized", "Unauthorized", 401),
    FORBIDDEN("urn:problem-type:simpl:forbidden", "Forbidden", 403),
    RESOURCE_NOT_FOUND("urn:problem-type:simpl:resourceNotFound", "Not Found", 404),
    NOT_FOUND("urn:problem-type:simpl:notFound", "Not Found", 404),
    METHOD_NOT_ALLOWED("urn:problem-type:simpl:methodNotAllowed", "Method Not Allowed", 405),
    NOT_ACCEPTABLE("urn:problem-type:simpl:notAcceptable", "Not Acceptable", 406),
    UNSUPPORTED_MEDIA_TYPE("urn:problem-type:simpl:unsupportedMediaType", "Unsupported Media Type", 415),
    INTERNAL("urn:problem-type:simpl:internalServerError", "Internal Server Error", 500),
    NOT_IMPLEMENTED("urn:problem-type:simpl:notImplemented", "Not Implemented", 501),
    PROVIDER_MISMATCH("urn:problem-type:simpl:catalogue:providerMismatch", "Provider mismatch", 403),
    DISCOVERY_POLICY_DENIED("urn:problem-type:simpl:catalogue:discoveryPolicyDenied", "Discovery policy denied", 403),
    ALREADY_EXISTS("urn:problem-type:simpl:catalogue:alreadyExists", "Already exists", 409),
    VERSION_CONFLICT("urn:problem-type:simpl:catalogue:versionConflict", "Version conflict", 409),
    INVALID_SIGNATURE("urn:problem-type:simpl:catalogue:invalidSignature", "Invalid signature", 422),
    SCHEMA_NOT_SUPPORTED("urn:problem-type:simpl:catalogue:schemaNotSupported", "Schema not supported", 422),
    SHACL_VIOLATION("urn:problem-type:simpl:catalogue:shaclViolation", "SHACL violation", 422),
    QUALITY_BELOW_THRESHOLD("urn:problem-type:simpl:catalogue:qualityBelowThreshold", "Quality below threshold", 422),
    QUALITY_CONFIGURATION_ERROR("urn:problem-type:simpl:catalogue:qualityConfigurationError", "Quality configuration error", 500),
    QUALITY_ASSESSMENT_FAILED("urn:problem-type:simpl:catalogue:qualityAssessmentFailed", "Quality assessment failed", 502),
    FEDERATION_UNAUTHENTICATED("urn:problem-type:simpl:catalogue:federationUnauthenticated", "Federation unauthenticated", 502),
    FEDERATION_UNAVAILABLE("urn:problem-type:simpl:catalogue:federationUnavailable", "Federation unavailable", 502),
    FEDERATION_TIMEOUT("urn:problem-type:simpl:catalogue:federationTimeout", "Federation timeout", 504),
    SCHEMA_NOT_FOUND("urn:problem-type:simpl:catalogue:schemaNotFound", "Schema not found", 404),
    ACTIVE_QUALITY_PROFILE_NOT_FOUND("urn:problem-type:simpl:catalogue:activeQualityProfileNotFound", "Active quality profile not found", 404);

    public final String uri;
    public final String title;
    public final int status;

    ProblemTypes(String uri, String title, int status) {
        this.uri = uri;
        this.title = title;
        this.status = status;
    }
}
