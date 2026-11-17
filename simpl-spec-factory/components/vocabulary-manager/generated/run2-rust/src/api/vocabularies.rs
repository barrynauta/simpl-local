//! Internal vocabulary endpoints.

use super::AppState;
use super::support::*;
use crate::auth::Principal;
use crate::model::{Status, Version, Vocabulary};
use crate::problem::{ApiError, ApiResult, Issue};
use crate::service::{NewVersionInput, required_text};
use crate::validation::{Warning, is_valid_name};
use axum::Extension;
use axum::body::Bytes;
use axum::extract::{Path, RawQuery, Request, State};
use axum::http::{HeaderMap, HeaderValue, StatusCode, header};
use axum::response::{IntoResponse, Json, Response};
use serde_json::{Value, json};
use std::cmp::Ordering;

const JSON: &[&str] = &["application/json"];
const TURTLE: &[&str] = &["text/turtle"];

fn vocab_href(name: &str) -> String {
    format!("/vocabularies/{name}")
}
fn version_href(name: &str, n: u32) -> String {
    format!("/vocabularies/{name}/versions/{n}")
}

/// OpenAPI `VocabularyCategory`: latest metadata of a vocabulary.
pub fn category_json(state: &AppState, v: &Vocabulary) -> Value {
    let latest = v.latest();
    json!({
        "href": vocab_href(&v.name),
        "contentHref": state.content_href(&format!("/content/{}", v.name)),
        "name": v.name,
        "version": latest.number,
        "description": latest.description,
        "status": v.status(),
        "createdBy": v.created_by,
        "creationDate": fmt_date(&v.creation_date),
        "lastUpdateDate": fmt_date(&v.last_update_date()),
    })
}

/// OpenAPI `VocabularyMetadata`: metadata of one version.
pub fn version_json(state: &AppState, v: &Vocabulary, ver: &Version) -> Value {
    json!({
        "href": version_href(&v.name, ver.number),
        "contentHref": state.content_href(&format!("/content/{}/versions/{}", v.name, ver.number)),
        "latestVersionHref": v.latest_active().map(|a| version_href(&v.name, a.number)),
        "name": v.name,
        "version": ver.number,
        "description": ver.description,
        "changelog": ver.changelog,
        "status": ver.status,
        "createdBy": ver.created_by,
        "creationDate": fmt_date(&ver.creation_date),
        "lastUpdateDate": fmt_date(&ver.last_update_date),
    })
}

/// OpenAPI `VocabularyVersionMetadataResponse` (201 of the create operations, D20).
fn created_json(state: &AppState, v: &Vocabulary, ver: &Version, warnings: &[Warning]) -> Value {
    let mut out = json!({
        "href": version_href(&v.name, ver.number),
        "contentHref": state.content_href(&format!("/content/{}/versions/{}", v.name, ver.number)),
        "name": v.name,
        "version": ver.number,
        "description": ver.description,
        "changelog": ver.changelog,
        "status": ver.status,
        "createdBy": ver.created_by,
        "creationDate": fmt_date(&ver.creation_date),
        "lastUpdateDate": fmt_date(&ver.last_update_date),
    });
    if !warnings.is_empty() {
        out["validationWarnings"] = json!(warnings);
    }
    out
}

fn path_name(name: &str) -> ApiResult<()> {
    validate_name_param(name, "path", "vocabularyName")
}

fn cmp_ci(a: &str, b: &str) -> Ordering {
    a.to_lowercase().cmp(&b.to_lowercase()).then_with(|| a.cmp(b))
}

