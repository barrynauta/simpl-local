//! Business rules: versioning (D7 to D11), validation pipeline (D15 to D20), external vocabularies.

use crate::model::{ExternalVocabulary, Status, Version, Vocabulary, normalize_namespace};
use crate::problem::{ApiError, ApiResult, Issue};
use crate::store::{NewContent, Store, StoreError};
use crate::validation::{self, ParsedTurtle, SemanticContext, Warning};
use chrono::{DateTime, DurationRound, TimeDelta, Utc};
use std::sync::Arc;
use tokio::sync::Mutex;

#[derive(Debug, Clone)]
pub struct ServiceConfig {
    /// Base IRI of the internal reference convention (D18).
    pub reference_base: String,
}

pub struct VocabularyService {
    store: Arc<dyn Store>,
    cfg: ServiceConfig,
    /// Serialises read-modify-write cycles within this process.
    write_lock: Mutex<()>,
}

impl From<StoreError> for ApiError {
    fn from(e: StoreError) -> Self {
        ApiError::Internal(e.to_string())
    }
}

/// Current time, truncated to milliseconds so it survives a store round trip unchanged.
pub fn now() -> DateTime<Utc> {
    let t = Utc::now();
    t.duration_trunc(TimeDelta::milliseconds(1)).unwrap_or(t)
}

pub struct NewVersionInput {
    pub description: String,
    pub changelog: String,
    pub file: Vec<u8>,
}

pub struct NewExternalInput {
    pub name: String,
    pub namespace_uri: String,
    pub alias_uris: Vec<String>,
    pub description: Option<String>,
    pub source_uri: Option<String>,
    pub file: Vec<u8>,
}

impl VocabularyService {
    pub fn new(store: Arc<dyn Store>, cfg: ServiceConfig) -> Self {
        VocabularyService { store, cfg, write_lock: Mutex::new(()) }
    }

    pub fn store(&self) -> &Arc<dyn Store> {
        &self.store
    }

    pub async fn list(&self) -> ApiResult<Vec<Vocabulary>> {
        Ok(self.store.list_vocabularies().await?)
    }

    pub async fn get(&self, name: &str) -> ApiResult<Vocabulary> {
        self.store
            .get_vocabulary(name)
            .await?
            .ok_or_else(|| ApiError::vocabulary_not_found(name))
    }

    pub async fn find(&self, name: &str) -> ApiResult<Option<Vocabulary>> {
        Ok(self.store.get_vocabulary(name).await?)
    }

    /// Syntax, then structure, then semantics. Each stage stops the pipeline if it fails
    /// (SIMPL-18181: no semantic validation after a syntax error).
    async fn validate_content(
        &self,
        file: &[u8],
        self_name: &str,
    ) -> ApiResult<(ParsedTurtle, Vec<Warning>)> {
        let doc = validation::parse_turtle(file).map_err(ApiError::BadRequest)?;
        let structure = validation::check_structure(&doc);
        if !structure.is_empty() {
            return Err(ApiError::BadRequest(structure));
        }
        let ctx = self.semantic_context(&doc, self_name).await?;
        let (errors, warnings) = validation::check_semantics(&doc, &ctx);
        if !errors.is_empty() {
            return Err(ApiError::BadRequest(errors));
        }
        Ok((doc, warnings))
    }

    async fn semantic_context(&self, doc: &ParsedTurtle, self_name: &str) -> ApiResult<SemanticContext> {
        let mut ctx = SemanticContext {
            base: self.cfg.reference_base.clone(),
            externals: self.store.list_external().await?,
            self_name: Some(self_name.to_string()),
            ..Default::default()
        };
        for r in validation::collect_internal_refs(doc, &self.cfg.reference_base) {
            if r.malformed.is_some() || r.name == self_name {
                continue;
            }
            if !ctx.vocabularies.contains_key(&r.name)
                && let Some(v) = self.store.get_vocabulary(&r.name).await?
            {
                ctx.vocabularies.insert(r.name.clone(), v);
            }
            let Some(voc) = ctx.vocabularies.get(&r.name) else { continue };
            let number = match r.version {
                Some(n) => n,
                None => voc.latest().number,
            };
            if voc.version(number).is_none() || ctx.terms.contains_key(&(r.name.clone(), number)) {
                continue;
            }
            let terms = match self.store.get_content(&r.name, number).await? {
                Some(src) => validation::parse_turtle(src.as_bytes())
                    .map(|d| validation::defined_terms(&d))
                    .unwrap_or_default(),
                None => Default::default(),
            };
            ctx.terms.insert((r.name.clone(), number), terms);
        }
        Ok(ctx)
    }

