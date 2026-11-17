//! Request parsing helpers shared by the handlers: query strings, content negotiation,
//! multipart bodies, pagination links.

use crate::problem::{ApiError, ApiResult, Issue};
use axum::extract::{FromRequest, Multipart, Request};
use axum::http::{HeaderMap, header};
use chrono::{DateTime, NaiveDate, NaiveDateTime, SecondsFormat, Utc};
use std::collections::{BTreeMap, HashMap};

pub const MAX_PAGE_SIZE: u32 = 100;
pub const DEFAULT_PAGE_SIZE: u32 = 20;

pub fn fmt_date(d: &DateTime<Utc>) -> String {
    d.to_rfc3339_opts(SecondsFormat::Secs, true)
}

/// 406 unless the `Accept` header allows one of `offered`.
pub fn check_accept(headers: &HeaderMap, offered: &[&str]) -> ApiResult<()> {
    let Some(accept) = headers.get(header::ACCEPT).and_then(|v| v.to_str().ok()) else {
        return Ok(());
    };
    if accept.trim().is_empty() {
        return Ok(());
    }
    for range in accept.split(',') {
        let mut parts = range.split(';');
        let media = parts.next().unwrap_or("").trim().to_ascii_lowercase();
        let q_zero = parts.any(|p| {
            let p = p.trim();
            p.strip_prefix("q=").is_some_and(|q| q.trim().parse::<f32>().map(|v| v == 0.0).unwrap_or(false))
        });
        if q_zero {
            continue;
        }
        if media == "*/*" {
            return Ok(());
        }
        for o in offered {
            let (otype, _) = o.split_once('/').unwrap_or((o, ""));
            if media == *o || media == format!("{otype}/*") {
                return Ok(());
            }
        }
    }
    Err(ApiError::NotAcceptable)
}

fn media_type(headers: &HeaderMap) -> Option<String> {
    headers
        .get(header::CONTENT_TYPE)
        .and_then(|v| v.to_str().ok())
        .map(|v| v.split(';').next().unwrap_or("").trim().to_ascii_lowercase())
}

/// 415 unless the body is JSON (`application/json` or any `+json` type).
pub fn require_json(headers: &HeaderMap) -> ApiResult<()> {
    match media_type(headers) {
        Some(m) if m == "application/json" || (m.starts_with("application/") && m.ends_with("+json")) => Ok(()),
        _ => Err(ApiError::UnsupportedMediaType(
            "Request content type is not supported. Use application/json.".into(),
        )),
    }
}

/// Parse a JSON request body into a generic value (400 if malformed).
pub fn parse_json(body: &[u8]) -> ApiResult<serde_json::Value> {
    serde_json::from_slice(body).map_err(|e| {
        ApiError::bad_request(Issue::schema_violation(
            "body",
            "body",
            format!("The request body is not valid JSON: {e}"),
            None,
        ))
    })
}

/// Raw query string into a map. Unknown parameter names give 400 (SIMPL-18164).
pub fn parse_query(raw: Option<&str>, allowed: &[&str]) -> ApiResult<HashMap<String, String>> {
    let pairs: Vec<(String, String)> = serde_urlencoded::from_str(raw.unwrap_or("")).map_err(|_| {
        ApiError::bad_request(Issue::schema_violation("query", "query", "The query string is malformed.", None))
    })?;
    let mut map = HashMap::new();
    let mut issues = Vec::new();
    for (k, v) in pairs {
        if !allowed.contains(&k.as_str()) {
            issues.push(Issue::schema_violation(
                "query",
                &k,
                format!("Query parameter '{k}' is not supported. Supported parameters: [{}].", allowed.join(", ")),
                Some(serde_json::Value::String(v)),
            ));
            continue;
        }
        map.insert(k, v);
    }
    if issues.is_empty() { Ok(map) } else { Err(ApiError::BadRequest(issues)) }
}

