"""Builds docs/openapi-decided.yaml: the published OpenAPI with every contract change of D79 (and D91) applied.

Run: python3 docs/tools/make_decided_openapi.py ../input/specs/api/vocabulary_openapi-tier1.yaml docs/openapi-decided.yaml
The published file stays unchanged (D39).
"""
import copy
import sys

import yaml

src, dst = sys.argv[1], sys.argv[2]
api = yaml.safe_load(open(src, encoding="utf-8"))
c = api["components"]
params, schemas, responses = c["parameters"], c["schemas"], c["responses"]

api["info"]["version"] = "1.0.0-decided"
api["info"]["description"] += (
    "\n\nDecided contract of factory run 9: the published contract with the changes of decisions D79 and D91 "
    "applied (401/403 everywhere, 405, 406, 413, 503, stricter limits, createdBy, latestVersionHref, "
    "validationWarnings always present).")
api["servers"] = [{"url": "http://localhost:8196/v1", "description": "Local stack (build profile)"}]

# D40: OpenID Connect through the Keycloak realm, no scopes checked.
c["securitySchemes"] = {"OpenIdConnect": {
    "type": "openIdConnect",
    "openIdConnectUrl": "http://localhost:8193/realms/simpl/.well-known/openid-configuration",
    "description": "Bearer token of the Keycloak realm simpl with audience vocabulary-manager-be (E1, E5). "
                   "Roles GA_VOCABULARY_VIEWER (reads) and GA_VOCABULARY_ADMIN (reads and writes) (D16)."}}
api["security"] = [{"OpenIdConnect": []}]

INT32 = {"type": "integer", "format": "int32", "minimum": 1, "maximum": 2147483647}

# D22, D91
params["PageQuery"]["schema"] = dict(INT32, default=1)
params["PageSizeQuery"]["schema"] = {"type": "integer", "format": "int32", "minimum": 1, "maximum": 100, "default": 20}
# D19
params["VocabularyNameQuery"]["schema"] = {"type": "string", "minLength": 1, "maxLength": 64, "pattern": "^[A-Za-z0-9]+$"}
params["VocabularyNameQuery"]["description"] = "Case-insensitive 'contains' filter on the vocabulary name (D19)."

problem = {"$ref": "#/components/schemas/Problem"}
def pj(desc, schema=problem, headers=None):
    r = {"description": desc, "content": {"application/problem+json": {"schema": schema}}}
    if headers:
        r["headers"] = headers
    return r

responses["UnauthorizedResponse"] = pj("No or invalid bearer token (D17). Type urn:problem-type:simpl:unauthorized.")
responses["ForbiddenResponse"] = pj("Valid token without the needed role (D16, D17). Type urn:problem-type:simpl:forbidden.")
responses["MethodNotAllowedResponse"] = pj("Method not declared on this path (D67, D77).")
responses["ServiceUnavailableResponse"] = pj(
    "Store or identity provider unavailable, write budget exhausted, or too many uploads (D44, D71, D92). "
    "Type urn:problem-type:belgif:serviceUnavailable.",
    headers={"Retry-After": {"schema": {"type": "integer", "example": 5}}})
responses["UploadTooLargeResponse"]["description"] += " Also when the whole multipart request is over 6 MB (D76)."

for path, item in api["paths"].items():
    for method, op in item.items():
        if method not in ("get", "post", "patch", "delete", "put"):
            continue
        rs = op["responses"]
        if op.get("security") == []:
            continue
        rs.setdefault("401", {"$ref": "#/components/responses/UnauthorizedResponse"})
        rs.setdefault("403", {"$ref": "#/components/responses/ForbiddenResponse"})
        rs.setdefault("405", {"$ref": "#/components/responses/MethodNotAllowedResponse"})
        rs.setdefault("406", {"$ref": "#/components/responses/NotAcceptableResponse"})
        rs.setdefault("503", {"$ref": "#/components/responses/ServiceUnavailableResponse"})
        rs.setdefault("400", {"$ref": "#/components/responses/BadRequestResponse"})
        if "requestBody" in op:
            rs.setdefault("415", {"$ref": "#/components/responses/UnsupportedMediaTypeResponse"})
            if "multipart/form-data" in op["requestBody"]["content"]:
                rs.setdefault("413", {"$ref": "#/components/responses/UploadTooLargeResponse"})
            else:
                rs.setdefault("413", pj("JSON request body over 1 MB."))
        for p in op.get("parameters", []):
            if p.get("name") == "version" and p.get("in") == "path":
                p["schema"] = dict(INT32)

