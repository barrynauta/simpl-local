#!/usr/bin/env python3
"""Builds docs/openapi-decided.yaml: the published OpenAPI with every change of D79 (and D91, D110, D116,
D125, D150, D169) applied (D87). The published file in input/specs/api stays unchanged (D39)."""
import copy, sys, yaml

src, dst = sys.argv[1], sys.argv[2]
api = yaml.safe_load(open(src))
c = api["components"]
S, P, R = c["schemas"], c["parameters"], c["responses"]
INT32 = 2147483647

api["info"]["version"] = "1.0.0-decided"
api["info"]["description"] = (api["info"]["description"] + "\n\nDecided contract of the Vocabulary Manager build "
    "(factory run 14): the published contract with the changes of decision D79 and its refinements applied.")
api["servers"] = [{"url": "http://localhost:8196/v1", "description": "Local stack (build profile run 14)"}]
c["securitySchemes"] = {"OpenIdConnect": {"type": "openIdConnect",
    "openIdConnectUrl": "http://localhost:8193/realms/simpl/.well-known/openid-configuration",
    "description": "Keycloak realm 'simpl' (E1, D40). Realm roles GA_VOCABULARY_VIEWER and GA_VOCABULARY_ADMIN (D16)."}}
api["security"] = [{"OpenIdConnect": []}]

problem = {"application/problem+json": {"schema": {"$ref": "#/components/schemas/Problem"}}}
def resp(desc): return {"description": desc, "content": copy.deepcopy(problem)}
R["UnauthorizedResponse"] = resp("No or invalid bearer token (D17). type urn:problem-type:simpl:unauthorized.")
R["ForbiddenResponse"] = resp("The token lacks the role of the operation (D16, D17). type urn:problem-type:simpl:forbidden.")
R["MethodNotAllowedResponse"] = resp("Method not declared on this path (D67, D139). Allow header lists the path's methods.")
R["MethodNotAllowedResponse"]["headers"] = {"Allow": {"schema": {"type": "string"}}}
R["ServiceUnavailableResponse"] = resp("Store or identity provider unavailable, or no free upload slot (D44, D92).")
R["ServiceUnavailableResponse"]["headers"] = {"Retry-After": {"schema": {"type": "integer", "format": "int32", "maximum": INT32}}}
R["PayloadTooLargeJsonResponse"] = resp("JSON body over 1 MB (D150).")

# Problem schema: truncated flag (D125).
S["Problem"]["properties"]["truncated"] = {"type": "boolean", "description": "True when the validation report holds only the first 1000 issues (D125)."}

# Parameters.
P["VocabularyNameQuery"]["schema"] = {"type": "string", "pattern": "^[A-Za-z0-9]+$", "minLength": 1, "maxLength": 64}
P["VocabularyNameQuery"]["description"] = "Case-insensitive 'contains' filter on the vocabulary name (D19)."
for name in ("CreationDateQuery", "LastUpdatedQuery"):
    P[name]["schema"] = {"type": "string", "pattern": "^\\d{4}-\\d{2}-\\d{2}(T.*)?$",
        "description": "format date (whole UTC day) or date-time (to the second, no offset means UTC) (D20, D110, D116, D130)."}
P["PageQuery"]["schema"] = {"type": "integer", "format": "int32", "default": 1, "minimum": 1, "maximum": INT32}
P["PageSizeQuery"]["schema"] = {"type": "integer", "format": "int32", "default": 20, "minimum": 1, "maximum": 100}
P["VersionPath"] = {"name": "version", "in": "path", "required": True, "description": "Version number (D49, D91).",
    "schema": {"type": "integer", "format": "int32", "minimum": 1, "maximum": INT32}}

# Schemas.
text1000 = {"type": "string", "minLength": 1, "maxLength": 1000, "description": "1 to 1000 characters after trimming (D37)."}
for name in ("CreateVocabularyPayload", "CreateNewVocabularyVersionPayload"):
    S[name]["additionalProperties"] = False
    S[name]["properties"]["description"] = dict(text1000)
    S[name]["properties"]["changelog"] = dict(text1000)
    S[name]["properties"]["vocabularyFile"] = {"type": "string", "format": "binary", "maxLength": 5242880,
        "description": "Non-empty, valid Turtle, at most 5 MB (D9, D15, D70, D76)."}
