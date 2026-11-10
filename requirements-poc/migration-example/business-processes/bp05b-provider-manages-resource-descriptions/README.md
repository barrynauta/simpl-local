<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / BP05B - Provider manages resource descriptions
</div>

# BP05B - Provider manages resource descriptions

## Canonical source
[https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)

> **See also: [Dynamic view](./bp05b-dynamic-view.md)** - sequence diagram showing how this business process executes at runtime, with links to each participating solution.
> **See also: [TCV dynamic view](./bp05b-tcv-dynamic-view.md)** - sequence diagrams of the technology components supporting resource-description management.

> **See also: [SA08 - Application orchestration used by a Participant](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa08-application-orchestration/README.md)** - an application workflow is linked and parameterised against an application resource description published through this business process (SA08.04).
>
> 📋 **Requirements:** the requirements of this process, from the high-level requirements down to the user stories, are in [functional-requirements](../../functional-requirements/bp05b-provider-manages-resource-descriptions/README.md). Each process step below lists the detailed requirements it needs.

## Overview

This business process covers the management of resource descriptions in the data space catalogue by both a _Provider_ and the _Governance Authority_. It includes the following main steps:

- **Create a new resource description** - the _Provider_ creates a new resource description with the required and optional contents, signs and submits it to the _Governance Authority_ for publication in the data space catalogue.
- **Create a new version of a resource description** - the _Provider_ creates a new version of an existing resource description, updates the contents, signs and submits it to the _Governance Authority_. This corresponds to an update; resource descriptions are immutable and cannot be changed in place.
- **Request resource description revocation** - the _Provider_ sends a request to revoke a resource description from the data space catalogue to the _Governance Authority_.
- **Publish a resource description** - having received a request to create a new resource description (or a new version), the _Governance Authority_ validates the received resource description and publishes it in the data space catalogue if valid. Consumers of an existing resource description are informed about the new version, and the old version is revoked by the _Governance Authority_.
- **Revoke a resource description** - having received a request to revoke a resource description, the _Governance Authority_ revokes it from the data space catalogue. Consumers of the resource description are informed about the revocation.

## Actors

- _Governance Authority_
- _Provider_

## Assumptions

- The creation of a new resource description, or a new version of one, always uses the latest version of a schema.
- If a new version of a resource description is created, usage contracts based on the old version continue without alteration.
- The revocation of a resource description does not alter or terminate usage contracts based on that resource description, which can still be inspected by the _Consumer_. Only new usage contracts are no longer possible with the revoked resource description, since it is no longer available in the data space catalogue.

## Prerequisites

- **Data space is configured** - the _Governance Authority_ has configured the data space catalogue with the corresponding vocabulary and schemas (containing quality rules) to have the general structure of a resource description (BP02). Data-space-specific configurations such as contract templates must also be defined.
- **Provider onboarded** - the _Provider_ must be successfully onboarded (BP03A).
- **Contract clauses stored and available for inclusion in resource descriptions** - the _Provider_ must have created and stored at least one set of contract clauses so they can be included in the resource description (BP05A).

![BP05B figure 1](./media/bp05b-figure-1.png)
*BP05B figure 1 - high-level diagram*

![BP05B figure 2](./media/bp05b-figure-2.png)
*BP05B figure 2 - detailed-level diagram*

## Process steps

### Trigger - resource description management

The _Provider_ decides whether to create a new resource description, create a new version of an existing one, or revoke an existing one. This initiates resource description management.

> 📋 **Requirements for this step**, part of [5B.1 A Provider consults its own Resource descriptions](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-12897/README.md):
>
> - [5B.1.1 A Provider consults an overview of its Resource descriptions](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-12897/L2-SIMPL-12898/README.md) · Defined · 1 Epic, 4 user stories
> - [5B.1.2 A Provider consults the details of one of its own resource descriptions](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-12897/L2-SIMPL-12903/README.md) · Defined · 3 Epics, 4 user stories
> - [5B.1.3 A Provider consults the version history of one of its own resource descriptions](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-12897/L2-SIMPL-12904/README.md) · Defined · 2 Epics, 3 user stories

