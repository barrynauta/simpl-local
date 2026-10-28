//! Vocabulary content validation: syntax (D16), structure (D16) and semantics (D17, D18).
//!
//! The functions here are pure. The service layer loads the referenced vocabularies from the
//! store and passes them in through [`SemanticContext`].

use crate::model::{ExternalVocabulary, Status, Vocabulary, normalize_namespace};
use crate::problem::Issue;
use oxrdf::{NamedOrBlankNode, Term, Triple};
use oxttl::TurtleParser;
use std::collections::{BTreeMap, BTreeSet, HashMap, HashSet};

pub const RDF_TYPE: &str = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
const OWL_ONTOLOGY: &str = "http://www.w3.org/2002/07/owl#Ontology";
const SKOS_CONCEPT_SCHEME: &str = "http://www.w3.org/2004/02/skos/core#ConceptScheme";
const CLASS_TYPES: [&str; 2] = [
    "http://www.w3.org/2002/07/owl#Class",
    "http://www.w3.org/2000/01/rdf-schema#Class",
];
const PROPERTY_TYPES: [&str; 5] = [
    "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property",
    "http://www.w3.org/2002/07/owl#ObjectProperty",
    "http://www.w3.org/2002/07/owl#DatatypeProperty",
    "http://www.w3.org/2002/07/owl#AnnotationProperty",
    "http://www.w3.org/2002/07/owl#FunctionalProperty",
];
const OWL_OBJECT_PROPERTY: &str = "http://www.w3.org/2002/07/owl#ObjectProperty";

/// Namespaces that are never reported as unresolved external references. They are the
/// W3C languages a vocabulary is written in (RDF, RDFS, OWL, XSD) plus SKOS, which D16
/// names as a structural requirement.
pub const BUILTIN_NAMESPACES: [&str; 6] = [
    "http://www.w3.org/1999/02/22-rdf-syntax-ns",
    "http://www.w3.org/2000/01/rdf-schema",
    "http://www.w3.org/2002/07/owl",
    "http://www.w3.org/2001/XMLSchema",
    "http://www.w3.org/XML/1998/namespace",
    "http://www.w3.org/2004/02/skos/core",
];

/// Maximum number of syntax errors reported for one file.
const MAX_SYNTAX_ERRORS: usize = 100;

/// A successfully parsed Turtle document.
#[derive(Debug, Clone)]
pub struct ParsedTurtle {
    pub triples: Vec<Triple>,
    /// Prefix declarations: (prefix name, namespace IRI).
    pub prefixes: Vec<(String, String)>,
}

/// A non-blocking validation warning (OpenAPI `VocabularyValidationWarning`).
#[derive(Debug, Clone, PartialEq, serde::Serialize)]
pub struct Warning {
    pub code: String,
    pub description: String,
    pub location: String,
}

/// Syntax validation (SIMPL-18181, D16): the file must parse as Turtle.
/// All errors are collected, the parser recovers after each error.
pub fn parse_turtle(content: &[u8]) -> Result<ParsedTurtle, Vec<Issue>> {
    let mut parser = TurtleParser::new().for_slice(content);
    let mut triples = Vec::new();
    let mut issues = Vec::new();
    for item in parser.by_ref() {
        match item {
            Ok(t) => triples.push(t),
            Err(e) => {
                if issues.len() < MAX_SYNTAX_ERRORS {
                    let loc = e.location();
                    issues.push(Issue::vocabulary_file(
                        format!("Encountered invalid Turtle syntax: {}", e.message()),
                        format!("line {}, column {}", loc.start.line + 1, loc.start.column + 1),
                    ));
                }
            }
        }
    }
    let prefixes = parser
        .prefixes()
        .map(|(p, iri)| (p.to_string(), iri.to_string()))
        .collect();
    if issues.is_empty() {
        Ok(ParsedTurtle { triples, prefixes })
    } else {
        Err(issues)
    }
}