S["CreateNewVocabularyVersionPayload"]["description"] += " A 'version' field gives 400 (D7)."
ext = S["CreateExternalVocabularyPayload"]
ext["additionalProperties"] = False
ext["properties"]["aliasUris"]["maxItems"] = 20
ext["properties"]["aliasUris"]["uniqueItems"] = True
ext["properties"]["description"] = {"type": "string", "maxLength": 1000, "description": "Blank means absent (D148)."}
ext["properties"]["vocabularyFile"] = {"type": "string", "format": "binary", "maxLength": 5242880}
S["ExternalVocabularyContentRequest"]["additionalProperties"] = False
es = S["ExternalVocabularySearchRequest"]
for k in ("namespaceUris", "aliasUris"):
    es["properties"][k]["minItems"] = 1
    es["properties"][k]["maxItems"] = 100
es["description"] += " At most 100 URIs in total (D38)."
S["VocabularySearchRequest"]["properties"]["items"]["maxItems"] = 100
S["VocabularySearchItemRequest"]["properties"]["version"].update({"format": "int32", "maximum": INT32})
S["VocabularyStatusPatchRequest"]["additionalProperties"] = False
for name in ("VocabularyMetadata", "VocabularyVersionMetadataResponse"):
    S[name]["properties"]["createdBy"] = {"type": "string", "description": "Creator (D27)."}
    S[name]["properties"]["latestVersionHref"] = {"type": "string", "description": "Always filled (D5)."}
S["VocabularyVersionMetadataResponse"]["required"] = ["validationWarnings"]
S["VocabularyValidationWarning"]["properties"]["code"]["enum"] = ["externalReferenceSkipped", "deprecatedVocabularyReference", "semanticReasoningSkipped"]
S["ExternalVocabulary"]["required"] = ["aliasUris"]
for name in ("VocabularyCategory", "VocabularyMetadata", "VocabularyVersionMetadataResponse", "VocabularySearchItem"):
    S[name]["properties"]["version"].update({"format": "int32", "minimum": 1, "maximum": INT32})
for k in ("page", "pageSize", "totalPages"):
    S["CollectionPage"]["properties"][k].update({"format": "int32"})

# Operations.
for path, item in api["paths"].items():
    for method, op in item.items():
        if method == "parameters":
            continue
        for p in op.get("parameters", []):
            if p.get("name") == "version" and p.get("in") == "path":
                p.clear(); p["$ref"] = "#/components/parameters/VersionPath"
        rs = op["responses"]
        rs["405"] = {"$ref": "#/components/responses/MethodNotAllowedResponse"}
        if path != "/health":
            rs["401"] = {"$ref": "#/components/responses/UnauthorizedResponse"}
            rs["403"] = {"$ref": "#/components/responses/ForbiddenResponse"}
            rs["406"] = {"$ref": "#/components/responses/NotAcceptableResponse"}
            rs["503"] = {"$ref": "#/components/responses/ServiceUnavailableResponse"}
            rs.setdefault("400", {"$ref": "#/components/responses/BadRequestResponse"})
        else:
            rs["400"] = {"$ref": "#/components/responses/BadRequestResponse"}
        if "requestBody" in op:
            rs.setdefault("415", {"$ref": "#/components/responses/UnsupportedMediaTypeResponse"})
            media = list(op["requestBody"]["content"].keys())[0]
            rs.setdefault("413", {"$ref": "#/components/responses/UploadTooLargeResponse" if media.startswith("multipart") else "#/components/responses/PayloadTooLargeJsonResponse"})
        if "{" in path or method in ("patch", "delete"):
            rs.setdefault("404", {"$ref": "#/components/responses/VocabularyNotFoundResponse"})
        if path.startswith("/external-vocabularies/{name}"):
            rs["404"] = {"$ref": "#/components/responses/ExternalVocabularyNotFoundResponse"}
        if path == "/vocabularies/{vocabularyName}/versions" and method == "post":
            rs["409"] = {"$ref": "#/components/responses/ConflictResponse"}
        if method == "post" and path == "/vocabularies/{vocabularyName}/versions":
            pass
        op["x-roles"] = ["GA_VOCABULARY_ADMIN"] if method in ("patch", "delete") or (method == "post" and path in ("/vocabularies", "/vocabularies/{vocabularyName}/versions", "/external-vocabularies")) else ["GA_VOCABULARY_VIEWER", "GA_VOCABULARY_ADMIN"]
        if path == "/health":
            op["x-roles"] = []
api["paths"]["/health"]["get"]["security"] = []

yaml.safe_dump(api, open(dst, "w"), sort_keys=False, allow_unicode=True, width=120)
print("written", dst)
