//! External vocabulary endpoints (OpenAPI only, no story).

use super::AppState;
use super::support::*;
use crate::auth::Principal;
use crate::model::ExternalVocabulary;
use crate::problem::{ApiError, ApiResult, Issue};
use crate::service::NewExternalInput;
use crate::validation::{is_absolute_uri, is_valid_name};
use axum::Extension;
use axum::body::Bytes;
use axum::extract::{Path, RawQuery, Request, State};
use axum::http::{HeaderMap, HeaderValue, StatusCode, header};
use axum::response::{IntoResponse, Json, Response};
use serde_json::{Value, json};

const JSON: &[&str] = &["application/json"];
const TURTLE: &[&str] = &["text/turtle"];

fn href(name: &str) -> String {
    format!("/external-vocabularies/{name}")
}

pub fn external_json(e: &ExternalVocabulary) -> Value {
    let mut v = json!({
        "href": href(&e.name),
        "name": e.name,
        "namespaceUri": e.namespace_uri,
        "aliasUris": e.alias_uris,
        "createdBy": e.created_by,
        "creationDate": fmt_date(&e.creation_date),
    });
    if let Some(s) = &e.source_uri {
        v["sourceUri"] = json!(s);
    }
    if let Some(d) = &e.description {
        v["description"] = json!(d);
    }
    v
}

fn path_name(name: &str) -> ApiResult<()> {
    validate_name_param(name, "path", "name")
}

/// `GET /external-vocabularies`.
pub async fn list(
    State(state): State<AppState>,
    headers: HeaderMap,
    RawQuery(raw): RawQuery,
) -> ApiResult<Response> {
    check_accept(&headers, JSON)?;
    let q = parse_query(raw.as_deref(), &["page", "pageSize"])?;
    let mut issues = Vec::new();
    let (page, page_size) = parse_paging(&q, &mut issues);
    if !issues.is_empty() {
        return Err(ApiError::BadRequest(issues));
    }
    let all = state.service.list_external().await?;
    let links = page_links("/external-vocabularies", &[], page, page_size, all.len());
    let items = paginate(all, page, page_size).iter().map(external_json).collect();
    Ok(Json(links.into_json(items)).into_response())
}

/// Split repeated / comma separated / JSON array alias values into a flat list.
fn alias_values(raw: Vec<String>) -> Vec<String> {
    let mut out = Vec::new();
    for r in raw {
        let t = r.trim();
        if t.starts_with('[')
            && let Ok(list) = serde_json::from_str::<Vec<String>>(t)
        {
            out.extend(list);
            continue;
        }
        out.extend(t.split(',').map(|s| s.trim().to_string()).filter(|s| !s.is_empty()));
    }
    out
}

/// `POST /external-vocabularies`.
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
    let namespace = form.first("namespaceUri");
    match &namespace {
        None => issues.push(Issue::schema_violation("body", "namespaceUri", "Field 'namespaceUri' is required.", None)),
        Some(n) if !is_absolute_uri(n) => issues.push(Issue::schema_violation(
            "body",
            "namespaceUri",
            "Field 'namespaceUri' must be an absolute URI.",
            Some(n.clone().into()),
        )),
        _ => {}
    }
    let aliases = alias_values(form.all("aliasUris"));
    for a in &aliases {
        if !is_absolute_uri(a) {
            issues.push(Issue::schema_violation(
                "body",
                "aliasUris",
                "Every alias URI must be an absolute URI.",
                Some(a.clone().into()),
            ));
        }
    }
    let source_uri = form.first("sourceUri").filter(|s| !s.trim().is_empty());
    if let Some(s) = &source_uri
        && !is_absolute_uri(s)
    {
        issues.push(Issue::schema_violation(
            "body",
            "sourceUri",
            "Field 'sourceUri' must be an absolute URI.",
            Some(s.clone().into()),
        ));
    }
    if form.file.is_none() {
        issues.push(Issue::schema_violation("body", "vocabularyFile", "Field 'vocabularyFile' is required.", None));
    }
    if !issues.is_empty() {
        return Err(ApiError::BadRequest(issues));
    }
    let description = form.first("description").filter(|s| !s.trim().is_empty());
    let file = form.file.expect("checked");
    check_turtle_file(&file)?;
    let e = state
        .service
        .create_external(
            NewExternalInput {
                name: name.expect("checked"),
                namespace_uri: namespace.expect("checked"),
                alias_uris: aliases,
                description,
                source_uri,
                file: file.bytes,
            },
            &principal.display_name(),
        )
        .await?;
    let mut resp = (StatusCode::CREATED, Json(external_json(&e))).into_response();
    if let Ok(loc) = HeaderValue::from_str(&href(&e.name)) {
        resp.headers_mut().insert(header::LOCATION, loc);
    }
    Ok(resp)
}

fn uri_list(v: &Value, key: &str, issues: &mut Vec<Issue>) -> Vec<String> {
    match v {
        Value::Array(a) => {
            let mut out = Vec::new();
            for x in a {
                match x.as_str() {
                    Some(s) if is_absolute_uri(s) => out.push(s.to_string()),
                    _ => issues.push(Issue::schema_violation(
                        "body",
                        key,
                        format!("Every value of '{key}' must be an absolute URI."),
                        Some(x.clone()),
                    )),
                }
            }
            out
        }
        _ => {
            issues.push(Issue::schema_violation("body", key, format!("'{key}' must be an array of URIs."), None));
            vec![]
        }
    }
}

