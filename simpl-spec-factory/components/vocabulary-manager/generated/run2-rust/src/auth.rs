//! Bearer token authentication and role-based access control (SIMPL-18196, D3, D4, D6).
//!
//! Tokens are JWTs signed by the identity provider. Signing keys come from its JWKS
//! endpoint. Roles are read from the top-level `roles` array claim.

use crate::problem::ApiError;
use axum::extract::{Request, State};
use axum::http::{Method, header};
use axum::middleware::Next;
use axum::response::{IntoResponse, Response};
use jsonwebtoken::jwk::JwkSet;
use jsonwebtoken::{Algorithm, DecodingKey, Validation, decode, decode_header};
use serde::Deserialize;
use std::collections::HashMap;
use std::sync::Arc;
use std::time::{Duration, Instant};
use tokio::sync::RwLock;

pub const ROLE_VIEWER: &str = "GA_VOCABULARY_VIEWER";
pub const ROLE_ADMIN: &str = "GA_VOCABULARY_ADMIN";

/// The authenticated caller.
#[derive(Debug, Clone)]
pub struct Principal {
    pub subject: String,
    pub username: Option<String>,
    pub email: Option<String>,
    pub roles: Vec<String>,
}

impl Principal {
    /// Value stored in `createdBy`: e-mail if present, else user name, else subject.
    pub fn display_name(&self) -> String {
        self.email
            .clone()
            .or_else(|| self.username.clone())
            .unwrap_or_else(|| self.subject.clone())
    }

    pub fn can_read(&self) -> bool {
        self.roles.iter().any(|r| r == ROLE_VIEWER || r == ROLE_ADMIN)
    }

    pub fn can_write(&self) -> bool {
        self.roles.iter().any(|r| r == ROLE_ADMIN)
    }
}

#[derive(Debug, Deserialize)]
struct Claims {
    sub: Option<String>,
    preferred_username: Option<String>,
    email: Option<String>,
    #[serde(default)]
    roles: Option<serde_json::Value>,
}

type KeyCache = (HashMap<String, (DecodingKey, Algorithm)>, Option<Instant>);

enum KeySource {
    Jwks {
        url: String,
        client: reqwest::Client,
        cache: RwLock<KeyCache>,
    },
    Static(Vec<(Option<String>, DecodingKey, Algorithm)>),
}

pub struct Authenticator {
    keys: KeySource,
    issuers: Vec<String>,
    audience: Option<String>,
}

const MIN_REFRESH_INTERVAL: Duration = Duration::from_secs(10);

impl Authenticator {
    pub fn with_jwks(jwks_url: String, issuers: Vec<String>, audience: Option<String>) -> Self {
        Authenticator {
            keys: KeySource::Jwks {
                url: jwks_url,
                client: reqwest::Client::builder()
                    .timeout(Duration::from_secs(5))
                    .build()
                    .expect("http client"),
                cache: RwLock::new((HashMap::new(), None)),
            },
            issuers,
            audience,
        }
    }

    /// Fixed keys, used by tests.
    pub fn with_static_keys(
        keys: Vec<(Option<String>, DecodingKey, Algorithm)>,
        issuers: Vec<String>,
    ) -> Self {
        Authenticator { keys: KeySource::Static(keys), issuers, audience: None }
    }

    /// True if the JWKS can be fetched (used by the health check).
    pub async fn key_source_reachable(&self) -> bool {
        match &self.keys {
            KeySource::Static(_) => true,
            KeySource::Jwks { url, client, .. } => client
                .get(url)
                .send()
                .await
                .map(|r| r.status().is_success())
                .unwrap_or(false),
        }
    }

    async fn refresh(&self) -> Result<(), String> {
        let KeySource::Jwks { url, client, cache } = &self.keys else {
            return Ok(());
        };
        {
            let c = cache.read().await;
            if let Some(t) = c.1
                && t.elapsed() < MIN_REFRESH_INTERVAL
            {
                return Ok(());
            }
        }
        let set: JwkSet = client
            .get(url)
            .send()
            .await
            .map_err(|e| e.to_string())?
            .json()
            .await
            .map_err(|e| e.to_string())?;
        let mut map = HashMap::new();
        for jwk in &set.keys {
            let Some(kid) = jwk.common.key_id.clone() else { continue };
            if let Ok(key) = DecodingKey::from_jwk(jwk) {
                let alg = jwk
                    .common
                    .key_algorithm
                    .and_then(|a| a.to_string().parse::<Algorithm>().ok())
                    .unwrap_or(Algorithm::RS256);
                map.insert(kid, (key, alg));
            }
        }
        let mut c = cache.write().await;
        *c = (map, Some(Instant::now()));
        Ok(())
    }

    async fn key_for(&self, kid: Option<&str>, alg: Algorithm) -> Option<DecodingKey> {
        match &self.keys {
            KeySource::Static(keys) => keys
                .iter()
                .find(|(k, _, a)| *a == alg && (k.is_none() || k.as_deref() == kid))
                .map(|(_, key, _)| key.clone()),
            KeySource::Jwks { cache, .. } => {
                let kid = kid?;
                for attempt in 0..2 {
                    {
                        let c = cache.read().await;
                        if let Some((key, a)) = c.0.get(kid)
                            && *a == alg
                        {
                            return Some(key.clone());
                        }
                    }
                    if attempt == 0 && let Err(e) = self.refresh().await {
                        tracing::warn!("cannot fetch JWKS: {e}");
                    }
                }
                None
            }
        }
    }