    /// SIMPL-18170: create a vocabulary; version 1 is ACTIVE (D7, D13).
    pub async fn create_vocabulary(
        &self,
        name: &str,
        input: NewVersionInput,
        created_by: &str,
    ) -> ApiResult<(Vocabulary, Vec<Warning>)> {
        let _guard = self.write_lock.lock().await;
        if self.store.get_vocabulary(name).await?.is_some() {
            return Err(ApiError::Conflict(format!(
                "A vocabulary with name '{name}' already exists."
            )));
        }
        let (doc, warnings) = self.validate_content(&input.file, name).await?;
        let t = now();
        let vocabulary = Vocabulary {
            name: name.to_string(),
            created_by: created_by.to_string(),
            creation_date: t,
            versions: vec![Version {
                number: 1,
                description: input.description,
                changelog: input.changelog,
                status: Status::Active,
                created_by: created_by.to_string(),
                creation_date: t,
                last_update_date: t,
            }],
        };
        let content = NewContent {
            version: 1,
            source: String::from_utf8_lossy(&input.file).into_owned(),
            triples: doc.triples,
        };
        self.store.save_vocabulary(&vocabulary, Some(&content)).await?;
        Ok((vocabulary, warnings))
    }

    /// SIMPL-18173/18190/18191: next number, new version ACTIVE, all older ones DEPRECATED (D7, D9, D11).
    pub async fn create_version(
        &self,
        name: &str,
        input: NewVersionInput,
        created_by: &str,
    ) -> ApiResult<(Vocabulary, Vec<Warning>)> {
        let _guard = self.write_lock.lock().await;
        let mut vocabulary = self.get(name).await?;
        if vocabulary.status() == Status::Deprecated {
            return Err(ApiError::Conflict(format!(
                "Vocabulary '{name}' is DEPRECATED. Re-activate it before creating a new version."
            )));
        }
        let (doc, warnings) = self.validate_content(&input.file, name).await?;
        let t = now();
        let number = vocabulary.next_version_number();
        for v in vocabulary.versions.iter_mut() {
            if v.status != Status::Deprecated {
                v.status = Status::Deprecated;
                v.last_update_date = t;
            }
        }
        vocabulary.versions.push(Version {
            number,
            description: input.description,
            changelog: input.changelog,
            status: Status::Active,
            created_by: created_by.to_string(),
            creation_date: t,
            last_update_date: t,
        });
        let content = NewContent {
            version: number,
            source: String::from_utf8_lossy(&input.file).into_owned(),
            triples: doc.triples,
        };
        self.store.save_vocabulary(&vocabulary, Some(&content)).await?;
        Ok((vocabulary, warnings))
    }

    /// SIMPL-18175, D10: DEPRECATED deprecates all versions; ACTIVE re-activates the latest version.
    pub async fn update_vocabulary_status(&self, name: &str, status: Status) -> ApiResult<Vocabulary> {
        let _guard = self.write_lock.lock().await;
        let mut vocabulary = self.get(name).await?;
        if vocabulary.status() == status {
            return Err(ApiError::Conflict(format!(
                "Vocabulary is already in status {status}"
            )));
        }
        let t = now();
        match status {
            Status::Deprecated => {
                for v in vocabulary.versions.iter_mut() {
                    if v.status != Status::Deprecated {
                        v.status = Status::Deprecated;
                        v.last_update_date = t;
                    }
                }
            }
            Status::Active => {
                let latest = vocabulary.latest().number;
                let v = vocabulary.version_mut(latest).expect("latest exists");
                v.status = Status::Active;
                v.last_update_date = t;
            }
        }
        self.store.save_vocabulary(&vocabulary, None).await?;
        Ok(vocabulary)
    }

    /// Version-level status change (OpenAPI only, D10).
    pub async fn update_version_status(
        &self,
        name: &str,
        number: u32,
        status: Status,
    ) -> ApiResult<Vocabulary> {
        let _guard = self.write_lock.lock().await;
        let mut vocabulary = self.get(name).await?;
        let current = vocabulary
            .version(number)
            .ok_or_else(|| ApiError::version_not_found(name, number))?
            .status;
        if current == status {
            return Err(ApiError::Conflict(format!(
                "Vocabulary version {number} is already in status {status}"
            )));
        }
        if status == Status::Active
            && let Some(other) = vocabulary
                .versions
                .iter()
                .find(|v| v.number != number && v.status == Status::Active)
        {
            return Err(ApiError::Conflict(format!(
                "Version {} of vocabulary '{name}' is ACTIVE. Only one version can be ACTIVE; deprecate it first.",
                other.number
            )));
        }
        let t = now();
        let v = vocabulary.version_mut(number).expect("checked");
        v.status = status;
        v.last_update_date = t;
        self.store.save_vocabulary(&vocabulary, None).await?;
        Ok(vocabulary)
    }

    /// Turtle content of a version (latest if `None`).
    pub async fn content(&self, name: &str, version: Option<u32>) -> ApiResult<(u32, String)> {
        let vocabulary = self.get(name).await?;
        let number = match version {
            Some(n) => {
                vocabulary
                    .version(n)
                    .ok_or_else(|| ApiError::version_not_found(name, n))?;
                n
            }
            None => vocabulary.latest().number,
        };
        let content = self
            .store
            .get_content(name, number)
            .await?
            .ok_or_else(|| ApiError::Internal(format!("content of {name} v{number} is missing")))?;
        Ok((number, content))
    }

