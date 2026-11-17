//! RFC 9457 Problem Details, shaped after the OpenAPI `Problem` schema (D5).

use axum::http::{HeaderValue, StatusCode, header};
use axum::response::{IntoResponse, Response};
use serde::Serialize;

pub const PROBLEM_JSON: &str = "application/problem+json";

#[derive(Debug, Clone, Serialize, PartialEq)]
pub struct Issue {
    #[serde(rename = "type", skip_serializing_if = "Option::is_none")]
    pub type_: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub href: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    pub detail: String,
    #[serde(rename = "in", skip_serializing_if = "Option::is_none")]
    pub in_: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub value: Option<serde_json::Value>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub location: Option<String>,
}

impl Issue {
    /// A Belgif `schemaViolation` issue for a request parameter or field.
    pub fn schema_violation(
        in_: &str,
        name: &str,
        detail: impl Into<String>,
        value: Option<serde_json::Value>,
    ) -> Self {
        Issue {
            type_: Some("urn:problem-type:belgif:input-validation:schemaViolation".into()),
            href: Some(
                "https://www.belgif.be/specification/rest/api-guide/issues/schemaViolation.html"
                    .into(),
            ),
            title: Some("Input isn't valid with respect to schema".into()),
            detail: detail.into(),
            in_: Some(in_.into()),
            name: Some(name.into()),
            value,
            location: None,
        }
    }

    /// A Belgif `invalidInput` issue, used for vocabulary content problems.
    pub fn invalid_input(
        in_: &str,
        name: &str,
        detail: impl Into<String>,
        location: Option<String>,
    ) -> Self {
        Issue {
            type_: Some("urn:problem-type:belgif:input-validation:invalidInput".into()),
            href: Some(
                "https://www.belgif.be/specification/rest/api-guide/issues/invalidInput.html"
                    .into(),
            ),
            title: Some("Invalid input".into()),
            detail: detail.into(),
            in_: Some(in_.into()),
            name: Some(name.into()),
            value: None,
            location,
        }
    }

    /// A content validation issue on the uploaded `vocabularyFile` part.
    pub fn vocabulary_file(detail: impl Into<String>, location: impl Into<String>) -> Self {
        Self::invalid_input("body", "vocabularyFile", detail, Some(location.into()))
    }
}

#[derive(Debug, Clone, Serialize, PartialEq)]
pub struct Problem {
    #[serde(rename = "type")]
    pub type_: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub href: Option<String>,
    pub title: String,
    pub status: u16,
    pub detail: String,
    pub instance: String,
    #[serde(skip_serializing_if = "Vec::is_empty")]
    pub issues: Vec<Issue>,
}

/// All error outcomes of the API. Each maps to one HTTP status and one Problem type.
#[derive(Debug, Clone, PartialEq)]
pub enum ApiError {
    BadRequest(Vec<Issue>),
    Unauthorized { detail: String, invalid: bool },
    Forbidden(String),
    NotFound { detail: String, issues: Vec<Issue> },
    MethodNotAllowed,
    NotAcceptable,
    Conflict(String),
    PayloadTooLarge(String),
    UnsupportedMediaType(String),
    Internal(String),
}

impl ApiError {
    pub fn status(&self) -> StatusCode {
        match self {
            ApiError::BadRequest(_) => StatusCode::BAD_REQUEST,
            ApiError::Unauthorized { .. } => StatusCode::UNAUTHORIZED,
            ApiError::Forbidden(_) => StatusCode::FORBIDDEN,
            ApiError::NotFound { .. } => StatusCode::NOT_FOUND,
            ApiError::MethodNotAllowed => StatusCode::METHOD_NOT_ALLOWED,
            ApiError::NotAcceptable => StatusCode::NOT_ACCEPTABLE,
            ApiError::Conflict(_) => StatusCode::CONFLICT,
            ApiError::PayloadTooLarge(_) => StatusCode::PAYLOAD_TOO_LARGE,
            ApiError::UnsupportedMediaType(_) => StatusCode::UNSUPPORTED_MEDIA_TYPE,
            ApiError::Internal(_) => StatusCode::INTERNAL_SERVER_ERROR,
        }
    }

    pub fn bad_request(issue: Issue) -> Self {
        ApiError::BadRequest(vec![issue])
    }

    pub fn vocabulary_not_found(name: &str) -> Self {
        let detail = format!("Vocabulary with name '{name}' was not found.");
        ApiError::NotFound {
            detail: detail.clone(),
            issues: vec![Issue {
                type_: None,
                href: None,
                title: None,
                detail,
                in_: Some("path".into()),
                name: Some("vocabularyName".into()),
                value: None,
                location: None,
            }],
        }
    }

    pub fn version_not_found(name: &str, version: u32) -> Self {
        let detail = format!("Version {version} of vocabulary '{name}' was not found.");
        ApiError::NotFound {
            detail: detail.clone(),
            issues: vec![Issue {
                type_: None,
                href: None,
                title: None,
                detail,
                in_: Some("path".into()),
                name: Some("version".into()),
                value: None,
                location: None,
            }],
        }
    }

