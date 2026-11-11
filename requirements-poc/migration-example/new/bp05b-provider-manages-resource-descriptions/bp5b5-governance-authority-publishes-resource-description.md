<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/tree/main/bp05b-provider-manages-resource-descriptions?ref_type=heads) / 5B.5 - The Governance Authority publishes a Resource description
</div>

# 5B.5 - The Governance Authority publishes a Resource description

## Canonical source
[https://simpl-programme.ec.europa.eu/book-page/tbp-5b5-governance-authority-publishes-resource-description](https://simpl-programme.ec.europa.eu/book-page/tbp-5b5-governance-authority-publishes-resource-description)

## About this requirement

This is high-level requirement 5B.5 of business process [BP05B - Provider manages resource descriptions](./README.md). In the backlog (Jira) it is item SIMPL-14687.

In the process it covers 4 steps: [BP05B.05 Validate the resource description](./README.md#bp05b05-validate-the-resource-description), [BP05B.06 Remediate validation issues](./README.md#bp05b06-remediate-validation-issues), [BP05B.07 Publish the resource description](./README.md#bp05b07-publish-the-resource-description) and [BP05B.11 Revoke the previous version of the resource description](./README.md#bp05b11-revoke-the-previous-version-of-the-resource-description). The dynamic view shows it in [Define and publish self-description](bp05b-dynamic-view.md#define-and-publish-self-description) and [the technology components view](bp05b-tcv-dynamic-view.md).

It is split into 13 detailed requirements, delivered through 16 Epics with 18 user stories in total. Each detailed requirement below says what Simpl-Open must do. Under it are its Epics: open one to read its user stories and their acceptance criteria.

## Description

Simpl-Open shall support the Governance Authority

- to publish a Resource description submitted via an API by a Provider.

## Detailed requirements

Select a title to jump to its section below.

| ID | Detailed requirement | Process step | Epics | User stories |
| --- | --- | --- | --- | --- |
| 5B.5.1 | [The Governance Authority validates the schema of a new Resource description or a new version of a Resource Description](#5b51-the-governance-authority-validates-the-schema-of-a-new-resource-description-or-a-new-version-of-a-resource-description) | BP05B.05 Validate the resource description | 1 | 1 |
| 5B.5.2 | [The Governance Authority notifies a Provider about an inactive schema](#5b52-the-governance-authority-notifies-a-provider-about-an-inactive-schema) | BP05B.06 Remediate validation issues | 1 | 1 |
| 5B.5.3 | [The Governance Authority validates the versioning of a new version of a Resource description](#5b53-the-governance-authority-validates-the-versioning-of-a-new-version-of-a-resource-description) | BP05B.05 Validate the resource description | 1 | 1 |
| 5B.5.4 | [The Governance Authority notifies a Provider about wrong versioning of new versions of a Resource description](#5b54-the-governance-authority-notifies-a-provider-about-wrong-versioning-of-new-versions-of-a-resource-description) | BP05B.06 Remediate validation issues | 1 | 1 |
| 5B.5.5 | [The Governance Authority validates the syntax of a new Resource description or a new version of a Resource Description](#5b55-the-governance-authority-validates-the-syntax-of-a-new-resource-description-or-a-new-version-of-a-resource-description) | BP05B.05 Validate the resource description | 2 | 3 |
| 5B.5.6 | [The Governance Authority provides feedback to a Provider about syntax validation errors](#5b56-the-governance-authority-provides-feedback-to-a-provider-about-syntax-validation-errors) | BP05B.06 Remediate validation issues | 2 | 2 |
| 5B.5.7 | [The Governance Authority validates the semantics of a new Resource description or a new version of a Resource Description](#5b57-the-governance-authority-validates-the-semantics-of-a-new-resource-description-or-a-new-version-of-a-resource-description) | BP05B.05 Validate the resource description | 2 | 2 |
| 5B.5.8 | [The Governance Authority provides feedback to a Provider about semantic validation errors](#5b58-the-governance-authority-provides-feedback-to-a-provider-about-semantic-validation-errors) | BP05B.06 Remediate validation issues | 1 | 1 |
| 5B.5.9 | [The Governance Authority publishes a new Resource description or a new version of a Resource description](#5b59-the-governance-authority-publishes-a-new-resource-description-or-a-new-version-of-a-resource-description) | BP05B.07 Publish the resource description | 1 | 0 |
| 5B.5.10 | [The Governance Authority revokes the previous version of a Resource description](#5b510-the-governance-authority-revokes-the-previous-version-of-a-resource-description) | BP05B.11 Revoke the previous version of the resource description | 1 | 1 |
| 5B.5.11 | [The Governance Authority evaluates the metadata quality of a new resource description or a new version of a resource description](#5b511-the-governance-authority-evaluates-the-metadata-quality-of-a-new-resource-description-or-a-new-version-of-a-resource-description) | BP05B.05 Validate the resource description | 1 | 3 |
| 5B.5.12 | [The Governance Authority notifies the Provider about the metadata quality](#5b512-the-governance-authority-notifies-the-provider-about-the-metadata-quality) | BP05B.06 Remediate validation issues | 1 | 1 |
| 5B.5.13 | [The Governance Authority approves/rejects the metadata of a new resource description or a new version of a resource description](#5b513-the-governance-authority-approvesrejects-the-metadata-of-a-new-resource-description-or-a-new-version-of-a-resource-description) | BP05B.05 Validate the resource description | 1 | 1 |

### 5B.5.1 The Governance Authority validates the schema of a new Resource description or a new version of a Resource Description

**Process step:** [BP05B.05 Validate the resource description](./README.md#bp05b05-validate-the-resource-description)<br>
**Public text:** [5B.5.1 on the requirements site](https://simpl-programme.ec.europa.eu/book-page/tbp-5b51-governance-authority-validates-schema-new-resource-description-or-new-version)

Simpl-Open shall support the Governance Authority

- to automatically validate that the schema of the Resource description is active and not revoked.

**Delivered through 1 Epic.** Each Epic page has the user stories and their acceptance criteria.

- [Epic: The Governance Authority checks the schema of a new resource description or a new version of a resource description](./bp5b5-governance-authority-publishes-resource-description/simpl-17332-the-governance-authority-checks-the-schema-of-a-new-resource.md)<br>
  *As Governance Authority, I want to automatically check the schema of a new resource description or new version of a resource description* · 1 user story

### 5B.5.2 The Governance Authority notifies a Provider about an inactive schema

**Process step:** [BP05B.06 Remediate validation issues](./README.md#bp05b06-remediate-validation-issues)<br>
**Public text:** [5B.5.2 on the requirements site](https://simpl-programme.ec.europa.eu/book-page/tbp-5b52-governance-authority-notifies-provider-about-inactive-schema)

Simpl-Open shall support the Governance Authority

- to automatically notify a Provider if the schema, referred by a submitted Resource description, is revoked.

**Delivered through 1 Epic.** Each Epic page has the user stories and their acceptance criteria.

- [Epic: The Governance Authority notifies a Provider about an invalid schema](./bp5b5-governance-authority-publishes-resource-description/simpl-17798-the-governance-authority-notifies-a-provider-about-an-invali.md)<br>
  *As Governance Authority, I want to notify a Provider about an invalid schema of a new resource description or a new version of a resource description* · 1 user story

### 5B.5.3 The Governance Authority validates the versioning of a new version of a Resource description

**Process step:** [BP05B.05 Validate the resource description](./README.md#bp05b05-validate-the-resource-description)<br>
**Public text:** [5B.5.3 on the requirements site](https://simpl-programme.ec.europa.eu/book-page/tbp-5b53-governance-authority-validates-versioning-new-version-resource-description)

Simpl-Open shall support the Governance Authority

- to automatically validate the versioning of a new version of a Resource description, so that e.g. there are no gaps between version numbers and the same version number is not set in two different versions of a Resource description.

**Delivered through 1 Epic.** Each Epic page has the user stories and their acceptance criteria.

- [Epic: The Governance Authority checks the version number of a new version of a Resource description](./bp5b5-governance-authority-publishes-resource-description/simpl-17799-the-governance-authority-checks-the-version-number-of-a-new.md)<br>
  *As Governance Authority, I want to automatically check the version number of a new version of a resource description* · 1 user story

### 5B.5.4 The Governance Authority notifies a Provider about wrong versioning of new versions of a Resource description

**Process step:** [BP05B.06 Remediate validation issues](./README.md#bp05b06-remediate-validation-issues)<br>
**Public text:** [5B.5.4 on the requirements site](https://simpl-programme.ec.europa.eu/book-page/tbp-5b54-governance-authority-notifies-provider-about-wrong-versioning-new-versions)

Simpl-Open shall support the Governance Authority

- to automatically notify a Provider if there are errors in the versioning provided in a new version of a Resource description.

**Delivered through 1 Epic.** Each Epic page has the user stories and their acceptance criteria.

- [Epic: The Governance Authority notifies a Provider about a wrong version number of a new version of a Resource description](./bp5b5-governance-authority-publishes-resource-description/simpl-17800-the-governance-authority-notifies-a-provider-about-a-wrong-v.md)<br>
  *As Governance Authority, I want to notify Providers about a wrong version number of a new version of a resource description* · 1 user story

### 5B.5.5 The Governance Authority validates the syntax of a new Resource description or a new version of a Resource Description

**Process step:** [BP05B.05 Validate the resource description](./README.md#bp05b05-validate-the-resource-description)<br>
**Public text:** [5B.5.5 on the requirements site](https://simpl-programme.ec.europa.eu/book-page/tbp-5b55-governance-authority-validates-syntax-new-resource-description-or-new-version)

Simpl-Open shall support the Governance Authority

- to automatically validate the syntax of a submitted Resource description.

**Delivered through 2 Epics.** Each Epic page has the user stories and their acceptance criteria.

- [Epic: The Governance Authority checks the syntax of a new Resource description or a new version of a Resource Description](./bp5b5-governance-authority-publishes-resource-description/simpl-982-the-governance-authority-checks-the-syntax-of-a-new-resource.md)<br>
  *As Governance Authority, I want to automatically check the syntax of a new resource description or a new version of a resource description* · 1 user story
- [Epic: The Governance Authority validates the syntax of the policies assigned to a new Resource description or a new version of a Resource Description](./bp5b5-governance-authority-publishes-resource-description/simpl-18402-the-governance-authority-validates-the-syntax-of-the-policie.md)<br>
  *As a Governance Authority, I want to automatically validate the syntax of policies in a new resource description or a new version of a resource description* · 2 user stories

### 5B.5.6 The Governance Authority provides feedback to a Provider about syntax validation errors

**Process step:** [BP05B.06 Remediate validation issues](./README.md#bp05b06-remediate-validation-issues)<br>
**Public text:** [5B.5.6 on the requirements site](https://simpl-programme.ec.europa.eu/book-page/tbp-5b56-governance-authority-notifies-provider-about-syntax-validation-errors)

Simpl-Open shall support the Governance Authority

- to automatically provide feedback to a Provider about syntax validation errors in a submitted Resource description.

**Delivered through 2 Epics.** Each Epic page has the user stories and their acceptance criteria.

- [Epic: The Governance Authority notifies a Provider about syntax validation errors](./bp5b5-governance-authority-publishes-resource-description/simpl-17801-the-governance-authority-notifies-a-provider-about-syntax-va.md)<br>
  *As Governance Authority, I want to notify a Provider about invalid syntax of a new resource description or a new version of a resource description* · 1 user story
- [Epic: The Governance Authority provides feedback to a Provider about syntax validation errors of a policy](./bp5b5-governance-authority-publishes-resource-description/simpl-25371-the-governance-authority-provides-feedback-to-a-provider-abo.md)<br>
  *As a Governance Authority, I want to provide feedback to a Provider about invalid syntax of a policy in a resource description* · 1 user story

### 5B.5.7 The Governance Authority validates the semantics of a new Resource description or a new version of a Resource Description

**Process step:** [BP05B.05 Validate the resource description](./README.md#bp05b05-validate-the-resource-description)<br>
**Public text:** [5B.5.7 on the requirements site](https://simpl-programme.ec.europa.eu/book-page/tbp-5b57-governance-authority-validates-semantics-new-resource-description-or-new-version)

Simpl-Open shall support the Governance Authority

- to automatically validate the semantics of a submitted Resource description based on the used schema.

**Delivered through 2 Epics.** Each Epic page has the user stories and their acceptance criteria.

- [Epic: The Governance Authority checks the semantics of a new Resource description or a new version of a Resource Description](./bp5b5-governance-authority-publishes-resource-description/simpl-889-the-governance-authority-checks-the-semantics-of-a-new-resou.md)<br>
  *As Governance Authority, I want to check the semantics of a new resource description or a new version of a resource description* · 2 user stories
- [Epic: The Governance Authority checks the semantics of a new Resource description or a new version of a Resource Description [Extension Consent]](./bp5b5-governance-authority-publishes-resource-description/simpl-29978-the-governance-authority-checks-the-semantics-of-a-new-resou.md)<br>
  no user stories yet

### 5B.5.8 The Governance Authority provides feedback to a Provider about semantic validation errors

**Process step:** [BP05B.06 Remediate validation issues](./README.md#bp05b06-remediate-validation-issues)<br>
**Public text:** [5B.5.8 on the requirements site](https://simpl-programme.ec.europa.eu/book-page/tbp-5b58-governance-authority-notifies-provider-about-semantic-validation-errors)

Simpl-Open shall support the Governance Authority

- to automatically provide feedback to a Provider about semantic validation errors in submitted Resource descriptions.

**Delivered through 1 Epic.** Each Epic page has the user stories and their acceptance criteria.

- [Epic: The Governance Authority provides feedback to a Provider about semantic validation errors](./bp5b5-governance-authority-publishes-resource-description/simpl-18040-the-governance-authority-provides-feedback-to-a-provider-abo.md)<br>
  *As Governance Authority, I want to provide feedback to a Provider about invalid semantics of a new resource description or a new version of a resource description* · 1 user story

### 5B.5.9 The Governance Authority publishes a new Resource description or a new version of a Resource description

**Process step:** [BP05B.07 Publish the resource description](./README.md#bp05b07-publish-the-resource-description)<br>
**Public text:** [5B.5.9 on the requirements site](https://simpl-programme.ec.europa.eu/book-page/tbp-5b59-governance-authority-publishes-new-resource-description-or-new-version-resource)

Simpl-Open shall support the Governance authority

- to automatically publish a validated new Resource description or a validated new version of a Resource description in the catalogue.

**Delivered through 1 Epic.** Each Epic page has the user stories and their acceptance criteria.

- [Epic: The Governance Authority publishes a new resource description or a new version of a resource description](./bp5b5-governance-authority-publishes-resource-description/simpl-4890-the-governance-authority-publishes-a-new-resource-descriptio.md)<br>
  *As Governance Authority, I want to automatically publish a valid new Resource description or a valid new version of a Resource description in the catalogue* · no user stories yet

### 5B.5.10 The Governance Authority revokes the previous version of a Resource description

**Process step:** [BP05B.11 Revoke the previous version of the resource description](./README.md#bp05b11-revoke-the-previous-version-of-the-resource-description)<br>
**Public text:** [5B.5.10 on the requirements site](https://simpl-programme.ec.europa.eu/book-page/tbp-5b510-governance-authority-revokes-previous-version-resource-description)

Simpl-Open shall support the Governance Authority

- to automatically revoke the previous version of a Resource Description, if a new resource version of a Resource  description is published.

**Delivered through 1 Epic.** Each Epic page has the user stories and their acceptance criteria.

- [Epic: The Governance Authority revokes the previous version of a new version of a resource description](./bp5b5-governance-authority-publishes-resource-description/simpl-18041-the-governance-authority-revokes-the-previous-version-of-a-n.md)<br>
  *As Governance Authority, I want to revoke the previous version of a new version of a resource description* · 1 user story

### 5B.5.11 The Governance Authority evaluates the metadata quality of a new resource description or a new version of a resource description

**Process step:** [BP05B.05 Validate the resource description](./README.md#bp05b05-validate-the-resource-description)<br>
**Public text:** [5B.5.11 on the requirements site](https://simpl-programme.ec.europa.eu/book-page/5b511-governance-authority-evaluates-metadata-quality-new-resource-description-or-new)

Simpl-Open shall support the Governance Authority to automatically evaluate the metadata quality of a submitted resource description by calculating and storing:

- Metadata quality overall score;
- Metadata quality dimension score for each dimension;
- Metadata quality classification for the metadata quality overall score;
- Metadata quality classification for each dimension.

The calculated metadata quality scores represent the current evaluation of the resource description based on the active metadata quality and governance rules and replace any previously calculated scores. Historical or versioned metadata quality evaluations are not required to be retained.

**Delivered through 1 Epic.** Each Epic page has the user stories and their acceptance criteria.

- [Epic: The Governance Authority evaluates the metadata quality of a Resource description](./bp5b5-governance-authority-publishes-resource-description/simpl-24060-the-governance-authority-evaluates-the-metadata-quality-of-a.md)<br>
  *As Governance Authority, I want Simpl-Open to automatically evaluate the metadata quality of a submitted resource description or a new version of a resource description by calculating* · 3 user stories

### 5B.5.12 The Governance Authority notifies the Provider about the metadata quality

**Process step:** [BP05B.06 Remediate validation issues](./README.md#bp05b06-remediate-validation-issues)<br>
**Public text:** [5B.5.12 on the requirements site](https://simpl-programme.ec.europa.eu/book-page/5b512-governance-authority-notifies-provider-about-metadata-quality)

Simpl-Open shall support the Governance Authority to automatically notify the metadata quality feedback to a Provider that submitted a resource description by:

- Informing them about the metadata quality overall score;
- Informing them about the metadata quality dimension score for each dimension;
- Informing them about the metadata quality classification for the metadata quality overall score;
- Informing them about the metadata quality classification for each dimension;
- Informing them whether the level of metadata quality was sufficient for publication based on the quality threshold.

The notification shall be triggered automatically after the metadata quality calculation is completed and shall not require a manual action by the Governance Authority.

The recipient of the notification shall be the Provider associated with the submitted resource description, as registered in the system.

Simpl-Open shall ensure reliable processing of the notification and shall log notification delivery failures.

Additionally, the Provider can at all times retrieve the metadata quality scores overall, for each dimension, and the individual metadata quality scores related to each dimension for their published resources.

Simpl-Open shall provide this retrieval capability via the UI and API without requiring a prior notification to be available.

**Delivered through 1 Epic.** Each Epic page has the user stories and their acceptance criteria.

- [Epic: The Governance Authority notifies the Provider about metadata quality results](./bp5b5-governance-authority-publishes-resource-description/simpl-24062-the-governance-authority-notifies-the-provider-about-metadat.md)<br>
  *As Governance Authority, I want Simpl-Open to automatically notify the Provider who submitted a resource description about the metadata quality assessment results* · 1 user story

### 5B.5.13 The Governance Authority approves/rejects the metadata of a new resource description or a new version of a resource description

**Process step:** [BP05B.05 Validate the resource description](./README.md#bp05b05-validate-the-resource-description)<br>
**Public text:** [5B.5.13 on the requirements site](https://simpl-programme.ec.europa.eu/book-page/5b513-governance-authority-approvesrejects-metadata-new-resource-description-or-new)

Simpl-Open shall support the Governance Authority to automatically approve or reject the metadata of a submitted resource description by:

- Evaluating if the metadata quality overall score is higher then the overall quality threshold;
- Evaluating if the metadata quality dimension score of each dimension is higher then the related dimension quality threshold.

**Delivered through 1 Epic.** Each Epic page has the user stories and their acceptance criteria.

- [Epic: The Governance Authority approves or rejects a Resource description based on metadata quality](./bp5b5-governance-authority-publishes-resource-description/simpl-24061-the-governance-authority-approves-or-rejects-a-resource-desc.md)<br>
  *As Governance Authority, I want Simpl-Open to automatically approve or reject the metadata of a submitted resource description or a new version of a resource description* · 1 user story