/// `GET /vocabularies` (SIMPL-18162, SIMPL-18164, D23, D29).
pub async fn list(
    State(state): State<AppState>,
    headers: HeaderMap,
    RawQuery(raw): RawQuery,
) -> ApiResult<Response> {
    check_accept(&headers, JSON)?;
    let q = parse_query(
        raw.as_deref(),
        &["name", "status", "creationDate", "lastUpdated", "sort", "page", "pageSize"],
    )?;
    let mut issues = Vec::new();
    let (page, page_size) = parse_paging(&q, &mut issues);
    let name = q.get("name").cloned();
    if let Some(n) = &name
        && !is_valid_name(n)
    {
        issues.push(name_issue(n, "query", "name"));
    }
    let status = match q.get("status") {
        None => None,
        Some(s) => match s.parse::<Status>() {
            Ok(st) => Some(st),
            Err(_) => {
                issues.push(Issue::schema_violation(
                    "query",
                    "status",
                    "Query parameter 'status' must be one of [ACTIVE, DEPRECATED].",
                    Some(s.clone().into()),
                ));
                None
            }
        },
    };
    let mut date_filter = |key: &str| match q.get(key) {
        None => None,
        Some(raw) => match parse_date_filter(raw) {
            Some(d) => Some(d),
            None => {
                issues.push(Issue::schema_violation(
                    "query",
                    key,
                    format!("Query parameter '{key}' must be an ISO 8601 date or date-time."),
                    Some(raw.clone().into()),
                ));
                None
            }
        },
    };
    let created = date_filter("creationDate");
    let updated = date_filter("lastUpdated");
    let sort_values = [
        "name", "-name", "status", "-status", "creationDate", "-creationDate", "lastUpdated", "-lastUpdated",
    ];
    let sort = q.get("sort").cloned();
    if let Some(s) = &sort
        && !sort_values.contains(&s.as_str())
    {
        issues.push(Issue::schema_violation(
            "query",
            "sort",
            format!("Query parameter 'sort' must be one of [{}].", sort_values.join(", ")),
            Some(s.clone().into()),
        ));
    }
    if !issues.is_empty() {
        return Err(ApiError::BadRequest(issues));
    }

    let mut items: Vec<Vocabulary> = state
        .service
        .list()
        .await?
        .into_iter()
        .filter(|v| {
            name.as_ref()
                .is_none_or(|n| v.name.to_lowercase().contains(&n.to_lowercase()))
        })
        .filter(|v| status.is_none_or(|s| v.status() == s))
        .filter(|v| created.is_none_or(|d| v.creation_date.date_naive() == d))
        .filter(|v| updated.is_none_or(|d| v.last_update_date().date_naive() == d))
        .collect();
    let sort_key = sort.clone().unwrap_or_else(|| "name".into());
    let (desc, field) = match sort_key.strip_prefix('-') {
        Some(f) => (true, f.to_string()),
        None => (false, sort_key.clone()),
    };
    items.sort_by(|a, b| {
        let o = match field.as_str() {
            "status" => a.status().cmp(&b.status()),
            "creationDate" => a.creation_date.cmp(&b.creation_date),
            "lastUpdated" => a.last_update_date().cmp(&b.last_update_date()),
            _ => Ordering::Equal,
        };
        let o = if field == "name" { cmp_ci(&a.name, &b.name) } else { o };
        let o = if desc { o.reverse() } else { o };
        o.then_with(|| cmp_ci(&a.name, &b.name))
    });

    let total = items.len();
    let mut extra: Vec<(String, String)> = Vec::new();
    if let Some(s) = sort {
        extra.push(("sort".into(), s));
    }
    for key in ["name", "status", "creationDate", "lastUpdated"] {
        if let Some(v) = q.get(key) {
            extra.push((key.into(), v.clone()));
        }
    }
    let links = page_links("/vocabularies", &extra, page, page_size, total);
    let page_items = paginate(items, page, page_size)
        .iter()
        .map(|v| category_json(&state, v))
        .collect();
    Ok(Json(links.into_json(page_items)).into_response())
}

/// `GET /vocabularies/{name}` (SIMPL-18170 retrieval).
pub async fn get_one(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(name): Path<String>,
) -> ApiResult<Response> {
    check_accept(&headers, JSON)?;
    path_name(&name)?;
    let v = state.service.get(&name).await?;
    Ok(Json(category_json(&state, &v)).into_response())
}

