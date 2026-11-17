//! Mapping between the domain aggregates and RDF triples stored in Fuseki.

use crate::model::{ExternalVocabulary, Status, Version, Vocabulary};
use chrono::{DateTime, SecondsFormat, Utc};
use oxrdf::{Literal, NamedNode, NamedOrBlankNode, Term, Triple};
use std::collections::BTreeMap;

pub const NS: &str = "https://simpl-open.eu/ns/vocabulary-manager#";
pub const URN: &str = "urn:simpl:vocabulary-manager:";
const XSD_DATETIME: &str = "http://www.w3.org/2001/XMLSchema#dateTime";
const XSD_INTEGER: &str = "http://www.w3.org/2001/XMLSchema#integer";
const RDF_TYPE: &str = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

pub fn meta_graph(name: &str) -> String {
    format!("{URN}meta:{name}")
}
pub fn source_graph(name: &str, version: u32) -> String {
    format!("{URN}source:{name}:{version}")
}
pub fn content_graph(name: &str, version: u32) -> String {
    format!("{URN}content:{name}:{version}")
}
pub fn external_meta_graph(name: &str) -> String {
    format!("{URN}external-meta:{name}")
}
pub fn external_source_graph(name: &str) -> String {
    format!("{URN}external-source:{name}")
}
pub fn external_content_graph(name: &str) -> String {
    format!("{URN}external-content:{name}")
}
pub fn vocabulary_iri(name: &str) -> String {
    format!("{URN}vocabulary:{name}")
}
pub fn version_iri(name: &str, version: u32) -> String {
    format!("{URN}vocabulary:{name}:version:{version}")
}
pub fn external_iri(name: &str) -> String {
    format!("{URN}external:{name}")
}

fn p(local: &str) -> NamedNode {
    NamedNode::new_unchecked(format!("{NS}{local}"))
}
fn node(iri: &str) -> NamedOrBlankNode {
    NamedNode::new_unchecked(iri).into()
}
fn s(v: &str) -> Term {
    Literal::new_simple_literal(v).into()
}
fn dt(v: &DateTime<Utc>) -> Term {
    Literal::new_typed_literal(
        v.to_rfc3339_opts(SecondsFormat::Millis, true),
        NamedNode::new_unchecked(XSD_DATETIME),
    )
    .into()
}
fn int(v: u32) -> Term {
    Literal::new_typed_literal(v.to_string(), NamedNode::new_unchecked(XSD_INTEGER)).into()
}
fn t(subject: &str, pred: &str, object: Term) -> Triple {
    Triple::new(node(subject), p(pred), object)
}
fn rdf_type(subject: &str, class: &str) -> Triple {
    Triple::new(
        node(subject),
        NamedNode::new_unchecked(RDF_TYPE),
        NamedNode::new_unchecked(format!("{NS}{class}")),
    )
}

/// Triples of the metadata graph of one vocabulary.
pub fn vocabulary_to_triples(v: &Vocabulary) -> Vec<Triple> {
    let vi = vocabulary_iri(&v.name);
    let mut out = vec![
        rdf_type(&vi, "Vocabulary"),
        t(&vi, "name", s(&v.name)),
        t(&vi, "createdBy", s(&v.created_by)),
        t(&vi, "creationDate", dt(&v.creation_date)),
    ];
    for ver in &v.versions {
        let x = version_iri(&v.name, ver.number);
        out.push(rdf_type(&x, "VocabularyVersion"));
        out.push(t(&x, "vocabulary", NamedNode::new_unchecked(vi.clone()).into()));
        out.push(t(&x, "versionNumber", int(ver.number)));
        out.push(t(&x, "description", s(&ver.description)));
        out.push(t(&x, "changelog", s(&ver.changelog)));
        out.push(t(&x, "status", s(ver.status.as_str())));
        out.push(t(&x, "createdBy", s(&ver.created_by)));
        out.push(t(&x, "creationDate", dt(&ver.creation_date)));
        out.push(t(&x, "lastUpdateDate", dt(&ver.last_update_date)));
    }
    out
}

pub fn external_to_triples(e: &ExternalVocabulary) -> Vec<Triple> {
    let x = external_iri(&e.name);
    let mut out = vec![
        rdf_type(&x, "ExternalVocabulary"),
        t(&x, "name", s(&e.name)),
        t(&x, "namespaceUri", s(&e.namespace_uri)),
        t(&x, "createdBy", s(&e.created_by)),
        t(&x, "creationDate", dt(&e.creation_date)),
    ];
    for a in &e.alias_uris {
        out.push(t(&x, "aliasUri", s(a)));
    }
    if let Some(v) = &e.source_uri {
        out.push(t(&x, "sourceUri", s(v)));
    }
    if let Some(v) = &e.description {
        out.push(t(&x, "description", s(v)));
    }
    out
}

pub fn source_triple(subject: &str, source: &str) -> Triple {
    t(subject, "source", s(source))
}

type Props = BTreeMap<String, Vec<String>>;

/// Group triples by subject: subject -> predicate local name -> literal values / IRIs.
fn group(triples: &[Triple]) -> BTreeMap<String, Props> {
    let mut out: BTreeMap<String, Props> = BTreeMap::new();
    for tr in triples {
        let NamedOrBlankNode::NamedNode(subj) = &tr.subject else {
            continue;
        };
        let pred = tr.predicate.as_str();
        let key = if pred == RDF_TYPE {
            "type".to_string()
        } else if let Some(local) = pred.strip_prefix(NS) {
            local.to_string()
        } else {
            continue;
        };
        let value = match &tr.object {
            Term::Literal(l) => l.value().to_string(),
            Term::NamedNode(n) => n.as_str().strip_prefix(NS).unwrap_or(n.as_str()).to_string(),
            _ => continue,
        };
        out.entry(subj.as_str().to_string())
            .or_default()
            .entry(key)
            .or_default()
            .push(value);
    }
    out
}

