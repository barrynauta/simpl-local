#!/usr/bin/env python3
"""Builds docs/openapi-decided.yaml: the published OpenAPI with every contract change of the
decisions applied (D79, D87, D91, D110, D116, D125, D150). The published file stays unchanged (D39).
Usage: python3 tools/make_openapi_decided.py ../input/specs/api/vocabulary_openapi-tier1.yaml docs/openapi-decided.yaml"""
import sys, copy, yaml

src, dst = sys.argv[1], sys.argv[2]
api = yaml.safe_load(open(src))
c = api["components"]; P = c["parameters"]; S = c["schemas"]; R = c["responses"]
INT32 = 2147483647

api["info"]["description"] += (" Decided contract of factory run 12: the published contract plus the changes of"
    " decisions D79, D91, D110, D116, D125 and D150 (see docs/api-notes.md).")
api["info"]["version"] = "1.0.0-decided"
api["servers"].append({"url": "http://localhost:8196/v1", "description": "Local stack of the factory build"})

# D40: OpenID Connect through Keycloak; scopes are not checked.
c["securitySchemes"] = {"KeycloakOidc": {"type": "openIdConnect",
    "openIdConnectUrl": "http://localhost:8193/realms/simpl/.well-known/openid-configuration",
    "description": "Bearer token of realm 'simpl' with audience vocabulary-manager-be. Roles GA_VOCABULARY_VIEWER (reads) and GA_VOCABULARY_ADMIN (reads and writes) (D16)."}}
api["security"] = [{"KeycloakOidc": []}]

def problem(desc, example):
    return {"description": desc, "content": {"application/problem+json": {"schema": {"$ref": "#/components/schemas/Problem"}, "example": example}}}
R["UnauthorizedResponse"] = problem("No or invalid bearer token (D17).", {"type": "urn:problem-type:simpl:unauthorized", "title": "Unauthorized", "status": 401, "detail": "A valid bearer token is required."})
R["ForbiddenResponse"] = problem("The token lacks the role of the operation (D16).", {"type": "urn:problem-type:simpl:forbidden", "title": "Forbidden", "status": 403, "detail": "The user does not have the role required for this operation."})
R["MethodNotAllowedResponse"] = problem("Undeclared method on a declared path (D67, D126, D139).", {"type": "urn:problem-type:simpl:methodNotAllowed", "title": "Method Not Allowed", "status": 405, "detail": "The method is not allowed for this resource."})
R["MethodNotAllowedResponse"]["headers"] = {"Allow": {"schema": {"type": "string"}}}
R["ServiceUnavailableResponse"] = problem("Store or identity provider not available, or too many uploads (D44, D92).", {"type": "urn:problem-type:belgif:serviceUnavailable", "href": "https://www.belgif.be/specification/rest/api-guide/problems/serviceUnavailable.html", "title": "Service Unavailable", "status": 503, "detail": "The vocabulary store is not available. Retry later."})
R["ServiceUnavailableResponse"]["headers"] = {"Retry-After": {"schema": {"type": "integer", "format": "int32"}, "example": 5}}
R["JsonTooLargeResponse"] = problem("JSON body above 1 MB (D110, D150).", {"type": "urn:problem-type:simpl:payloadTooLarge", "title": "Payload Too Large", "status": 413, "detail": "The request body is larger than 1 MB."})

# Parameters (D19, D22, D49, D91, D116)
P["VocabularyNameQuery"]["schema"] = {"type": "string", "minLength": 1, "maxLength": 64, "pattern": "^[A-Za-z0-9]+$"}
P["VocabularyNameQuery"]["description"] = "Case-insensitive 'contains' filter on the vocabulary name (D19)."
for q in ("CreationDateQuery", "LastUpdatedQuery"):
    P[q]["schema"] = {"type": "string", "pattern": "^\\d{4}-\\d{2}-\\d{2}(T.*)?$",
        "anyOf": [{"format": "date"}, {"format": "date-time"}]}
    P[q]["description"] += " A date matches the whole UTC day; a date-time matches to the second; no offset means UTC (D20, D110, D130)."
P["PageQuery"]["schema"] = {"type": "integer", "format": "int32", "default": 1, "minimum": 1, "maximum": INT32}
P["PageSizeQuery"]["schema"] = {"type": "integer", "format": "int32", "default": 20, "minimum": 1, "maximum": 100}
P["VersionPath"] = {"name": "version", "in": "path", "required": True, "description": "Version number of the vocabulary.",
    "schema": {"type": "integer", "format": "int32", "minimum": 1, "maximum": INT32}}

# Schemas
TEXT = {"type": "string", "minLength": 1, "maxLength": 1000, "description": "1 to 1000 characters after trimming (D37)."}
for name in ("CreateVocabularyPayload", "CreateNewVocabularyVersionPayload", "CreateExternalVocabularyPayload",
             "ExternalVocabularyContentRequest", "VocabularyStatusPatchRequest"):
    S[name]["additionalProperties"] = False
for name in ("CreateVocabularyPayload", "CreateNewVocabularyVersionPayload"):
    S[name]["properties"]["description"] = dict(TEXT, example=S[name]["properties"]["description"].get("example"))
    S[name]["properties"]["changelog"] = dict(TEXT, example=S[name]["properties"]["changelog"].get("example"))
    S[name]["properties"]["vocabularyFile"]["description"] = "Turtle (RDF 1.1), at most 5 MB, at least one statement (D9, D15, D70)."
