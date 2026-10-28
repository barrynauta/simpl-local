//! Apache Jena Fuseki adapter (D27), over SPARQL 1.1 Query and Update.
//!
//! Layout: one named graph with the metadata of each vocabulary, one graph with the
//! original Turtle text of each version, and one graph with the parsed triples of each
//! version. Every write is a single SPARQL Update request, which Fuseki runs in one
//! transaction, so a new version and the deprecation of the older ones are atomic (D9).

use super::rdf_mapping::*;
use super::{NewContent, Store, StoreError};
use crate::model::{ExternalVocabulary, Vocabulary};
use async_trait::async_trait;
use oxrdf::{Term, Triple};
use std::time::Duration;

pub struct FusekiStore {
    client: reqwest::Client,
    query_url: String,
    update_url: String,
    auth: Option<(String, String)>,
}

impl FusekiStore {
    /// `base_url` like `http://fuseki:3030`, `dataset` like `vocab`.
    pub fn new(base_url: &str, dataset: &str, auth: Option<(String, String)>) -> Self {
        let base = base_url.trim_end_matches('/');
        FusekiStore {
            client: reqwest::Client::builder()
                .timeout(Duration::from_secs(60))
                .build()
                .expect("http client"),
            query_url: format!("{base}/{dataset}/query"),
            update_url: format!("{base}/{dataset}/update"),
            auth,
        }
    }

    fn with_auth(&self, rb: reqwest::RequestBuilder) -> reqwest::RequestBuilder {
        match &self.auth {
            Some((u, p)) => rb.basic_auth(u, Some(p)),
            None => rb,
        }
    }

    async fn construct(&self, query: String) -> Result<Vec<Triple>, StoreError> {
        let resp = self
            .with_auth(self.client.post(&self.query_url))
            .header("Accept", "application/n-triples")
            .form(&[("query", query)])
            .send()
            .await
            .map_err(|e| StoreError::Unavailable(e.to_string()))?;
        if !resp.status().is_success() {
            let status = resp.status();
            let body = resp.text().await.unwrap_or_default();
            return Err(StoreError::Other(format!("query failed: {status} {body}")));
        }
        let body = resp
            .bytes()
            .await
            .map_err(|e| StoreError::Unavailable(e.to_string()))?;
        oxttl::NTriplesParser::new()
            .for_slice(&body)
            .collect::<Result<Vec<_>, _>>()
            .map_err(|e| StoreError::Other(format!("bad n-triples from store: {e}")))
    }

    async fn update(&self, update: String) -> Result<(), StoreError> {
        let resp = self
            .with_auth(self.client.post(&self.update_url))
            .header("Content-Type", "application/sparql-update")
            .body(update)
            .send()
            .await
            .map_err(|e| StoreError::Unavailable(e.to_string()))?;
        if !resp.status().is_success() {
            let status = resp.status();
            let body = resp.text().await.unwrap_or_default();
            return Err(StoreError::Other(format!("update failed: {status} {body}")));
        }
        Ok(())
    }

    async fn source_literal(&self, graph: &str) -> Result<Option<String>, StoreError> {
        let triples = self
            .construct(format!(
                "CONSTRUCT {{ ?s ?p ?o }} WHERE {{ GRAPH <{graph}> {{ ?s ?p ?o }} }}"
            ))
            .await?;
        Ok(triples.into_iter().find_map(|t| match t.object {
            Term::Literal(l) => Some(l.value().to_string()),
            _ => None,
        }))
    }
}

fn graph_construct_prefix(prefix: &str) -> String {
    format!(
        "CONSTRUCT {{ ?s ?p ?o }} WHERE {{ GRAPH ?g {{ ?s ?p ?o }} FILTER(STRSTARTS(STR(?g), \"{prefix}\")) }}"
    )
}

#[async_trait]
impl Store for FusekiStore {
    async fn list_vocabularies(&self) -> Result<Vec<Vocabulary>, StoreError> {
        let triples = self
            .construct(graph_construct_prefix(&format!("{URN}meta:")))
            .await?;
        triples_to_vocabularies(&triples).map_err(StoreError::Other)
    }

