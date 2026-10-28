<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP02 - Configuration of data space Governance Authority](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/tree/main/bp02-configuration-governance-authority?ref_type=heads) / BP02C - Manage resource description schemas
</div>

# BP02C - Manage resource description schemas

## Canonical source
[https://simpl-programme.ec.europa.eu/book-page/bp02c-manage-resource-description-schemas](https://simpl-programme.ec.europa.eu/book-page/bp02c-manage-resource-description-schemas)

## Overview

Resource Description Schema Management is part of BP02 - Configuration of data space _Governance Authority_ process. It outlines the management of schemas within a data space, ensuring integrity and consistency of resource descriptions.

A schema defines structure and format for metadata describing resources. Schemas specify mandatory and optional properties, data types, and validation rules. Each resource type (data, application, infrastructure) has one schema. Properties include default mandatory properties from Simpl-Open and custom properties defined by the _Governance Authority_.

![BP02C figure 1](./media/bp02c-figure-1.png)
*BP02C figure 1*

![BP02C figure 2](./media/bp02c-figure-2.png)
*BP02C figure 2*

## Actors

- _Governance Authority_

## Assumptions

- _Providers_ may propose schema modifications; the _Governance Authority_ reviews and approves changes manually.
- New schema versions preserve existing resource descriptions without alteration.
- Revoking a schema doesn't alter existing descriptions; new ones must use non-revoked schemas.
- External vocabularies can be referenced for domain-specific properties.
- Contract clauses schema (Service level agreement, license agreement, billing schema) are default properties imposed by Simpl-Open.

## Prerequisites

- **Governance Authority setup:** must be set up (Business Process 02A).
- **Internal vocabularies configured:** the _Governance Authority_ has configured the catalogue with the corresponding vocabularies (Business Process 02B).
- **End-User authenticated & authorised:** the End-User is authenticated and has the appropriate role and permissions (Business Process 03B).

## Process steps

### Trigger resource description schema management

The _Governance Authority_ decides to create a new schema, create a new version, or revoke an existing schema.

### BP02C.01 Create a new schema

The _Governance Authority_ creates the schema by providing a comprehensive and structured definition, detailing its custom mandatory and custom recommended properties, data types, constraints, validation rules, and any relationships or dependencies.

### BP02C.02 Create a new version of the schema

The _Governance Authority_ selects an existing schema from the data space catalogue. The _Governance Authority_ creates the new version of the schema by providing a comprehensive and structured definition, detailing its custom mandatory and custom recommended properties, data types, constraints, validation rules, and any relationships or dependencies.

### BP02C.03 Revoke a schema

The _Governance Authority_ revokes an existing schema if it becomes obsolete, superseded, or no longer relevant to the data space.

### BP02C.04 Validate the schema

Simpl-Open enriches the schema with default properties, then validates through:

- Syntactic validation according to imposed structure and formatting.
- Semantic validation of schema properties based on vocabularies used.

### BP02C.05 Publish the schema

Upon successful validation, the new schema or new version of an existing schema is published in the data space catalogue. If it concerns the creation of a new version of an existing schema, the previous version is revoked.

### BP02C.06 Inform Providers about changes of the schema

_Providers_ are notified about changes (e.g. via email).

### BP02C.07 Send report

Simpl-Open sends a report to the _Governance Authority_ about validation issues of the schema.

## Outcomes

- **Schema published or revoked:** New schema, new version, or revocation becomes available or unavailable for _Providers_ (Business Process 5B).
- **Report related to invalid schema sent:** Validation report detailing syntax errors, missing properties, or invalid rules is sent to _Governance Authority_.

## High-level requirements

| ID | Title | Issue ID | Canonical source |
|----|-------|----------|------------------|
| 2C.1 | Governance Authority - retrieving schemas and their versions. | SIMPL-11209 | [2c1-...](https://simpl-programme.ec.europa.eu/book-page/2c1-governance-authority-retrieving-schemas-and-their-versions) |
| 2C.2 | Governance Authority - creating a new schema. | SIMPL-11264 | [2c2-...](https://simpl-programme.ec.europa.eu/book-page/2c2-governance-authority-creating-new-schema) |
| 2C.3 | Governance Authority - creating a new version of an existing schema. | SIMPL-11265 | [2c3-...](https://simpl-programme.ec.europa.eu/book-page/2c3-governance-authority-creating-new-version-existing-schema) |
| 2C.4 | Governance Authority - validating a schema. | SIMPL-11268 | [2c4-...](https://simpl-programme.ec.europa.eu/book-page/2c4-governance-authority-validating-schema) |
| 2C.5 | Governance Authority - receiving feedback about schema validation issues. | SIMPL-11275 | [2c5-...](https://simpl-programme.ec.europa.eu/book-page/2c5-governance-authority-receiving-feedback-about-schema-validation-issues) |
| 2C.6 | Governance Authority - publishing a schema. | SIMPL-11269 | [2c6-...](https://simpl-programme.ec.europa.eu/book-page/2c6-governance-authority-publishing-schema) |
| 2C.7 | Governance Authority - revoking and retaining a schema. | SIMPL-11267 | [2c7-...](https://simpl-programme.ec.europa.eu/book-page/2c7-governance-authority-revoking-and-retaining-schema) |
| 2C.8 | Governance Authority - notifying Providers about schema changes. | SIMPL-11271 | [2c8-...](https://simpl-programme.ec.europa.eu/book-page/2c8-governance-authority-notifying-providers-about-schema-changes) |

> **Status at time of capture (10 September 2026):** every high-level requirement listed here carries status *Proposed* on the source site.

### Requirement descriptions

**2C.1 - Governance Authority - retrieving schemas and their versions** (SIMPL-11209)

Simpl-Open shall allow the retrieval of schemas and their versions, enabling the Governance Authority to review them and take action.

Detailed requirements:

- 2C.1.1 - Governance Authority - retrieving schemas and schema versions

**2C.2 - Governance Authority - creating a new schema** (SIMPL-11264)

Simpl-Open shall allow the Governance Authority to create a schema, so that resource descriptions created from it align with the standards and rules defined by the Governance Authority for its data space, and so that advanced search contains properties relevant for the selected catalogues during search. This applies to federated search.

Detailed requirements:

- 2C.2.1 - Governance Authority - creating a new schema for a new resource type
- 2C.2.2 - The Governance Authority versions a new schema

**2C.3 - Governance Authority - creating a new version of an existing schema** (SIMPL-11265)

Simpl-Open shall enable the Governance Authority to create a new version of an existing schema, so that resource descriptions created from the new version align with the standards and rules defined by the Governance Authority for its data space.

Detailed requirements:

- 2C.3.1 - Governance Authority - creating a new version of an existing schema for a resource type
- 2C.3.2 - The Governance Authority versions a new version of a schema

**2C.4 - Governance Authority - validating a schema** (SIMPL-11268)

Simpl-Open shall allow the Governance Authority to syntactically and semantically validate a schema. This includes verifying the structural correctness of the schema as well as ensuring the semantic integrity of its properties against the vocabularies used to define them. The validation process shall also confirm that Simpl's default properties are correctly applied, ensuring consistency with Simpl governance rules.

Detailed requirements:

- 2C.4.1 - Governance Authority - validating a schema's syntax, semantics, and default properties

**2C.5 - Governance Authority - receiving feedback about schema validation issues** (SIMPL-11275)

Simpl-Open shall provide feedback to the Governance Authority about the validation issues of a schema.

Detailed requirements:

- 2C.5.1 - Governance Authority - receiving feedback and report about schema validation issues

**2C.6 - Governance Authority - publishing a schema** (SIMPL-11269)

Simpl-Open shall enable the Governance Authority to publish and version schemas, ensuring that any resource descriptions published with one of the resource description schemas (or a version of it) within the data space adhere to the defined standards and rules, and that other types of schemas (federated search schema) can be made available.

Detailed requirements:

- 2C.6.1 - Governance Authority - publishing a validated schema

**2C.7 - Governance Authority - revoking and retaining a schema** (SIMPL-11267)

Simpl-Open shall allow the Governance Authority to revoke a schema when needed and retain it.

Detailed requirements:

- 2C.7.1 - Governance Authority - ensuring that a revoked schema is not available for publishing a new resource description
- 2C.7.2 - Governance Authority - revoking a schema
- 2C.7.3 - Governance Authority - retaining a revoked schema for existing resource descriptions

**2C.8 - Governance Authority - notifying Providers about schema changes** (SIMPL-11271)

Simpl-Open shall notify Providers when a resource description schema is published, a new version of an existing resource description schema is published, or a resource description schema is revoked, so that Providers are aware and can, if required or relevant, adapt their resource descriptions accordingly.

Detailed requirements:

- 2C.8.1 - Governance Authority - notifying Providers about schema changes

## Source defects observed at capture (10 September 2026)

Recorded so this mirror can be reconciled when the source pages are corrected.

1. **Step BP02C.05 "Publish the schema" is missing from the source page.** The published sequence runs BP02C.01, .02, .03, .04, .06, .07, skipping .05, even though 2C.6 "publishing a schema" is a high-level requirement. The step is retained in this mirror because the numbering gap makes it clear something was dropped rather than removed on purpose.
2. **The process-step headings are not marked up as headings** on the source page, unlike BP02A. They render as plain body text. The same defect appears in BP14A.03 and BP14B.03.
3. **Every high-level requirement link in this repo was pointing at a legacy `2b*` slug** (for example 2C.1 at `2b10-...`, 2C.2 at `2b3-...`). The site now serves them at `2c1-` to `2c8-`. All eight links were repointed in this pass.
4. **2C.5 was retitled** from "receiving notification about schema validation issues" to "receiving feedback about schema validation issues" (changelog, 17 August 2026). Its detailed requirement 2C.5.1 is inconsistently named on the same page: the body calls it "receiving notification and report", the book navigation calls it "receiving feedback and report".
5. **Words broken by stray spaces:** "res ource descriptions" in the 2C.2 description, "publishing a new resource descriptio n" in 2C.7.1. Normalised here.
6. **The 2C.4 description says "Simpl's default properties"** rather than "Simpl-Open's".