/// Extract `status` from a `VocabularyStatusPatchRequest` body.
fn parse_status_body(headers: &HeaderMap, body: &Bytes) -> ApiResult<Status> {
    require_json(headers)?;
    let value = parse_json(body)?;
    let raw = value.get("status");
    match raw {
        None => Err(ApiError::bad_request(Issue::schema_violation(
            "body",
            "status",
            "Field 'status' is required.",
            None,
        ))),
        Some(v) => v.as_str().and_then(|s| s.parse::<Status>().ok()).ok_or_else(|| {
            ApiError::bad_request(Issue::schema_violation(
                "body",
                "status",
                "Field 'status' must be one of [ACTIVE, DEPRECATED].",
                Some(v.clone()),
            ))
        }),
    }
}

/// `PATCH /vocabularies/{name}` (SIMPL-18175, D10).
pub async fn patch_status(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(name): Path<String>,
    body: Bytes,
) -> ApiResult<Response> {
    check_accept(&headers, JSON)?;
    path_name(&name)?;
    let status = parse_status_body(&headers, &body)?;
    let v = state.service.update_vocabulary_status(&name, status).await?;
    Ok(Json(category_json(&state, &v)).into_response())
}

/// `GET /vocabularies/{name}/versions` (SIMPL-18168).
pub async fn list_versions(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(name): Path<String>,
    RawQuery(raw): RawQuery,
) -> ApiResult<Response> {
    check_accept(&headers, JSON)?;
    path_name(&name)?;
    let q = parse_query(raw.as_deref(), &["sort", "page", "pageSize"])?;
    let mut issues = Vec::new();
    let (page, page_size) = parse_paging(&q, &mut issues);
    let sort_values = ["name", "-name", "status", "-status", "creationDate", "-creationDate"];
    let sort = q.get("sort").cloned();
    if let Some(s) = &sort
        && !sort_values.contains(&s.as_str())
    {
        issues.push(Issue::schema_violation(
            "query",
            "sort",
            format!("Query parameter 'sort' must be one of [{}].", sort_values.join(", ")),
            Some(s.clone().into()),
        ));
    }
    if !issues.is_empty() {
        return Err(ApiError::BadRequest(issues));
    }
    let v = state.service.get(&name).await?;
    let mut versions = v.versions.clone();
    match sort.as_deref() {
        None => versions.sort_by(|a, b| b.number.cmp(&a.number)),
        Some(s) => {
            let (desc, field) = match s.strip_prefix('-') {
                Some(f) => (true, f),
                None => (false, s),
            };
            versions.sort_by(|a, b| {
                let o = match field {
                    "status" => a.status.cmp(&b.status),
                    "creationDate" => a.creation_date.cmp(&b.creation_date),
                    _ => Ordering::Equal,
                };
                let o = o.then_with(|| a.number.cmp(&b.number));
                if desc { o.reverse() } else { o }
            });
        }
    }
    let total = versions.len();
    let mut extra = Vec::new();
    if let Some(s) = sort {
        extra.push(("sort".to_string(), s));
    }
    let links = page_links(&format!("/vocabularies/{name}/versions"), &extra, page, page_size, total);
    let items = paginate(versions, page, page_size)
        .iter()
        .map(|ver| version_json(&state, &v, ver))
        .collect();
    Ok(Json(links.into_json(items)).into_response())
}

/// `GET /vocabularies/{name}/versions/{version}` (SIMPL-18166, SIMPL-18176).
pub async fn get_version(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path((name, version)): Path<(String, String)>,
) -> ApiResult<Response> {
    check_accept(&headers, JSON)?;
    path_name(&name)?;
    let n = parse_version_param(&version)?;
    let v = state.service.get(&name).await?;
    let ver = v.version(n).ok_or_else(|| ApiError::version_not_found(&name, n))?;
    Ok(Json(version_json(&state, &v, ver)).into_response())
}

/// `PATCH /vocabularies/{name}/versions/{version}` (OpenAPI only, D10).
pub async fn patch_version_status(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path((name, version)): Path<(String, String)>,
    body: Bytes,
) -> ApiResult<Response> {
    check_accept(&headers, JSON)?;
    path_name(&name)?;
    let n = parse_version_param(&version)?;
    let status = parse_status_body(&headers, &body)?;
    let v = state.service.update_version_status(&name, n, status).await?;
    let ver = v.version(n).expect("updated version exists");
    Ok(Json(version_json(&state, &v, ver)).into_response())
}