    pub fn external_not_found(detail: impl Into<String>) -> Self {
        ApiError::NotFound { detail: detail.into(), issues: vec![] }
    }

    pub fn to_problem(&self) -> Problem {
        let instance = format!("urn:uuid:{}", uuid::Uuid::new_v4());
        let status = self.status().as_u16();
        let (type_, href, title, detail, issues): (&str, Option<&str>, &str, String, Vec<Issue>) =
            match self {
                ApiError::BadRequest(issues) => (
                    "urn:problem-type:belgif:badRequest",
                    Some("https://www.belgif.be/specification/rest/api-guide/problems/badRequest.html"),
                    "Bad Request",
                    "The input message is incorrect".into(),
                    issues.clone(),
                ),
                ApiError::Unauthorized { detail, invalid } => {
                    if *invalid {
                        (
                            "urn:problem-type:belgif:invalidAccessToken",
                            Some("https://www.belgif.be/specification/rest/api-guide/problems/invalidAccessToken.html"),
                            "Invalid Access Token",
                            detail.clone(),
                            vec![],
                        )
                    } else {
                        (
                            "urn:problem-type:belgif:noAccessToken",
                            Some("https://www.belgif.be/specification/rest/api-guide/problems/noAccessToken.html"),
                            "No Access Token",
                            detail.clone(),
                            vec![],
                        )
                    }
                }
                ApiError::Forbidden(detail) => (
                    "urn:problem-type:belgif:missingPermission",
                    Some("https://www.belgif.be/specification/rest/api-guide/problems/missingPermission.html"),
                    "Missing Permission",
                    detail.clone(),
                    vec![],
                ),
                ApiError::NotFound { detail, issues } => (
                    "urn:problem-type:belgif:resourceNotFound",
                    Some("https://www.belgif.be/specification/rest/api-guide/problems/resourceNotFound.html"),
                    "Resource Not Found",
                    detail.clone(),
                    issues.clone(),
                ),
                ApiError::MethodNotAllowed => (
                    "urn:problem-type:simpl:methodNotAllowed",
                    None,
                    "Method Not Allowed",
                    "The HTTP method is not supported for this resource.".into(),
                    vec![],
                ),
                ApiError::NotAcceptable => (
                    "urn:problem-type:simpl:notAcceptable",
                    None,
                    "Not Acceptable",
                    "Requested media type cannot be served.".into(),
                    vec![],
                ),
                ApiError::Conflict(detail) => (
                    "urn:problem-type:simpl:conflict",
                    None,
                    "Conflict",
                    detail.clone(),
                    vec![],
                ),
                ApiError::PayloadTooLarge(detail) => (
                    "urn:problem-type:simpl:payloadTooLarge",
                    None,
                    "Payload Too Large",
                    detail.clone(),
                    vec![Issue::invalid_input("body", "vocabularyFile", detail.clone(), None)],
                ),
                ApiError::UnsupportedMediaType(detail) => (
                    "urn:problem-type:simpl:unsupportedMediaType",
                    None,
                    "Unsupported Media Type",
                    detail.clone(),
                    vec![],
                ),
                ApiError::Internal(_) => (
                    "urn:problem-type:belgif:internalServerError",
                    Some("https://www.belgif.be/specification/rest/api-guide/problems/internalServerError.html"),
                    "Internal Server Error",
                    "Internal Server Error".into(),
                    vec![],
                ),
            };
        Problem {
            type_: type_.into(),
            href: href.map(Into::into),
            title: title.into(),
            status,
            detail,
            instance,
            issues,
        }
    }
}

impl IntoResponse for ApiError {
    fn into_response(self) -> Response {
        if let ApiError::Internal(msg) = &self {
            tracing::error!("internal error: {msg}");
        }
        let status = self.status();
        let body = serde_json::to_vec(&self.to_problem()).unwrap_or_default();
        let mut resp = (status, body).into_response();
        resp.headers_mut()
            .insert(header::CONTENT_TYPE, HeaderValue::from_static(PROBLEM_JSON));
        if let ApiError::Unauthorized { invalid, .. } = &self {
            let value = if *invalid {
                "Bearer error=\"invalid_token\""
            } else {
                "Bearer"
            };
            resp.headers_mut()
                .insert(header::WWW_AUTHENTICATE, HeaderValue::from_static(value));
        }
        resp
    }
}

pub type ApiResult<T> = Result<T, ApiError>;

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn bad_request_problem_shape() {
        let p = ApiError::bad_request(Issue::schema_violation("query", "sort", "bad", None)).to_problem();
        assert_eq!(p.status, 400);
        assert_eq!(p.type_, "urn:problem-type:belgif:badRequest");
        assert!(p.instance.starts_with("urn:uuid:"));
        let json = serde_json::to_value(&p).unwrap();
        assert_eq!(json["issues"][0]["in"], "query");
        assert_eq!(json["issues"][0]["name"], "sort");
    }

    #[test]
    fn not_found_has_path_issue() {
        let p = ApiError::vocabulary_not_found("Abc").to_problem();
        assert_eq!(p.status, 404);
        assert_eq!(p.issues[0].in_.as_deref(), Some("path"));
    }
}