    async fn get_vocabulary(&self, name: &str) -> Result<Option<Vocabulary>, StoreError> {
        let g = meta_graph(name);
        let triples = self
            .construct(format!(
                "CONSTRUCT {{ ?s ?p ?o }} WHERE {{ GRAPH <{g}> {{ ?s ?p ?o }} }}"
            ))
            .await?;
        Ok(triples_to_vocabularies(&triples)
            .map_err(StoreError::Other)?
            .into_iter()
            .find(|v| v.name == name))
    }

    async fn save_vocabulary(
        &self,
        vocabulary: &Vocabulary,
        new_content: Option<&NewContent>,
    ) -> Result<(), StoreError> {
        let g = meta_graph(&vocabulary.name);
        let mut update = format!(
            "DROP SILENT GRAPH <{g}> ;\nINSERT DATA {{ GRAPH <{g}> {{\n{}}} }}",
            to_ntriples(&vocabulary_to_triples(vocabulary))
        );
        if let Some(c) = new_content {
            let sg = source_graph(&vocabulary.name, c.version);
            let cg = content_graph(&vocabulary.name, c.version);
            let src = source_triple(&version_iri(&vocabulary.name, c.version), &c.source);
            update.push_str(&format!(
                " ;\nDROP SILENT GRAPH <{sg}> ;\nDROP SILENT GRAPH <{cg}> ;\nINSERT DATA {{ GRAPH <{sg}> {{\n{}}} GRAPH <{cg}> {{\n{}}} }}",
                to_ntriples(&[src]),
                to_ntriples(&c.triples)
            ));
        }
        self.update(update).await
    }

    async fn get_content(&self, name: &str, version: u32) -> Result<Option<String>, StoreError> {
        self.source_literal(&source_graph(name, version)).await
    }

    async fn list_external(&self) -> Result<Vec<ExternalVocabulary>, StoreError> {
        let triples = self
            .construct(graph_construct_prefix(&format!("{URN}external-meta:")))
            .await?;
        triples_to_externals(&triples).map_err(StoreError::Other)
    }

    async fn get_external(&self, name: &str) -> Result<Option<ExternalVocabulary>, StoreError> {
        let g = external_meta_graph(name);
        let triples = self
            .construct(format!(
                "CONSTRUCT {{ ?s ?p ?o }} WHERE {{ GRAPH <{g}> {{ ?s ?p ?o }} }}"
            ))
            .await?;
        Ok(triples_to_externals(&triples)
            .map_err(StoreError::Other)?
            .into_iter()
            .find(|e| e.name == name))
    }

    async fn save_external(
        &self,
        external: &ExternalVocabulary,
        source: &str,
        triples: &[Triple],
    ) -> Result<(), StoreError> {
        let mg = external_meta_graph(&external.name);
        let sg = external_source_graph(&external.name);
        let cg = external_content_graph(&external.name);
        let src = source_triple(&external_iri(&external.name), source);
        let update = format!(
            "DROP SILENT GRAPH <{mg}> ;\nDROP SILENT GRAPH <{sg}> ;\nDROP SILENT GRAPH <{cg}> ;\nINSERT DATA {{ GRAPH <{mg}> {{\n{}}} GRAPH <{sg}> {{\n{}}} GRAPH <{cg}> {{\n{}}} }}",
            to_ntriples(&external_to_triples(external)),
            to_ntriples(&[src]),
            to_ntriples(triples)
        );
        self.update(update).await
    }

    async fn get_external_content(&self, name: &str) -> Result<Option<String>, StoreError> {
        self.source_literal(&external_source_graph(name)).await
    }

    async fn delete_external(&self, name: &str) -> Result<bool, StoreError> {
        if self.get_external(name).await?.is_none() {
            return Ok(false);
        }
        let update = format!(
            "DROP SILENT GRAPH <{}> ;\nDROP SILENT GRAPH <{}> ;\nDROP SILENT GRAPH <{}>",
            external_meta_graph(name),
            external_source_graph(name),
            external_content_graph(name)
        );
        self.update(update).await?;
        Ok(true)
    }

    async fn ping(&self) -> bool {
        match self
            .with_auth(self.client.post(&self.query_url))
            .header("Accept", "application/sparql-results+json")
            .form(&[("query", "ASK {}")])
            .timeout(Duration::from_secs(3))
            .send()
            .await
        {
            Ok(r) => r.status().is_success(),
            Err(_) => false,
        }
    }
}