### BP05B.01 Create a new resource description

The _Provider_ selects the resource type (data, application, or infrastructure) and enters required and optional common metadata.

> 📋 **Requirements for this step**, part of [5B.2 A Provider creates a new Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14688/README.md):
>
> - [5B.2.1 A provider retrieves a Resource type](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14730/README.md) · Defined · 2 Epics, 7 user stories
> - [5B.2.2 A Provider adds common metadata to a new Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14731/README.md) · Defined · 3 Epics, 8 user stories

### BP05B.02 Add resource type specific metadata

Depending on the selected resource type, the _Provider_ adds the resource type specific required and optional metadata.

> 📋 **Requirements for this step**, part of [5B.2 A Provider creates a new Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14688/README.md):
>
> - [5B.2.3 A Provider adds resource type specific metadata to a new Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14732/README.md) · Defined · 6 Epics, 6 user stories

### BP05B.03 Add policy and contract related metadata

For access and usage control, the _Provider_ configures the policies that shall be applied to the new resource. The _Provider_ also adds required and optional metadata related to the usage contract for the specific resource.

> 📋 **Requirements for this step**, part of [5B.2 A Provider creates a new Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14688/README.md):
>
> - [5B.2.4 A Provider assigns access policies to a new Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14733/README.md) · In Progress · 5 Epics, 14 user stories
> - [5B.2.5 A Provider assigns usage policies to a new Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14734/README.md) · In Progress · 4 Epics, 9 user stories
> - [5B.2.6 A Provider adds a set of contract clauses to a new resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/README.md) · In Progress · 9 Epics, 24 user stories

### BP05B.04 Sign and submit the resource description

The _Provider_ signs the new resource description with the _Provider_'s credentials and submits it to the _Governance Authority_.

> 📋 **Requirements for this step**, part of [5B.4 A Provider signs and submits a new Resource description or a new version](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14729/README.md):
>
> - [5B.4.1 A Provider signs and submits a new Resource description or a new version of a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14729/L2-SIMPL-14745/README.md) · Defined · 3 Epics, 13 user stories

### BP05B.05 Validate the resource description

When receiving a request to publish a new resource description (or a new version), the _Governance Authority_ performs syntactic validation, semantic validation against schema and vocabulary, and quality-rules validation.

> 📋 **Requirements for this step**, part of [5B.5 The Governance Authority publishes a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14687/README.md):
>
> - [5B.5.1 The Governance Authority validates the schema of a new Resource description or a new version of a Resource Description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14689/README.md) · Defined · 1 Epic, 1 user story
> - [5B.5.3 The Governance Authority validates the versioning of a new version of a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14864/README.md) · Defined · 1 Epic, 1 user story
> - [5B.5.5 The Governance Authority validates the syntax of a new Resource description or a new version of a Resource Description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14747/README.md) · In Progress · 2 Epics, 3 user stories
> - [5B.5.7 The Governance Authority validates the semantics of a new Resource description or a new version of a Resource Description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14749/README.md) · Defined · 2 Epics, 2 user stories
> - [5B.5.11 The Governance Authority evaluates the metadata quality of a new resource description or a new version of a resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-22687/README.md) · Defined · 1 Epic, 3 user stories
> - [5B.5.13 The Governance Authority approves/rejects the metadata of a new resource description or a new version of a resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-24049/README.md) · Defined · 1 Epic, 1 user story

### BP05B.06 Remediate validation issues

In case of validation issues, the _Governance Authority_ informs the _Provider_. The _Provider_ must remediate and resubmit the resource description for publication.

