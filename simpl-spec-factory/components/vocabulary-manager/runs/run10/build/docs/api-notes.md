# API notes

Base URL `http://localhost:8196/v1`. The decided contract is `openapi-decided.yaml`; the published file in
`input/specs/api` is unchanged (D39). This page lists behaviour the build chose where the contract and decisions
are silent, and the free texts (D55).

## Problem bodies

All errors are `application/problem+json` (RFC 9457, Belgif shape) with `type`, `title`, `status`, `detail`,
`instance` (`urn:uuid:...`) and, where useful, `issues`.

| Status | type | Notes |
|---|---|---|
| 400 | `urn:problem-type:belgif:badRequest` | issues: `urn:problem-type:belgif:input-validation:schemaViolation` for request shape, `...:invalidInput` for content validation (with `location`) |
| 401 | `urn:problem-type:simpl:unauthorized` | header `WWW-Authenticate: Bearer` |
| 403 | `urn:problem-type:simpl:forbidden` | |
| 404 | `urn:problem-type:belgif:resourceNotFound` | one issue with `in` and `name` for vocabulary, version and external vocabulary lookups; an unknown path has no issues |
| 405 | `urn:problem-type:simpl:methodNotAllowed` | header `Allow` |
| 406 | `urn:problem-type:simpl:notAcceptable` | |
| 409 | `urn:problem-type:simpl:conflict` | |
| 413 | `urn:problem-type:simpl:payloadTooLarge` | one issue `in: body`, `name: vocabularyFile` (or `body` for a JSON body over 1 MB) |
| 415 | `urn:problem-type:simpl:unsupportedMediaType` | |
| 500 | `urn:problem-type:belgif:internalServerError` | no internals in the body |
| 503 | `urn:problem-type:belgif:serviceUnavailable` | header `Retry-After: 5` |

## Choices the build made (not fixed by a decision)

- External vocabulary name conflict detail: `External vocabulary with name '<Name>' already exists.`
- Several 400 issues are returned together (path, query and body issues of one request).
- A JSON body with duplicate keys, trailing content or that is not an object gives 400 with an issue `name: body`.
- Content validation reports at most 1000 issues per response (the rest is cut).
- Syntax error detail: `Encountered invalid Turtle syntax: <parser message>`.
- A part sent as a file or as a text field is read the same way; the part's own Content-Type is not checked.
- External registration resolves relative IRIs against the namespace URI (no decision fixes a base for it).
- `HEAD` and `OPTIONS` are not declared operations and get 405 like any other undeclared method.
- `Content-Disposition` uses `attachment; filename="<Name>.ttl"` (latest) and `"<Name>-v<n>.ttl"`.
- Description and changelog are stored trimmed.
- The identical-version check (D46) compares the SHA-256 of the uploaded bytes with the stored one.

## Known limitation

A multipart part with an empty name (`name=""`) is dropped by the servlet container's multipart parser before the
application sees it, so it does not produce the 400 of D70 (see DEVIATIONS.md).
