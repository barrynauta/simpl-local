# Traceability

Code paths are relative to `build/`. "IT" = integration test in `tests/api.rs`; "UT" = unit test
module inside the named source file. `smoke.sh` runs against the real stack (Keycloak and Fuseki).

## API stories (13 in scope)

| Story | Summary | Code | Own tests |
|---|---|---|---|
| SIMPL-18162 | List metadata of all vocabularies, paginated; 400 bad paging; 403; 500 | `api/vocabularies.rs::list`, `category_json`; `api/support.rs::parse_paging`, `page_links`; `auth.rs::require_auth` | IT `list_filters_sorting_and_paging`, `authentication_and_roles`, `store_failure_gives_500`; UT `support::paging_limits`, `links`; smoke |
| SIMPL-18164 | Filter and sort on name, status, creationDate, lastUpdated; empty result; paging with sort; invalid filter or sort 400 | `api/vocabularies.rs::list`; `api/support.rs::parse_query`, `parse_date_filter` | IT `list_filters_sorting_and_paging`; UT `support::date_filters` |
| SIMPL-18166 | Full details of a version; 404 if missing | `api/vocabularies.rs::get_version`, `version_json` | IT `new_version_deprecates_previous_versions` |
| SIMPL-18168 | Version history; 404 if vocabulary missing | `api/vocabularies.rs::list_versions` | IT `new_version_deprecates_previous_versions`, `not_acceptable_and_not_found` |
| SIMPL-18170 | Create vocabulary, version 1 ACTIVE, status ACTIVE; invalid data rejected, nothing created | `api/vocabularies.rs::create`; `service.rs::create_vocabulary` | IT `create_vocabulary_and_read_it_back`, `create_rejects_invalid_metadata`; smoke |
| SIMPL-18173 | New version: next number, ACTIVE, all previous DEPRECATED; invalid data, missing vocabulary, 401, 403, unsupported format rejected | `api/vocabularies.rs::create_version`; `service.rs::create_version` | IT `new_version_deprecates_previous_versions`, `new_version_for_missing_vocabulary_is_404`, `create_rejects_invalid_metadata` (415), `authentication_and_roles`; smoke |
| SIMPL-18175 | Update vocabulary status ACTIVE/DEPRECATED; 404; invalid value 400 | `api/vocabularies.rs::patch_status`; `service.rs::update_vocabulary_status` | IT `status_changes` |
| SIMPL-18181 | Syntax validation on create and new version; all errors with location; no semantic step, nothing created or deprecated | `validation.rs::parse_turtle`; `service.rs::validate_content` | IT `syntax_errors_are_reported_with_locations`; UT `validation::syntax_errors_are_all_reported_with_line_and_column` |
| SIMPL-18183 | Semantic validation after syntax; all errors; warnings for unresolved external namespaces; not created on errors | `validation.rs::check_semantics`; `service.rs::semantic_context` | IT `semantic_references_between_vocabularies`, `warnings_for_unregistered_external_namespaces`; UT `validation::*` |
| SIMPL-18187 | Valid creation is persisted and available, identifier in the response | `service.rs::create_vocabulary`, `create_version`; `store/fuseki.rs::save_vocabulary`; `Location` and `href` in `created_response` | IT `create_vocabulary_and_read_it_back`; smoke (data survives a backend restart, checked by hand) |
| SIMPL-18190 | Automatic version number; manual version rejected, nothing created | `model.rs::next_version_number`; `api/vocabularies.rs::version_input` | IT `new_version_deprecates_previous_versions`, `manual_version_field_is_rejected` |
| SIMPL-18194 | Structured report with all errors and locations; success without error details | `problem.rs` (issues), `api/vocabularies.rs::created_json` | IT `syntax_errors_are_reported_with_locations`, `semantic_references_between_vocabularies` |
| SIMPL-18196 | RBAC on all endpoints: 403 without role, authentication error without token | `auth.rs::require_auth`, `required_access`, `Authenticator::verify` | IT `authentication_and_roles`; UT `auth::access_rules`, `role_checks`; smoke (401, 403 with real Keycloak tokens) |

## API behaviour behind the UI stories (19 in scope)

