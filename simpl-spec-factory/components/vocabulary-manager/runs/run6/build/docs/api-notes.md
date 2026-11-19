# API notes

Contract: `input/specs/api/vocabulary_openapi-tier1.yaml` (not edited, D39). Base URL `http://localhost:8196/v1`. The build implements every operation of the file (19 operations on 13 paths) plus the decided changes below.

## Decided changes to the file (D39)

| Point | Decision | Behaviour |
|---|---|---|
| 401 and 403 on every secured operation | D17, D48 | `urn:problem-type:simpl:unauthorized` / `urn:problem-type:simpl:forbidden`, problem+json |
| 503 on every operation that reads or writes the store | D44 | `urn:problem-type:belgif:serviceUnavailable`, `Retry-After: 5` |
| Name filter | D19 | `^[A-Za-z0-9]+$`, 1 to 64, case-insensitive contains |
| Page size | D22 | 1 to 100, default 20; page from 1, default 1 |
| `createdBy` on version objects | D27 | email claim, else preferred_username |
| `validationWarnings` | D10 | always present on 201, codes `externalReferenceSkipped`, `deprecatedVocabularyReference` |
| Description and changelog | D37 | 1 to 1000 characters after trimming |
| `version` form field on POST .../versions | D7 | 400, in body, name `version` |
| `latestVersionHref` | D5 | always filled |
| Unknown query parameters, repeated parameters, unknown fields | D21, D52 | 400 InputValidationProblem, one issue per name |
| Search limits | D38 | 1 to 100 items; external at most 100 URIs; no paging links |
| Identical new version | D46 | 409 ConflictResponse |
| Invalid path value | D49 | 400 schemaViolation, in path |
| Undeclared method | finish line | 405 problem+json with `Allow` header, also for HEAD, OPTIONS and non-standard methods such as QUERY |
| Unknown path | D55 | 404 problem+json |

## Problem types

400 `urn:problem-type:belgif:badRequest` (issues typed `urn:problem-type:belgif:input-validation:invalidInput`, `schemaViolation` or `unknownInput`), 404 `urn:problem-type:belgif:resourceNotFound`, 500 `urn:problem-type:belgif:internalServerError`, 503 `urn:problem-type:belgif:serviceUnavailable`; 401, 403, 405, 406, 409, 413, 415 use `urn:problem-type:simpl:...` (D48). No problem body carries a stack trace or an internal host name.

## Order of checks on writes (D51)

401, 403, 415, 413, 400 (path, unknown fields and parameters, field rules, version field), 404, 409 (duplicate name, same status, identical version), then content validation (syntax, then semantic).

## Fuzzing (Schemathesis 4.33.4, seed 1, 30 examples, 300 s per run)

Three runs, each about 44 000 to 46 000 generated requests, none gave a 5xx.

- Run 1 found 13 operations where a non-standard method (QUERY) gave 400 instead of 405. Fixed (firewall rejection handler).
- Run 2 found that a long description or changelog (about 70 KB) gave 413 'file is too large' instead of the D37 400. Fixed (Tomcat form-post limit).
- Final run: 11 findings, all decided changes of the contract: `pageSize` above 100 (D22), `name=000` accepted on the list (D19 relaxes the pattern), an invalid calendar date (D20), unknown JSON properties (D52), non-Turtle files on create and register (D9, D30), an empty `aliasUris` array in the external search (D38), an empty changelog (D37).

Reports: `reports/schemathesis-before-fix.txt`, `reports/schemathesis-after-fix.txt`, `reports/schemathesis-final.txt`.