/// Structure validation (D16): the graph is not empty and declares at least one
/// `owl:Ontology` or `skos:ConceptScheme`.
pub fn check_structure(doc: &ParsedTurtle) -> Vec<Issue> {
    if doc.triples.is_empty() {
        return vec![Issue::vocabulary_file(
            "The vocabulary graph is empty. It must contain at least one statement.",
            "document",
        )];
    }
    let has_root = doc.triples.iter().any(|t| {
        t.predicate.as_str() == RDF_TYPE
            && matches!(&t.object, Term::NamedNode(n) if n.as_str() == OWL_ONTOLOGY || n.as_str() == SKOS_CONCEPT_SCHEME)
    });
    if has_root {
        vec![]
    } else {
        vec![Issue::vocabulary_file(
            "The vocabulary must declare at least one owl:Ontology or skos:ConceptScheme.",
            "document",
        )]
    }
}

/// External vocabularies only need to parse and contain at least one statement.
pub fn check_not_empty(doc: &ParsedTurtle) -> Vec<Issue> {
    if doc.triples.is_empty() {
        vec![Issue::vocabulary_file(
            "The vocabulary graph is empty. It must contain at least one statement.",
            "document",
        )]
    } else {
        vec![]
    }
}

/// A reference to an internal vocabulary found in a document (D18).
#[derive(Debug, Clone, PartialEq)]
pub struct InternalRef {
    pub name: String,
    /// `None` means "the latest version".
    pub version: Option<u32>,
    /// Namespace as written, e.g. `{base}/Name/1#`.
    pub namespace: String,
    /// Local names of the terms used from that namespace.
    pub terms: BTreeSet<String>,
    /// Set when the reference is malformed (bad name or version segment).
    pub malformed: Option<String>,
}

/// Every IRI that occurs in the document (subject, predicate, object, datatype).
fn all_iris(doc: &ParsedTurtle) -> Vec<String> {
    let mut out = Vec::new();
    for t in &doc.triples {
        if let NamedOrBlankNode::NamedNode(n) = &t.subject {
            out.push(n.as_str().to_string());
        }
        out.push(t.predicate.as_str().to_string());
        match &t.object {
            Term::NamedNode(n) => out.push(n.as_str().to_string()),
            Term::Literal(l) => out.push(l.datatype().as_str().to_string()),
            _ => {}
        }
    }
    out
}

/// (name, version, namespace, term, error) of an internal reference IRI.
type ParsedRef = (String, Option<u32>, String, Option<String>, Option<String>);

/// Parse an IRI against the internal reference convention (D18):
/// `{base}/{Name}/{n}#term` or `{base}/{Name}#term`. Returns (name, version, namespace, term, error).
fn parse_internal_iri(
    iri: &str,
    base: &str,
) -> Option<ParsedRef> {
    let base = base.trim_end_matches('/');
    let rest = iri.strip_prefix(base)?.strip_prefix('/')?;
    let (path, term) = match rest.split_once('#') {
        Some((p, t)) => (p, if t.is_empty() { None } else { Some(t.to_string()) }),
        None => (rest, None),
    };
    let segments: Vec<&str> = path.split('/').collect();
    let name = segments[0].to_string();
    let mut error = None;
    if !crate::validation::is_valid_name(&name) {
        error = Some(format!("'{name}' is not a valid vocabulary name"));
    }
    let version = match segments.len() {
        1 => None,
        2 if segments[1].is_empty() => None,
        2 => match segments[1].parse::<u32>() {
            Ok(n) if n >= 1 => Some(n),
            _ => {
                error = Some(format!("'{}' is not a valid version number", segments[1]));
                None
            }
        },
        _ => {
            error = Some("the reference has too many path segments".to_string());
            None
        }
    };
    let namespace = match version {
        Some(v) => format!("{base}/{name}/{v}#"),
        None if error.is_none() => format!("{base}/{name}#"),
        None => format!("{base}/{path}"),
    };
    Some((name, version, namespace, term, error))
}

