use std::sync::Arc;
use vocabulary_manager::api::{self, ApiConfig, AppState};
use vocabulary_manager::auth::Authenticator;
use vocabulary_manager::config::Config;
use vocabulary_manager::service::{ServiceConfig, VocabularyService};
use vocabulary_manager::store::Store;
use vocabulary_manager::store::fuseki::FusekiStore;
use vocabulary_manager::store::memory::MemoryStore;

#[tokio::main]
async fn main() {
    let cfg = Config::from_env();

    // `vocabulary-manager healthcheck`: used by the container health check (no curl in the image).
    if std::env::args().nth(1).as_deref() == Some("healthcheck") {
        let port = cfg.bind_addr.rsplit(':').next().unwrap_or("8080");
        let url = format!("http://127.0.0.1:{port}/v1/health");
        let ok = reqwest::get(&url).await.map(|r| r.status().is_success()).unwrap_or(false);
        std::process::exit(if ok { 0 } else { 1 });
    }

    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| "info,tower_http=info".into()),
        )
        .init();

    let store: Arc<dyn Store> = match cfg.store.as_str() {
        "memory" => Arc::new(MemoryStore::new()),
        _ => Arc::new(FusekiStore::new(&cfg.fuseki_url, &cfg.fuseki_dataset, cfg.fuseki_auth.clone())),
    };
    let service = Arc::new(VocabularyService::new(
        store,
        ServiceConfig { reference_base: cfg.reference_base.clone() },
    ));
    let auth = Arc::new(Authenticator::with_jwks(
        cfg.oidc_jwks_url.clone(),
        cfg.oidc_issuers.clone(),
        cfg.oidc_audience.clone(),
    ));
    let state = AppState {
        service,
        auth,
        cfg: Arc::new(ApiConfig {
            public_base_url: cfg.public_base_url.clone(),
            max_upload_bytes: cfg.max_upload_bytes,
            cors_origins: cfg.cors_origins.clone(),
        }),
    };
    let app = api::router(state).layer(tower_http::trace::TraceLayer::new_for_http());
    let listener = tokio::net::TcpListener::bind(&cfg.bind_addr).await.expect("bind");
    tracing::info!(
        "vocabulary manager listening on {} (store={}, issuers={:?})",
        cfg.bind_addr,
        cfg.store,
        cfg.oidc_issuers
    );
    axum::serve(listener, app)
        .with_graceful_shutdown(async {
            let _ = tokio::signal::ctrl_c().await;
        })
        .await
        .expect("server");
}