/// `page` (min 1) and `pageSize` (1 to 100, D29).
pub fn parse_paging(q: &HashMap<String, String>, issues: &mut Vec<Issue>) -> (u32, u32) {
    let mut get = |key: &str, default: u32, max: Option<u32>| -> u32 {
        match q.get(key) {
            None => default,
            Some(raw) => match raw.trim().parse::<i64>() {
                Ok(n) if n >= 1 && max.is_none_or(|m| n <= m as i64) => n as u32,
                Ok(_) => {
                    let detail = match max {
                        Some(m) => format!("Query parameter '{key}' must be between 1 and {m}."),
                        None => format!("Query parameter '{key}' must be at least 1."),
                    };
                    issues.push(Issue::schema_violation("query", key, detail, Some(raw.clone().into())));
                    default
                }
                Err(_) => {
                    issues.push(Issue::schema_violation(
                        "query",
                        key,
                        format!("Query parameter '{key}' must be an integer."),
                        Some(raw.clone().into()),
                    ));
                    default
                }
            },
        }
    };
    let page = get("page", 1, None);
    let size = get("pageSize", DEFAULT_PAGE_SIZE, Some(MAX_PAGE_SIZE));
    (page, size)
}

/// Date filter: `YYYY-MM-DD` or an ISO 8601 date-time; compared as a UTC calendar day.
pub fn parse_date_filter(raw: &str) -> Option<NaiveDate> {
    let raw = raw.trim();
    let b = raw.as_bytes();
    let shape_ok = b.len() >= 10
        && b[..4].iter().all(u8::is_ascii_digit)
        && b[4] == b'-'
        && b[5..7].iter().all(u8::is_ascii_digit)
        && b[7] == b'-'
        && b[8..10].iter().all(u8::is_ascii_digit)
        && (b.len() == 10 || b[10] == b'T');
    if !shape_ok {
        return None;
    }
    if b.len() == 10 {
        return NaiveDate::parse_from_str(raw, "%Y-%m-%d").ok();
    }
    if let Ok(dt) = DateTime::parse_from_rfc3339(raw) {
        return Some(dt.with_timezone(&Utc).date_naive());
    }
    for fmt in ["%Y-%m-%dT%H:%M:%S%.f", "%Y-%m-%dT%H:%M:%S", "%Y-%m-%dT%H:%M"] {
        if let Ok(dt) = NaiveDateTime::parse_from_str(raw, fmt) {
            return Some(dt.date());
        }
    }
    None
}

pub fn validate_name_param(name: &str, in_: &str, field: &str) -> ApiResult<()> {
    if crate::validation::is_valid_name(name) {
        Ok(())
    } else {
        Err(ApiError::bad_request(name_issue(name, in_, field)))
    }
}

pub fn name_issue(name: &str, in_: &str, field: &str) -> Issue {
    Issue::schema_violation(
        in_,
        field,
        format!("'{field}' must match ^[A-Z][a-zA-Z0-9]*$ and be 3 to 64 characters long."),
        Some(name.to_string().into()),
    )
}

pub fn parse_version_param(raw: &str) -> ApiResult<u32> {
    match raw.parse::<i64>() {
        Ok(n) if n >= 1 && n <= u32::MAX as i64 => Ok(n as u32),
        _ => Err(ApiError::bad_request(Issue::schema_violation(
            "path",
            "version",
            "Path parameter 'version' must be an integer of at least 1.",
            Some(raw.to_string().into()),
        ))),
    }
}

/// An uploaded file part.
pub struct UploadedFile {
    pub filename: Option<String>,
    pub content_type: Option<String>,
    pub bytes: Vec<u8>,
}

/// Parsed multipart body: text fields (all values per name) and the `vocabularyFile` part.
pub struct Form {
    pub fields: BTreeMap<String, Vec<String>>,
    pub file: Option<UploadedFile>,
}

impl Form {
    pub fn first(&self, name: &str) -> Option<String> {
        self.fields.get(name).and_then(|v| v.first().cloned())
    }
    pub fn all(&self, name: &str) -> Vec<String> {
        self.fields.get(name).cloned().unwrap_or_default()
    }
    pub fn has(&self, name: &str) -> bool {
        self.fields.contains_key(name)
    }
}

fn too_large(max: usize) -> ApiError {
    let mb = max as f64 / (1024.0 * 1024.0);
    ApiError::PayloadTooLarge(format!(
        "Uploaded vocabulary file is too large. The maximum allowed upload size is {mb:.0} MB. Please upload a smaller Turtle file."
    ))
}

