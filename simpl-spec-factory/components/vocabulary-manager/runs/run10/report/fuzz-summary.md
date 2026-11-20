# API fuzzing (step 70)

Schemathesis against `http://localhost:8196/v1`, contract `specs/api/vocabulary_openapi-tier1.yaml`, 30 examples per operation, seed 1. Known failures from `fuzz-known.yaml` are explained by a decision; the rest are findings.

Operations: 20. Failed checks: 14. Explained by a decision: 7. Findings: 7. Server errors: 0.

## Findings

| Operation | Check | Status | Server answer |
|---|---|---|---|
| POST /vocabularies/searches | ST202 Schema-valid request rejected | 400 | 'version' must be a |
| GET /content/{vocabularyName}/versions/{version} | ST202 Schema-valid request rejected | 400 | 'version' must be a |
| GET /external-vocabularies | ST202 Schema-valid request rejected | 400 | 'pageSize' must be a |
| GET /vocabularies | ST202 Schema-valid request rejected | 400 | 'creationDate' is no |
| GET /vocabularies/{vocabularyName}/versions | ST202 Schema-valid request rejected | 400 | 'pageSize' must be a |
| GET /vocabularies/{vocabularyName}/versions/{version} | ST202 Schema-valid request rejected | 400 | 'version' must be a |
| PATCH /vocabularies/{vocabularyName}/versions/{version} | ST202 Schema-valid request rejected | 400 | 'version' must be a |

## Explained by a decision

| Operation | Check | Decision | Reason |
|---|---|---|---|
| POST /external-vocabularies | ST202 Schema-valid request rejected | D9 | the OpenAPI types the file as binary; the decisions require valid Turtle |
| POST /external-vocabularies/content | ST202 Schema-valid request rejected | D52 | unknown JSON properties are rejected, also where the schema allows additional properties; D38 limits on search bodies |
| POST /external-vocabulary/searches | ST202 Schema-valid request rejected | D38 | each search array has at least 1 entry when present; at most 100 URIs in total |
| POST /vocabularies | ST202 Schema-valid request rejected | D9 | the OpenAPI types the file as binary; the decisions require valid Turtle |
| POST /vocabularies/{vocabularyName}/versions | ST202 Schema-valid request rejected | D37 | changelog 1 to 1000 characters after trimming |
| GET /vocabularies | ST201 Schema-violating request accepted | D19 | the name filter pattern is relaxed to ^[A-Za-z0-9]+$ |
| PATCH /vocabularies/{vocabularyName} | ST202 Schema-valid request rejected | D52 | unknown JSON properties are rejected, also where the schema allows additional properties; D38 limits on search bodies |