> 📋 **Requirements for this step**, part of [5B.5 The Governance Authority publishes a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14687/README.md):
>
> - [5B.5.2 The Governance Authority notifies a Provider about an inactive schema](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14746/README.md) · Defined · 1 Epic, 1 user story
> - [5B.5.4 The Governance Authority notifies a Provider about wrong versioning of new versions of a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14865/README.md) · Defined · 1 Epic, 1 user story
> - [5B.5.6 The Governance Authority provides feedback to a Provider about syntax validation errors](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14748/README.md) · In Progress · 2 Epics, 2 user stories
> - [5B.5.8 The Governance Authority provides feedback to a Provider about semantic validation errors](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14750/README.md) · Defined · 1 Epic, 1 user story
> - [5B.5.12 The Governance Authority notifies the Provider about the metadata quality](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-22688/README.md) · Defined · 1 Epic, 1 user story

### BP05B.07 Publish the resource description

Upon successful validation, the new (or new version of the) resource description is published in the data space catalogue.

> 📋 **Requirements for this step**, part of [5B.5 The Governance Authority publishes a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14687/README.md):
>
> - [5B.5.9 The Governance Authority publishes a new Resource description or a new version of a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14753/README.md) · Defined · 1 Epic, 0 user stories

### BP05B.08 Create a new version of a resource description

The _Provider_ selects a previously published resource description and initiates the creation of a new version, updating required and optional metadata.

> 📋 **Requirements for this step**, part of [5B.3 A Provider creates a new version of a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14728/README.md):
>
> - [5B.3.1 A Provider updates the common metadata of a new version of a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14738/README.md) · Defined · 4 Epics, 6 user stories

### BP05B.09 Update resource type specific metadata

Resource-type-specific metadata can be updated for a new version of a resource description.

> 📋 **Requirements for this step**, part of [5B.3 A Provider creates a new version of a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14728/README.md):
>
> - [5B.3.2 A Provider updates the resource type specific metadata of a new version of a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14739/README.md) · Defined · 3 Epics, 4 user stories

### BP05B.10 Update policies and contract related metadata

Policies and contract-related metadata can be updated for a new version of a resource description. After this step, creation of a new version continues with signing and submission, identical to the new-resource flow.

> 📋 **Requirements for this step**, part of [5B.3 A Provider creates a new version of a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14728/README.md):
>
> - [5B.3.3 A Provider updates the access policies of a new version of a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14740/README.md) · In Progress · 1 Epic, 5 user stories
> - [5B.3.4 A Provider updates the usage policies of a new version of a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14741/README.md) · In Progress · 1 Epic, 5 user stories
> - [5B.3.5 A Provider updates the set of contract clauses of a new version of a resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14743/README.md) · Defined · 2 Epics, 2 user stories

### BP05B.11 Revoke the previous version of the resource description

If the new version of the resource description is valid, the _Governance Authority_ revokes the current (previous) resource description version.

> 📋 **Requirements for this step**, part of [5B.5 The Governance Authority publishes a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14687/README.md):
>
> - [5B.5.10 The Governance Authority revokes the previous version of a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14754/README.md) · Defined · 1 Epic, 1 user story

### BP05B.12 Inform about new version

If a resource description is updated, the _Consumers_ of this resource are informed about the new version in the data space catalogue.

> 📋 **Requirements for this step**, part of [5B.6 A Provider notifies Consumers about a new version of a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-21118/README.md):
>
> - [5B.6.1 A Provider notifies Consumers about a new version of a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-21118/L2-SIMPL-14755/README.md) · In Progress · 1 Epic, 1 user story

### BP05B.13 Request resource description revocation

The _Provider_ selects a previously published resource description and initiates the revocation of this resource description.

> 📋 **Requirements for this step**, part of [5B.7 A Provider requests the revocation of a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14699/README.md):
>
> - [5B.7.1 A Provider requests the revocation of a resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14699/L2-SIMPL-14639/README.md) · Defined · 1 Epic, 2 user stories

### BP05B.14 Revoke a resource description

Upon receiving a request for revocation, the _Governance Authority_ revokes the resource description from the data space catalogue.

> 📋 **Requirements for this step**, part of [5B.8 The Governance Authority revokes a resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14640/README.md):
>
> - [5B.8.1 The Governance Authority revokes a resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14640/L2-SIMPL-14641/README.md) · Defined · 1 Epic, 2 user stories