/// Read a `multipart/form-data` body (415 if it is not one, 413 above `max_file`, D30).
pub async fn read_form(req: Request, max_file: usize) -> ApiResult<Form> {
    match media_type(req.headers()) {
        Some(m) if m == "multipart/form-data" => {}
        _ => {
            return Err(ApiError::UnsupportedMediaType(
                "Request content type is not supported. Use multipart/form-data.".into(),
            ));
        }
    }
    let mut multipart = Multipart::from_request(req, &()).await.map_err(|e| {
        ApiError::bad_request(Issue::schema_violation("body", "body", format!("Malformed multipart body: {e}"), None))
    })?;
    let map_err = |e: axum::extract::multipart::MultipartError| {
        if e.status() == axum::http::StatusCode::PAYLOAD_TOO_LARGE {
            too_large(max_file)
        } else {
            ApiError::bad_request(Issue::schema_violation("body", "body", format!("Malformed multipart body: {e}"), None))
        }
    };
    let mut form = Form { fields: BTreeMap::new(), file: None };
    while let Some(mut field) = multipart.next_field().await.map_err(map_err)? {
        let name = field.name().unwrap_or("").to_string();
        if name == "vocabularyFile" {
            let filename = field.file_name().map(String::from);
            let content_type = field.content_type().map(String::from);
            let mut bytes = Vec::new();
            while let Some(chunk) = field.chunk().await.map_err(map_err)? {
                bytes.extend_from_slice(&chunk);
                if bytes.len() > max_file {
                    return Err(too_large(max_file));
                }
            }
            form.file = Some(UploadedFile { filename, content_type, bytes });
        } else {
            let mut bytes = Vec::new();
            while let Some(chunk) = field.chunk().await.map_err(map_err)? {
                bytes.extend_from_slice(&chunk);
                if bytes.len() > max_file {
                    return Err(too_large(max_file));
                }
            }
            form.fields
                .entry(name)
                .or_default()
                .push(String::from_utf8_lossy(&bytes).into_owned());
        }
    }
    Ok(form)
}

/// D15: only Turtle files are accepted; anything else is 415.
pub fn check_turtle_file(file: &UploadedFile) -> ApiResult<()> {
    let ct = file
        .content_type
        .as_deref()
        .map(|c| c.split(';').next().unwrap_or("").trim().to_ascii_lowercase())
        .unwrap_or_default();
    let ext = file
        .filename
        .as_deref()
        .and_then(|f| f.rsplit_once('.').map(|(_, e)| e.to_ascii_lowercase()));
    let turtle_types = ["text/turtle", "application/x-turtle", "application/turtle"];
    let generic_types = ["", "application/octet-stream", "text/plain"];
    let ok = if turtle_types.contains(&ct.as_str()) {
        true
    } else if generic_types.contains(&ct.as_str()) {
        ext.as_deref().is_none_or(|e| e == "ttl")
    } else {
        false
    };
    if ok {
        Ok(())
    } else {
        Err(ApiError::UnsupportedMediaType(format!(
            "The vocabulary file must be in Turtle format (text/turtle, .ttl). Received content type '{}'{}.",
            if ct.is_empty() { "none" } else { &ct },
            file.filename.as_deref().map(|f| format!(" and file name '{f}'")).unwrap_or_default()
        )))
    }
}

/// Pagination envelope fields (OpenAPI `CollectionPage`).
pub struct PageLinks {
    pub page: u32,
    pub page_size: u32,
    pub total: usize,
    pub total_pages: u32,
    pub self_: String,
    pub first: String,
    pub prev: Option<String>,
    pub next: Option<String>,
    pub last: Option<String>,
}

pub fn page_links(path: &str, extra: &[(String, String)], page: u32, page_size: u32, total: usize) -> PageLinks {
    let total_pages = total.div_ceil(page_size as usize) as u32;
    let link = |p: u32| {
        let mut pairs = vec![("page".to_string(), p.to_string()), ("pageSize".to_string(), page_size.to_string())];
        pairs.extend(extra.iter().cloned());
        format!("{path}?{}", serde_urlencoded::to_string(&pairs).unwrap_or_default())
    };
    PageLinks {
        page,
        page_size,
        total,
        total_pages,
        self_: link(page),
        first: link(1),
        prev: (page > 1 && total_pages > 0).then(|| link((page - 1).min(total_pages))),
        next: (page < total_pages).then(|| link(page + 1)),
        last: (total_pages > 0).then(|| link(total_pages)),
    }
}

