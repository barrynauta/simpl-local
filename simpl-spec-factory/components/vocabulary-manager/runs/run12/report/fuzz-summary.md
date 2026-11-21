# API fuzzing (step 70)

Schemathesis against `http://localhost:8196/v1`, contract `specs/api/vocabulary_openapi-tier1.yaml`, 30 examples per operation, seed 1. Known failures from `fuzz-known.yaml` are explained by a decision; the rest are findings.

Operations: 20. Failed checks: 14. Explained by a decision: 14. Findings: 0. Server errors: 0.

## Findings

| Operation | Check | Status | Server answer |
|---|---|---|---|
| none | | | |

## Explained by a decision

| Operation | Check | Decision | Reason |
|---|---|---|---|
| POST /external-vocabularies | ST202 Schema-valid request rejected | D9 | the OpenAPI types the file as binary; the decisions require valid Turtle |
| POST /external-vocabularies/content | ST202 Schema-valid request rejected | D52 | unknown JSON properties are rejected, also where the schema allows additional properties; D38 limits on search bodies |
| POST /external-vocabulary/searches | ST202 Schema-valid request rejected | D38 | each search array has at least 1 entry when present; at most 100 URIs in total |
| POST /vocabularies | ST202 Schema-valid request rejected | D9 | the OpenAPI types the file as binary; the decisions require valid Turtle |
| POST /vocabularies/searches | ST202 Schema-valid request rejected | D91 | integers are 32-bit, 1 to 2147483647 (D91, D112) |
| POST /vocabularies/{vocabularyName}/versions | ST202 Schema-valid request rejected | D37 | changelog 1 to 1000 characters after trimming |
| GET /content/{vocabularyName}/versions/{version} | ST202 Schema-valid request rejected | D49 | a version in the path must be a positive integer |
| GET /external-vocabularies | ST202 Schema-valid request rejected | D22 | page at least 1, pageSize 1 to 100 |
| GET /vocabularies | ST201 Schema-violating request accepted | D19 | the name filter pattern is relaxed to ^[A-Za-z0-9]+$ |
| GET /vocabularies | ST202 Schema-valid request rejected | D20 | a creationDate that matches the pattern but is not a real date is refused |
| GET /vocabularies/{vocabularyName}/versions | ST202 Schema-valid request rejected | D22 | page at least 1, pageSize 1 to 100 |
| GET /vocabularies/{vocabularyName}/versions/{version} | ST202 Schema-valid request rejected | D49 | a version in the path must be a positive integer |
| PATCH /vocabularies/{vocabularyName} | ST202 Schema-valid request rejected | D52 | unknown JSON properties are rejected, also where the schema allows additional properties; D38 limits on search bodies |
| PATCH /vocabularies/{vocabularyName}/versions/{version} | ST202 Schema-valid request rejected | D49 | a version in the path must be a positive integer |
