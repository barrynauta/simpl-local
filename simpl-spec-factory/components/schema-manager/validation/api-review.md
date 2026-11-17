# API review (step 21b): schema-manager

Input: `specs/api/schema_openapi.yaml` (Schema Management API 1.1.0, OpenAPI 3.0.1, 18 operations), the step 21a checks, the findings of 20b (`F-`) and 23b (`D-`), and the decisions of step 30 (`D1` to `D43`). Findings are in [`api-findings.json`](./api-findings.json). Points that 20b, 23b or step 30 already cover are referred to, not repeated.

## Summary

13 findings: 0 blockers, 5 major, 8 minor.

| Rule | Blocker | Major | Minor |
|---|---|---|---|
| SEC-07 | | 1 (P-001) | |
| OWASP-11 | | 1 (P-002) | |
| API-46 | | 1 (P-003) | |
| API-58 | | 1 (P-004) | |
| builder (no rule) | | 1 (P-005) | 4 (P-009 to P-012) |
| API-22 | | | 1 (P-006) |
| API-52 (with API-18, API-19, API-20, API-50) | | | 1 (P-007) |
| API-30 | | | 1 (P-008) |
| API-49 | | | 1 (P-013) |
| **Total** | **0** | **5** | **8** |

The five findings with most impact:

1. **P-002, webhook target URLs (OWASP-11, MUST).** Any authenticated participant may register any http or https URL (D28, D29), and the service then POSTs to it (D24). There is no allow list, so this is a server-side request forgery path. D29 needs an allow list or a recorded accepted risk.
2. **P-005, which contract the builder uses.** The frozen OpenAPI lacks GET /schemas/{schemaName} (D16), 401 (D26), the D30 list fields and the D15 Location format. It still lists the nine out-of-scope operations (D2), and its versions response points to a file that is not in the set. D1 puts decisions above story text, but nothing puts them above the OpenAPI.
3. **P-001, security scheme.** An OAuth2 code flow "provided by gateway" with placeholder URLs and unused read and write scopes. The decisions say the service validates an OIDC JWT itself and checks GA_SCHEMA_ADMIN or GA_SCHEMA_VIEWER (D26, D27, D39). The webhook operations list 403, which D28 makes unreachable.
4. **P-004, schemas allow what decisions forbid.** resourceType is an open pattern in six places, but D6 allows only three values. The version path parameter is any string, while D3 makes it a whole number; it is open whether a malformed version gets 400 or 404.
5. **P-003, 404 problem type.** GET /schemas/{schemaName}/{version} answers `urn:problem-type:iana:notFound`. Every other schema 404 answers `urn:problem-type:simpl:notFound`.

## Blockers

None. Every gap found is either covered by a decision or can be built in one obvious way. The major findings carry questions for step 30:

| Finding | Question |
|---|---|
| P-001 | Declare an openIdConnect (or bearer JWT) scheme without scopes, state the role per operation, drop "provided by gateway" and remove 403 from the webhook operations? |
| P-002 | Add a configured allow list of target hosts to D29 (with the D43 test receiver on it) and 400 for other hosts, or record an accepted risk against OWASP-11? Is plain http allowed outside tests? |
| P-003 | Is `urn:problem-type:simpl:notFound` with title "Resource Not Found" the one 404 type for every schema and webhook operation, including D16? |
| P-004 | Add enum [Data, Application, Infrastructure] and a whole-number pattern for version. Is a malformed version 400 or 404? |
| P-005 | Will step 30 hand over a patched OpenAPI as the build contract, or should D1 say that decisions win over the OpenAPI? |

## Rule by rule: schema-manager-be

Review rules (`check: review`) that apply to `api-spec`: 18 `API-` rules and SEC-07. No `OWASP-` rule has check `review` on `api-spec`; OWASP-11 (code, config) is raised in P-002 because the API surface creates the risk.

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| API-01 | MUST | partly met | A CDM is named in `sources.yaml` (schema-manager-be.md), but it describes another revision and is not approved. Already raised as D-010. |
| API-02 | SHOULD | partly met | Schema names differ from the CDM: SchemaCategory against CDM Schema, and other attribute names. Raised as D-006 and D-014, and settled by D30 and D36. |
| API-03 | SHOULD | partly met | The file is committed source with hand-written examples and an external `$ref`. But `servers[0].description` is "Generated server" (line 18), the default text of a code-first generator. The input set cannot show which came first. |
| API-10 | SHOULD | met | schemas, versions, vocabularies, webhooks, metadataQualityProfiles, qualityProfiles are plural. |
| API-11 | SHOULD | met | GET reads, POST creates, PATCH changes the status only (revoke, D9), DELETE removes a webhook. The DELETE of a quality profile is a soft delete, but it is out of scope (D2). |
| API-15 | SHOULD | met | No British spelling in property names. |
| API-19 | SHOULD | partly met | `graphName` names the RDF storage rather than the version URI it carries (D30), and `metadata` is a generic wrapper name. See P-007. |
| API-22 | SHOULD | partly met | Integer examples for string versions, names with spaces against the name pattern, resourceType values outside D6, truncated 406 detail texts. See P-006. |
| API-28 | SHOULD | met | /schemas and /webhooks are collections; /schemas/{schemaName} and /schemas/{schemaName}/{version} are documents; /qualityProfiles/active is a document. |
| API-30 | SHOULD | partly met | The version detail is /schemas/{schemaName}/{version}, not /schemas/{schemaName}/versions/{version}; the vocabularies use the second form. See P-008. |
| API-49 | SHOULD | partly met | The business purpose is stated, but only for resource description schemas. See P-013. |
| API-50 | SHOULD | partly met | The Webhook carries `webhookId`, not `id`. A schema is identified by its business key `name` (D4, D5), which API-55 allows. See P-007. |
| API-51 | SHOULD | met | The only reference property in scope, `latestVersion`, is named after the relation. |
| API-54 | SHOULD | not applicable | No identifier schema types are defined; identifiers are inline strings. |
| API-55 | SHOULD | not applicable | No standardised business identifier applies. |
| API-58 | SHOULD | partly met | status, schemaType and webhook events have enums. resourceType has none, although D6 makes it a closed set. See P-004. |
| API-60 | SHOULD | met | Every path parameter is `required: true`. The query parameters are optional by design. The conditional resourceType of D4 cannot be expressed in OpenAPI 3.0 and is described. |
| API-62 | MUST | not applicable | The input set shows no Kafka producer or consumer. Events go out as HTTP webhooks (D24). |
| API-79 | SHOULD | not applicable | No Kafka topics. |
| SEC-07 | SHOULD | partly met | An oauth2 scheme is declared, but with placeholder URLs, unused scopes and "provided by gateway". This does not match D26, D27 and D39. See P-001. |

