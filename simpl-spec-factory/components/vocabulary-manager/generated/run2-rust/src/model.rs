//! Domain model: internal vocabularies, their versions, and cached external vocabularies.

use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use std::fmt;
use std::str::FromStr;

/// Status of a vocabulary or a vocabulary version (OpenAPI `VocabularyStatus`).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize, PartialOrd, Ord)]
#[serde(rename_all = "UPPERCASE")]
pub enum Status {
    Active,
    Deprecated,
}

impl Status {
    pub fn as_str(&self) -> &'static str {
        match self {
            Status::Active => "ACTIVE",
            Status::Deprecated => "DEPRECATED",
        }
    }
}

impl fmt::Display for Status {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(self.as_str())
    }
}

impl FromStr for Status {
    type Err = ();
    fn from_str(s: &str) -> Result<Self, Self::Err> {
        match s {
            "ACTIVE" => Ok(Status::Active),
            "DEPRECATED" => Ok(Status::Deprecated),
            _ => Err(()),
        }
    }
}

/// One version of an internal vocabulary. The Turtle content is stored separately.
#[derive(Debug, Clone, PartialEq)]
pub struct Version {
    pub number: u32,
    pub description: String,
    pub changelog: String,
    pub status: Status,
    pub created_by: String,
    pub creation_date: DateTime<Utc>,
    pub last_update_date: DateTime<Utc>,
}

/// An internal vocabulary with all its versions (the aggregate that is stored and loaded as a unit).
#[derive(Debug, Clone, PartialEq)]
pub struct Vocabulary {
    pub name: String,
    pub created_by: String,
    pub creation_date: DateTime<Utc>,
    /// Ordered by version number, ascending. Never empty.
    pub versions: Vec<Version>,
}

impl Vocabulary {
    pub fn latest(&self) -> &Version {
        self.versions
            .iter()
            .max_by_key(|v| v.number)
            .expect("a vocabulary always has at least one version")
    }

    pub fn version(&self, number: u32) -> Option<&Version> {
        self.versions.iter().find(|v| v.number == number)
    }

    pub fn version_mut(&mut self, number: u32) -> Option<&mut Version> {
        self.versions.iter_mut().find(|v| v.number == number)
    }

    /// D10: the vocabulary is ACTIVE as long as one of its versions is ACTIVE.
    pub fn status(&self) -> Status {
        if self.versions.iter().any(|v| v.status == Status::Active) {
            Status::Active
        } else {
            Status::Deprecated
        }
    }

    /// The highest ACTIVE version, if any (used for `latestVersionHref`, SIMPL-18559).
    pub fn latest_active(&self) -> Option<&Version> {
        self.versions
            .iter()
            .filter(|v| v.status == Status::Active)
            .max_by_key(|v| v.number)
    }

    /// Last change of the vocabulary or any of its versions.
    pub fn last_update_date(&self) -> DateTime<Utc> {
        self.versions
            .iter()
            .map(|v| v.last_update_date)
            .max()
            .unwrap_or(self.creation_date)
    }

    pub fn next_version_number(&self) -> u32 {
        self.latest().number + 1
    }
}

/// A cached copy of an external vocabulary, used as a validation dependency.
#[derive(Debug, Clone, PartialEq)]
pub struct ExternalVocabulary {
    pub name: String,
    /// Normalised canonical namespace URI (no trailing `/` or `#`).
    pub namespace_uri: String,
    /// Normalised alias URIs.
    pub alias_uris: Vec<String>,
    pub source_uri: Option<String>,
    pub description: Option<String>,
    pub created_by: String,
    pub creation_date: DateTime<Utc>,
}

/// Normalise a namespace URI for comparison: trim whitespace and trailing `/` or `#`.
pub fn normalize_namespace(uri: &str) -> String {
    let mut s = uri.trim();
    while let Some(stripped) = s.strip_suffix('/').or_else(|| s.strip_suffix('#')) {
        s = stripped;
    }
    s.to_string()
}

#[cfg(test)]
mod tests {
    use super::*;
    use chrono::TimeZone;

    fn v(number: u32, status: Status) -> Version {
        let t = Utc.with_ymd_and_hms(2026, 1, number, 0, 0, 0).unwrap();
        Version {
            number,
            description: format!("d{number}"),
            changelog: "c".into(),
            status,
            created_by: "u".into(),
            creation_date: t,
            last_update_date: t,
        }
    }

    #[test]
    fn vocabulary_status_is_active_if_any_version_active() {
        let mut voc = Vocabulary {
            name: "Abc".into(),
            created_by: "u".into(),
            creation_date: Utc::now(),
            versions: vec![v(1, Status::Deprecated), v(2, Status::Active)],
        };
        assert_eq!(voc.status(), Status::Active);
        voc.versions[1].status = Status::Deprecated;
        assert_eq!(voc.status(), Status::Deprecated);
        assert_eq!(voc.next_version_number(), 3);
        assert!(voc.latest_active().is_none());
    }

    #[test]
    fn normalize_strips_trailing_separators() {
        assert_eq!(normalize_namespace(" http://purl.org/dc/terms/ "), "http://purl.org/dc/terms");
        assert_eq!(normalize_namespace("http://x.org/ns#"), "http://x.org/ns");
        assert_eq!(normalize_namespace("http://x.org/ns"), "http://x.org/ns");
    }

    #[test]
    fn status_parse() {
        assert_eq!("ACTIVE".parse::<Status>(), Ok(Status::Active));
        assert!("active".parse::<Status>().is_err());
    }
}