/// Collect all references to internal vocabularies (D18), grouped by namespace.
pub fn collect_internal_refs(doc: &ParsedTurtle, base: &str) -> Vec<InternalRef> {
    let mut map: BTreeMap<String, InternalRef> = BTreeMap::new();
    let mut add = |iri: &str| {
        if let Some((name, version, namespace, term, malformed)) = parse_internal_iri(iri, base) {
            let entry = map.entry(namespace.clone()).or_insert_with(|| InternalRef {
                name,
                version,
                namespace,
                terms: BTreeSet::new(),
                malformed,
            });
            if let Some(t) = term {
                entry.terms.insert(t);
            }
        }
    };
    for iri in all_iris(doc) {
        add(&iri);
    }
    for (_, ns) in &doc.prefixes {
        add(ns);
    }
    map.into_values().collect()
}

/// The local name of an IRI: the part after the last `#` or `/`.
pub fn local_name(iri: &str) -> &str {
    let idx = iri.rfind(['#', '/']).map(|i| i + 1).unwrap_or(0);
    &iri[idx..]
}

/// The namespace of an IRI: everything up to and including the last `#` or `/`.
pub fn namespace_of(iri: &str) -> &str {
    let idx = iri.rfind(['#', '/']).map(|i| i + 1).unwrap_or(iri.len());
    &iri[..idx]
}

/// Local names of all IRI subjects of a document: the terms a vocabulary version defines.
pub fn defined_terms(doc: &ParsedTurtle) -> HashSet<String> {
    doc.triples
        .iter()
        .filter_map(|t| match &t.subject {
            NamedOrBlankNode::NamedNode(n) => Some(local_name(n.as_str()).to_string()),
            _ => None,
        })
        .filter(|s| !s.is_empty())
        .collect()
}

/// Everything semantic validation needs to know about other vocabularies.
#[derive(Debug, Default)]
pub struct SemanticContext {
    /// Base IRI of the internal reference convention (D18).
    pub base: String,
    /// Referenced internal vocabularies that exist, by name.
    pub vocabularies: HashMap<String, Vocabulary>,
    /// Terms defined by referenced versions, by (name, version).
    pub terms: HashMap<(String, u32), HashSet<String>>,
    /// Registered external vocabularies.
    pub externals: Vec<ExternalVocabulary>,
    /// Name of the vocabulary being created or versioned. Its own namespace
    /// (`{base}/{Name}#` or `{base}/{Name}/{n}#`) is not a reference (D18, clarified 28 Oct 2026).
    pub self_name: Option<String>,
}

fn prefix_location(doc: &ParsedTurtle, namespace: &str) -> String {
    let wanted = normalize_namespace(namespace);
    match doc
        .prefixes
        .iter()
        .find(|(_, ns)| normalize_namespace(ns) == wanted)
    {
        Some((p, ns)) => format!("prefix '{p}' -> '{ns}'"),
        None => format!("namespace '{namespace}'"),
    }
}

fn is_external_registered(namespace: &str, externals: &[ExternalVocabulary]) -> bool {
    let ns = normalize_namespace(namespace);
    externals.iter().any(|e| {
        std::iter::once(&e.namespace_uri)
            .chain(e.alias_uris.iter())
            .any(|r| {
                !r.is_empty()
                    && (ns == *r
                        || ns.starts_with(&format!("{r}/"))
                        || ns.starts_with(&format!("{r}#")))
            })
    })
}

