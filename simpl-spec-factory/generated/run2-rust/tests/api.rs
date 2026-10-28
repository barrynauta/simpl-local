//! Integration tests: the full HTTP router with an in-memory store and JWTs signed by a test key.

use axum::Router;
use axum::body::Body;
use axum::http::{Request, StatusCode, header};
use http_body_util::BodyExt;
use jsonwebtoken::{Algorithm, DecodingKey, EncodingKey, Header, encode};
use serde_json::{Value, json};
use std::sync::Arc;
use std::sync::atomic::Ordering;
use tower::ServiceExt;
use vocabulary_manager::api::{self, ApiConfig, AppState};
use vocabulary_manager::auth::Authenticator;
use vocabulary_manager::service::{ServiceConfig, VocabularyService};
use vocabulary_manager::store::memory::MemoryStore;

const ISSUER: &str = "http://localhost:8184/realms/simpl";
const BASE: &str = "https://vocabulary.simpl.local/vocabularies";
const KEY: &str = include_str!("fixtures/test-key.pem");
const PUB: &str = include_str!("fixtures/test-pub.pem");
const OTHER_KEY: &str = include_str!("fixtures/other-key.pem");

struct App {
    router: Router,
    store: Arc<MemoryStore>,
}

fn app_with_limit(max_upload: usize) -> App {
    let store = Arc::new(MemoryStore::new());
    let service = Arc::new(VocabularyService::new(
        store.clone(),
        ServiceConfig { reference_base: BASE.into() },
    ));
    let auth = Arc::new(Authenticator::with_static_keys(
        vec![(Some("k1".into()), DecodingKey::from_rsa_pem(PUB.as_bytes()).unwrap(), Algorithm::RS256)],
        vec![ISSUER.into()],
    ));
    let state = AppState {
        service,
        auth,
        cfg: Arc::new(ApiConfig {
            public_base_url: "http://localhost:8097/v1".into(),
            max_upload_bytes: max_upload,
            cors_origins: vec!["http://localhost:4333".into(), "http://localhost:4323".into()],
        }),
    };
    App { router: api::router(state), store }
}

fn app() -> App {
    app_with_limit(10 * 1024 * 1024)
}

fn token_with(roles: &[&str], key: &str, iss: &str, exp_offset: i64) -> String {
    let now = chrono::Utc::now().timestamp();
    let claims = json!({
        "sub": "user-1",
        "preferred_username": "ga-admin",
        "email": "ga-admin@example.com",
        "iss": iss,
        "exp": now + exp_offset,
        "iat": now,
        "roles": roles,
    });
    let mut h = Header::new(Algorithm::RS256);
    h.kid = Some("k1".into());
    encode(&h, &claims, &EncodingKey::from_rsa_pem(key.as_bytes()).unwrap()).unwrap()
}

fn admin() -> String {
    token_with(&["GA_VOCABULARY_ADMIN"], KEY, ISSUER, 300)
}
fn viewer() -> String {
    token_with(&["GA_VOCABULARY_VIEWER"], KEY, ISSUER, 300)
}
fn norole() -> String {
    token_with(&["offline_access"], KEY, ISSUER, 300)
}

struct Resp {
    status: StatusCode,
    headers: axum::http::HeaderMap,
    body: Vec<u8>,
}

impl Resp {
    fn json(&self) -> Value {
        serde_json::from_slice(&self.body).unwrap_or(Value::Null)
    }
    fn text(&self) -> String {
        String::from_utf8_lossy(&self.body).into_owned()
    }
    fn content_type(&self) -> String {
        self.headers
            .get(header::CONTENT_TYPE)
            .map(|v| v.to_str().unwrap().to_string())
            .unwrap_or_default()
    }
}

async fn send(app: &App, req: Request<Body>) -> Resp {
    let resp = app.router.clone().oneshot(req).await.unwrap();
    let status = resp.status();
    let headers = resp.headers().clone();
    let body = resp.into_body().collect().await.unwrap().to_bytes().to_vec();
    Resp { status, headers, body }
}

async fn get(app: &App, uri: &str, token: Option<&str>) -> Resp {
    let mut b = Request::get(uri);
    if let Some(t) = token {
        b = b.header(header::AUTHORIZATION, format!("Bearer {t}"));
    }
    send(app, b.body(Body::empty()).unwrap()).await
}

