//! Persistence port. The production adapter talks to Apache Jena Fuseki (D27);
//! the in-memory adapter is used by the tests.

pub mod fuseki;
pub mod memory;
pub mod rdf_mapping;

use crate::model::{ExternalVocabulary, Vocabulary};
use async_trait::async_trait;
use oxrdf::Triple;

#[derive(Debug, thiserror::Error)]
pub enum StoreError {
    #[error("store unavailable: {0}")]
    Unavailable(String),
    #[error("store error: {0}")]
    Other(String),
}

/// Content of a newly created version: the original Turtle text and its parsed triples.
#[derive(Debug, Clone)]
pub struct NewContent {
    pub version: u32,
    pub source: String,
    pub triples: Vec<Triple>,
}

#[async_trait]
pub trait Store: Send + Sync {
    /// All internal vocabularies with their versions (metadata only).
    async fn list_vocabularies(&self) -> Result<Vec<Vocabulary>, StoreError>;

    async fn get_vocabulary(&self, name: &str) -> Result<Option<Vocabulary>, StoreError>;

    /// Store the whole vocabulary aggregate, and the content of one new version if given.
    /// Must be atomic: either everything is stored or nothing (D9).
    async fn save_vocabulary(
        &self,
        vocabulary: &Vocabulary,
        new_content: Option<&NewContent>,
    ) -> Result<(), StoreError>;

    /// Original Turtle text of one version.
    async fn get_content(&self, name: &str, version: u32) -> Result<Option<String>, StoreError>;

    async fn list_external(&self) -> Result<Vec<ExternalVocabulary>, StoreError>;

    async fn get_external(&self, name: &str) -> Result<Option<ExternalVocabulary>, StoreError>;

    async fn save_external(
        &self,
        external: &ExternalVocabulary,
        source: &str,
        triples: &[Triple],
    ) -> Result<(), StoreError>;

    async fn get_external_content(&self, name: &str) -> Result<Option<String>, StoreError>;

    /// Returns true if something was deleted.
    async fn delete_external(&self, name: &str) -> Result<bool, StoreError>;

    /// Health probe.
    async fn ping(&self) -> bool;
}
