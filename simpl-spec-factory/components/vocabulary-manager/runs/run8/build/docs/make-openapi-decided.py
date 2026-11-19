"""Builds docs/openapi-decided.yaml: the published contract with the decided changes (D79 and the decisions it cites)."""
import copy, sys, yaml

src, dst = sys.argv[1], sys.argv[2]
d = yaml.safe_load(open(src))
c = d["components"]; s = c["schemas"]; r = c["responses"]; p = c["parameters"]

d["info"]["version"] = "1.0.0-decided"
d["info"]["description"] = (
    d["info"]["description"]
    + "\n\nDecided version (D87): the published contract with every change of D79 applied, plus D17 (401/403),"
    " D27 (createdBy), D41/D66 (external 404 body), D44 (503 with Retry-After), D46 (409 on new versions),"
    " D63 (406) and D73 (absolute URIs). Not expressible per operation: an undeclared method returns 405"
    " application/problem+json (D67); a query parameter given twice returns 400 (D52); a multipart part with an empty"
    " name returns 400 (D70); the whole multipart request is at most 6 MB, the vocabularyFile part at most 5 MB"
    " (413, D15, D76); description and changelog limits apply after trimming (D37); content downloads have"
    " Content-Type exactly text/turtle without charset (D70).")
d["servers"] = [{"url": "http://localhost:8196/v1", "description": "Local build stack (build profile run 8)"}] + d["servers"]

# D40: OpenID Connect on the Keycloak realm of E1; scopes are neither requested nor checked (D16).
c["securitySchemes"] = {"OpenIdConnect": {
    "type": "openIdConnect",
    "description": "Bearer JWT from the Keycloak realm 'simpl' with audience vocabulary-manager-be (E1, E5). Realm roles"
                   " GA_VOCABULARY_VIEWER (reads) and GA_VOCABULARY_ADMIN (writes) are checked (D16).",
    "openIdConnectUrl": "http://localhost:8193/realms/simpl/.well-known/openid-configuration"}}
d["security"] = [{"OpenIdConnect": []}]

def problem_response(desc, example, headers=None):
    resp = {"description": desc, "content": {"application/problem+json": {
        "schema": {"$ref": "#/components/schemas/Problem"}, "example": example}}}
    if headers:
        resp["headers"] = headers
    return resp

r["UnauthorizedResponse"] = problem_response(
    "No token, or the token is invalid, expired, of another issuer or without the audience (D17, E5, E7).",
    {"type": "urn:problem-type:simpl:unauthorized", "title": "Unauthorized", "status": 401,
     "detail": "Authentication is required.", "instance": "urn:uuid:123e4567-e89b-12d3-a456-426614174000"},
    {"WWW-Authenticate": {"schema": {"type": "string"}, "example": "Bearer"}})
r["ForbiddenResponse"] = problem_response(
    "Valid token without the role this operation needs (D16, D17).",
    {"type": "urn:problem-type:simpl:forbidden", "title": "Forbidden", "status": 403,
     "detail": "You do not have the role this operation needs.", "instance": "urn:uuid:123e4567-e89b-12d3-a456-426614174000"})
r["ServiceUnavailableResponse"] = problem_response(
    "The store or the identity provider is not available (D44). After a 503 on a write, check with GET before resubmitting.",
    {"type": "urn:problem-type:belgif:serviceUnavailable",
     "href": "https://www.belgif.be/specification/rest/api-guide/problems/serviceUnavailable.html",
     "title": "Service Unavailable", "status": 503, "detail": "The vocabulary store is not available. Retry later.",
     "instance": "urn:uuid:123e4567-e89b-12d3-a456-426614174000"},
    {"Retry-After": {"schema": {"type": "integer"}, "example": 5}})
r["MethodNotAllowedResponse"] = problem_response(
    "The method is not declared for this path (D67, D77).",
    {"type": "urn:problem-type:simpl:methodNotAllowed", "title": "Method Not Allowed", "status": 405,
     "detail": "Method PUT is not allowed on this resource.", "instance": "urn:uuid:123e4567-e89b-12d3-a456-426614174000"},
    {"Allow": {"schema": {"type": "string"}, "example": "GET, POST"}})

ref = lambda name: {"$ref": "#/components/responses/" + name}
for path, ops in d["paths"].items():
    for method, op in ops.items():
        if method not in ("get", "post", "patch", "delete", "put"):
            continue
        resp = op["responses"]
        if path == "/health":
            continue
        resp.setdefault("401", ref("UnauthorizedResponse"))
        resp.setdefault("403", ref("ForbiddenResponse"))
        resp.setdefault("406", ref("NotAcceptableResponse"))
        resp["503"] = ref("ServiceUnavailableResponse")
        if path.startswith("/external-vocabularies/{name}"):
            resp["404"] = ref("ExternalVocabularyNotFoundResponse")
        if method in ("post", "patch", "delete") or op.get("parameters"):
            resp.setdefault("400", ref("BadRequestResponse"))
        op["responses"] = dict(sorted(resp.items(), key=lambda kv: kv[0]))

# D46: identical version is a conflict.
d["paths"]["/vocabularies/{vocabularyName}/versions"]["post"]["responses"]["409"] = ref("ConflictResponse")
d["paths"]["/vocabularies/{vocabularyName}/versions"]["post"]["responses"] = dict(
    sorted(d["paths"]["/vocabularies/{vocabularyName}/versions"]["post"]["responses"].items()))

