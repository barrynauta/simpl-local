//! Configuration from environment variables.

use std::env;

#[derive(Debug, Clone)]
pub struct Config {
    pub bind_addr: String,
    pub public_base_url: String,
    pub store: String,
    pub fuseki_url: String,
    pub fuseki_dataset: String,
    pub fuseki_auth: Option<(String, String)>,
    pub oidc_issuers: Vec<String>,
    pub oidc_jwks_url: String,
    pub oidc_audience: Option<String>,
    pub max_upload_bytes: usize,
    pub reference_base: String,
    pub cors_origins: Vec<String>,
}

fn var(key: &str, default: &str) -> String {
    env::var(key).ok().filter(|v| !v.trim().is_empty()).unwrap_or_else(|| default.to_string())
}

fn list(key: &str, default: &str) -> Vec<String> {
    var(key, default)
        .split(',')
        .map(|s| s.trim().to_string())
        .filter(|s| !s.is_empty())
        .collect()
}

impl Config {
    pub fn from_env() -> Self {
        let fuseki_auth = match (env::var("FUSEKI_USER"), env::var("FUSEKI_PASSWORD")) {
            (Ok(u), Ok(p)) if !u.is_empty() => Some((u, p)),
            _ => None,
        };
        Config {
            bind_addr: var("BIND_ADDR", "0.0.0.0:8080"),
            public_base_url: var("PUBLIC_BASE_URL", "http://localhost:8097/v1"),
            store: var("STORE", "fuseki"),
            fuseki_url: var("FUSEKI_URL", "http://fuseki:3030"),
            fuseki_dataset: var("FUSEKI_DATASET", "vocab"),
            fuseki_auth,
            oidc_issuers: list("OIDC_ISSUERS", "http://localhost:8184/realms/simpl"),
            oidc_jwks_url: var(
                "OIDC_JWKS_URL",
                "http://keycloak:8080/realms/simpl/protocol/openid-connect/certs",
            ),
            oidc_audience: env::var("OIDC_AUDIENCE").ok().filter(|s| !s.is_empty()),
            max_upload_bytes: var("MAX_UPLOAD_BYTES", "10485760").parse().unwrap_or(10 * 1024 * 1024),
            reference_base: var(
                "REFERENCE_BASE",
                "https://vocabulary.simpl.local/vocabularies",
            ),
            cors_origins: list("CORS_ALLOWED_ORIGINS", "http://localhost:4333,http://localhost:4323"),
        }
    }
}
