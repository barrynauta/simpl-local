//! In-memory store, used by the tests and for running without Fuseki (`STORE=memory`).

use super::{NewContent, Store, StoreError};
use crate::model::{ExternalVocabulary, Vocabulary};
use async_trait::async_trait;
use oxrdf::Triple;
use std::collections::HashMap;
use std::sync::atomic::{AtomicBool, Ordering};
use tokio::sync::RwLock;

#[derive(Default)]
struct Data {
    vocabularies: HashMap<String, Vocabulary>,
    contents: HashMap<(String, u32), String>,
    externals: HashMap<String, (ExternalVocabulary, String)>,
}

#[derive(Default)]
pub struct MemoryStore {
    data: RwLock<Data>,
    /// When set, every call fails: lets tests exercise the 500 path.
    pub fail: AtomicBool,
}

impl MemoryStore {
    pub fn new() -> Self {
        Self::default()
    }

    fn check(&self) -> Result<(), StoreError> {
        if self.fail.load(Ordering::SeqCst) {
            Err(StoreError::Unavailable("simulated failure".into()))
        } else {
            Ok(())
        }
    }
}

#[async_trait]
impl Store for MemoryStore {
    async fn list_vocabularies(&self) -> Result<Vec<Vocabulary>, StoreError> {
        self.check()?;
        Ok(self.data.read().await.vocabularies.values().cloned().collect())
    }

    async fn get_vocabulary(&self, name: &str) -> Result<Option<Vocabulary>, StoreError> {
        self.check()?;
        Ok(self.data.read().await.vocabularies.get(name).cloned())
    }

    async fn save_vocabulary(
        &self,
        vocabulary: &Vocabulary,
        new_content: Option<&NewContent>,
    ) -> Result<(), StoreError> {
        self.check()?;
        let mut data = self.data.write().await;
        data.vocabularies
            .insert(vocabulary.name.clone(), vocabulary.clone());
        if let Some(c) = new_content {
            data.contents
                .insert((vocabulary.name.clone(), c.version), c.source.clone());
        }
        Ok(())
    }

    async fn get_content(&self, name: &str, version: u32) -> Result<Option<String>, StoreError> {
        self.check()?;
        Ok(self
            .data
            .read()
            .await
            .contents
            .get(&(name.to_string(), version))
            .cloned())
    }

    async fn list_external(&self) -> Result<Vec<ExternalVocabulary>, StoreError> {
        self.check()?;
        Ok(self
            .data
            .read()
            .await
            .externals
            .values()
            .map(|(e, _)| e.clone())
            .collect())
    }

    async fn get_external(&self, name: &str) -> Result<Option<ExternalVocabulary>, StoreError> {
        self.check()?;
        Ok(self.data.read().await.externals.get(name).map(|(e, _)| e.clone()))
    }

    async fn save_external(
        &self,
        external: &ExternalVocabulary,
        source: &str,
        _triples: &[Triple],
    ) -> Result<(), StoreError> {
        self.check()?;
        self.data
            .write()
            .await
            .externals
            .insert(external.name.clone(), (external.clone(), source.to_string()));
        Ok(())
    }

    async fn get_external_content(&self, name: &str) -> Result<Option<String>, StoreError> {
        self.check()?;
        Ok(self.data.read().await.externals.get(name).map(|(_, s)| s.clone()))
    }

    async fn delete_external(&self, name: &str) -> Result<bool, StoreError> {
        self.check()?;
        Ok(self.data.write().await.externals.remove(name).is_some())
    }

    async fn ping(&self) -> bool {
        !self.fail.load(Ordering::SeqCst)
    }
}