# D19: name filter is a case-insensitive contains match.
p["VocabularyNameQuery"]["description"] = "Case-insensitive 'contains' match on the vocabulary name (D19)."
p["VocabularyNameQuery"]["schema"] = {"type": "string", "minLength": 1, "maxLength": 64, "pattern": "^[A-Za-z0-9]+$"}
# D20: date or date-time to the second.
for q in ("CreationDateQuery", "LastUpdatedQuery"):
    p[q]["description"] += " A date matches the whole UTC day; a date-time matches to the second (D20)."
# D22: pageSize at most 100.
p["PageSizeQuery"]["schema"]["maximum"] = 100
p["PageQuery"]["description"] = "Page number, from 1. A page beyond the last returns an empty items array (D22)."

# D37, D52, D7: payloads.
text_rule = lambda what: {"type": "string", "minLength": 1, "maxLength": 1000,
                          "description": what + " 1 to 1000 characters after trimming (D37)."}
file_prop = {"type": "string", "format": "binary",
             "description": "Non-empty, valid Turtle (RDF 1.1); at most 5 MB (D9, D15, D70)."}
for name in ("CreateVocabularyPayload", "CreateNewVocabularyVersionPayload"):
    sch = s[name]
    sch["additionalProperties"] = False
    sch["properties"]["vocabularyFile"] = file_prop
    sch["properties"]["description"] = {**text_rule("Description of the version."),
                                        "example": sch["properties"]["description"].get("example")}
    sch["properties"]["changelog"] = {**text_rule("Change log of the version."),
                                      "example": sch["properties"]["changelog"].get("example")}
s["CreateNewVocabularyVersionPayload"]["description"] += " A `version` field returns 400 (D7)."
ext = s["CreateExternalVocabularyPayload"]
ext["additionalProperties"] = False
ext["properties"]["vocabularyFile"] = {**file_prop, "description": "Non-empty, valid Turtle; syntax check only (D30, D70)."}
ext["properties"]["namespaceUri"].update({"maxLength": 2048, "description": "Absolute URI (RFC 3986), stored as sent (D73)."})
ext["properties"]["aliasUris"].update({"maxItems": 20, "uniqueItems": True,
    "description": "One multipart part per URI, at most 20, absolute, none equal to the namespace URI or to another alias"
                   " after removing a trailing '#' or '/' (D56, D73)."})
ext["properties"]["aliasUris"]["items"]["maxLength"] = 2048
ext["properties"]["sourceUri"].update({"maxLength": 2048, "description": "Absolute URI (D73)."})
ext["properties"]["description"].update({"maxLength": 1000, "description": "At most 1000 characters after trimming (D56)."})
s["ExternalVocabularyContentRequest"]["additionalProperties"] = False
s["ExternalVocabularyContentRequest"]["properties"]["namespaceUri"]["maxLength"] = 2048
s["VocabularyStatusPatchRequest"]["additionalProperties"] = False

# D38: search limits.
s["VocabularySearchRequest"]["properties"]["items"]["maxItems"] = 100
for arr in ("namespaceUris", "aliasUris"):
    s["ExternalVocabularySearchRequest"]["properties"][arr].update({"minItems": 1, "maxItems": 100})
s["ExternalVocabularySearchRequest"]["description"] += (
    " At most 100 URIs in total over namespaceUris and aliasUris (D38). Results are not paged: page 1, pageSize 100,"
    " no paging links.")
s["VocabularySearchResponse"]["description"] = (
    "Not paged (D38): page 1, pageSize 100, total = items returned, totalPages 1 (0 when empty), no paging links.")

# D10: warnings, closed code set, always present.
s["VocabularyValidationWarning"]["required"] = ["code", "description", "location"]
s["VocabularyValidationWarning"]["properties"]["code"]["enum"] = [
    "externalReferenceSkipped", "deprecatedVocabularyReference", "semanticReasoningSkipped"]

# D27, D5: createdBy on version objects, latestVersionHref always filled.
created_by = {"type": "string", "description": "Email of the creator, or preferred_username (D27).",
              "example": "ga-admin@example.com"}
for name in ("VocabularyMetadata", "VocabularyVersionMetadataResponse"):
    props = s[name]["properties"]
    new = {}
    for k, v in props.items():
        new[k] = v
        if k == "status":
            new["createdBy"] = created_by
    if "latestVersionHref" not in new:
        new = {**{k: new[k] for k in ("href", "contentHref")},
               "latestVersionHref": {"type": "string", "example": "/vocabularies/HealthcareVocabulary/versions/1"},
               **{k: v for k, v in new.items() if k not in ("href", "contentHref")}}
    new["latestVersionHref"] = {"type": "string", "description": "Always filled, also on the latest version (D5).",
                                "example": "/vocabularies/HealthcareVocabulary/versions/3"}
    s[name]["properties"] = new
s["VocabularyVersionMetadataResponse"]["required"] = ["validationWarnings"]
s["VocabularyVersionMetadataResponse"]["properties"]["validationWarnings"]["description"] = (
    "Always present, an empty array when there are none (D10).")

class Dumper(yaml.SafeDumper):
    def ignore_aliases(self, data):
        return True
header = ("# Vocabulary Management API, decided version (D87). Generated from specs/api/vocabulary_openapi-tier1.yaml\n"
          "# by applying the contract changes of D79; the published file stays unchanged (D39).\n")
with open(dst, "w") as out:
    out.write(header)
    yaml.dump(d, out, Dumper=Dumper, sort_keys=False, allow_unicode=True, width=120)