/// Semantic validation (SIMPL-18182/18183/18184, D17). Returns (errors, warnings).
/// All checks run; nothing stops at the first issue.
pub fn check_semantics(doc: &ParsedTurtle, ctx: &SemanticContext) -> (Vec<Issue>, Vec<Warning>) {
    let mut errors = Vec::new();
    let mut warnings = Vec::new();

    // (a), (b), (c): references to internal vocabularies.
    for r in collect_internal_refs(doc, &ctx.base) {
        if ctx.self_name.as_deref() == Some(r.name.as_str()) {
            continue;
        }
        let location = prefix_location(doc, &r.namespace);
        if let Some(why) = &r.malformed {
            errors.push(Issue::vocabulary_file(
                format!(
                    "Invalid reference to an internal vocabulary: {why}. Use '{}/{{Name}}/{{version}}#'.",
                    ctx.base.trim_end_matches('/')
                ),
                location,
            ));
            continue;
        }
        let Some(voc) = ctx.vocabularies.get(&r.name) else {
            errors.push(Issue::vocabulary_file(
                format!("Referenced internal vocabulary '{}' does not exist.", r.name),
                location,
            ));
            continue;
        };
        let version = match r.version {
            Some(n) => match voc.version(n) {
                Some(v) => v,
                None => {
                    errors.push(Issue::vocabulary_file(
                        format!(
                            "Referenced internal vocabulary '{}' version {} does not exist.",
                            r.name, n
                        ),
                        location,
                    ));
                    continue;
                }
            },
            None => voc.latest(),
        };
        if version.status == Status::Deprecated {
            let advice = match voc.latest_active() {
                Some(a) => format!(
                    " Use the latest ACTIVE version {} ('{}/{}/{}#') instead.",
                    a.number,
                    ctx.base.trim_end_matches('/'),
                    voc.name,
                    a.number
                ),
                None => " The vocabulary has no ACTIVE version.".to_string(),
            };
            errors.push(Issue::vocabulary_file(
                format!(
                    "Referenced internal vocabulary '{}' version {} is deprecated.{}",
                    r.name, version.number, advice
                ),
                location.clone(),
            ));
        }
        if let Some(defined) = ctx.terms.get(&(r.name.clone(), version.number)) {
            for term in &r.terms {
                if !defined.contains(term) {
                    errors.push(Issue::vocabulary_file(
                        format!(
                            "Term '{}' does not exist in referenced internal vocabulary '{}' version {}.",
                            term, r.name, version.number
                        ),
                        format!("resource '{}{}'", r.namespace, term),
                    ));
                }
            }
        }
    }

    // (d) one IRI declared both as a class and as a property.
    let mut classes: BTreeSet<&str> = BTreeSet::new();
    let mut properties: BTreeSet<&str> = BTreeSet::new();
    let mut object_properties: HashSet<&str> = HashSet::new();
    for t in &doc.triples {
        if t.predicate.as_str() != RDF_TYPE {
            continue;
        }
        let (NamedOrBlankNode::NamedNode(s), Term::NamedNode(o)) = (&t.subject, &t.object) else {
            continue;
        };
        if CLASS_TYPES.contains(&o.as_str()) {
            classes.insert(s.as_str());
        }
        if PROPERTY_TYPES.contains(&o.as_str()) {
            properties.insert(s.as_str());
        }
        if o.as_str() == OWL_OBJECT_PROPERTY {
            object_properties.insert(s.as_str());
        }
    }
    for iri in classes.intersection(&properties) {
        errors.push(Issue::vocabulary_file(
            format!("'{iri}' is declared both as a class and as a property."),
            format!("resource '{iri}'"),
        ));
    }

    // OpenAPI example: a literal value for an object property.
    for t in &doc.triples {
        if let Term::Literal(l) = &t.object
            && object_properties.contains(t.predicate.as_str())
        {
            let subject = match &t.subject {
                NamedOrBlankNode::NamedNode(n) => n.as_str().to_string(),
                NamedOrBlankNode::BlankNode(b) => format!("_:{}", b.as_str()),
            };
            errors.push(Issue::vocabulary_file(
                "Literal value for object property",
                format!("statement '{} {} {}'", subject, t.predicate.as_str(), l.value()),
            ));
        }
    }

    // Warnings: namespaces that are neither internal, built in, defined here, nor registered.
    let internal_base = normalize_namespace(&ctx.base);
    let own: HashSet<String> = doc
        .triples
        .iter()
        .filter_map(|t| match &t.subject {
            NamedOrBlankNode::NamedNode(n) => Some(normalize_namespace(namespace_of(n.as_str()))),
            _ => None,
        })
        .collect();
    let mut candidates: BTreeMap<String, String> = BTreeMap::new();
    for (_, ns) in &doc.prefixes {
        candidates.entry(normalize_namespace(ns)).or_insert_with(|| ns.clone());
    }
    for iri in all_iris(doc) {
        let ns = namespace_of(&iri).to_string();
        if ns.is_empty() {
            continue;
        }
        candidates.entry(normalize_namespace(&ns)).or_insert(ns);
    }
    for (norm, ns) in candidates {
        if norm.is_empty()
            || own.contains(&norm)
            || BUILTIN_NAMESPACES.contains(&norm.as_str())
            || norm == internal_base
            || norm.starts_with(&format!("{internal_base}/"))
            || is_external_registered(&ns, &ctx.externals)
        {
            continue;
        }
        warnings.push(Warning {
            code: "externalReferenceSkipped".into(),
            description: format!(
                "Cannot resolve external namespace '{ns}' because no cached external vocabulary is registered."
            ),
            location: prefix_location(doc, &ns),
        });
    }

    (errors, warnings)
}

