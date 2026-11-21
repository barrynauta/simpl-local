#!/usr/bin/env python3
"""Builds docs/openapi-decided.yaml from the published OpenAPI with every contract change of D79 applied
(D39, D87, D91, D110, D116, D125, D150). The published file in input/specs/api stays unchanged."""
import copy
import sys
import yaml

src, dst = sys.argv[1], sys.argv[2]
api = yaml.safe_load(open(src))
c = api["components"]
INT32 = 2147483647

api["info"]["version"] = "1.1.0"
api["info"]["description"] = (
    "This API allows the Governance Authority to manage internal vocabularies. "
    "Decided contract of the Vocabulary Manager build: the published contract 1.0.0 with the changes of "
    "decision D79 (and D91, D110, D116, D125, D150) applied.")

# D40: OpenID Connect through the Keycloak realm of E1, no scopes.
c["securitySchemes"] = {"OpenIdConnect": {
    "type": "openIdConnect",
    "openIdConnectUrl": "http://localhost:8193/realms/simpl/.well-known/openid-configuration",
    "description": "Bearer token of the Keycloak realm 'simpl' with audience vocabulary-manager-be. "
                   "Roles GA_VOCABULARY_VIEWER (reads) and GA_VOCABULARY_ADMIN (reads and writes), D16."}}
api["security"] = [{"OpenIdConnect": []}]

problem = lambda t, title, status, detail: {"application/problem+json": {
    "schema": {"$ref": "#/components/schemas/Problem"},
    "example": {"type": t, "title": title, "status": status, "detail": detail}}}
r = c["responses"]
r["UnauthorizedResponse"] = {"description": "No or invalid token (D17).", "headers": {
    "WWW-Authenticate": {"schema": {"type": "string"}}},
    "content": problem("urn:problem-type:simpl:unauthorized", "Unauthorized", 401, "A valid bearer token is required.")}
r["ForbiddenResponse"] = {"description": "Valid token without the required role (D16, D17).",
    "content": problem("urn:problem-type:simpl:forbidden", "Forbidden", 403, "You are not authorized to perform this action.")}
r["ServiceUnavailableResponse"] = {"description": "Store or identity provider not available, or too many uploads (D44, D92).",
    "headers": {"Retry-After": {"schema": {"type": "integer", "example": 5}}},
    "content": problem("urn:problem-type:belgif:serviceUnavailable", "Service Unavailable", 503,
                       "The vocabulary store is not available. Retry later.")}
r["MethodNotAllowedResponse"] = {"description": "Undeclared method (D67, D126, D139).",
    "headers": {"Allow": {"schema": {"type": "string"}}},
    "content": problem("urn:problem-type:simpl:methodNotAllowed", "Method Not Allowed", 405,
                       "The method is not allowed on this resource.")}
r["JsonTooLargeResponse"] = {"description": "JSON body over 1 MB (D150).",
    "content": problem("urn:problem-type:simpl:payloadTooLarge", "Payload Too Large", 413, "The request body is larger than 1 MB.")}

# Parameters (D19, D22, D49, D91, D112, D116).
p = c["parameters"]
p["VocabularyNameQuery"]["schema"] = {"type": "string", "minLength": 1, "maxLength": 64, "pattern": "^[A-Za-z0-9]+$"}
p["VocabularyNameQuery"]["description"] = "Case-insensitive 'contains' filter on the vocabulary name (D19)."
for name, fmt in (("CreationDateQuery", "creationDate"), ("LastUpdatedQuery", "lastUpdated")):
    p[name]["schema"] = {"type": "string", "pattern": r"^\d{4}-\d{2}-\d{2}(T.*)?$",
                         "anyOf": [{"format": "date"}, {"format": "date-time"}]}
    p[name]["description"] = "A date matches its whole UTC day; a date-time matches to the second (D20, D130)."
p["PageQuery"]["schema"] = {"type": "integer", "format": "int32", "default": 1, "minimum": 1, "maximum": INT32}
p["PageSizeQuery"]["schema"] = {"type": "integer", "format": "int32", "default": 20, "minimum": 1, "maximum": 100}
p["VersionPath"] = {"name": "version", "in": "path", "required": True, "description": "Version number of the vocabulary.",
                    "schema": {"type": "integer", "format": "int32", "minimum": 1, "maximum": INT32}}