impl PageLinks {
    pub fn into_json(self, items: Vec<serde_json::Value>) -> serde_json::Value {
        let mut m = serde_json::Map::new();
        m.insert("page".into(), self.page.into());
        m.insert("totalPages".into(), self.total_pages.into());
        m.insert("total".into(), (self.total as u64).into());
        m.insert("pageSize".into(), self.page_size.into());
        m.insert("self".into(), self.self_.into());
        m.insert("first".into(), self.first.into());
        if let Some(p) = self.prev {
            m.insert("prev".into(), p.into());
        }
        if let Some(n) = self.next {
            m.insert("next".into(), n.into());
        }
        if let Some(l) = self.last {
            m.insert("last".into(), l.into());
        }
        m.insert("items".into(), items.into());
        serde_json::Value::Object(m)
    }
}

pub fn paginate<T>(items: Vec<T>, page: u32, page_size: u32) -> Vec<T> {
    let start = (page as usize - 1).saturating_mul(page_size as usize);
    items.into_iter().skip(start).take(page_size as usize).collect()
}

#[cfg(test)]
mod tests {
    use super::*;
    use axum::http::HeaderValue;

    #[test]
    fn accept_negotiation() {
        let mut h = HeaderMap::new();
        assert!(check_accept(&h, &["application/json"]).is_ok());
        h.insert(header::ACCEPT, HeaderValue::from_static("application/xml"));
        assert_eq!(check_accept(&h, &["application/json"]), Err(ApiError::NotAcceptable));
        h.insert(header::ACCEPT, HeaderValue::from_static("text/html, application/*;q=0.8"));
        assert!(check_accept(&h, &["application/json"]).is_ok());
        h.insert(header::ACCEPT, HeaderValue::from_static("text/turtle"));
        assert!(check_accept(&h, &["text/turtle"]).is_ok());
    }

    #[test]
    fn paging_limits() {
        let mut issues = vec![];
        let q: HashMap<String, String> = [("page".into(), "0".into()), ("pageSize".into(), "101".into())].into();
        parse_paging(&q, &mut issues);
        assert_eq!(issues.len(), 2);
        let mut issues = vec![];
        let q: HashMap<String, String> = [("page".into(), "2".into()), ("pageSize".into(), "100".into())].into();
        assert_eq!(parse_paging(&q, &mut issues), (2, 100));
        assert!(issues.is_empty());
        let mut issues = vec![];
        let q: HashMap<String, String> = [("page".into(), "abc".into())].into();
        parse_paging(&q, &mut issues);
        assert_eq!(issues.len(), 1);
    }

    #[test]
    fn date_filters() {
        assert_eq!(parse_date_filter("2026-03-01"), NaiveDate::from_ymd_opt(2026, 3, 1));
        assert_eq!(parse_date_filter("2026-03-01T23:30:00-02:00"), NaiveDate::from_ymd_opt(2026, 3, 2));
        assert_eq!(parse_date_filter("2026-03-01T10:00:00"), NaiveDate::from_ymd_opt(2026, 3, 1));
        assert_eq!(parse_date_filter("01-03-2026"), None);
        assert_eq!(parse_date_filter("2026-13-01"), None);
    }

    #[test]
    fn links() {
        let l = page_links("/vocabularies", &[("sort".into(), "name".into())], 1, 20, 21);
        assert_eq!(l.total_pages, 2);
        assert_eq!(l.self_, "/vocabularies?page=1&pageSize=20&sort=name");
        assert!(l.prev.is_none());
        assert_eq!(l.next.as_deref(), Some("/vocabularies?page=2&pageSize=20&sort=name"));
        let empty = page_links("/vocabularies", &[], 1, 20, 0);
        assert_eq!(empty.total_pages, 0);
        assert!(empty.last.is_none() && empty.next.is_none());
    }

    #[test]
    fn turtle_file_detection() {
        let f = |name: Option<&str>, ct: Option<&str>| UploadedFile {
            filename: name.map(String::from),
            content_type: ct.map(String::from),
            bytes: vec![],
        };
        assert!(check_turtle_file(&f(Some("a.ttl"), Some("text/turtle"))).is_ok());
        assert!(check_turtle_file(&f(Some("a.ttl"), Some("application/octet-stream"))).is_ok());
        assert!(check_turtle_file(&f(Some("a.ttl"), None)).is_ok());
        assert!(check_turtle_file(&f(Some("a.rdf"), Some("application/rdf+xml"))).is_err());
        assert!(check_turtle_file(&f(Some("a.json"), Some("application/octet-stream"))).is_err());
        assert!(check_turtle_file(&f(Some("a.ttl"), Some("application/ld+json"))).is_err());
    }
}