/// Vocabulary and external vocabulary names: `^[A-Z][a-zA-Z0-9]*$`, 3 to 64 characters.
pub fn is_valid_name(name: &str) -> bool {
    let len = name.chars().count();
    (3..=64).contains(&len)
        && name.chars().next().is_some_and(|c| c.is_ascii_uppercase())
        && name.chars().all(|c| c.is_ascii_alphanumeric())
}

/// Absolute URI check for `format: uri` fields.
pub fn is_absolute_uri(value: &str) -> bool {
    oxiri::Iri::parse(value.trim()).is_ok()
}

#[cfg(test)]
mod tests {
    use super::*;
    use chrono::Utc;

    const BASE: &str = "https://vocabulary.simpl.local/vocabularies";

    fn parse(s: &str) -> ParsedTurtle {
        parse_turtle(s.as_bytes()).expect("valid turtle")
    }

    fn vocab(name: &str, statuses: &[Status]) -> Vocabulary {
        let now = Utc::now();
        Vocabulary {
            name: name.into(),
            created_by: "u".into(),
            creation_date: now,
            versions: statuses
                .iter()
                .enumerate()
                .map(|(i, s)| crate::model::Version {
                    number: i as u32 + 1,
                    description: "d".into(),
                    changelog: "c".into(),
                    status: *s,
                    created_by: "u".into(),
                    creation_date: now,
                    last_update_date: now,
                })
                .collect(),
        }
    }

    #[test]
    fn valid_turtle_parses() {
        let doc = parse("@prefix ex: <http://ex.org/> . ex:a a ex:B .");
        assert_eq!(doc.triples.len(), 1);
        assert_eq!(doc.prefixes, vec![("ex".to_string(), "http://ex.org/".to_string())]);
    }

    #[test]
    fn syntax_errors_are_all_reported_with_line_and_column() {
        let src = "@prefix ex: <http://ex.org/> .\nex:a ex:b ex:c\nex:d ex:e ex:f .\nex:g ex:h \"unterminated .\n";
        let issues = parse_turtle(src.as_bytes()).unwrap_err();
        assert!(!issues.is_empty());
        for i in &issues {
            assert!(i.location.as_ref().unwrap().starts_with("line "));
            assert_eq!(i.name.as_deref(), Some("vocabularyFile"));
        }
        let two = "@prefix ex: <http://ex.org/> .\nex:a ex:b .\nex:c ex:d ex:e .\nex:f ex:g .\n";
        let issues = parse_turtle(two.as_bytes()).unwrap_err();
        assert!(issues.len() >= 2, "parser must continue after the first error: {issues:?}");
    }

    #[test]
    fn structure_requires_ontology_or_concept_scheme() {
        assert_eq!(check_structure(&parse("")).len(), 1);
        let no_root = parse("<http://ex.org/a> <http://ex.org/b> <http://ex.org/c> .");
        assert_eq!(check_structure(&no_root)[0].location.as_deref(), Some("document"));
        let ont = parse("<http://ex.org/o> a <http://www.w3.org/2002/07/owl#Ontology> .");
        assert!(check_structure(&ont).is_empty());
        let cs = parse("@prefix skos: <http://www.w3.org/2004/02/skos/core#> . <http://ex.org/s> a skos:ConceptScheme .");
        assert!(check_structure(&cs).is_empty());
    }