fn created_response(state: &AppState, v: &Vocabulary, number: u32, warnings: &[Warning]) -> Response {
    let ver = v.version(number).expect("created version exists");
    let mut resp = (StatusCode::CREATED, Json(created_json(state, v, ver, warnings))).into_response();
    if let Ok(loc) = HeaderValue::from_str(&version_href(&v.name, number)) {
        resp.headers_mut().insert(header::LOCATION, loc);
    }
    resp
}

/// Common multipart checks for both create operations: required fields, no `version` (D8), Turtle file (D15).
fn version_input(form: crate::api::support::Form, issues: &mut Vec<Issue>) -> ApiResult<NewVersionInput> {
    if form.has("version") {
        issues.push(Issue::schema_violation(
            "body",
            "version",
            "Field 'version' must not be provided. The version number is assigned automatically.",
            form.first("version").map(Value::from),
        ));
    }
    let description = required_text(form.first("description"), "description", issues);
    let changelog = required_text(form.first("changelog"), "changelog", issues);
    if form.file.is_none() {
        issues.push(Issue::schema_violation(
            "body",
            "vocabularyFile",
            "Field 'vocabularyFile' is required.",
            None,
        ));
    }
    if !issues.is_empty() {
        return Err(ApiError::BadRequest(std::mem::take(issues)));
    }
    let file = form.file.expect("checked");
    check_turtle_file(&file)?;
    Ok(NewVersionInput { description, changelog, file: file.bytes })
}

/// `POST /vocabularies` (SIMPL-18170, 18181, 18183, 18187, 18194).
pub async fn create(
    State(state): State<AppState>,
    Extension(principal): Extension<Principal>,
    req: Request,
) -> ApiResult<Response> {
    check_accept(req.headers(), JSON)?;
    let form = read_form(req, state.cfg.max_upload_bytes).await?;
    let mut issues = Vec::new();
    let name = form.first("name");
    match &name {
        None => issues.push(Issue::schema_violation("body", "name", "Field 'name' is required.", None)),
        Some(n) if !is_valid_name(n) => issues.push(name_issue(n, "body", "name")),
        _ => {}
    }
    let input = version_input(form, &mut issues)?;
    let name = name.expect("checked");
    let (v, warnings) = state
        .service
        .create_vocabulary(&name, input, &principal.display_name())
        .await?;
    Ok(created_response(&state, &v, 1, &warnings))
}

/// `POST /vocabularies/{name}/versions` (SIMPL-18173, 18190, 18191, D8, D9, D11).
pub async fn create_version(
    State(state): State<AppState>,
    Extension(principal): Extension<Principal>,
    Path(name): Path<String>,
    req: Request,
) -> ApiResult<Response> {
    check_accept(req.headers(), JSON)?;
    path_name(&name)?;
    let form = read_form(req, state.cfg.max_upload_bytes).await?;
    let mut issues = Vec::new();
    let input = version_input(form, &mut issues)?;
    let (v, warnings) = state
        .service
        .create_version(&name, input, &principal.display_name())
        .await?;
    let number = v.latest().number;
    Ok(created_response(&state, &v, number, &warnings))
}