async fn json_req(app: &App, method: &str, uri: &str, token: &str, body: Value) -> Resp {
    let req = Request::builder()
        .method(method)
        .uri(uri)
        .header(header::AUTHORIZATION, format!("Bearer {token}"))
        .header(header::CONTENT_TYPE, "application/json")
        .body(Body::from(body.to_string()))
        .unwrap();
    send(app, req).await
}

type Part<'a> = (&'a str, Option<&'a str>, Option<&'a str>, &'a [u8]);

/// Build a multipart body. Each part: (name, filename, content type, value).
fn multipart(parts: &[Part<'_>]) -> (String, Vec<u8>) {
    let boundary = "XBOUNDARYX";
    let mut body = Vec::new();
    for (name, filename, ct, value) in parts {
        body.extend_from_slice(format!("--{boundary}\r\n").as_bytes());
        match filename {
            Some(f) => body.extend_from_slice(
                format!("Content-Disposition: form-data; name=\"{name}\"; filename=\"{f}\"\r\n").as_bytes(),
            ),
            None => body.extend_from_slice(format!("Content-Disposition: form-data; name=\"{name}\"\r\n").as_bytes()),
        }
        if let Some(c) = ct {
            body.extend_from_slice(format!("Content-Type: {c}\r\n").as_bytes());
        }
        body.extend_from_slice(b"\r\n");
        body.extend_from_slice(value);
        body.extend_from_slice(b"\r\n");
    }
    body.extend_from_slice(format!("--{boundary}--\r\n").as_bytes());
    (format!("multipart/form-data; boundary={boundary}"), body)
}

async fn post_form(app: &App, uri: &str, token: &str, parts: &[Part<'_>]) -> Resp {
    let (ct, body) = multipart(parts);
    let req = Request::post(uri)
        .header(header::AUTHORIZATION, format!("Bearer {token}"))
        .header(header::CONTENT_TYPE, ct)
        .body(Body::from(body))
        .unwrap();
    send(app, req).await
}

fn ontology(extra: &str) -> String {
    format!(
        "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n@prefix ex: <https://example.eu/test#> .\nex:Onto a owl:Ontology .\nex:Patient a owl:Class ; rdfs:label \"Patient\" .\n{extra}\n"
    )
}

async fn create(app: &App, name: &str, ttl: &str) -> Resp {
    post_form(
        app,
        "/v1/vocabularies",
        &admin(),
        &[
            ("name", None, None, name.as_bytes()),
            ("description", None, None, b"A test vocabulary"),
            ("changelog", None, None, b"Initial version"),
            ("vocabularyFile", Some("v.ttl"), Some("text/turtle"), ttl.as_bytes()),
        ],
    )
    .await
}

async fn new_version(app: &App, name: &str, ttl: &str) -> Resp {
    post_form(
        app,
        &format!("/v1/vocabularies/{name}/versions"),
        &admin(),
        &[
            ("description", None, None, b"Updated"),
            ("changelog", None, None, b"Second version"),
            ("vocabularyFile", Some("v.ttl"), Some("text/turtle"), ttl.as_bytes()),
        ],
    )
    .await
}

fn assert_problem(r: &Resp, status: u16) {
    assert_eq!(r.status.as_u16(), status, "body: {}", r.text());
    assert!(r.content_type().starts_with("application/problem+json"), "content type {}", r.content_type());
    assert_eq!(r.json()["status"], status);
}

#[tokio::test]
async fn health_is_public() {
    let app = app();
    let r = get(&app, "/v1/health", None).await;
    assert_eq!(r.status, StatusCode::OK);
    assert_eq!(r.json()["status"], "UP");
    app.store.fail.store(true, Ordering::SeqCst);
    let r = get(&app, "/v1/health", None).await;
    assert_eq!(r.status, StatusCode::SERVICE_UNAVAILABLE);
    assert_eq!(r.json()["status"], "DOWN");
}

#[tokio::test]
async fn authentication_and_roles() {
    let app = app();
    assert_problem(&get(&app, "/v1/vocabularies", None).await, 401);
    assert_problem(&get(&app, "/v1/vocabularies", Some("garbage")).await, 401);
    let wrong_key = token_with(&["GA_VOCABULARY_ADMIN"], OTHER_KEY, ISSUER, 300);
    assert_problem(&get(&app, "/v1/vocabularies", Some(&wrong_key)).await, 401);
    let wrong_iss = token_with(&["GA_VOCABULARY_ADMIN"], KEY, "http://evil/realms/simpl", 300);
    assert_problem(&get(&app, "/v1/vocabularies", Some(&wrong_iss)).await, 401);
    let expired = token_with(&["GA_VOCABULARY_ADMIN"], KEY, ISSUER, -3600);
    assert_problem(&get(&app, "/v1/vocabularies", Some(&expired)).await, 401);
    assert_problem(&get(&app, "/v1/vocabularies", Some(&norole())).await, 403);
    assert_eq!(get(&app, "/v1/vocabularies", Some(&viewer())).await.status, StatusCode::OK);
    assert_eq!(get(&app, "/v1/vocabularies", Some(&admin())).await.status, StatusCode::OK);

    // Viewer cannot write, but can search.
    let r = post_form(&app, "/v1/vocabularies", &viewer(), &[("name", None, None, b"Abc")]).await;
    assert_problem(&r, 403);
    let r = json_req(&app, "PATCH", "/v1/vocabularies/Abc", &viewer(), json!({"status": "DEPRECATED"})).await;
    assert_problem(&r, 403);
    let r = json_req(&app, "POST", "/v1/vocabularies/searches", &viewer(), json!({"items": [{"name": "Abc"}]})).await;
    assert_eq!(r.status, StatusCode::OK);
    let r = json_req(&app, "POST", "/v1/external-vocabulary/searches", &viewer(), json!({"namespaceUri": "http://x.org"})).await;
    assert_eq!(r.status, StatusCode::OK);
}

#[tokio::test]
async fn create_vocabulary_and_read_it_back() {
    let app = app();
    let r = create(&app, "HealthVocab", &ontology("")).await;
    assert_eq!(r.status, StatusCode::CREATED, "{}", r.text());
    assert_eq!(r.headers[header::LOCATION], "/vocabularies/HealthVocab/versions/1");
    let j = r.json();
    assert_eq!(j["version"], 1);
    assert_eq!(j["status"], "ACTIVE");
    assert_eq!(j["changelog"], "Initial version");
    assert_eq!(j["contentHref"], "http://localhost:8097/v1/content/HealthVocab/versions/1");
    assert!(j.get("validationWarnings").is_none());

    let r = get(&app, "/v1/vocabularies/HealthVocab", Some(&viewer())).await;
    let j = r.json();
    assert_eq!(j["status"], "ACTIVE");
    assert_eq!(j["version"], 1);
    assert_eq!(j["description"], "A test vocabulary");
    assert_eq!(j["createdBy"], "ga-admin@example.com");
    for f in ["name", "version", "description", "status", "creationDate", "lastUpdateDate", "href", "contentHref"] {
        assert!(j.get(f).is_some(), "missing {f}");
    }

    let r = get(&app, "/v1/content/HealthVocab", Some(&viewer())).await;
    assert_eq!(r.status, StatusCode::OK);
    assert!(r.content_type().starts_with("text/turtle"));
    assert_eq!(r.text(), ontology(""));
    assert_eq!(r.headers[header::CONTENT_DISPOSITION], "attachment; filename=\"HealthVocab.ttl\"");

    // Duplicate name (D13).
    assert_problem(&create(&app, "HealthVocab", &ontology("")).await, 409);
}

#[tokio::test]
async fn create_rejects_invalid_metadata() {
    let app = app();
    let ttl = ontology("");
    let r = post_form(
        &app,
        "/v1/vocabularies",
        &admin(),
        &[("name", None, None, b"bad-name"), ("vocabularyFile", Some("v.ttl"), Some("text/turtle"), ttl.as_bytes())],
    )
    .await;
    assert_problem(&r, 400);
    let names: Vec<String> = r.json()["issues"].as_array().unwrap().iter().map(|i| i["name"].as_str().unwrap().to_string()).collect();
    assert!(names.contains(&"name".to_string()));
    assert!(names.contains(&"description".to_string()));
    assert!(names.contains(&"changelog".to_string()));
    // Nothing was created.
    let r = get(&app, "/v1/vocabularies", Some(&admin())).await;
    assert_eq!(r.json()["total"], 0);

    // Not multipart: 415.
    let r = json_req(&app, "POST", "/v1/vocabularies", &admin(), json!({"name": "Abc"})).await;
    assert_problem(&r, 415);

    // Not Turtle: 415 (D15).
    let r = post_form(
        &app,
        "/v1/vocabularies",
        &admin(),
        &[
            ("name", None, None, b"RdfXml"),
            ("description", None, None, b"d"),
            ("changelog", None, None, b"c"),
            ("vocabularyFile", Some("v.rdf"), Some("application/rdf+xml"), b"<rdf:RDF/>"),
        ],
    )
    .await;
    assert_problem(&r, 415);
}

#[tokio::test]
async fn syntax_errors_are_reported_with_locations() {
    let app = app();
    let bad = "@prefix ex: <http://ex.org/> .\nex:a a ex:B\nex:c ex:d .\nex:e ex:f ex:g .\n";
    let r = create(&app, "BrokenVocab", bad).await;
    assert_problem(&r, 400);
    let issues = r.json()["issues"].as_array().unwrap().clone();
    assert!(!issues.is_empty());
    assert!(issues.iter().all(|i| i["location"].as_str().unwrap().starts_with("line ")));
    assert!(issues.iter().all(|i| i["name"] == "vocabularyFile" && i["in"] == "body"));
    assert_problem(&get(&app, "/v1/vocabularies/BrokenVocab", Some(&admin())).await, 404);
}

#[tokio::test]
async fn structure_errors() {
    let app = app();
    let r = create(&app, "NoRoot", "<http://ex.org/a> <http://ex.org/b> <http://ex.org/c> .").await;
    assert_problem(&r, 400);
    assert_eq!(r.json()["issues"][0]["location"], "document");
    let r = create(&app, "EmptyOne", "").await;
    assert_problem(&r, 400);
}

#[tokio::test]
async fn new_version_deprecates_previous_versions() {
    let app = app();
    assert_eq!(create(&app, "Lifecycle", &ontology("")).await.status, StatusCode::CREATED);
    let r = new_version(&app, "Lifecycle", &ontology("ex:Doctor a owl:Class .")).await;
    assert_eq!(r.status, StatusCode::CREATED, "{}", r.text());
    assert_eq!(r.json()["version"], 2);
    assert_eq!(r.headers[header::LOCATION], "/vocabularies/Lifecycle/versions/2");
    let r = new_version(&app, "Lifecycle", &ontology("")).await;
    assert_eq!(r.json()["version"], 3);

    let r = get(&app, "/v1/vocabularies/Lifecycle/versions", Some(&viewer())).await;
    let j = r.json();
    assert_eq!(j["total"], 3);
    let items = j["items"].as_array().unwrap();
    assert_eq!(items[0]["version"], 3);
    assert_eq!(items[0]["status"], "ACTIVE");
    assert_eq!(items[1]["status"], "DEPRECATED");
    assert_eq!(items[2]["status"], "DEPRECATED");
    assert_eq!(items[2]["latestVersionHref"], "/vocabularies/Lifecycle/versions/3");

    let r = get(&app, "/v1/vocabularies/Lifecycle/versions/1", Some(&viewer())).await;
    assert_eq!(r.json()["status"], "DEPRECATED");
    assert_eq!(r.json()["changelog"], "Initial version");
    assert_problem(&get(&app, "/v1/vocabularies/Lifecycle/versions/9", Some(&viewer())).await, 404);
    assert_problem(&get(&app, "/v1/vocabularies/Lifecycle/versions/0", Some(&viewer())).await, 400);

    let r = get(&app, "/v1/content/Lifecycle/versions/2", Some(&viewer())).await;
    assert!(r.text().contains("ex:Doctor"));
    assert_eq!(r.headers[header::CONTENT_DISPOSITION], "attachment; filename=\"Lifecycle-v2.ttl\"");

    let r = get(&app, "/v1/vocabularies/Lifecycle", Some(&viewer())).await;
    assert_eq!(r.json()["version"], 3);
    assert_eq!(r.json()["description"], "Updated");
}

#[tokio::test]
async fn manual_version_field_is_rejected() {
    let app = app();
    create(&app, "Manual", &ontology("")).await;
    let ttl = ontology("");
    let r = post_form(
        &app,
        "/v1/vocabularies/Manual/versions",
        &admin(),
        &[
            ("description", None, None, b"d"),
            ("changelog", None, None, b"c"),
            ("version", None, None, b"5"),
            ("vocabularyFile", Some("v.ttl"), Some("text/turtle"), ttl.as_bytes()),
        ],
    )
    .await;
    assert_problem(&r, 400);
    assert_eq!(r.json()["issues"][0]["name"], "version");
    let r = get(&app, "/v1/vocabularies/Manual/versions", Some(&admin())).await;
    assert_eq!(r.json()["total"], 1);
}

#[tokio::test]
async fn new_version_for_missing_vocabulary_is_404() {
    let app = app();
    assert_problem(&new_version(&app, "Missing", &ontology("")).await, 404);
}

#[tokio::test]
async fn status_changes() {
    let app = app();
    create(&app, "Status", &ontology("")).await;
    new_version(&app, "Status", &ontology("")).await;
    let r = json_req(&app, "PATCH", "/v1/vocabularies/Status", &admin(), json!({"status": "DEPRECATED"})).await;
    assert_eq!(r.status, StatusCode::OK, "{}", r.text());
    assert_eq!(r.json()["status"], "DEPRECATED");
    let r = get(&app, "/v1/vocabularies/Status/versions", Some(&admin())).await;
    assert!(r.json()["items"].as_array().unwrap().iter().all(|i| i["status"] == "DEPRECATED"));
    // Same status again: 409.
    assert_problem(&json_req(&app, "PATCH", "/v1/vocabularies/Status", &admin(), json!({"status": "DEPRECATED"})).await, 409);
    // New version on a deprecated vocabulary: 409 (D11).
    assert_problem(&new_version(&app, "Status", &ontology("")).await, 409);
    // Re-activate: only the latest version becomes ACTIVE (D10).
    let r = json_req(&app, "PATCH", "/v1/vocabularies/Status", &admin(), json!({"status": "ACTIVE"})).await;
    assert_eq!(r.json()["status"], "ACTIVE");
    let r = get(&app, "/v1/vocabularies/Status/versions/2", Some(&admin())).await;
    assert_eq!(r.json()["status"], "ACTIVE");
    let r = get(&app, "/v1/vocabularies/Status/versions/1", Some(&admin())).await;
    assert_eq!(r.json()["status"], "DEPRECATED");
    // Invalid status value: 400.
    assert_problem(&json_req(&app, "PATCH", "/v1/vocabularies/Status", &admin(), json!({"status": "ARCHIVED"})).await, 400);
    // Unknown vocabulary: 404.
    assert_problem(&json_req(&app, "PATCH", "/v1/vocabularies/Nope", &admin(), json!({"status": "ACTIVE"})).await, 404);
    // Version-level: cannot activate v1 while v2 is ACTIVE.
    assert_problem(&json_req(&app, "PATCH", "/v1/vocabularies/Status/versions/1", &admin(), json!({"status": "ACTIVE"})).await, 409);
    let r = json_req(&app, "PATCH", "/v1/vocabularies/Status/versions/2", &admin(), json!({"status": "DEPRECATED"})).await;
    assert_eq!(r.json()["status"], "DEPRECATED");
    let r = json_req(&app, "PATCH", "/v1/vocabularies/Status/versions/1", &admin(), json!({"status": "ACTIVE"})).await;
    assert_eq!(r.status, StatusCode::OK);
    let r = get(&app, "/v1/vocabularies/Status", Some(&admin())).await;
    assert_eq!(r.json()["status"], "ACTIVE");
}

#[tokio::test]
async fn list_filters_sorting_and_paging() {
    let app = app();
    for n in ["Alpha", "BravoShacl", "CharlieShacl"] {
        assert_eq!(create(&app, n, &ontology("")).await.status, StatusCode::CREATED);
    }
    json_req(&app, "PATCH", "/v1/vocabularies/Alpha", &admin(), json!({"status": "DEPRECATED"})).await;

    let r = get(&app, "/v1/vocabularies?name=Shacl", Some(&viewer())).await;
    assert_eq!(r.json()["total"], 2);
    let r = get(&app, "/v1/vocabularies?status=DEPRECATED", Some(&viewer())).await;
    assert_eq!(r.json()["items"][0]["name"], "Alpha");
    let r = get(&app, "/v1/vocabularies?sort=-name", Some(&viewer())).await;
    assert_eq!(r.json()["items"][0]["name"], "CharlieShacl");
    let r = get(&app, "/v1/vocabularies?sort=name&page=2&pageSize=2", Some(&viewer())).await;
    let j = r.json();
    assert_eq!(j["items"].as_array().unwrap().len(), 1);
    assert_eq!(j["items"][0]["name"], "CharlieShacl");
    assert_eq!(j["totalPages"], 2);
    assert_eq!(j["prev"], "/vocabularies?page=1&pageSize=2&sort=name");
    let today = chrono::Utc::now().format("%Y-%m-%d").to_string();
    let r = get(&app, &format!("/v1/vocabularies?creationDate={today}"), Some(&viewer())).await;
    assert_eq!(r.json()["total"], 3);
    let r = get(&app, "/v1/vocabularies?lastUpdated=2001-01-01", Some(&viewer())).await;
    assert_eq!(r.json()["total"], 0);
    assert_eq!(r.json()["items"], json!([]));

    for bad in [
        "sort=size",
        "page=0",
        "pageSize=101",
        "pageSize=abc",
        "status=OLD",
        "name=shacl",
        "creationDate=yesterday",
        "color=red",
    ] {
        assert_problem(&get(&app, &format!("/v1/vocabularies?{bad}"), Some(&viewer())).await, 400);
    }
    assert_eq!(get(&app, "/v1/vocabularies?pageSize=100", Some(&viewer())).await.status, StatusCode::OK);
}

#[tokio::test]
async fn semantic_references_between_vocabularies() {
    let app = app();
    create(&app, "BaseVocab", &ontology("")).await;
    let referencing = |ns: &str, term: &str| {
        ontology(&format!(
            "@prefix base: <{BASE}/{ns}> .\nex:Inpatient rdfs:subClassOf base:{term} ."
        ))
    };
    // Valid reference to an ACTIVE version with an existing term.
    let r = create(&app, "GoodRef", &referencing("BaseVocab/1#", "Patient")).await;
    assert_eq!(r.status, StatusCode::CREATED, "{}", r.text());
    // Unknown term.
    let r = create(&app, "BadTerm", &referencing("BaseVocab/1#", "Nurse")).await;
    assert_problem(&r, 400);
    // Missing version and missing vocabulary.
    assert_problem(&create(&app, "BadVersion", &referencing("BaseVocab/4#", "Patient")).await, 400);
    assert_problem(&create(&app, "BadVocab", &referencing("NoSuchVocab/1#", "Patient")).await, 400);
    // Deprecated reference (SIMPL-18184): error naming the version and the ACTIVE alternative.
    new_version(&app, "BaseVocab", &ontology("")).await;
    let r = create(&app, "OldRef", &referencing("BaseVocab/1#", "Patient")).await;
    assert_problem(&r, 400);
    let detail = r.json()["issues"][0]["detail"].as_str().unwrap().to_string();
    assert!(detail.contains("deprecated") && detail.contains("version 2"), "{detail}");
    // All errors reported, not just the first.
    let two = ontology(&format!(
        "@prefix a: <{BASE}/BaseVocab/1#> .\n@prefix b: <{BASE}/Ghost/1#> .\nex:X rdfs:subClassOf a:Patient , b:Thing ."
    ));
    let r = create(&app, "TwoErrors", &two).await;
    assert_eq!(r.json()["issues"].as_array().unwrap().len(), 2);
}

#[tokio::test]
async fn own_namespace_is_not_a_reference() {
    // D18 clarification (28 Oct 2026): a vocabulary's own namespace defines its terms.
    let app = app();
    let own = |ns: &str| ontology(&format!("@prefix me: <{BASE}/{ns}> .\nme:NewTerm a owl:Class ."));
    let r = create(&app, "SelfRef", &own("SelfRef/1#")).await;
    assert_eq!(r.status, StatusCode::CREATED, "{}", r.text());
    let r = new_version(&app, "SelfRef", &own("SelfRef/2#")).await;
    assert_eq!(r.status, StatusCode::CREATED, "{}", r.text());
    // Version 1 is DEPRECATED now; using its own versioned namespace is still not a deprecated reference.
    let r = new_version(&app, "SelfRef", &own("SelfRef/1#")).await;
    assert_eq!(r.status, StatusCode::CREATED, "{}", r.text());
    let r = new_version(&app, "SelfRef", &own("SelfRef#")).await;
    assert_eq!(r.status, StatusCode::CREATED, "{}", r.text());
    // Another vocabulary's namespace is still checked.
    assert_problem(&new_version(&app, "SelfRef", &own("Ghost/1#")).await, 400);
}

#[tokio::test]
async fn warnings_for_unregistered_external_namespaces() {
    let app = app();
    let ttl = ontology("@prefix dcterms: <http://purl.org/dc/terms/> .\nex:Onto dcterms:title \"T\" .");
    let r = create(&app, "WithDc", &ttl).await;
    assert_eq!(r.status, StatusCode::CREATED);
    let w = &r.json()["validationWarnings"];
    assert_eq!(w[0]["code"], "externalReferenceSkipped");
    assert_eq!(w[0]["location"], "prefix 'dcterms' -> 'http://purl.org/dc/terms/'");

    // Register Dublin Core: no warning anymore.
    let dc = "@prefix dcterms: <http://purl.org/dc/terms/> .\ndcterms:title a <http://www.w3.org/1999/02/22-rdf-syntax-ns#Property> .";
    let r = post_form(
        &app,
        "/v1/external-vocabularies",
        &admin(),
        &[
            ("name", None, None, b"DublinCoreTerms"),
            ("namespaceUri", None, None, b"http://purl.org/dc/terms/"),
            ("aliasUris", None, None, b"https://purl.org/dc/terms/"),
            ("vocabularyFile", Some("dc.ttl"), Some("text/turtle"), dc.as_bytes()),
        ],
    )
    .await;
    assert_eq!(r.status, StatusCode::CREATED, "{}", r.text());
    assert_eq!(r.json()["namespaceUri"], "http://purl.org/dc/terms");
    let r = create(&app, "WithDcTwo", &ttl).await;
    assert!(r.json().get("validationWarnings").is_none(), "{}", r.text());
}

#[tokio::test]
async fn external_vocabularies() {
    let app = app();
    let dc = "@prefix dcterms: <http://purl.org/dc/terms/> .\ndcterms:title a <http://www.w3.org/1999/02/22-rdf-syntax-ns#Property> .";
    let parts = |name: &'static [u8]| {
        vec![
            ("name", None, None, name),
            ("namespaceUri", None, None, b"http://purl.org/dc/terms/" as &[u8]),
            ("aliasUris", None, None, b"https://purl.org/dc/terms/" as &[u8]),
            ("vocabularyFile", Some("dc.ttl"), Some("text/turtle"), dc.as_bytes()),
        ]
    };
    assert_eq!(post_form(&app, "/v1/external-vocabularies", &admin(), &parts(b"DublinCore")).await.status, StatusCode::CREATED);
    assert_problem(&post_form(&app, "/v1/external-vocabularies", &admin(), &parts(b"DublinCore")).await, 409);
    assert_problem(&post_form(&app, "/v1/external-vocabularies", &viewer(), &parts(b"Other")).await, 403);

    let r = get(&app, "/v1/external-vocabularies", Some(&viewer())).await;
    assert_eq!(r.json()["total"], 1);
    assert_eq!(r.json()["items"][0]["aliasUris"][0], "https://purl.org/dc/terms");
    let r = get(&app, "/v1/external-vocabularies/DublinCore", Some(&viewer())).await;
    assert_eq!(r.json()["href"], "/external-vocabularies/DublinCore");
    let r = get(&app, "/v1/external-vocabularies/DublinCore/content", Some(&viewer())).await;
    assert_eq!(r.text(), dc);

    let r = json_req(&app, "POST", "/v1/external-vocabulary/searches", &viewer(), json!({"aliasUri": "https://purl.org/dc/terms"})).await;
    assert_eq!(r.json()["total"], 1);
    let r = json_req(&app, "POST", "/v1/external-vocabulary/searches", &viewer(), json!({"namespaceUris": ["http://purl.org/dc/terms/", "https://schema.org"]})).await;
    assert_eq!(r.json()["total"], 1);
    let r = json_req(&app, "POST", "/v1/external-vocabulary/searches", &viewer(), json!({"namespaceUri": "https://schema.org"})).await;
    assert_eq!(r.json()["items"], json!([]));
    assert_problem(&json_req(&app, "POST", "/v1/external-vocabulary/searches", &viewer(), json!({})).await, 400);
    assert_problem(
        &json_req(&app, "POST", "/v1/external-vocabulary/searches", &viewer(), json!({"namespaceUri": "http://a.org", "aliasUri": "http://b.org"})).await,
        400,
    );

    let r = json_req(&app, "POST", "/v1/external-vocabularies/content", &admin(), json!({"namespaceUri": "https://purl.org/dc/terms/"})).await;
    assert_eq!(r.status, StatusCode::OK);
    assert_eq!(r.headers[header::CONTENT_DISPOSITION], "attachment; filename=\"DublinCore.ttl\"");
    assert_problem(&json_req(&app, "POST", "/v1/external-vocabularies/content", &admin(), json!({"namespaceUri": "https://nope.org/"})).await, 404);

    let del = Request::delete("/v1/external-vocabularies/DublinCore")
        .header(header::AUTHORIZATION, format!("Bearer {}", admin()))
        .body(Body::empty())
        .unwrap();
    assert_eq!(send(&app, del).await.status, StatusCode::NO_CONTENT);
    assert_problem(&get(&app, "/v1/external-vocabularies/DublinCore", Some(&viewer())).await, 404);
}

#[tokio::test]
async fn vocabulary_search() {
    let app = app();
    create(&app, "SearchMe", &ontology("")).await;
    new_version(&app, "SearchMe", &ontology("")).await;
    let r = json_req(
        &app,
        "POST",
        "/v1/vocabularies/searches",
        &viewer(),
        json!({"items": [{"name": "SearchMe"}, {"name": "SearchMe", "version": 1}, {"name": "Missing"}]}),
    )
    .await;
    let j = r.json();
    assert_eq!(j["total"], 2);
    assert_eq!(j["items"][0]["latest"], true);
    assert_eq!(j["items"][0]["version"], 2);
    assert_eq!(j["items"][1]["status"], "DEPRECATED");
    assert_eq!(j["items"][1]["latest"], false);
    assert_problem(&json_req(&app, "POST", "/v1/vocabularies/searches", &viewer(), json!({"items": []})).await, 400);
    assert_problem(&json_req(&app, "POST", "/v1/vocabularies/searches", &viewer(), json!({"items": [{"name": "Abc", "x": 1}]})).await, 400);
}

#[tokio::test]
async fn upload_limit_is_enforced() {
    let app = app_with_limit(1024);
    let big = ontology(&format!("ex:Big rdfs:comment \"{}\" .", "x".repeat(4096)));
    assert_problem(&create(&app, "TooBig", &big).await, 413);
}

#[tokio::test]
async fn not_acceptable_and_not_found() {
    let app = app();
    let req = Request::get("/v1/vocabularies")
        .header(header::AUTHORIZATION, format!("Bearer {}", admin()))
        .header(header::ACCEPT, "application/xml")
        .body(Body::empty())
        .unwrap();
    assert_problem(&send(&app, req).await, 406);
    assert_problem(&get(&app, "/v1/vocabularies/Unknown", Some(&admin())).await, 404);
    assert_problem(&get(&app, "/v1/content/Unknown", Some(&admin())).await, 404);
    assert_problem(&get(&app, "/v1/vocabularies/lowercase", Some(&admin())).await, 400);
    assert_problem(&get(&app, "/v1/nothing-here", Some(&admin())).await, 404);
}

#[tokio::test]
async fn store_failure_gives_500() {
    let app = app();
    app.store.fail.store(true, Ordering::SeqCst);
    let r = get(&app, "/v1/vocabularies", Some(&admin())).await;
    assert_problem(&r, 500);
    assert!(r.json().get("items").is_none());
}

#[tokio::test]
async fn cors_preflight_for_ui_origins() {
    let app = app();
    let req = Request::builder()
        .method("OPTIONS")
        .uri("/v1/vocabularies")
        .header(header::ORIGIN, "http://localhost:4333")
        .header(header::ACCESS_CONTROL_REQUEST_METHOD, "POST")
        .header(header::ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type")
        .body(Body::empty())
        .unwrap();
    let r = send(&app, req).await;
    assert!(r.status.is_success(), "{}", r.status);
    assert_eq!(r.headers[header::ACCESS_CONTROL_ALLOW_ORIGIN], "http://localhost:4333");
    let req = Request::builder()
        .method("OPTIONS")
        .uri("/v1/vocabularies")
        .header(header::ORIGIN, "http://evil.example")
        .header(header::ACCESS_CONTROL_REQUEST_METHOD, "POST")
        .body(Body::empty())
        .unwrap();
    let r = send(&app, req).await;
    assert!(r.headers.get(header::ACCESS_CONTROL_ALLOW_ORIGIN).is_none());
}
