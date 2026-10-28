//! HTTP layer: routes under `/v1`, request validation, JSON views, CORS, problem mapping.

pub mod external;
pub mod support;
pub mod vocabularies;

use crate::auth::{Authenticator, require_auth};
use crate::problem::ApiError;
use crate::service::VocabularyService;
use axum::Router;
use axum::extract::{DefaultBodyLimit, Request, State};
use axum::http::{HeaderValue, Method, StatusCode, header};
use axum::middleware::{self, Next};
use axum::response::{IntoResponse, Json, Response};
use axum::routing::{get, post};
use serde_json::json;
use std::sync::Arc;
use tower_http::cors::{AllowOrigin, CorsLayer};

#[derive(Debug, Clone)]
pub struct ApiConfig {
    /// Absolute base URL of the API, used for `contentHref`, e.g. `http://localhost:8097/v1`.
    pub public_base_url: String,
    /// Maximum size of an uploaded vocabulary file in bytes (D30).
    pub max_upload_bytes: usize,
    pub cors_origins: Vec<String>,
}

#[derive(Clone)]
pub struct AppState {
    pub service: Arc<VocabularyService>,
    pub auth: Arc<Authenticator>,
    pub cfg: Arc<ApiConfig>,
}

impl AppState {
    pub fn content_href(&self, path: &str) -> String {
        format!("{}{}", self.cfg.public_base_url.trim_end_matches('/'), path)
    }
}

pub fn router(state: AppState) -> Router {
    let protected = Router::new()
        .route(
            "/vocabularies",
            get(vocabularies::list).post(vocabularies::create),
        )
        .route("/vocabularies/searches", post(vocabularies::search))
        .route(
            "/vocabularies/{vocabularyName}",
            get(vocabularies::get_one).patch(vocabularies::patch_status),
        )
        .route(
            "/vocabularies/{vocabularyName}/versions",
            get(vocabularies::list_versions).post(vocabularies::create_version),
        )
        .route(
            "/vocabularies/{vocabularyName}/versions/{version}",
            get(vocabularies::get_version).patch(vocabularies::patch_version_status),
        )
        .route("/content/{vocabularyName}", get(vocabularies::download_latest))
        .route(
            "/content/{vocabularyName}/versions/{version}",
            get(vocabularies::download_version),
        )
        .route(
            "/external-vocabularies",
            get(external::list).post(external::create),
        )
        .route("/external-vocabulary/searches", post(external::search))
        .route("/external-vocabularies/content", post(external::content_by_namespace))
        .route("/external-vocabularies/{name}/content", get(external::content_by_name))
        .route(
            "/external-vocabularies/{name}",
            get(external::get_one).delete(external::delete),
        )
        .route_layer(middleware::from_fn_with_state(state.auth.clone(), require_auth));

    let v1 = Router::new()
        .route("/health", get(health))
        .merge(protected);

    let cors = cors_layer(&state.cfg.cors_origins);
    let body_limit = state.cfg.max_upload_bytes + 1024 * 1024;

    Router::new()
        .nest("/v1", v1)
        .fallback(not_found)
        .with_state(state)
        .layer(DefaultBodyLimit::max(body_limit))
        .layer(middleware::from_fn(problem_for_bare_errors))
        .layer(cors)
}

fn cors_layer(origins: &[String]) -> CorsLayer {
    let origins: Vec<HeaderValue> = origins
        .iter()
        .filter_map(|o| HeaderValue::from_str(o.trim()).ok())
        .collect();
    CorsLayer::new()
        .allow_origin(AllowOrigin::list(origins))
        .allow_methods([
            Method::GET,
            Method::POST,
            Method::PATCH,
            Method::DELETE,
            Method::OPTIONS,
        ])
        .allow_headers([header::AUTHORIZATION, header::CONTENT_TYPE, header::ACCEPT])
        .expose_headers([header::LOCATION, header::CONTENT_DISPOSITION])
        .max_age(std::time::Duration::from_secs(3600))
}

async fn not_found() -> Response {
    ApiError::NotFound {
        detail: "The requested resource was not found.".into(),
        issues: vec![],
    }
    .into_response()
}

/// Turn error responses produced by the framework (405, 413, ...) into Problem Details.
/// JSON bodies written by handlers (for example the 503 health response) are kept.
async fn problem_for_bare_errors(req: Request, next: Next) -> Response {
    let resp = next.run(req).await;
    let status = resp.status();
    if !(status.is_client_error() || status.is_server_error()) {
        return resp;
    }
    let is_problem = resp
        .headers()
        .get(header::CONTENT_TYPE)
        .and_then(|v| v.to_str().ok())
        .is_some_and(|v| v.starts_with(crate::problem::PROBLEM_JSON) || v.starts_with("application/json"));
    if is_problem {
        return resp;
    }
    let err = match status {
        StatusCode::METHOD_NOT_ALLOWED => ApiError::MethodNotAllowed,
        StatusCode::PAYLOAD_TOO_LARGE => ApiError::PayloadTooLarge(
            "The request body is larger than the configured upload limit.".into(),
        ),
        StatusCode::UNSUPPORTED_MEDIA_TYPE => {
            ApiError::UnsupportedMediaType("Request content type is not supported.".into())
        }
        StatusCode::NOT_FOUND => ApiError::NotFound {
            detail: "The requested resource was not found.".into(),
            issues: vec![],
        },
        StatusCode::BAD_REQUEST | StatusCode::UNPROCESSABLE_ENTITY => {
            ApiError::bad_request(crate::problem::Issue::schema_violation(
                "body",
                "body",
                "The request could not be parsed.",
                None,
            ))
        }
        s if s.is_server_error() => ApiError::Internal(format!("framework error {s}")),
        _ => return resp,
    };
    let mut new = err.into_response();
    // Keep the original status (for example 405 keeps its Allow header semantics).
    *new.status_mut() = status;
    if let Some(allow) = resp.headers().get(header::ALLOW) {
        new.headers_mut().insert(header::ALLOW, allow.clone());
    }
    new
}

/// `GET /v1/health` (public). UP, DEGRADED (identity provider unreachable) or DOWN (store unreachable).
pub async fn health(State(state): State<AppState>) -> Response {
    if !state.service.store().ping().await {
        return (StatusCode::SERVICE_UNAVAILABLE, Json(json!({"status": "DOWN"}))).into_response();
    }
    if !state.auth.key_source_reachable().await {
        return (StatusCode::OK, Json(json!({"status": "DEGRADED"}))).into_response();
    }
    (StatusCode::OK, Json(json!({"status": "UP"}))).into_response()
}