fn single_uri(v: &Value, key: &str, issues: &mut Vec<Issue>) -> Option<String> {
    match v.as_str() {
        Some(s) if is_absolute_uri(s) => Some(s.to_string()),
        _ => {
            issues.push(Issue::schema_violation(
                "body",
                key,
                format!("'{key}' must be an absolute URI."),
                Some(v.clone()),
            ));
            None
        }
    }
}

/// `POST /external-vocabulary/searches`: exactly one single criterion, or one batch request.
pub async fn search(
    State(state): State<AppState>,
    headers: HeaderMap,
    body: Bytes,
) -> ApiResult<Response> {
    check_accept(&headers, JSON)?;
    require_json(&headers)?;
    let value = parse_json(&body)?;
    let Some(obj) = value.as_object() else {
        return Err(ApiError::bad_request(Issue::schema_violation("body", "body", "The body must be a JSON object.", None)));
    };
    let mut issues = Vec::new();
    let allowed = ["namespaceUri", "aliasUri", "namespaceUris", "aliasUris"];
    for k in obj.keys().filter(|k| !allowed.contains(&k.as_str())) {
        issues.push(Issue::schema_violation("body", k, format!("Property '{k}' is not allowed."), None));
    }
    let single_ns = obj.get("namespaceUri");
    let single_alias = obj.get("aliasUri");
    let batch_ns = obj.get("namespaceUris");
    let batch_alias = obj.get("aliasUris");
    let singles = single_ns.is_some() as u8 + single_alias.is_some() as u8;
    let batch = batch_ns.is_some() || batch_alias.is_some();
    if singles + batch as u8 != 1 {
        issues.push(Issue::schema_violation(
            "body",
            "body",
            "Submit exactly one of 'namespaceUri', 'aliasUri', or a batch with 'namespaceUris' and/or 'aliasUris'.",
            None,
        ));
    }
    let mut namespaces = Vec::new();
    let mut aliases = Vec::new();
    if let Some(v) = single_ns {
        namespaces.extend(single_uri(v, "namespaceUri", &mut issues));
    }
    if let Some(v) = single_alias {
        aliases.extend(single_uri(v, "aliasUri", &mut issues));
    }
    if let Some(v) = batch_ns {
        namespaces.extend(uri_list(v, "namespaceUris", &mut issues));
    }
    if let Some(v) = batch_alias {
        aliases.extend(uri_list(v, "aliasUris", &mut issues));
    }
    if batch && namespaces.is_empty() && aliases.is_empty() && issues.is_empty() {
        issues.push(Issue::schema_violation(
            "body",
            "body",
            "A batch request needs at least one namespace URI or alias URI.",
            None,
        ));
    }
    if !issues.is_empty() {
        return Err(ApiError::BadRequest(issues));
    }
    let found = state.service.search_external(&namespaces, &aliases).await?;
    let total = found.len();
    let page_size = (total as u32).max(DEFAULT_PAGE_SIZE);
    Ok(Json(json!({
        "items": found.iter().map(external_json).collect::<Vec<_>>(),
        "page": 1,
        "pageSize": page_size,
        "total": total,
        "totalPages": if total == 0 { 0 } else { 1 },
    }))
    .into_response())
}

fn turtle(filename: &str, content: String) -> Response {
    let mut resp = (StatusCode::OK, content).into_response();
    resp.headers_mut()
        .insert(header::CONTENT_TYPE, HeaderValue::from_static("text/turtle; charset=utf-8"));
    if let Ok(v) = HeaderValue::from_str(&format!("attachment; filename=\"{filename}.ttl\"")) {
        resp.headers_mut().insert(header::CONTENT_DISPOSITION, v);
    }
    resp
}

/// `POST /external-vocabularies/content`.
pub async fn content_by_namespace(
    State(state): State<AppState>,
    headers: HeaderMap,
    body: Bytes,
) -> ApiResult<Response> {
    check_accept(&headers, TURTLE)?;
    require_json(&headers)?;
    let value = parse_json(&body)?;
    let mut issues = Vec::new();
    let ns = match value.get("namespaceUri") {
        None => {
            issues.push(Issue::schema_violation("body", "namespaceUri", "Field 'namespaceUri' is required.", None));
            None
        }
        Some(v) => single_uri(v, "namespaceUri", &mut issues),
    };
    if !issues.is_empty() {
        return Err(ApiError::BadRequest(issues));
    }
    let (e, content) = state.service.external_by_namespace(&ns.expect("checked")).await?;
    Ok(turtle(&e.name, content))
}

/// `GET /external-vocabularies/{name}/content`.
pub async fn content_by_name(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(name): Path<String>,
) -> ApiResult<Response> {
    check_accept(&headers, TURTLE)?;
    path_name(&name)?;
    let content = state.service.external_content_by_name(&name).await?;
    Ok(turtle(&name, content))
}

/// `GET /external-vocabularies/{name}`.
pub async fn get_one(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(name): Path<String>,
) -> ApiResult<Response> {
    check_accept(&headers, JSON)?;
    path_name(&name)?;
    let e = state.service.get_external(&name).await?;
    Ok(Json(external_json(&e)).into_response())
}

/// `DELETE /external-vocabularies/{name}`.
pub async fn delete(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(name): Path<String>,
) -> ApiResult<Response> {
    check_accept(&headers, JSON)?;
    path_name(&name)?;
    state.service.delete_external(&name).await?;
    Ok(StatusCode::NO_CONTENT.into_response())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn alias_parsing() {
        assert_eq!(
            alias_values(vec!["https://a.org, https://b.org".into(), "[\"https://c.org\"]".into()]),
            vec!["https://a.org", "https://b.org", "https://c.org"]
        );
    }
}