    #[test]
    fn parse_internal_reference_forms() {
        let r = parse_internal_iri(&format!("{BASE}/Health/2#Patient"), BASE).unwrap();
        assert_eq!((r.0.as_str(), r.1, r.3.as_deref()), ("Health", Some(2), Some("Patient")));
        let r = parse_internal_iri(&format!("{BASE}/Health#Patient"), BASE).unwrap();
        assert_eq!((r.1, r.2.as_str()), (None, format!("{BASE}/Health#").as_str()));
        let r = parse_internal_iri(&format!("{BASE}/Health/x#P"), BASE).unwrap();
        assert!(r.4.is_some());
        assert!(parse_internal_iri("http://other.org/x", BASE).is_none());
    }

    fn ctx_with(v: Vocabulary, terms: &[&str]) -> SemanticContext {
        let mut ctx = SemanticContext { base: BASE.into(), ..Default::default() };
        for ver in &v.versions {
            ctx.terms.insert(
                (v.name.clone(), ver.number),
                terms.iter().map(|s| s.to_string()).collect(),
            );
        }
        ctx.vocabularies.insert(v.name.clone(), v);
        ctx
    }

    fn doc_ref(ns_suffix: &str, term: &str) -> ParsedTurtle {
        parse(&format!(
            "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n@prefix ref: <{BASE}/{ns_suffix}> .\n<http://ex.org/o> a owl:Ontology .\n<http://ex.org/C> a owl:Class ; <http://www.w3.org/2000/01/rdf-schema#subClassOf> ref:{term} ."
        ))
    }

    #[test]
    fn reference_to_active_version_with_existing_term_is_valid() {
        let ctx = ctx_with(vocab("Health", &[Status::Active]), &["Patient"]);
        let (errors, warnings) = check_semantics(&doc_ref("Health/1#", "Patient"), &ctx);
        assert!(errors.is_empty(), "{errors:?}");
        assert!(warnings.is_empty(), "{warnings:?}");
    }

    #[test]
    fn missing_term_is_an_error() {
        let ctx = ctx_with(vocab("Health", &[Status::Active]), &["Patient"]);
        let (errors, _) = check_semantics(&doc_ref("Health/1#", "Doctor"), &ctx);
        assert_eq!(errors.len(), 1);
        assert!(errors[0].detail.contains("Doctor"));
    }

    #[test]
    fn deprecated_version_is_an_error_and_points_to_active() {
        let ctx = ctx_with(vocab("Health", &[Status::Deprecated, Status::Active]), &["Patient"]);
        let (errors, _) = check_semantics(&doc_ref("Health/1#", "Patient"), &ctx);
        assert_eq!(errors.len(), 1);
        assert!(errors[0].detail.contains("deprecated"));
        assert!(errors[0].detail.contains("version 2"));
        assert!(errors[0].location.as_ref().unwrap().starts_with("prefix 'ref'"));
    }

    #[test]
    fn missing_vocabulary_or_version_is_an_error() {
        let ctx = ctx_with(vocab("Health", &[Status::Active]), &["Patient"]);
        let (errors, _) = check_semantics(&doc_ref("Health/7#", "Patient"), &ctx);
        assert_eq!(errors.len(), 1);
        assert!(errors[0].detail.contains("version 7 does not exist"));
        let (errors, _) = check_semantics(&doc_ref("Nothing/1#", "Patient"), &ctx);
        assert!(errors[0].detail.contains("does not exist"));
    }

    #[test]
    fn unversioned_reference_resolves_to_latest() {
        let ctx = ctx_with(vocab("Health", &[Status::Deprecated, Status::Active]), &["Patient"]);
        let (errors, _) = check_semantics(&doc_ref("Health#", "Patient"), &ctx);
        assert!(errors.is_empty(), "{errors:?}");
    }

