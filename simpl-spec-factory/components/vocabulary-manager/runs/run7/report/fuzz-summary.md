# API fuzzing (step 70)

Schemathesis against `http://localhost:8196/v1`, contract `specs/api/vocabulary_openapi-tier1.yaml`, 30 examples per operation, seed 1. Known failures from `fuzz-known.yaml` are explained by a decision; the rest are findings.

Operations: 20. Failed checks: 11. Explained by a decision: 9. Findings: 2. Server errors: 0.

## Findings

| Operation | Check | Status | Server answer |
|---|---|---|---|
| POST /external-vocabulary/searches | ST202 Schema-valid request rejected | 400 | 'aliasUris' must be |
| POST /vocabularies/{vocabularyName}/versions | ST202 Schema-valid request rejected | 400 | 'changelog' must be |

## Explained by a decision

| Operation | Check | Decision | Reason |
|---|---|---|---|
| POST /external-vocabularies | ST202 Schema-valid request rejected | D9 | the OpenAPI types the file as binary; the decisions require valid Turtle |
| POST /external-vocabularies/content | ST202 Schema-valid request rejected | D52 | unknown JSON properties are rejected, also where the schema allows additional properties; D38 limits on search bodies |
| POST /vocabularies | ST202 Schema-valid request rejected | D9 | the OpenAPI types the file as binary; the decisions require valid Turtle |
| GET /external-vocabularies | ST202 Schema-valid request rejected | D22 | page at least 1, pageSize 1 to 100 |
| GET /vocabularies | ST201 Schema-violating request accepted | D19 | the name filter pattern is relaxed to ^[A-Za-z0-9]+$ |
| GET /vocabularies | ST202 Schema-valid request rejected | D20 | a creationDate that matches the pattern but is not a real date is refused |
| GET /vocabularies/{vocabularyName}/versions | ST202 Schema-valid request rejected | D22 | page at least 1, pageSize 1 to 100 |
| PATCH /vocabularies/{vocabularyName} | ST202 Schema-valid request rejected | D52 | unknown JSON properties are rejected, also where the schema allows additional properties; D38 limits on search bodies |
| PATCH /vocabularies/{vocabularyName}/versions/{version} | ST202 Schema-valid request rejected | D52 | unknown JSON properties are rejected, also where the schema allows additional properties; D38 limits on search bodies |