| Story | What the API provides | Code | Own tests |
|---|---|---|---|
| SIMPL-18161 | List with name, description, version, status, created, last updated, creator; error as Problem | `api/vocabularies.rs::list`, `category_json` | IT `list_filters_sorting_and_paging`, `store_failure_gives_500` |
| SIMPL-18163 | Status filter, name contains (D23), sortable columns | `api/vocabularies.rs::list` | IT `list_filters_sorting_and_paging` |
| SIMPL-18165 | Download newest version | `api/vocabularies.rs::download_latest` | IT `create_vocabulary_and_read_it_back`; smoke |
| SIMPL-18167 | Version list with version, status, date, description, changelog | `api/vocabularies.rs::list_versions` | IT `new_version_deprecates_previous_versions` |
| SIMPL-18169 | Required fields, name format, unique name (409), file format (415) | `api/vocabularies.rs::create`; `service.rs::create_vocabulary` | IT `create_rejects_invalid_metadata`, `create_vocabulary_and_read_it_back` |
| SIMPL-18172 | New version, previous DEPRECATED, errors as Problem | `service.rs::create_version` | IT `new_version_deprecates_previous_versions` |
| SIMPL-18174 | Status change, not found 404, failure 500 | `service.rs::update_vocabulary_status` | IT `status_changes`, `store_failure_gives_500` |
| SIMPL-18176 | Deprecated versions readable by VIEWER, with id, date, creator, changelog | `api/vocabularies.rs::version_json` (includes `createdBy`) | IT `new_version_deprecates_previous_versions`, `authentication_and_roles` |
| SIMPL-18178 | Status per vocabulary so the UI can warn | `category_json` `status` | IT `status_changes` |
| SIMPL-18180 | Syntax and structure errors, all of them, with location | `validation.rs::parse_turtle`, `check_structure` | IT `syntax_errors_are_reported_with_locations`, `structure_errors`; UT `validation::structure_requires_ontology_or_concept_scheme` |
| SIMPL-18182 | Semantic validation report | `validation.rs::check_semantics` | IT `semantic_references_between_vocabularies` |
| SIMPL-18184 | Deprecated or missing referenced version: error naming it, advice to use latest ACTIVE; no change stored | `validation.rs::check_semantics` (D17, D18) | IT `semantic_references_between_vocabularies`; UT `validation::deprecated_version_is_an_error_and_points_to_active`, `missing_vocabulary_or_version_is_an_error` |
| SIMPL-18186 | Valid version stored as ACTIVE at once | `service.rs::create_vocabulary`, `create_version` | IT `create_vocabulary_and_read_it_back` |
| SIMPL-18189 | Assigned number returned in the 201 response | `created_json` `version` | IT `new_version_deprecates_previous_versions` |
| SIMPL-18191 | New ACTIVE, all previous DEPRECATED | `service.rs::create_version` | IT `new_version_deprecates_previous_versions`; smoke |
| SIMPL-18193 | Structured report on 400, warnings on 201 | `problem.rs`, `created_json` | IT `syntax_errors_are_reported_with_locations`, `warnings_for_unregistered_external_namespaces` |
| SIMPL-18195 | 403 for restricted actions, 401 unauthenticated; hiding is UI (D26) | `auth.rs::require_auth` | IT `authentication_and_roles` |
| SIMPL-18452 | Download a selected version | `api/vocabularies.rs::download_version` | IT `new_version_deprecates_previous_versions`; smoke |
| SIMPL-18559 | Link from a deprecated version to the latest ACTIVE one | `version_json` `latestVersionHref` | IT `new_version_deprecates_previous_versions` |

## OpenAPI-only operations

| Operation | Code | Own tests |
|---|---|---|
| `GET /health` | `api/mod.rs::health` | IT `health_is_public`; smoke |
| `POST /vocabularies/searches` | `api/vocabularies.rs::search` | IT `vocabulary_search`, `authentication_and_roles` |
| `PATCH /vocabularies/{name}/versions/{n}` | `api/vocabularies.rs::patch_version_status`; `service.rs::update_version_status` | IT `status_changes` |
| External vocabularies (6 operations) | `api/external.rs`; `service.rs` external functions | IT `external_vocabularies`, `warnings_for_unregistered_external_namespaces`; UT `external::alias_parsing` |

## Decisions