    #[test]
    fn own_namespace_is_not_a_reference() {
        // D18 clarification: Health defines its terms in its own namespace, versioned or not.
        let mut ctx = ctx_with(vocab("Health", &[Status::Deprecated, Status::Active]), &["Patient"]);
        ctx.self_name = Some("Health".into());
        for ns in ["Health/1#", "Health/3#", "Health#"] {
            let (errors, warnings) = check_semantics(&doc_ref(ns, "Brandnew"), &ctx);
            assert!(errors.is_empty(), "{ns}: {errors:?}");
            assert!(warnings.is_empty(), "{ns}: {warnings:?}");
        }
        // A new vocabulary that does not exist yet may also use its own namespace.
        let ctx = SemanticContext { base: BASE.into(), self_name: Some("Fresh".into()), ..Default::default() };
        let (errors, _) = check_semantics(&doc_ref("Fresh/1#", "Anything"), &ctx);
        assert!(errors.is_empty(), "{errors:?}");
        // Other vocabularies' namespaces still count.
        let (errors, _) = check_semantics(&doc_ref("Other/1#", "X"), &ctx);
        assert_eq!(errors.len(), 1);
    }

    #[test]
    fn class_and_property_clash_is_an_error() {
        let doc = parse("@prefix owl: <http://www.w3.org/2002/07/owl#> .\n<http://ex.org/o> a owl:Ontology .\n<http://ex.org/X> a owl:Class , owl:ObjectProperty .");
        let ctx = SemanticContext { base: BASE.into(), ..Default::default() };
        let (errors, _) = check_semantics(&doc, &ctx);
        assert_eq!(errors.len(), 1);
        assert_eq!(errors[0].location.as_deref(), Some("resource 'http://ex.org/X'"));
    }

    #[test]
    fn literal_for_object_property_is_an_error() {
        let doc = parse("@prefix owl: <http://www.w3.org/2002/07/owl#> .\n<http://ex.org/o> a owl:Ontology .\n<http://ex.org/p> a owl:ObjectProperty .\n<http://ex.org/s> <http://ex.org/p> \"lit\" .");
        let ctx = SemanticContext { base: BASE.into(), ..Default::default() };
        let (errors, _) = check_semantics(&doc, &ctx);
        assert_eq!(errors.len(), 1);
        assert_eq!(errors[0].detail, "Literal value for object property");
    }

    #[test]
    fn unknown_external_namespace_gives_warning_unless_registered() {
        let doc = parse("@prefix owl: <http://www.w3.org/2002/07/owl#> .\n@prefix dcterms: <http://purl.org/dc/terms/> .\n<http://ex.org/o> a owl:Ontology ; dcterms:title \"x\" .");
        let mut ctx = SemanticContext { base: BASE.into(), ..Default::default() };
        let (errors, warnings) = check_semantics(&doc, &ctx);
        assert!(errors.is_empty());
        assert_eq!(warnings.len(), 1);
        assert_eq!(warnings[0].code, "externalReferenceSkipped");
        assert_eq!(warnings[0].location, "prefix 'dcterms' -> 'http://purl.org/dc/terms/'");
        ctx.externals.push(ExternalVocabulary {
            name: "DublinCoreTerms".into(),
            namespace_uri: "http://purl.org/dc/terms".into(),
            alias_uris: vec![],
            source_uri: None,
            description: None,
            created_by: "u".into(),
            creation_date: Utc::now(),
        });
        let (_, warnings) = check_semantics(&doc, &ctx);
        assert!(warnings.is_empty());
    }

    #[test]
    fn names_follow_the_pattern() {
        assert!(is_valid_name("HealthcareVocabulary"));
        assert!(is_valid_name("Abc"));
        assert!(!is_valid_name("Ab"));
        assert!(!is_valid_name("healthcare"));
        assert!(!is_valid_name("Health-care"));
        assert!(!is_valid_name(&format!("A{}", "b".repeat(64))));
    }

    #[test]
    fn local_name_and_namespace() {
        assert_eq!(local_name("http://x.org/ns#Term"), "Term");
        assert_eq!(namespace_of("http://x.org/ns/Term"), "http://x.org/ns/");
    }
}
