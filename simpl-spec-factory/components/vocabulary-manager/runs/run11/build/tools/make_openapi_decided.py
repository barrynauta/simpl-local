"""Builds docs/openapi-decided.yaml: the published OpenAPI with every change of D79 applied (D87).

Usage: python3 tools/make_openapi_decided.py <published openapi> <output>
The published file is not changed (D39).
"""
import copy
import sys

import yaml

src, out = sys.argv[1], sys.argv[2]
api = yaml.safe_load(open(src, encoding="utf-8"))
c = api["components"]
p = c["parameters"]
s = c["schemas"]
r = c["responses"]

api["info"]["version"] = "1.0.0-decided"
api["info"]["description"] += (
    " This is the decided contract of the factory build: the published file with the changes of "
    "decision D79 (and D91, D110, D116, D121, D125) applied."
)
# D40: OpenID Connect through the Keycloak realm, no scopes checked.
c["securitySchemes"] = {
    "OpenIdConnect": {
        "type": "openIdConnect",
        "openIdConnectUrl": "http://localhost:8193/realms/simpl/.well-known/openid-configuration",
        "description": "Bearer token from the Keycloak realm 'simpl', audience vocabulary-manager-be. "
        "Realm roles GA_VOCABULARY_VIEWER (reads) and GA_VOCABULARY_ADMIN (reads and writes) (D16).",
    }
}
api["security"] = [{"OpenIdConnect": []}]

INT32 = {"type": "integer", "format": "int32", "minimum": 1, "maximum": 2147483647}
p["PageQuery"]["schema"] = {**INT32, "default": 1}
p["PageSizeQuery"]["schema"] = {**INT32, "maximum": 100, "default": 20}  # D22
p["VocabularyNameQuery"]["schema"] = {"type": "string", "minLength": 1, "maxLength": 64, "pattern": "^[A-Za-z0-9]+$"}  # D19
p["VocabularyNameQuery"]["description"] = "Case-insensitive 'contains' match on the vocabulary name (D19)."
for name in ("CreationDateQuery", "LastUpdatedQuery"):  # D20, D116
    p[name]["schema"] = {"oneOf": [{"type": "string", "format": "date"}, {"type": "string", "format": "date-time"}]}

# Problem responses added to every secured operation (D17, D44, D63, D67).
r["UnauthorizedResponse"] = {
    "description": "No or invalid bearer token (D17).",
    "content": {"application/problem+json": {"schema": {"$ref": "#/components/schemas/Problem"},
        "example": {"type": "urn:problem-type:simpl:unauthorized", "title": "Unauthorized", "status": 401,
                    "detail": "The bearer token is missing or not valid."}}},
}
r["ForbiddenResponse"] = {
    "description": "The user does not have the role needed (D16, D17).",
    "content": {"application/problem+json": {"schema": {"$ref": "#/components/schemas/Problem"},
        "example": {"type": "urn:problem-type:simpl:forbidden", "title": "Forbidden", "status": 403,
                    "detail": "The user does not have the role needed for this operation."}}},
}
r["MethodNotAllowedResponse"] = {
    "description": "Undeclared method (D67, D126, D139); carries Allow.",
    "headers": {"Allow": {"schema": {"type": "string"}}},
    "content": {"application/problem+json": {"schema": {"$ref": "#/components/schemas/Problem"}}},
}
r["ServiceUnavailableResponse"] = {
    "description": "Store or identity provider not available, or too many uploads (D44, D92).",
    "headers": {"Retry-After": {"schema": {"type": "integer", "example": 5}}},
    "content": {"application/problem+json": {"schema": {"$ref": "#/components/schemas/Problem"},
        "example": {"type": "urn:problem-type:belgif:serviceUnavailable", "title": "Service Unavailable",
                    "status": 503, "detail": "The vocabulary store is not available. Retry later."}}},
}
r["PayloadTooLargeJsonResponse"] = copy.deepcopy(r["UploadTooLargeResponse"])
r["PayloadTooLargeJsonResponse"]["description"] = "JSON body above 1 MB (D110)."

