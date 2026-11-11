<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/tree/main/bp05b-provider-manages-resource-descriptions?ref_type=heads) / 5B.3 - A Provider creates a new version of a Resource description
</div>

# 5B.3 - A Provider creates a new version of a Resource description

## Canonical source
[https://simpl-programme.ec.europa.eu/book-page/tbp-5b3-provider-creates-new-version-resource-description](https://simpl-programme.ec.europa.eu/book-page/tbp-5b3-provider-creates-new-version-resource-description)

## About this requirement

This is high-level requirement 5B.3 of business process [BP05B - Provider manages resource descriptions](./README.md). In the backlog it is item SIMPL-14728, status **In Progress**. The status is the one in the backlog (Jira). The requirements site shows all requirements as Proposed.

In the process it covers 3 steps: [BP05B.08 Create a new version of a resource description](./README.md#bp05b08-create-a-new-version-of-a-resource-description), [BP05B.09 Update resource type specific metadata](./README.md#bp05b09-update-resource-type-specific-metadata) and [BP05B.10 Update policies and contract related metadata](./README.md#bp05b10-update-policies-and-contract-related-metadata). The dynamic view shows it in [Define and publish self-description](bp05b-dynamic-view.md#define-and-publish-self-description).

It is split into 5 detailed requirements, delivered through 11 Epics with 23 user stories in total. Each detailed requirement below says what Simpl-Open must do. Under it are its Epics: open one to read its user stories and their acceptance criteria.

## Description

Simpl-Open shall allow a Provider

- to create a new version of a previously published Resource description based on the latest version of the schema for the resource type of that Resource Description.

## Detailed requirements

Select a title to jump to its section below.

| ID | Detailed requirement | Status | Process step | Epics | User stories |
| --- | --- | --- | --- | --- | --- |
| 5B.3.1 | [A Provider updates the common metadata of a new version of a Resource description](#5b31-a-provider-updates-the-common-metadata-of-a-new-version-of-a-resource-description) | Defined | BP05B.08 Create a new version of a resource description | 4 | 7 |
| 5B.3.2 | [A Provider updates the resource type specific metadata of a new version of a Resource description](#5b32-a-provider-updates-the-resource-type-specific-metadata-of-a-new-version-of-a-resource-description) | Defined | BP05B.09 Update resource type specific metadata | 3 | 4 |
| 5B.3.3 | [A Provider updates the access policies of a new version of a Resource description](#5b33-a-provider-updates-the-access-policies-of-a-new-version-of-a-resource-description) | In Progress | BP05B.10 Update policies and contract related metadata | 1 | 5 |
| 5B.3.4 | [A Provider updates the usage policies of a new version of a Resource description](#5b34-a-provider-updates-the-usage-policies-of-a-new-version-of-a-resource-description) | In Progress | BP05B.10 Update policies and contract related metadata | 1 | 5 |
| 5B.3.5 | [A Provider updates the set of contract clauses of a new version of a resource description](#5b35-a-provider-updates-the-set-of-contract-clauses-of-a-new-version-of-a-resource-description) | Defined | BP05B.10 Update policies and contract related metadata | 2 | 2 |

### 5B.3.1 A Provider updates the common metadata of a new version of a Resource description

**Status:** Defined (backlog item SIMPL-14738)<br>
**Process step:** [BP05B.08 Create a new version of a resource description](./README.md#bp05b08-create-a-new-version-of-a-resource-description)<br>
**Public text:** [5B.3.1 on the requirements site](https://simpl-programme.ec.europa.eu/book-page/tbp-5b31-provider-updates-common-metadata-new-version-resource-description)

Simpl-Open shall allow a Provider via an UI and an API

- to update the common metadata, which are present for every type of Resource, in a new version of a Resource description.

**Delivered through 4 Epics.** Each Epic page has the user stories and their acceptance criteria.

- [Epic: A Provider updates the resource source address for resource sharing](./bp5b3-provider-creates-new-version-resource-description/simpl-12250-a-provider-updates-the-resource-source-address-for-resource.md)<br>
  *As a Provider, I want to update via API or UI, as part of a resource description, the Resource Source based on the resource address template that is valid for the resource sharing method, which I use for sharing my resources* · 1 user story · status Defined
- [Epic: A Provider updates common metadata of a new version of a Resource description via UI](./bp5b3-provider-creates-new-version-resource-description/simpl-18028-a-provider-updates-common-metadata-of-a-new-version-of-a-res.md)<br>
  *As a Provider end user, I want to update the common metadata of a new version of a resource description via UI* · 5 user stories · status Defined
- [Epic: A Provider starts the creation of a new version of a Resource description via UI](./bp5b3-provider-creates-new-version-resource-description/simpl-19293-a-provider-starts-the-creation-of-a-new-version-of-a-resourc.md)<br>
  *As a Provider end user, I want to start the creation of a new version of a Resource description via UI* · 1 user story · status Defined
- [Epic: A Provider updates common metadata of a new version of a Resource description via UI (Improvement)](./bp5b3-provider-creates-new-version-resource-description/simpl-32140-a-provider-updates-common-metadata-of-a-new-version-of-a-res.md)<br>
  *As a Provider end user, I want to update the common metadata of a new version of a resource description via UI* · no user stories yet · status For DG Connect Review

### 5B.3.2 A Provider updates the resource type specific metadata of a new version of a Resource description

**Status:** Defined (backlog item SIMPL-14739)<br>
**Process step:** [BP05B.09 Update resource type specific metadata](./README.md#bp05b09-update-resource-type-specific-metadata)<br>
**Public text:** [5B.3.2 on the requirements site](https://simpl-programme.ec.europa.eu/book-page/tbp-5b32-provider-updates-resource-type-specific-metadata-new-version-resource)

Simpl-Open shall allow a Provider via an UI and an API

- to update the resource type specific metadata of a new version of a Resource description.

**Delivered through 3 Epics.** Each Epic page has the user stories and their acceptance criteria.

- [Epic: A Provider updates the resource type specific metadata of a new version of a Resource description via UI](./bp5b3-provider-creates-new-version-resource-description/simpl-18029-a-provider-updates-the-resource-type-specific-metadata-of-a.md)<br>
  *As a Provider end user, I want to update the resource type specific metadata of a new version of resource description via UI* · 3 user stories · status Defined
- [Epic: A Provider updates the deployment script of a new version of an infrastructure Resource description via UI](./bp5b3-provider-creates-new-version-resource-description/simpl-21310-a-provider-updates-the-deployment-script-of-a-new-version-of.md)<br>
  *As a Provider end user, I want to update the deployment script of a new version of an infrastructure resource description via UI* · no user stories yet · status Defined
- [Epic: A Provider updates the link to the deployment script in a new version of an infrastructure Resource description via UI](./bp5b3-provider-creates-new-version-resource-description/simpl-24801-a-provider-updates-the-link-to-the-deployment-script-in-a-ne.md)<br>
  *As a Provider end user, I want to update the link to the deployment script in a new version of an infrastructure resource description via UI* · 1 user story · status Defined

### 5B.3.3 A Provider updates the access policies of a new version of a Resource description

**Status:** In Progress (backlog item SIMPL-14740)<br>
**Process step:** [BP05B.10 Update policies and contract related metadata](./README.md#bp05b10-update-policies-and-contract-related-metadata)<br>
**Public text:** [5B.3.3 on the requirements site](https://simpl-programme.ec.europa.eu/book-page/tbp-5b33-provider-updates-access-policies-new-version-resource-description)

Simpl-Open shall allow a Provider via an UI and an API

- to update the access polices of a new version of a Resource description.

**Delivered through 1 Epic.** Each Epic page has the user stories and their acceptance criteria.

- [Epic: A Provider updates the access policies of a new version of a Resource description by using policy templatesvia UI](./bp5b3-provider-creates-new-version-resource-description/simpl-4887-a-provider-updates-the-access-policies-of-a-new-version-of-a.md)<br>
  *As a Provider end user, I want to use, via UI, an access policy template to assign an access policy to a new version of a resource description* · 5 user stories · status In Progress

### 5B.3.4 A Provider updates the usage policies of a new version of a Resource description

**Status:** In Progress (backlog item SIMPL-14741)<br>
**Process step:** [BP05B.10 Update policies and contract related metadata](./README.md#bp05b10-update-policies-and-contract-related-metadata)<br>
**Public text:** [5B.3.4 on the requirements site](https://simpl-programme.ec.europa.eu/book-page/tbp-5b34-provider-updates-usage-policies-new-version-resource-description)

Simpl-Open shall allow a Provider via an UI and an API

- to update the usage policies of a new version of a Resource description.

**Delivered through 1 Epic.** Each Epic page has the user stories and their acceptance criteria.

- [Epic: A Provider updates the usage policies of a new version of a resource description by using policy template via UI](./bp5b3-provider-creates-new-version-resource-description/simpl-18030-a-provider-updates-the-usage-policies-of-a-new-version-of-a.md)<br>
  *As a Provider end user, I want to update the usage policies using an usage policy template to a new version of a resource description via UI* · 5 user stories · status In Progress

### 5B.3.5 A Provider updates the set of contract clauses of a new version of a resource description

**Status:** Defined (backlog item SIMPL-14743)<br>
**Process step:** [BP05B.10 Update policies and contract related metadata](./README.md#bp05b10-update-policies-and-contract-related-metadata)<br>
**Public text:** [5B.3.5 on the requirements site](https://simpl-programme.ec.europa.eu/book-page/tbp-5b35-provider-updates-set-contract-clauses-new-version-resource-description)

Simpl-Open shall allow a Provider via an UI and an API

- to update the set of contract clauses of a new version of a Resource description.

**Delivered through 2 Epics.** Each Epic page has the user stories and their acceptance criteria.

- [Epic: A Provider updates the contract clauses, resulting in a new version of the resource description via UI](./bp5b3-provider-creates-new-version-resource-description/simpl-16252-a-provider-updates-the-contract-clauses-resulting-in-a-new-v.md)<br>
  *As a Provider, I want to update contract clauses resulting in a new version of the resource description* · 1 user story · status Defined
- [Epic: A Provider removes contract clauses from a resource description via UI, resulting in a new version of the resource description](./bp5b3-provider-creates-new-version-resource-description/simpl-30635-a-provider-removes-contract-clauses-from-a-resource-descript.md)<br>
  *As a Provider, I want to remove contract clauses from a resource description, resulting in a new version of the resource description* · 1 user story · status Defined