### BP05B.15 Inform about revocation

If a resource description is revoked, the _Consumers_ of this resource are informed about the revocation.

> 📋 **Requirements for this step**, part of [5B.9 The Provider notifies Consumers about the revocation of a resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-21117/README.md):
>
> - [5B.9.1 A Provider notifies Consumers about the revocation of a resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-21117/L2-SIMPL-14642/README.md) · In Progress · 1 Epic, 2 user stories

## High-level requirements

📋 Each title opens the requirement page in [functional-requirements](../../functional-requirements/bp05b-provider-manages-resource-descriptions/README.md), with its text, its detailed requirements, Epics and user stories.

| ID | Title | Status | Process steps | Dynamic view | Canonical source |
|----|-------|--------|---------------|--------------|------------------|
| 5B.1 | [A Provider consults its own Resource descriptions](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-12897/README.md) | Defined | [Trigger](#trigger---resource-description-management) | [Retrieve SD metadata](bp05b-dynamic-view.md#retrieve-sd-metadata), [Retrieve full self-description](bp05b-dynamic-view.md#retrieve-full-self-description) | [tbp-5b1-…](https://simpl-programme.ec.europa.eu/book-page/tbp-5b1-provider-consults-its-own-resource-descriptions) |
| 5B.2 | [A Provider creates a new Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14688/README.md) | In Progress | [01](#bp05b01-create-a-new-resource-description), [02](#bp05b02-add-resource-type-specific-metadata), [03](#bp05b03-add-policy-and-contract-related-metadata) | [Define and publish self-description](bp05b-dynamic-view.md#define-and-publish-self-description), [TCV](bp05b-tcv-dynamic-view.md) | [tbp-5b2-…](https://simpl-programme.ec.europa.eu/book-page/tbp-5b2-provider-creates-new-resource-description) |
| 5B.3 | [A Provider creates a new version of a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14728/README.md) | In Progress | [08](#bp05b08-create-a-new-version-of-a-resource-description), [09](#bp05b09-update-resource-type-specific-metadata), [10](#bp05b10-update-policies-and-contract-related-metadata) | [Define and publish self-description](bp05b-dynamic-view.md#define-and-publish-self-description) | [tbp-5b3-…](https://simpl-programme.ec.europa.eu/book-page/tbp-5b3-provider-creates-new-version-resource-description) |
| 5B.4 | [A Provider signs and submits a new Resource description or a new version](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14729/README.md) | Defined | [04](#bp05b04-sign-and-submit-the-resource-description) | [Define and publish self-description](bp05b-dynamic-view.md#define-and-publish-self-description), [TCV](bp05b-tcv-dynamic-view.md) | [tbp-5b4-…](https://simpl-programme.ec.europa.eu/book-page/tbp-5b4-provider-signs-and-submits-new-resource-description-or-new-version-resource) |
| 5B.5 | [The Governance Authority publishes a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14687/README.md) | In Progress | [05](#bp05b05-validate-the-resource-description), [06](#bp05b06-remediate-validation-issues), [07](#bp05b07-publish-the-resource-description), [11](#bp05b11-revoke-the-previous-version-of-the-resource-description) | [Define and publish self-description](bp05b-dynamic-view.md#define-and-publish-self-description), [TCV](bp05b-tcv-dynamic-view.md) | [tbp-5b5-…](https://simpl-programme.ec.europa.eu/book-page/tbp-5b5-governance-authority-publishes-resource-description) |
| 5B.6 | [A Provider notifies Consumers about a new version of a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-21118/README.md) | In Progress | [12](#bp05b12-inform-about-new-version) | - | [5b6-…](https://simpl-programme.ec.europa.eu/book-page/5b6-provider-notifies-consumers-about-new-version-resource-description) |
| 5B.7 | [A Provider requests the revocation of a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14699/README.md) | Defined | [13](#bp05b13-request-resource-description-revocation) | [Revoke a self-description](bp05b-dynamic-view.md#revoke-a-self-description-sd) | [5b7-…](https://simpl-programme.ec.europa.eu/book-page/5b7-provider-requests-revocation-resource-description) |
| 5B.8 | [The Governance Authority revokes a resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14640/README.md) | Defined | [14](#bp05b14-revoke-a-resource-description) | [Revoke a self-description](bp05b-dynamic-view.md#revoke-a-self-description-sd) | [5b8-…](https://simpl-programme.ec.europa.eu/book-page/5b8-governance-authority-revokes-resource-description) |
| 5B.9 | [The Provider notifies Consumers about the revocation of a resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-21117/README.md) | In Progress | [15](#bp05b15-inform-about-revocation) | - | [5b9-…](https://simpl-programme.ec.europa.eu/book-page/5b9-provider-notifies-consumers-about-revocation-resource-description) |

### Detailed requirements

📋 Open a high-level requirement to see its detailed requirements. Each one opens its own page, with the Epics and user stories under it.

<details><summary><b>5B.1 A Provider consults its own Resource descriptions</b> (3 detailed requirements, 6 Epics, 11 user stories)</summary>

| ID | Detailed requirement | Status | Epics | User stories |
| --- | --- | --- | --- | --- |
| 5B.1.1 | [A Provider consults an overview of its Resource descriptions](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-12897/L2-SIMPL-12898/README.md) | Defined | 1 | 4 |
| 5B.1.2 | [A Provider consults the details of one of its own resource descriptions](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-12897/L2-SIMPL-12903/README.md) | Defined | 3 | 4 |
| 5B.1.3 | [A Provider consults the version history of one of its own resource descriptions](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-12897/L2-SIMPL-12904/README.md) | Defined | 2 | 3 |

</details>

<details><summary><b>5B.2 A Provider creates a new Resource description</b> (6 detailed requirements, 29 Epics, 68 user stories)</summary>

| ID | Detailed requirement | Status | Epics | User stories |
| --- | --- | --- | --- | --- |
| 5B.2.1 | [A provider retrieves a Resource type](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14730/README.md) | Defined | 2 | 7 |
| 5B.2.2 | [A Provider adds common metadata to a new Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14731/README.md) | Defined | 3 | 8 |
| 5B.2.3 | [A Provider adds resource type specific metadata to a new Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14732/README.md) | Defined | 6 | 6 |
| 5B.2.4 | [A Provider assigns access policies to a new Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14733/README.md) | In Progress | 5 | 14 |
| 5B.2.5 | [A Provider assigns usage policies to a new Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14734/README.md) | In Progress | 4 | 9 |
| 5B.2.6 | [A Provider adds a set of contract clauses to a new resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/README.md) | In Progress | 9 | 24 |

</details>

<details><summary><b>5B.3 A Provider creates a new version of a Resource description</b> (5 detailed requirements, 11 Epics, 22 user stories)</summary>

| ID | Detailed requirement | Status | Epics | User stories |
| --- | --- | --- | --- | --- |
| 5B.3.1 | [A Provider updates the common metadata of a new version of a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14738/README.md) | Defined | 4 | 6 |
| 5B.3.2 | [A Provider updates the resource type specific metadata of a new version of a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14739/README.md) | Defined | 3 | 4 |
| 5B.3.3 | [A Provider updates the access policies of a new version of a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14740/README.md) | In Progress | 1 | 5 |
| 5B.3.4 | [A Provider updates the usage policies of a new version of a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14741/README.md) | In Progress | 1 | 5 |
| 5B.3.5 | [A Provider updates the set of contract clauses of a new version of a resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14743/README.md) | Defined | 2 | 2 |

</details>

<details><summary><b>5B.4 A Provider signs and submits a new Resource description or a new version</b> (1 detailed requirement, 3 Epics, 13 user stories)</summary>

| ID | Detailed requirement | Status | Epics | User stories |
| --- | --- | --- | --- | --- |
| 5B.4.1 | [A Provider signs and submits a new Resource description or a new version of a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14729/L2-SIMPL-14745/README.md) | Defined | 3 | 13 |

</details>

<details><summary><b>5B.5 The Governance Authority publishes a Resource description</b> (13 detailed requirements, 16 Epics, 18 user stories)</summary>

| ID | Detailed requirement | Status | Epics | User stories |
| --- | --- | --- | --- | --- |
| 5B.5.1 | [The Governance Authority validates the schema of a new Resource description or a new version of a Resource Description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14689/README.md) | Defined | 1 | 1 |
| 5B.5.2 | [The Governance Authority notifies a Provider about an inactive schema](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14746/README.md) | Defined | 1 | 1 |
| 5B.5.3 | [The Governance Authority validates the versioning of a new version of a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14864/README.md) | Defined | 1 | 1 |
| 5B.5.4 | [The Governance Authority notifies a Provider about wrong versioning of new versions of a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14865/README.md) | Defined | 1 | 1 |
| 5B.5.5 | [The Governance Authority validates the syntax of a new Resource description or a new version of a Resource Description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14747/README.md) | In Progress | 2 | 3 |
| 5B.5.6 | [The Governance Authority provides feedback to a Provider about syntax validation errors](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14748/README.md) | In Progress | 2 | 2 |
| 5B.5.7 | [The Governance Authority validates the semantics of a new Resource description or a new version of a Resource Description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14749/README.md) | Defined | 2 | 2 |
| 5B.5.8 | [The Governance Authority provides feedback to a Provider about semantic validation errors](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14750/README.md) | Defined | 1 | 1 |
| 5B.5.9 | [The Governance Authority publishes a new Resource description or a new version of a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14753/README.md) | Defined | 1 | 0 |
| 5B.5.10 | [The Governance Authority revokes the previous version of a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14754/README.md) | Defined | 1 | 1 |
| 5B.5.11 | [The Governance Authority evaluates the metadata quality of a new resource description or a new version of a resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-22687/README.md) | Defined | 1 | 3 |
| 5B.5.12 | [The Governance Authority notifies the Provider about the metadata quality](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-22688/README.md) | Defined | 1 | 1 |
| 5B.5.13 | [The Governance Authority approves/rejects the metadata of a new resource description or a new version of a resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-24049/README.md) | Defined | 1 | 1 |

</details>

<details><summary><b>5B.6 A Provider notifies Consumers about a new version of a Resource description</b> (1 detailed requirement, 1 Epic, 1 user story)</summary>

| ID | Detailed requirement | Status | Epics | User stories |
| --- | --- | --- | --- | --- |
| 5B.6.1 | [A Provider notifies Consumers about a new version of a Resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-21118/L2-SIMPL-14755/README.md) | In Progress | 1 | 1 |

</details>

<details><summary><b>5B.7 A Provider requests the revocation of a Resource description</b> (1 detailed requirement, 1 Epic, 2 user stories)</summary>

| ID | Detailed requirement | Status | Epics | User stories |
| --- | --- | --- | --- | --- |
| 5B.7.1 | [A Provider requests the revocation of a resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14699/L2-SIMPL-14639/README.md) | Defined | 1 | 2 |

</details>

<details><summary><b>5B.8 The Governance Authority revokes a resource description</b> (1 detailed requirement, 1 Epic, 2 user stories)</summary>

| ID | Detailed requirement | Status | Epics | User stories |
| --- | --- | --- | --- | --- |
| 5B.8.1 | [The Governance Authority revokes a resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-14640/L2-SIMPL-14641/README.md) | Defined | 1 | 2 |

</details>

<details><summary><b>5B.9 The Provider notifies Consumers about the revocation of a resource description</b> (1 detailed requirement, 1 Epic, 2 user stories)</summary>

| ID | Detailed requirement | Status | Epics | User stories |
| --- | --- | --- | --- | --- |
| 5B.9.1 | [A Provider notifies Consumers about the revocation of a resource description](../../functional-requirements/bp05b-provider-manages-resource-descriptions/L0-SIMPL-10768/L1-SIMPL-21117/L2-SIMPL-14642/README.md) | In Progress | 1 | 2 |

</details>