/// `POST /vocabularies/searches`.
pub async fn search(
    State(state): State<AppState>,
    headers: HeaderMap,
    body: Bytes,
) -> ApiResult<Response> {
    check_accept(&headers, JSON)?;
    require_json(&headers)?;
    let value = parse_json(&body)?;
    let mut issues = Vec::new();
    let Some(obj) = value.as_object() else {
        return Err(ApiError::bad_request(Issue::schema_violation("body", "body", "The body must be a JSON object.", None)));
    };
    for k in obj.keys().filter(|k| *k != "items") {
        issues.push(Issue::schema_violation("body", k, format!("Property '{k}' is not allowed."), None));
    }
    let mut requests: Vec<(String, Option<u32>)> = Vec::new();
    match obj.get("items").and_then(Value::as_array) {
        None => issues.push(Issue::schema_violation("body", "items", "Field 'items' is required and must be an array.", None)),
        Some(arr) if arr.is_empty() => {
            issues.push(Issue::schema_violation("body", "items", "Field 'items' must contain at least one item.", None))
        }
        Some(arr) => {
            for (i, item) in arr.iter().enumerate() {
                let Some(o) = item.as_object() else {
                    issues.push(Issue::schema_violation("body", &format!("items[{i}]"), "Each item must be an object.", None));
                    continue;
                };
                for k in o.keys().filter(|k| *k != "name" && *k != "version") {
                    issues.push(Issue::schema_violation("body", &format!("items[{i}].{k}"), format!("Property '{k}' is not allowed."), None));
                }
                let name = o.get("name").and_then(Value::as_str);
                match name {
                    Some(n) if is_valid_name(n) => {}
                    Some(n) => issues.push(name_issue(n, "body", &format!("items[{i}].name"))),
                    None => issues.push(Issue::schema_violation("body", &format!("items[{i}].name"), "Field 'name' is required.", None)),
                }
                let version = match o.get("version") {
                    None | Some(Value::Null) => None,
                    Some(v) => match v.as_u64() {
                        Some(n) if n >= 1 && n <= u32::MAX as u64 => Some(n as u32),
                        _ => {
                            issues.push(Issue::schema_violation(
                                "body",
                                &format!("items[{i}].version"),
                                "Field 'version' must be an integer of at least 1.",
                                Some(v.clone()),
                            ));
                            None
                        }
                    },
                };
                if let Some(n) = name {
                    requests.push((n.to_string(), version));
                }
            }
        }
    }
    if !issues.is_empty() {
        return Err(ApiError::BadRequest(issues));
    }
    let mut items = Vec::new();
    for (name, version) in requests {
        let Some(v) = state.service.find(&name).await? else { continue };
        match version {
            None => {
                let latest = v.latest();
                items.push(json!({
                    "href": vocab_href(&v.name),
                    "contentHref": state.content_href(&format!("/content/{}", v.name)),
                    "name": v.name,
                    "version": latest.number,
                    "status": v.status(),
                    "latest": true,
                }));
            }
            Some(n) => {
                if let Some(ver) = v.version(n) {
                    items.push(json!({
                        "href": version_href(&v.name, n),
                        "contentHref": state.content_href(&format!("/content/{}/versions/{}", v.name, n)),
                        "name": v.name,
                        "version": n,
                        "status": ver.status,
                        "latest": false,
                    }));
                }
            }
        }
    }
    let total = items.len();
    let page_size = (total as u32).max(DEFAULT_PAGE_SIZE);
    Ok(Json(json!({
        "items": items,
        "page": 1,
        "pageSize": page_size,
        "total": total,
        "totalPages": if total == 0 { 0 } else { 1 },
    }))
    .into_response())
}

fn turtle_response(filename: String, content: String) -> Response {
    let mut resp = (StatusCode::OK, content).into_response();
    resp.headers_mut().insert(
        header::CONTENT_TYPE,
        HeaderValue::from_static("text/turtle; charset=utf-8"),
    );
    if let Ok(v) = HeaderValue::from_str(&format!("attachment; filename=\"{filename}\"")) {
        resp.headers_mut().insert(header::CONTENT_DISPOSITION, v);
    }
    resp
}

/// `GET /content/{name}` (SIMPL-18165, D24).
pub async fn download_latest(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(name): Path<String>,
) -> ApiResult<Response> {
    check_accept(&headers, TURTLE)?;
    path_name(&name)?;
    let (_, content) = state.service.content(&name, None).await?;
    Ok(turtle_response(format!("{name}.ttl"), content))
}

/// `GET /content/{name}/versions/{version}` (SIMPL-18452, D24).
pub async fn download_version(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path((name, version)): Path<(String, String)>,
) -> ApiResult<Response> {
    check_accept(&headers, TURTLE)?;
    path_name(&name)?;
    let n = parse_version_param(&version)?;
    let (_, content) = state.service.content(&name, Some(n)).await?;
    Ok(turtle_response(format!("{name}-v{n}.ttl"), content))
}