| # | Code | Own tests | Status |
|---|---|---|---|
| D1 | `api/mod.rs::router` | all IT | Implemented |
| D2 | `api/mod.rs::router` (health outside auth) | IT `health_is_public`, `authentication_and_roles` | Implemented |
| D3 | `auth.rs::required_access`, `Principal::can_read/can_write` | UT `auth::*`; IT `authentication_and_roles` | Implemented |
| D4 | `problem.rs`, `auth.rs::require_auth` | IT `authentication_and_roles` | Implemented |
| D5 | `problem.rs`, `api/mod.rs::problem_for_bare_errors` | UT `problem::*`; IT `assert_problem` helper everywhere | Implemented |
| D6 | `deploy/keycloak/simpl-realm.json`, `docker-compose.yml`, `auth.rs` | smoke | Implemented |
| D7 | `model.rs::next_version_number`, `service.rs` | IT `new_version_deprecates_previous_versions` | Implemented |
| D8 | `api/vocabularies.rs::version_input` | IT `manual_version_field_is_rejected` | Implemented |
| D9 | `service.rs::create_version`, `store/fuseki.rs::save_vocabulary` | IT `new_version_deprecates_previous_versions` | Implemented (atomic in Fuseki; in-memory store trivially) |
| D10 | `model.rs::status`, `service.rs::update_*_status` | IT `status_changes`; UT `model::vocabulary_status_is_active_if_any_version_active` | Implemented |
| D11 | `service.rs::create_version` | IT `status_changes` | Implemented |
| D12 | none (UI) | none | Out of scope (UI); API requires `changelog` |
| D13 | `service.rs::create_vocabulary` | IT `create_vocabulary_and_read_it_back` | Implemented |
| D14 | `service.rs` (no publish step) | IT `create_vocabulary_and_read_it_back` | Implemented |
| D15 | `api/support.rs::check_turtle_file` | UT `support::turtle_file_detection`; IT `create_rejects_invalid_metadata` | Implemented |
| D16 | `validation.rs::parse_turtle`, `check_structure` | UT and IT listed above | Implemented |
| D17 | `validation.rs::check_semantics` | UT `validation::*`; IT `semantic_references_between_vocabularies`, `warnings_for_unregistered_external_namespaces` | Implemented, plus one extra check (see BUILD-LOG) |
| D18 | `validation.rs::parse_internal_iri`, `check_semantics` (`self_name`), `service.rs::semantic_context`, `config.rs` `REFERENCE_BASE` | UT `validation::parse_internal_reference_forms`, `own_namespace_is_not_a_reference`; IT `own_namespace_is_not_a_reference` | Implemented (incl. 28 Oct clarification) |
| D19 | `service.rs::validate_content` | IT `semantic_references_between_vocabularies` | Implemented |
| D20 | `api/vocabularies.rs::created_json` | IT `warnings_for_unregistered_external_namespaces` | Implemented |
| D21 | none | none | Out of scope (UI design) |
| D22 | `category_json` | IT `create_vocabulary_and_read_it_back` | API side implemented; columns are UI |
| D23 | `api/vocabularies.rs::list` | IT `list_filters_sorting_and_paging` | Implemented |
| D24 | `download_latest`, `download_version` | IT, smoke | Implemented |
| D25 | `version_json` `latestVersionHref`, status fields | IT `new_version_deprecates_previous_versions` | API support only; display is UI |
| D26 | `auth.rs` (403) | IT `authentication_and_roles` | API side implemented; hiding is UI |
| D27 | `store/fuseki.rs`, `deploy/fuseki/assembler.ttl` | smoke; UT `rdf_mapping::*` | Implemented |
| D28 | synchronous responses | IT | Implemented (nothing to send) |
| D29 | `api/support.rs::parse_paging` | UT `support::paging_limits`; IT `list_filters_sorting_and_paging` | Implemented |
| D30 | `api/support.rs::read_form`, `MAX_UPLOAD_BYTES` | IT `upload_limit_is_enforced` | Implemented |

## Out of scope

| Item | Reason |
|---|---|
| SIMPL-393, SIMPL-395, SIMPL-28680 | Out of scope per digest section 2 |
| Manual semantic review (BP02B.02), sunset policy (BP02B.05), offering vocabularies to other participants | Out of scope per digest section 2 |
| All UI rendering (D12, D21, D22 columns, D24 screen, D25 display, D26 hiding) | No UI in this build |
| OpenAPI OAuth2 scopes `read` / `write` | Roles from the `roles` claim are used instead (D3); scopes are not checked |