    // ---- external vocabularies ----

    pub async fn list_external(&self) -> ApiResult<Vec<ExternalVocabulary>> {
        let mut list = self.store.list_external().await?;
        list.sort_by(|a, b| a.name.cmp(&b.name));
        Ok(list)
    }

    pub async fn get_external(&self, name: &str) -> ApiResult<ExternalVocabulary> {
        self.store.get_external(name).await?.ok_or_else(|| {
            ApiError::external_not_found(format!("External vocabulary with name '{name}' was not found."))
        })
    }

    pub async fn create_external(
        &self,
        input: NewExternalInput,
        created_by: &str,
    ) -> ApiResult<ExternalVocabulary> {
        let _guard = self.write_lock.lock().await;
        if self.store.get_external(&input.name).await?.is_some() {
            return Err(ApiError::Conflict(format!(
                "An external vocabulary with name '{}' already exists.",
                input.name
            )));
        }
        let namespace = normalize_namespace(&input.namespace_uri);
        let mut aliases: Vec<String> = Vec::new();
        for a in &input.alias_uris {
            let n = normalize_namespace(a);
            if !n.is_empty() && n != namespace && !aliases.contains(&n) {
                aliases.push(n);
            }
        }
        let existing = self.store.list_external().await?;
        for e in &existing {
            let theirs: Vec<&String> = std::iter::once(&e.namespace_uri).chain(e.alias_uris.iter()).collect();
            if let Some(clash) = std::iter::once(&namespace)
                .chain(aliases.iter())
                .find(|u| theirs.contains(u))
            {
                return Err(ApiError::Conflict(format!(
                    "Namespace URI '{clash}' is already registered by external vocabulary '{}'.",
                    e.name
                )));
            }
        }
        let doc = validation::parse_turtle(&input.file).map_err(ApiError::BadRequest)?;
        let empty = validation::check_not_empty(&doc);
        if !empty.is_empty() {
            return Err(ApiError::BadRequest(empty));
        }
        let external = ExternalVocabulary {
            name: input.name,
            namespace_uri: namespace,
            alias_uris: aliases,
            source_uri: input.source_uri.map(|s| s.trim().to_string()),
            description: input.description,
            created_by: created_by.to_string(),
            creation_date: now(),
        };
        self.store
            .save_external(&external, &String::from_utf8_lossy(&input.file), &doc.triples)
            .await?;
        Ok(external)
    }

    pub async fn delete_external(&self, name: &str) -> ApiResult<()> {
        let _guard = self.write_lock.lock().await;
        if self.store.delete_external(name).await? {
            Ok(())
        } else {
            Err(ApiError::external_not_found(format!(
                "External vocabulary with name '{name}' was not found."
            )))
        }
    }

    pub async fn external_content_by_name(&self, name: &str) -> ApiResult<String> {
        self.get_external(name).await?;
        self.store.get_external_content(name).await?.ok_or_else(|| {
            ApiError::external_not_found(format!(
                "Content of external vocabulary '{name}' was not found."
            ))
        })
    }

    /// Resolve by canonical namespace or alias (normalised).
    pub async fn external_by_namespace(&self, uri: &str) -> ApiResult<(ExternalVocabulary, String)> {
        let n = normalize_namespace(uri);
        let found = self
            .store
            .list_external()
            .await?
            .into_iter()
            .find(|e| e.namespace_uri == n || e.alias_uris.contains(&n));
        let Some(e) = found else {
            return Err(ApiError::external_not_found(format!(
                "External vocabulary with namespaceUri '{n}' was not found."
            )));
        };
        let content = self.external_content_by_name(&e.name).await?;
        Ok((e, content))
    }

    /// Search by canonical namespaces and/or aliases (normalised, deduplicated).
    pub async fn search_external(
        &self,
        namespaces: &[String],
        aliases: &[String],
    ) -> ApiResult<Vec<ExternalVocabulary>> {
        let ns: Vec<String> = namespaces.iter().map(|s| normalize_namespace(s)).collect();
        let al: Vec<String> = aliases.iter().map(|s| normalize_namespace(s)).collect();
        Ok(self
            .list_external()
            .await?
            .into_iter()
            .filter(|e| ns.contains(&e.namespace_uri) || e.alias_uris.iter().any(|a| al.contains(a)))
            .collect())
    }
}

/// Validation helper for multipart text fields.
pub fn required_text(value: Option<String>, field: &str, issues: &mut Vec<Issue>) -> String {
    match value {
        Some(v) if !v.trim().is_empty() => v,
        Some(_) => {
            issues.push(Issue::schema_violation(
                "body",
                field,
                format!("Field '{field}' must not be blank."),
                None,
            ));
            String::new()
        }
        None => {
            issues.push(Issue::schema_violation(
                "body",
                field,
                format!("Field '{field}' is required."),
                None,
            ));
            String::new()
        }
    }
}
