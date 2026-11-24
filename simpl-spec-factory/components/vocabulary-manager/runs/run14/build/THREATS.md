# Threat model (D186), STRIDE per operation group

| Group | Threat | Mitigation or decision |
|---|---|---|
| All API | Spoofing: forged or foreign tokens | Signature with the realm keys (Nimbus), issuer and audience checked, no clock skew, unknown key id refetched at most once per 10 s, fail closed with 503 when keys cannot be fetched (D17, D44, D88, D132) |
| All API | Spoofing: brute force of tokens or passwords | Login at Keycloak; failed-token limiting belongs to the Tier 1 gateway (D164, D185) |
| All API | Elevation of privilege | Default deny: operation table with a role rule per operation, start fails for a handler without a rule, 403 before body checks (D16, D84) |
| All API | Information disclosure in errors | Problem bodies without stack traces or store details (BLG-err-sanit); 401/403 texts without token content (D197) |
| All API | Denial of service | Limits of Q2 (sizes, items, pages, parts), 4 upload slots per instance (D92), proxy body limit, timeouts and circuit breakers (D44, D103); call-rate limit at the gateway (D46) |
| All API | Repudiation | createdBy per version and external vocabulary; logs carry token subject, operation, correlation id (D27, D80, D86) |
| Create, new version | Tampering: injection into the store | SPARQL built from fixed templates with values written by Jena's term formatter in one pass (D94, REUSE.md); names checked by pattern |
| Create, new version | Malicious content (huge or binary files, deep nesting) | 5 MB file limit, 6 MB request, parse with Jena RIOT, report capped at 1000 issues (D15, D76, D125) |
| Create, new version | Lost updates or duplicates under concurrency | Conditional store updates with receipts, one transaction per write (D8, D43, D82) |
| Status changes | Unwanted deprecation | Admin role only; UI confirmation dialog (D209); reversible by PATCH (D4) |
| Downloads | Content served as active content | Content-Type exactly text/turtle with attachment disposition; UI CSP without inline scripts (D70, D123) |
| External vocabularies | Server-side request forgery | The service never fetches URIs; namespace and source URIs are stored as text only (OWA-API7, E3) |
| Store | Tampering or reading by others | No host port, internal network, three users with least privilege, no anonymous access (D83, D107) |
| Identity provider down | Denial of service | Cached keys keep working; health DEGRADED (D44) |
| Logs | Disclosure of personal data | No email, name or token in any log (D80, D173) |

Alert path: the platform's (D185, D186). Metrics an alert would use: `vocabulary_api_requests_seconds_count`
with status 401, 403 and 5xx, `resilience4j_circuitbreaker_state`, and `GET /v1/health`.