# Version path parameter of D91 on inline parameters done above; 409 on new version (D46).
api["paths"]["/vocabularies/{vocabularyName}/versions"]["post"]["responses"]["409"] = {
    "$ref": "#/components/responses/ConflictResponse"}
# GET and DELETE external vocabulary 404 body (D41).
for m in ("get", "delete"):
    api["paths"]["/external-vocabularies/{name}"][m]["responses"]["404"] = {
        "$ref": "#/components/responses/ExternalVocabularyNotFoundResponse"}

# D37, D56, D79: field limits.
text = {"type": "string", "minLength": 1, "maxLength": 1000,
        "description": "1 to 1000 characters after trimming (D37)."}
for name in ("CreateVocabularyPayload", "CreateNewVocabularyVersionPayload"):
    schemas[name]["additionalProperties"] = False
    schemas[name]["properties"]["description"].update(text)
    schemas[name]["properties"]["changelog"].update(text)
    schemas[name]["properties"]["vocabularyFile"]["description"] = (
        "Turtle (RDF 1.1), at most 5 242 880 bytes (D15); valid and with at least one statement (D9, D70).")
schemas["CreateNewVocabularyVersionPayload"]["description"] += " A 'version' field gives 400 (D7)."
ext = schemas["CreateExternalVocabularyPayload"]
ext["additionalProperties"] = False
ext["properties"]["aliasUris"]["maxItems"] = 20
ext["properties"]["aliasUris"]["uniqueItems"] = True
ext["properties"]["aliasUris"]["description"] += " One part per URI, at most 20, no repeats of the namespace or another alias (D56)."
ext["properties"]["description"]["maxLength"] = 1000
schemas["ExternalVocabularyContentRequest"]["additionalProperties"] = False
schemas["VocabularyStatusPatchRequest"]["additionalProperties"] = False

# D38: search limits.
schemas["VocabularySearchRequest"]["properties"]["items"]["maxItems"] = 100
schemas["VocabularySearchItemRequest"]["properties"]["version"].update({"format": "int32", "maximum": 2147483647})
for f in ("namespaceUris", "aliasUris"):
    schemas["ExternalVocabularySearchRequest"]["properties"][f]["minItems"] = 1
    schemas["ExternalVocabularySearchRequest"]["properties"][f]["maxItems"] = 100
schemas["ExternalVocabularySearchRequest"]["description"] += " At most 100 URIs in total (D38)."

# D27, D5, D10: wider response schemas.
created_by = {"type": "string", "example": "johndoe@example.com", "description": "Creator (D27)."}
for name in ("VocabularyMetadata", "VocabularyVersionMetadataResponse"):
    schemas[name]["properties"]["createdBy"] = copy.deepcopy(created_by)
schemas["VocabularyVersionMetadataResponse"]["properties"]["latestVersionHref"] = {"type": "string"}
schemas["VocabularyMetadata"]["properties"]["latestVersionHref"]["nullable"] = False
schemas["VocabularyMetadata"]["properties"]["latestVersionHref"]["description"] = "Always filled (D5)."
schemas["VocabularyVersionMetadataResponse"]["required"] = ["validationWarnings"]
schemas["VocabularyValidationWarning"]["properties"]["code"]["enum"] = [
    "externalReferenceSkipped", "deprecatedVocabularyReference", "semanticReasoningSkipped"]

with open(dst, "w", encoding="utf-8") as out:
    out.write("# Decided contract (D87): generated by docs/tools/make_decided_openapi.py from the published file.\n")
    yaml.safe_dump(api, out, sort_keys=False, allow_unicode=True, width=120)
print("written", dst)