ext = S["CreateExternalVocabularyPayload"]["properties"]
ext["aliasUris"]["maxItems"] = 20; ext["aliasUris"]["uniqueItems"] = True
ext["aliasUris"]["description"] += " One part per URI, at most 20, absolute, none equal to the namespace URI or another alias (D56, D73)."
ext["description"] = {"type": "string", "maxLength": 1000, "description": "Optional; blank means absent (D56, D148)."}
S["VocabularySearchRequest"]["properties"]["items"]["maxItems"] = 100
S["VocabularySearchItemRequest"]["properties"]["version"].update({"format": "int32", "maximum": INT32})
esr = S["ExternalVocabularySearchRequest"]["properties"]
for a in ("namespaceUris", "aliasUris"):
    esr[a]["minItems"] = 1; esr[a]["maxItems"] = 100
S["ExternalVocabularySearchRequest"]["description"] += " At most 100 URIs in total (D38)."
for m in ("VocabularyMetadata", "VocabularyVersionMetadataResponse"):
    S[m]["properties"]["createdBy"] = {"type": "string", "example": "johndoe@example.com", "description": "Creator: email claim, else preferred_username (D27). Personal data (D165)."}
    S[m]["properties"]["latestVersionHref"] = {"type": "string", "example": "/vocabularies/HealthcareVocabulary/versions/3", "description": "Always filled (D5)."}
S["VocabularyVersionMetadataResponse"]["required"] = ["validationWarnings"]
S["VocabularyValidationWarning"]["properties"]["code"]["enum"] = ["externalReferenceSkipped", "deprecatedVocabularyReference", "semanticReasoningSkipped"]
S["VocabularyValidationWarning"]["required"] = ["code", "description", "location"]
S["ExternalVocabulary"]["required"] = ["href", "name", "namespaceUri", "aliasUris", "createdBy", "creationDate"]
S["VocabularyValidationProblem"]["allOf"].append({"type": "object", "properties": {"truncated": {"type": "boolean", "description": "True when more than 1000 issues were found (D125)."}}})
for k in ("page", "totalPages", "pageSize"):
    S["CollectionPage"]["properties"][k]["format"] = "int32"
for k in ("version",):
    for m in ("VocabularyCategory", "VocabularyMetadata", "VocabularyVersionMetadataResponse", "VocabularySearchItem"):
        S[m]["properties"][k]["format"] = "int32"

# Operations
SECURED_EXTRA = {"401": {"$ref": "#/components/responses/UnauthorizedResponse"}, "403": {"$ref": "#/components/responses/ForbiddenResponse"}}
for path, ops in api["paths"].items():
    for method, op in ops.items():
        rs = op["responses"]
        if path != "/health":
            rs.update(SECURED_EXTRA)
            rs["503"] = {"$ref": "#/components/responses/ServiceUnavailableResponse"}
            rs["406"] = {"$ref": "#/components/responses/NotAcceptableResponse"}
        if method in ("patch",) or (method == "post" and "application/json" in op.get("requestBody", {}).get("content", {})):
            rs["413"] = {"$ref": "#/components/responses/JsonTooLargeResponse"}
            rs["415"] = {"$ref": "#/components/responses/UnsupportedMediaTypeResponse"}
        for prm in op.get("parameters", []):
            pass
        op["parameters"] = [ {"$ref": "#/components/parameters/VersionPath"} if isinstance(p, dict) and p.get("name") == "version" and p.get("in") == "path" else p for p in op.get("parameters", [])]
        if "requestBody" in op and "multipart/form-data" in op["requestBody"]["content"]:
            op["requestBody"]["content"]["multipart/form-data"]["encoding"] = {"vocabularyFile": {"contentType": "text/turtle"}}
            if path == "/external-vocabularies":
                op["requestBody"]["content"]["multipart/form-data"]["encoding"]["aliasUris"] = {"style": "form", "explode": True}
api["paths"]["/vocabularies/{vocabularyName}/versions"]["post"]["responses"]["409"] = {"$ref": "#/components/responses/ConflictResponse"}
api["paths"]["/vocabularies/{vocabularyName}/versions"]["post"]["description"] += " A 'version' form field gives 400 (D7). Same bytes and description as the latest version give 409 (D46)."
for m in ("get", "delete"):
    api["paths"]["/external-vocabularies/{name}"][m]["responses"]["404"] = {"$ref": "#/components/responses/ExternalVocabularyNotFoundResponse"}
api["paths"]["/health"]["get"]["responses"]["400"] = {"$ref": "#/components/responses/BadRequestResponse"}
api["paths"]["/health"]["get"]["responses"]["406"] = {"$ref": "#/components/responses/NotAcceptableResponse"}
for p in ("/vocabularies/searches", "/external-vocabulary/searches"):
    api["paths"][p]["post"]["description"] = (api["paths"][p]["post"].get("description", "") +
        " Not paged: page 1, pageSize 100, total = number of items, totalPages 1 (0 when empty), no links (D38, D153).")
api["x-decided-rules"] = {
    "unknownQueryParameters": "400 on every operation, also repeated parameters (D21, D52, D138)",
    "unknownBodyFields": "400, one issue per unknown name (D52)",
    "undeclaredMethods": "405 with Allow, before 401 (D67, D126, D139)",
    "multipartRequestLimit": "6 MB, 413 (D76); more than 64 parts 400 (D110)",
    "jsonBodyLimit": "1 MB, 413 (D110, D150)",
    "problemTypes": "belgif for 400, 404, 500, 503; simpl for 401, 403, 405, 406, 409, 413, 415 (D48)",
}
yaml.safe_dump(api, open(dst, "w"), sort_keys=False, allow_unicode=True, width=120)
print("written", dst)
