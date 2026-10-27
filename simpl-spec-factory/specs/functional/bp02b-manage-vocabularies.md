<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP02 - Configuration of data space Governance Authority](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/tree/main/bp02-configuration-governance-authority?ref_type=heads) / BP02B - Manage vocabularies
</div>

# BP02B - Manage vocabularies

## Canonical source
[https://simpl-programme.ec.europa.eu/book-page/bp02b-manage-vocabularies](https://simpl-programme.ec.europa.eu/book-page/bp02b-manage-vocabularies)

## Overview

Vocabulary Management is part of BP02 - Set-up and configuration of data space _Governance Authority_ process. It outlines the management of vocabularies within a data space, enabling semantic interoperability of data structures across domains and data providers. The process governs the lifecycle of native vocabularies while enabling registration and use of external vocabularies.

![BP02B figure 1](./media/bp02b-figure-1.png)
*BP02B figure 1*

## Actors

- _Governance Authority_

## Assumptions

- Vocabularies are managed by the _Governance Authority_ and centrally stored in the Vocabulary Hub.
- _Providers_ can propose documented modifications to vocabularies via email with metadata and impact analysis.
- External vocabularies can be referenced within the data space; Simpl-Open does not control these external vocabularies.

## Prerequisites

- **Governance Authority setup:** must be completed before vocabulary management (Business Process 02A).
- **Governance Authority configured:** must be configured before vocabulary management (Business Process 02B).

## Process steps

### Trigger vocabulary management

The _Governance Authority_ initiates the management of a vocabulary in the data space.

### BP02B.01 - Create new vocabulary

The _Governance Authority_ creates a new vocabulary using RDF Schema or OWL according to defined syntactic and structural rules.

### BP02B.02 - Validate vocabulary

The vocabulary is verified through both syntax and semantic validation. While syntax validation is fully automated, semantic validation is performed through a combination of automated checks and manual review.

### BP02B.03 - Publish vocabulary

Once validated, the vocabulary is published and the previous version is marked as deprecated.

### BP02B.04 - Create new version of vocabulary

The _Governance Authority_ selects an existing vocabulary and creates a new version to incorporate changes and stakeholder input.

### BP02B.05 - Deprecate vocabulary version

A vocabulary version is deprecated when no longer aligned with business needs or replaced by an updated version. Deprecation occurs in two scenarios:

1. **Version Update** - automatic deprecation when a new version publishes, while remaining accessible for legacy schemas.
2. **Full Vocabulary Deprecation** - all versions deprecated when obsolete.

> **Important Note:** Deprecation does not mean deletion. Deprecated vocabularies remain available for reference and maintenance of existing implementations.

### BP02B.06 - Send validation notification

Simpl-Open sends a report to the _Governance Authority_ highlighting any validation issues detected.

## Outcomes

- **Vocabulary version published, updated or deprecated:** New or updated vocabulary provided for semantic schema validation; older versions may be deprecated when replaced.
- **Report related to invalid vocabulary sent:** Simpl-Open sends details of validation issues; vocabulary not published if issues present.

## High-level requirements

| ID | Title | Issue ID | Canonical source |
|----|-------|----------|------------------|
| 2B.1 | The Governance Authority consults vocabularies. | SIMPL-15931 | [2b1-...](https://simpl-programme.ec.europa.eu/book-page/2b1-governance-authority-consults-vocabularies) |
| 2B.2 | The Governance Authority creates a new vocabulary. | SIMPL-15933 | [2b2-...](https://simpl-programme.ec.europa.eu/book-page/2b2-governance-authority-creates-new-vocabulary) |
| 2B.3 | The Governance Authority creates a new version of a vocabulary. | SIMPL-15934 | [2b3-...](https://simpl-programme.ec.europa.eu/book-page/2b3-governance-authority-creates-new-version-vocabulary) |
| 2B.4 | The Governance Authority validates a new vocabulary or a new version of a vocabulary. | SIMPL-15936 | [2b4-...](https://simpl-programme.ec.europa.eu/book-page/2b4-governance-authority-validates-new-vocabulary-or-new-version-vocabulary) |
| 2B.5 | The Governance Authority publishes a new vocabulary or a new version of a vocabulary. | SIMPL-15937 | [2b5-...](https://simpl-programme.ec.europa.eu/book-page/2b5-governance-authority-publishes-new-vocabulary-or-new-version-vocabulary) |
| 2B.6 | The Governance Authority deprecates a vocabulary. | SIMPL-15935 | [2b6-...](https://simpl-programme.ec.europa.eu/book-page/2b6-governance-authority-deprecates-vocabulary) |

> **Status at time of capture (10 September 2026):** every high-level requirement listed here carries status *Proposed* on the source site.

### Requirement descriptions

**2B.1 - The Governance Authority consults vocabularies** (SIMPL-15931)

Simpl-Open shall allow the Governance Authority to consult the existing vocabularies.

Detailed requirements:

- 2B.1.1 - The Governance Authority consults an overview of the existing vocabularies
- 2B.1.2 - The Governance Authority consults a vocabulary
- 2B.1.3 - The Governance Authority consults the version history of a vocabulary

**2B.2 - The Governance Authority creates a new vocabulary** (SIMPL-15933)

Simpl-Open shall allow the Governance Authority to create a new vocabulary.

Detailed requirements:

- 2B.2.1 - The Governance Authority creates a new vocabulary

**2B.3 - The Governance Authority creates a new version of a vocabulary** (SIMPL-15934)

Simpl-Open shall allow the Governance Authority to create a new version of a previously published vocabulary.

Detailed requirements:

- 2B.3.1 - The Governance Authority creates a new version of a vocabulary

**2B.4 - The Governance Authority validates a new vocabulary or a new version of a vocabulary** (SIMPL-15936)

Simpl-Open shall allow the Governance Authority to validate a new vocabulary or a new version of a vocabulary with respect to syntax, semantics, and the status of the vocabularies referenced by it.

Detailed requirements:

- 2B.4.1 - The Governance Authority validates the syntax of a new vocabulary or a new version of a vocabulary
- 2B.4.2 - The Governance Authority validates the semantics of a new vocabulary or a new version of a vocabulary
- 2B.4.3 - The Governance Authority verifies the status of the vocabularies used to create a new vocabulary or a new version of a vocabulary
- 2B.4.4 - The Governance Authority receives a notification about vocabulary validation errors

**2B.5 - The Governance Authority publishes a new vocabulary or a new version of a vocabulary** (SIMPL-15937)

Simpl-Open shall support the Governance Authority to publish a validated new vocabulary or a validated new version of a vocabulary.

Detailed requirements:

- 2B.5.1 - The Governance Authority publishes a new vocabulary or a new version of a vocabulary
- 2B.5.2 - The Governance Authority versions a new vocabulary or a new version of a vocabulary
- 2B.5.3 - The Governance Authority deprecates the previously published version of a vocabulary

**2B.6 - The Governance Authority deprecates a vocabulary** (SIMPL-15935)

Simpl-Open shall allow the Governance Authority to deprecate a previously published vocabulary. A deprecated vocabulary can still be used and referenced, but this will be discouraged.

Detailed requirements:

- 2B.6.1 - The Governance Authority deprecates a vocabulary

## Source defects observed at capture (10 September 2026)

Recorded so this mirror can be reconciled when the source pages are corrected.

1. **The step heading BP02B.01 is broken mid-word** on the source page: "Create new vocabular y".
2. **Words broken by stray spaces in requirement titles:** "2B.1.2 - T he Governance Authority consults a vocabulary", "2B.1.3 - S impl-Open shall allow...", "2B.5.2 - T he Governance Authority versions...". Normalised here.
3. **The Jira issue IDs are out of sequence against the requirement numbering:** 2B.5 is SIMPL-15937 while 2B.6 is SIMPL-15935. The same pattern was observed in SA06.
4. **The repo previously omitted the "Trigger vocabulary management" step**, which the source page does carry. Added in this pass.