for path, ops in api["paths"].items():
    for method, op in ops.items():
        resp = op["responses"]
        if path != "/health":
            resp.setdefault("401", {"$ref": "#/components/responses/UnauthorizedResponse"})
            resp.setdefault("403", {"$ref": "#/components/responses/ForbiddenResponse"})
            resp.setdefault("406", {"$ref": "#/components/responses/NotAcceptableResponse"})
        resp.setdefault("405", {"$ref": "#/components/responses/MethodNotAllowedResponse"})
        resp.setdefault("503", {"$ref": "#/components/responses/ServiceUnavailableResponse"})
        if method in ("post", "patch", "delete") or path == "/health":
            resp.setdefault("400", {"$ref": "#/components/responses/BadRequestResponse"})
        if method in ("post", "patch") and "requestBody" in op and "application/json" in op["requestBody"]["content"]:
            resp.setdefault("413", {"$ref": "#/components/responses/PayloadTooLargeJsonResponse"})
            resp.setdefault("415", {"$ref": "#/components/responses/UnsupportedMediaTypeResponse"})
        for param in op.get("parameters", []):
            if param.get("name") == "version" and param.get("in") == "path":
                param["schema"] = dict(INT32)  # D49, D91, D116
        if "{vocabularyName}" in path and method in ("get", "patch", "post"):
            resp.setdefault("404", {"$ref": "#/components/responses/VocabularyNotFoundResponse"})
    if path.startswith("/external-vocabularies/{name}"):
        for op in ops.values():
            op["responses"]["404"] = {"$ref": "#/components/responses/ExternalVocabularyNotFoundResponse"}  # D41
api["paths"]["/vocabularies/{vocabularyName}/versions"]["post"]["responses"].setdefault(
    "409", {"$ref": "#/components/responses/ConflictResponse"})  # D46

TEXT = {"type": "string", "minLength": 1, "maxLength": 1000, "description": "1 to 1000 characters after trimming (D37)."}
TURTLE_FILE = {"type": "string", "format": "binary", "description": "Turtle (RDF 1.1), at most 5 MB (D9, D15, D76)."}
for name in ("CreateVocabularyPayload", "CreateNewVocabularyVersionPayload", "CreateExternalVocabularyPayload"):
    s[name]["additionalProperties"] = False  # D52
    s[name]["properties"]["vocabularyFile"] = dict(TURTLE_FILE)
for name in ("CreateVocabularyPayload", "CreateNewVocabularyVersionPayload"):
    s[name]["properties"]["description"] = {**TEXT, "example": "Vocabulary defining healthcare resource types."}
    s[name]["properties"]["changelog"] = {**TEXT, "example": "Initial version."}
for path, method in (("/vocabularies", "post"), ("/vocabularies/{vocabularyName}/versions", "post"), ("/external-vocabularies", "post")):
    api["paths"][path][method]["requestBody"]["content"]["multipart/form-data"]["encoding"] = {
        "vocabularyFile": {"contentType": "text/turtle"}}
s["CreateExternalVocabularyPayload"]["properties"]["aliasUris"]["maxItems"] = 20  # D56
s["CreateExternalVocabularyPayload"]["properties"]["aliasUris"]["uniqueItems"] = True
s["CreateExternalVocabularyPayload"]["properties"]["description"] = {"type": "string", "maxLength": 1000}
s["ExternalVocabularyContentRequest"]["additionalProperties"] = False
s["VocabularySearchRequest"]["properties"]["items"]["maxItems"] = 100  # D38
s["VocabularySearchItemRequest"]["properties"]["version"] = dict(INT32)
s["ExternalVocabularySearchRequest"]["properties"]["namespaceUris"]["minItems"] = 1  # D38, D137
s["ExternalVocabularySearchRequest"]["properties"]["aliasUris"]["minItems"] = 1
s["ExternalVocabularySearchRequest"]["description"] += " At most 100 URIs in total (D38)."
s["VocabularyStatusPatchRequest"]["additionalProperties"] = False

# Wider (D5, D10, D27, D110).
for name in ("VocabularyMetadata", "VocabularyVersionMetadataResponse"):
    s[name]["properties"]["createdBy"] = {"type": "string", "example": "johndoe@example.com"}
s["VocabularyVersionMetadataResponse"]["properties"]["latestVersionHref"] = {"type": "string",
    "example": "/vocabularies/HealthcareVocabulary/versions/2"}
s["VocabularyVersionMetadataResponse"]["required"] = ["validationWarnings"]
s["VocabularyValidationWarning"]["properties"]["code"]["enum"] = [
    "externalReferenceSkipped", "deprecatedVocabularyReference", "semanticReasoningSkipped"]
s["ExternalVocabulary"]["required"] = ["aliasUris"]
s["VocabularyValidationProblem"]["allOf"][1]["properties"]["truncated"] = {
    "type": "boolean", "description": "True when the report holds only the first 1000 issues (D125)."}

yaml.safe_dump(api, open(out, "w", encoding="utf-8"), sort_keys=False, allow_unicode=True, width=120)
print("written", out)