fn one<'a>(props: &'a Props, key: &str) -> Result<&'a str, String> {
    props
        .get(key)
        .and_then(|v| v.first())
        .map(String::as_str)
        .ok_or_else(|| format!("missing property {key}"))
}

fn date(props: &Props, key: &str) -> Result<DateTime<Utc>, String> {
    DateTime::parse_from_rfc3339(one(props, key)?)
        .map(|d| d.with_timezone(&Utc))
        .map_err(|e| format!("bad date in {key}: {e}"))
}

fn has_type(props: &Props, class: &str) -> bool {
    props.get("type").is_some_and(|v| v.iter().any(|c| c == class))
}

/// Rebuild vocabulary aggregates from metadata triples.
pub fn triples_to_vocabularies(triples: &[Triple]) -> Result<Vec<Vocabulary>, String> {
    let grouped = group(triples);
    let mut vocabs: BTreeMap<String, Vocabulary> = BTreeMap::new();
    for (subject, props) in &grouped {
        if has_type(props, "Vocabulary") {
            vocabs.insert(
                subject.clone(),
                Vocabulary {
                    name: one(props, "name")?.to_string(),
                    created_by: one(props, "createdBy")?.to_string(),
                    creation_date: date(props, "creationDate")?,
                    versions: vec![],
                },
            );
        }
    }
    for props in grouped.values() {
        if !has_type(props, "VocabularyVersion") {
            continue;
        }
        let parent = one(props, "vocabulary")?;
        let Some(voc) = vocabs.get_mut(parent) else {
            continue;
        };
        voc.versions.push(Version {
            number: one(props, "versionNumber")?
                .parse()
                .map_err(|e| format!("bad version number: {e}"))?,
            description: one(props, "description")?.to_string(),
            changelog: one(props, "changelog")?.to_string(),
            status: one(props, "status")?
                .parse::<Status>()
                .map_err(|_| "bad status".to_string())?,
            created_by: one(props, "createdBy")?.to_string(),
            creation_date: date(props, "creationDate")?,
            last_update_date: date(props, "lastUpdateDate")?,
        });
    }
    let mut out: Vec<Vocabulary> = vocabs
        .into_values()
        .filter(|v| !v.versions.is_empty())
        .collect();
    for v in &mut out {
        v.versions.sort_by_key(|x| x.number);
    }
    Ok(out)
}

pub fn triples_to_externals(triples: &[Triple]) -> Result<Vec<ExternalVocabulary>, String> {
    let grouped = group(triples);
    let mut out = Vec::new();
    for props in grouped.values() {
        if !has_type(props, "ExternalVocabulary") {
            continue;
        }
        out.push(ExternalVocabulary {
            name: one(props, "name")?.to_string(),
            namespace_uri: one(props, "namespaceUri")?.to_string(),
            alias_uris: props.get("aliasUri").cloned().unwrap_or_default(),
            source_uri: props.get("sourceUri").and_then(|v| v.first().cloned()),
            description: props.get("description").and_then(|v| v.first().cloned()),
            created_by: one(props, "createdBy")?.to_string(),
            creation_date: date(props, "creationDate")?,
        });
    }
    Ok(out)
}

/// Serialise triples as N-Triples lines, usable inside SPARQL `INSERT DATA`.
pub fn to_ntriples(triples: &[Triple]) -> String {
    let mut s = String::new();
    for tr in triples {
        s.push_str(&tr.to_string());
        s.push_str(" .\n");
    }
    s
}

#[cfg(test)]
mod tests {
    use super::*;
    use chrono::TimeZone;

    #[test]
    fn vocabulary_roundtrip() {
        let d = Utc.with_ymd_and_hms(2026, 3, 1, 10, 0, 0).unwrap();
        let v = Vocabulary {
            name: "Health".into(),
            created_by: "ga-admin@example.com".into(),
            creation_date: d,
            versions: vec![
                Version {
                    number: 1,
                    description: "first \"quoted\"\nline".into(),
                    changelog: "Initial version".into(),
                    status: Status::Deprecated,
                    created_by: "x".into(),
                    creation_date: d,
                    last_update_date: d,
                },
                Version {
                    number: 2,
                    description: "second".into(),
                    changelog: "More".into(),
                    status: Status::Active,
                    created_by: "x".into(),
                    creation_date: d,
                    last_update_date: d,
                },
            ],
        };
        let nt = to_ntriples(&vocabulary_to_triples(&v));
        let parsed: Vec<Triple> = oxttl::NTriplesParser::new()
            .for_slice(nt.as_bytes())
            .collect::<Result<_, _>>()
            .unwrap();
        assert_eq!(triples_to_vocabularies(&parsed).unwrap(), vec![v]);
    }

    #[test]
    fn external_roundtrip() {
        let e = ExternalVocabulary {
            name: "DublinCoreTerms".into(),
            namespace_uri: "http://purl.org/dc/terms".into(),
            alias_uris: vec!["https://purl.org/dc/terms".into()],
            source_uri: Some("https://example.org/dc.ttl".into()),
            description: None,
            created_by: "u".into(),
            creation_date: Utc.with_ymd_and_hms(2026, 3, 1, 10, 0, 0).unwrap(),
        };
        assert_eq!(triples_to_externals(&external_to_triples(&e)).unwrap(), vec![e]);
    }
}