Totals: 6 met, 10 partly met, 0 not met, 4 not applicable.

## Coverage

### COV-01: operations no acceptance criterion covers (A-007, 9 operations)

| Operation | Class | Basis |
|---|---|---|
| GET /vocabularies | should not be there (retire) | Legacy; the Vocabulary Manager owns it. D2 puts it out of scope; already raised as D-008. |
| POST /vocabularies | should not be there (retire) | Same as above (D2, D-008). |
| PATCH /vocabularies/{vocabularyName} | should not be there (retire) | Same as above (D2, D-008). |
| GET /vocabularies/{vocabularyName}/versions | should not be there (retire) | Same as above (D2, D-008). |
| POST /vocabularies/{vocabularyName}/versions | should not be there (retire) | Same as above (D2, D-008). |
| GET /vocabularies/{vocabularyName}/versions/{version} | should not be there (retire) | Same as above (D2, D-008). |
| PATCH /metadataQualityProfiles/{metadataQualityProfileName} | should not be there for this build (owner to confirm) | No story and no process step. D2 puts it out of scope; already raised as D-009. |
| DELETE /metadataQualityProfiles/{metadataQualityProfileName} | should not be there for this build (owner to confirm) | Same as above (D2, D-009). |
| GET /schemas/{schemaName}/qualityProfiles/active | should not be there for this build (owner to confirm) | Same as above (D2, D-009). |

In numbers: 0 stories missing, 9 should not be there (6 retire, 3 out of the build until the owner decides), 0 internal plumbing. All nine are already settled by D2. They are new only in P-005: the file still lists them, so a builder who generates code from it will produce them.

### COV-02: operations a story names that the OpenAPI lacks (A-008, 6 places)

| Story | Named operation | Already covered by |
|---|---|---|
| SIMPL-13343 | GET /api/v1/schemas | D15 (base path is /v1); SIMPL-32258 replaces this story (D1) |
| SIMPL-32258 | GET /api/v1/schemas | D15 |
| SIMPL-20067 | GET /schemas/{schemaName} | F-005, D16 |
| SIMPL-31098 | GET /schemas/{schemaName} | F-005, D16. The validation hint reads `status` from this call; D7 and D18 read status through GET /schemas instead |
| SIMPL-33537 | GET /schemas/{schemaName} | F-005, D16 |
| SIMPL-31098 | PATCH /schemas/FedSearch | A concrete name for PATCH /schemas/{schemaName}. Publishing through PATCH is F-002, withdrawn by D9 |

In numbers: 6 of 6 already covered by 20b findings or step 30 decisions; 0 new.

## 21a triage in numbers

8 findings from 21a: 4 real, 2 harmless, 2 coverage checks classified above.

| Id | Rule | Verdict | Reason |
|---|---|---|---|
| A-001 | API-63 | harmless | No Kafka in the input set; events are HTTP webhooks (D24), so no AsyncAPI file is due. The webhook payload belongs in the OpenAPI, as part of P-005. |
| A-002 | API-13 | harmless | `@context` and `@graph` are JSON-LD keywords that the standard requires. The operation is also out of scope (D2). |
| A-003 | API-18 | real (minor) | The `metadata` wrapper on GetAllSchemasResponsePayload. D30 keeps it for this build. The two vocabulary payloads are out of scope. Part of P-007. |
| A-004 | API-20 | real (minor) | Webhook.webhookId. Part of P-007. |
| A-005 | API-21 | real (minor) | 3 of 6 places are in scope (POST /schemas, PATCH /schemas/{schemaName}, POST /schemas/{schemaName}/versions); the 3 /vocabularies places are out of scope. Part of P-006. |
| A-006 | API-52 | real (minor) | schemaName and webhookId as path parameter names. D5 keeps them for this build. Part of P-007. |
| A-007 | COV-01 | classified | 9 operations, all already out of scope by D2 (see above). |
| A-008 | COV-02 | classified | 6 places, all already covered (see above). |