# Schemas.
s = c["schemas"]
s["Problem"]["properties"]["truncated"] = {"type": "boolean", "description": "True when the report holds the first 1000 issues only (D125)."}
text = {"type": "string", "minLength": 1, "maxLength": 1000, "description": "1 to 1000 characters after trimming (D37)."}
for name in ("CreateVocabularyPayload", "CreateNewVocabularyVersionPayload"):
    s[name]["additionalProperties"] = False
    s[name]["properties"]["description"].update(text)
    s[name]["properties"]["changelog"].update(text)
s["CreateVocabularyPayload"]["properties"]["name"]["description"] = "Unique case-insensitively (D37)."
ext = s["CreateExternalVocabularyPayload"]
ext["additionalProperties"] = False
ext["properties"]["aliasUris"].update({"maxItems": 20, "uniqueItems": True})
ext["properties"]["description"].update({"maxLength": 1000})
for name in ("VocabularyMetadata", "VocabularyVersionMetadataResponse"):
    s[name]["properties"]["createdBy"] = {"type": "string", "example": "johndoe@example.com"}
    s[name]["properties"]["version"].update({"format": "int32", "minimum": 1})
s["VocabularyVersionMetadataResponse"]["properties"]["latestVersionHref"] = copy.deepcopy(
    s["VocabularyMetadata"]["properties"]["latestVersionHref"])
s["VocabularyMetadata"]["properties"]["latestVersionHref"]["nullable"] = False
s["VocabularyVersionMetadataResponse"]["required"] = ["validationWarnings", "createdBy", "latestVersionHref"]
s["VocabularyValidationWarning"]["properties"]["code"]["enum"] = [
    "externalReferenceSkipped", "deprecatedVocabularyReference", "semanticReasoningSkipped"]
s["VocabularySearchRequest"]["properties"]["items"]["maxItems"] = 100
s["VocabularySearchItemRequest"]["properties"]["version"].update({"format": "int32", "maximum": INT32})
for arr in ("namespaceUris", "aliasUris"):
    s["ExternalVocabularySearchRequest"]["properties"][arr].update({"minItems": 1, "maxItems": 100})
s["ExternalVocabulary"]["required"] = ["aliasUris"]
s["ExternalVocabularyContentRequest"]["additionalProperties"] = False
s["VocabularyStatusPatchRequest"]["additionalProperties"] = False

# Operations.
for path, item in api["paths"].items():
    for method, op in item.items():
        if method not in ("get", "post", "patch", "delete"):
            continue
        resp = op["responses"]
        op["parameters"] = [ {"$ref": "#/components/parameters/VersionPath"} if prm.get("name") == "version" and prm.get("in") == "path" else prm
                             for prm in op.get("parameters", [])]
        if op["operationId"] != "getHealth":
            resp["401"] = {"$ref": "#/components/responses/UnauthorizedResponse"}
            resp["403"] = {"$ref": "#/components/responses/ForbiddenResponse"}
            resp["503"] = {"$ref": "#/components/responses/ServiceUnavailableResponse"}
            resp.setdefault("400", {"$ref": "#/components/responses/BadRequestResponse"})
            resp.setdefault("406", {"$ref": "#/components/responses/NotAcceptableResponse"})
        else:
            resp.setdefault("400", {"$ref": "#/components/responses/BadRequestResponse"})
        if method in ("post", "patch") and "application/json" in op.get("requestBody", {}).get("content", {}):
            resp["413"] = {"$ref": "#/components/responses/JsonTooLargeResponse"}
        if "multipart/form-data" in op.get("requestBody", {}).get("content", {}):
            op["requestBody"]["content"]["multipart/form-data"]["encoding"] = {
                "vocabularyFile": {"contentType": "text/turtle"}}
        if op["operationId"] in ("getExternalVocabulary", "deleteExternalVocabulary"):
            resp["404"] = {"$ref": "#/components/responses/ExternalVocabularyNotFoundResponse"}
        if op["operationId"] == "createVocabularyVersion":
            resp["409"] = {"$ref": "#/components/responses/ConflictResponse"}
        op["x-roles"] = ([] if op["operationId"] == "getHealth" else
                         ["GA_VOCABULARY_ADMIN"] if method in ("patch", "delete") or op["operationId"] in (
                             "createVocabulary", "createVocabularyVersion", "createExternalVocabulary")
                         else ["GA_VOCABULARY_VIEWER", "GA_VOCABULARY_ADMIN"])

api["paths"]["/health"]["get"]["parameters"] = []
yaml.safe_dump(api, open(dst, "w"), sort_keys=False, allow_unicode=True, width=120)
print("written", dst)