    /// Validate a bearer token and return the principal.
    pub async fn verify(&self, token: &str) -> Result<Principal, ApiError> {
        let invalid = |detail: &str| ApiError::Unauthorized { detail: detail.to_string(), invalid: true };
        let header = decode_header(token).map_err(|_| invalid("The access token is malformed."))?;
        if !matches!(
            header.alg,
            Algorithm::RS256 | Algorithm::RS384 | Algorithm::RS512 | Algorithm::PS256 | Algorithm::ES256
        ) {
            return Err(invalid("The access token uses an unsupported signing algorithm."));
        }
        let key = self
            .key_for(header.kid.as_deref(), header.alg)
            .await
            .ok_or_else(|| invalid("The access token is signed with an unknown key."))?;
        let mut validation = Validation::new(header.alg);
        validation.leeway = 30;
        validation.set_issuer(&self.issuers);
        validation.set_required_spec_claims(&["exp", "iss"]);
        match &self.audience {
            Some(aud) => validation.set_audience(&[aud]),
            None => validation.validate_aud = false,
        }
        let data = decode::<Claims>(token, &key, &validation).map_err(|e| {
            use jsonwebtoken::errors::ErrorKind;
            match e.kind() {
                ErrorKind::ExpiredSignature => invalid("The access token has expired."),
                ErrorKind::InvalidIssuer => invalid("The access token has an unexpected issuer."),
                ErrorKind::InvalidAudience => invalid("The access token has an unexpected audience."),
                _ => invalid("The access token is invalid."),
            }
        })?;
        let c = data.claims;
        let roles = match c.roles {
            Some(serde_json::Value::Array(a)) => a
                .into_iter()
                .filter_map(|v| v.as_str().map(String::from))
                .collect(),
            Some(serde_json::Value::String(s)) => vec![s],
            _ => vec![],
        };
        Ok(Principal {
            subject: c.sub.unwrap_or_default(),
            username: c.preferred_username,
            email: c.email,
            roles,
        })
    }
}

/// Which access an operation needs (D3).
#[derive(Debug, PartialEq, Eq, Clone, Copy)]
pub enum Access {
    Read,
    Write,
}

/// D3: VIEWER may call every GET and the two search POSTs; everything else needs ADMIN.
pub fn required_access(method: &Method, path: &str) -> Access {
    let path = path.trim_end_matches('/');
    if method == Method::GET || method == Method::HEAD {
        return Access::Read;
    }
    if method == Method::POST
        && (path.ends_with("/vocabularies/searches") || path.ends_with("/external-vocabulary/searches"))
    {
        return Access::Read;
    }
    Access::Write
}

/// Axum middleware: 401 without a valid token, 403 without the required role.
pub async fn require_auth(
    State(auth): State<Arc<Authenticator>>,
    mut req: Request,
    next: Next,
) -> Response {
    let token = req
        .headers()
        .get(header::AUTHORIZATION)
        .and_then(|v| v.to_str().ok())
        .and_then(|v| {
            let (scheme, rest) = v.split_once(' ')?;
            scheme.eq_ignore_ascii_case("bearer").then(|| rest.trim().to_string())
        })
        .filter(|t| !t.is_empty());
    let Some(token) = token else {
        return ApiError::Unauthorized {
            detail: "An access token is required to access this resource.".into(),
            invalid: false,
        }
        .into_response();
    };
    let principal = match auth.verify(&token).await {
        Ok(p) => p,
        Err(e) => return e.into_response(),
    };
    let access = required_access(req.method(), req.uri().path());
    let allowed = match access {
        Access::Read => principal.can_read(),
        Access::Write => principal.can_write(),
    };
    if !allowed {
        let needed = match access {
            Access::Read => format!("{ROLE_VIEWER} or {ROLE_ADMIN}"),
            Access::Write => ROLE_ADMIN.to_string(),
        };
        return ApiError::Forbidden(format!(
            "The caller does not have the role required for this operation ({needed})."
        ))
        .into_response();
    }
    req.extensions_mut().insert(principal);
    next.run(req).await
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn access_rules() {
        assert_eq!(required_access(&Method::GET, "/v1/vocabularies"), Access::Read);
        assert_eq!(required_access(&Method::POST, "/v1/vocabularies/searches"), Access::Read);
        assert_eq!(required_access(&Method::POST, "/v1/external-vocabulary/searches"), Access::Read);
        assert_eq!(required_access(&Method::POST, "/v1/vocabularies"), Access::Write);
        assert_eq!(required_access(&Method::PATCH, "/v1/vocabularies/Abc"), Access::Write);
        assert_eq!(required_access(&Method::DELETE, "/v1/external-vocabularies/Abc"), Access::Write);
        assert_eq!(required_access(&Method::POST, "/v1/external-vocabularies/content"), Access::Write);
    }

    #[test]
    fn role_checks() {
        let p = |roles: &[&str]| Principal {
            subject: "s".into(),
            username: None,
            email: None,
            roles: roles.iter().map(|s| s.to_string()).collect(),
        };
        assert!(p(&[ROLE_VIEWER]).can_read());
        assert!(!p(&[ROLE_VIEWER]).can_write());
        assert!(p(&[ROLE_ADMIN]).can_read() && p(&[ROLE_ADMIN]).can_write());
        assert!(!p(&["offline_access"]).can_read());
        assert_eq!(p(&[]).display_name(), "s");
    }
}
