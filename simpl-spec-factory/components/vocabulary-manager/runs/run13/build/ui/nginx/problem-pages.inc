# Answers that nginx gives itself are problem+json (D122, D158). Backend errors pass through.
proxy_intercept_errors off;
error_page 400 @bad_request;
error_page 405 @method_not_allowed;
error_page 408 @request_timeout;
error_page 413 @payload_too_large;
error_page 414 /__vm_problem/414;
error_page 494 =431 @header_too_large;
error_page 502 503 504 =503 @unavailable;

location @bad_request {
    default_type application/problem+json;
    return 400 '{"type":"urn:problem-type:belgif:badRequest","title":"Bad Request","status":400,"detail":"The request could not be read."}';
}
location @method_not_allowed {
    default_type application/problem+json;
    add_header Allow $vm_allow always;
    return 405 '{"type":"urn:problem-type:simpl:methodNotAllowed","title":"Method Not Allowed","status":405,"detail":"The method is not allowed on this resource."}';
}
location @request_timeout {
    default_type application/problem+json;
    return 408 '{"type":"urn:problem-type:simpl:requestTimeout","title":"Request Timeout","status":408,"detail":"The request was not received in time."}';
}
location @payload_too_large {
    default_type application/problem+json;
    return 413 '{"type":"urn:problem-type:simpl:payloadTooLarge","title":"Payload Too Large","status":413,"detail":"The request body is too large."}';
}
location = /__vm_problem/414 {
    internal;
    # From outside this path is unknown: the backend answers it like any unknown path (D155).
    error_page 404 = @to_backend;
    default_type application/problem+json;
    return 414 '{"type":"urn:problem-type:simpl:uriTooLong","title":"URI Too Long","status":414,"detail":"The request URI is too long."}';
}
location @header_too_large {
    default_type application/problem+json;
    return 431 '{"type":"urn:problem-type:simpl:requestHeaderFieldsTooLarge","title":"Request Header Fields Too Large","status":431,"detail":"The request headers are too large."}';
}
location @unavailable {
    default_type application/problem+json;
    add_header Retry-After 5 always;
    return 503 '{"type":"urn:problem-type:belgif:serviceUnavailable","title":"Service Unavailable","status":503,"detail":"The vocabulary service is not available. Retry later."}';
}
location @to_backend {
    include /etc/nginx/snippets/proxy.inc;
}
