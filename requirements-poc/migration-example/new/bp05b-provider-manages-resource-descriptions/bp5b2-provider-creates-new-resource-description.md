<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/tree/main/bp05b-provider-manages-resource-descriptions?ref_type=heads) / 5B.2 - A Provider creates a new Resource description
</div>

# 5B.2 - A Provider creates a new Resource description

## Canonical source
[https://simpl-programme.ec.europa.eu/book-page/tbp-5b2-provider-creates-new-resource-description](https://simpl-programme.ec.europa.eu/book-page/tbp-5b2-provider-creates-new-resource-description)

## About this requirement

This is high-level requirement 5B.2 of business process [BP05B - Provider manages resource descriptions](./README.md). In the backlog it is item SIMPL-14688, status **In Progress**. The status is the one in the backlog (Jira). The requirements site shows all requirements as Proposed.

In the process it covers 3 steps: [BP05B.01 Create a new resource description](./README.md#bp05b01-create-a-new-resource-description), [BP05B.02 Add resource type specific metadata](./README.md#bp05b02-add-resource-type-specific-metadata) and [BP05B.03 Add policy and contract related metadata](./README.md#bp05b03-add-policy-and-contract-related-metadata). The dynamic view shows it in [Define and publish self-description](bp05b-dynamic-view.md#define-and-publish-self-description) and [the technology components view](bp05b-tcv-dynamic-view.md).

It is split into 6 detailed requirements, delivered through 29 Epics with 68 user stories in total. Each detailed requirement below says what Simpl-Open must do. Under it are its Epics: open one to read its user stories and their acceptance criteria.

## Description

Simpl-Open shall allow a Provider

- to create new resource descriptions based on the latest versions of the resource description schema for the resource types.

## Detailed requirements

Select a title to jump to its section below.

| ID | Detailed requirement | Status | Process step | Epics | User stories |
| --- | --- | --- | --- | --- | --- |
| 5B.2.1 | [A provider retrieves a Resource type](#5b21-a-provider-retrieves-a-resource-type) | Defined | BP05B.01 Create a new resource description | 2 | 7 |
| 5B.2.2 | [A Provider adds common metadata to a new Resource description](#5b22-a-provider-adds-common-metadata-to-a-new-resource-description) | Defined | BP05B.01 Create a new resource description | 3 | 8 |
| 5B.2.3 | [A Provider adds resource type specific metadata to a new Resource description](#5b23-a-provider-adds-resource-type-specific-metadata-to-a-new-resource-description) | Defined | BP05B.02 Add resource type specific metadata | 6 | 6 |
| 5B.2.4 | [A Provider assigns access policies to a new Resource description](#5b24-a-provider-assigns-access-policies-to-a-new-resource-description) | In Progress | BP05B.03 Add policy and contract related metadata | 5 | 14 |
| 5B.2.5 | [A Provider assigns usage policies to a new Resource description](#5b25-a-provider-assigns-usage-policies-to-a-new-resource-description) | In Progress | BP05B.03 Add policy and contract related metadata | 4 | 9 |
| 5B.2.6 | [A Provider adds a set of contract clauses to a new resource description](#5b26-a-provider-adds-a-set-of-contract-clauses-to-a-new-resource-description) | In Progress | BP05B.03 Add policy and contract related metadata | 9 | 24 |

### 5B.2.1 A provider retrieves a Resource type

**Status:** Defined (backlog item SIMPL-14730)<br>
**Process step:** [BP05B.01 Create a new resource description](./README.md#bp05b01-create-a-new-resource-description)<br>
**Public text:** [5B.2.1 on the requirements site](https://simpl-programme.ec.europa.eu/book-page/tbp-5b21-provider-retrieves-resource-type)

Simpl-Open shall allow a Provider via UI and API to

- retrieve a Resource type with the latest version of its associated schema.

**Delivered through 2 Epics.** Each Epic page has the user stories and their acceptance criteria.

- [Epic: A Provider retrieves from the Governance Authority a list of published resource types](./bp5b2-provider-creates-new-resource-description/simpl-2285-a-provider-retrieves-from-the-governance-authority-a-list-of.md)<br>
  *As a Provider, I want to retrieve a list of published resource types from the Governance Authority* · 3 user stories · status Defined
- [Epic: A Provider retrieves from the Governance Authority the schema of a published resource type](./bp5b2-provider-creates-new-resource-description/simpl-19176-a-provider-retrieves-from-the-governance-authority-the-schem.md)<br>
  *As a Provider, I want to retrieve the schema of a published resource type from the Governance Authority* · 4 user stories · status Defined

### 5B.2.2 A Provider adds common metadata to a new Resource description

**Status:** Defined (backlog item SIMPL-14731)<br>
**Process step:** [BP05B.01 Create a new resource description](./README.md#bp05b01-create-a-new-resource-description)<br>
**Public text:** [5B.2.2 on the requirements site](https://simpl-programme.ec.europa.eu/book-page/tbp-5b22-provider-adds-common-metadata-new-resource-description)

Simpl-Open shall allow and support a Provider via UI and API

- to add common metadata, which are present for every type of Resource, to a new Resource description.

**Delivered through 3 Epics.** Each Epic page has the user stories and their acceptance criteria.

- [Epic: A Provider defines the resource source address for resource sharing](./bp5b2-provider-creates-new-resource-description/simpl-8340-a-provider-defines-the-resource-source-address-for-resource.md)<br>
  *As a Provider, I want to provide via API or UI, as part of a resource description, the Resource Source based on the resource address template that is valid for the resource sharing method, which I use for sharing my resources* · 6 user stories · status Defined
- [Epic: A Provider adds common metadata to a new Resource description via UI](./bp5b2-provider-creates-new-resource-description/simpl-15863-a-provider-adds-common-metadata-to-a-new-resource-descriptio.md)<br>
  *As a Provider end user, I want to add common metadata to a new resource description via UI* · 2 user stories · status Defined
- [Epic: A Provider defines the resource source address for resource sharing [Improvement]](./bp5b2-provider-creates-new-resource-description/simpl-25754-a-provider-defines-the-resource-source-address-for-resource.md)<br>
  no user stories yet · status Defined

### 5B.2.3 A Provider adds resource type specific metadata to a new Resource description

**Status:** Defined (backlog item SIMPL-14732)<br>
**Process step:** [BP05B.02 Add resource type specific metadata](./README.md#bp05b02-add-resource-type-specific-metadata)<br>
**Public text:** [5B.2.3 on the requirements site](https://simpl-programme.ec.europa.eu/book-page/tbp-5b23-provider-adds-resource-type-specific-metadata-new-resource-description)

Simpl-Open shall allow a Provider via UI and API

- to add resource type specific metadata to a new Resource description.

**Delivered through 6 Epics.** Each Epic page has the user stories and their acceptance criteria.

- [Epic: A Provider adds resource type specific metadata to a new Resource description via UI](./bp5b2-provider-creates-new-resource-description/simpl-17409-a-provider-adds-resource-type-specific-metadata-to-a-new-res.md)<br>
  *As a Provider end user, I want to add resource type specific metadata to a new resource description via UI* · 3 user stories · status Defined
- [Epic: A Provider uploads a deployment script to a new infrastructure Resource description via UI](./bp5b2-provider-creates-new-resource-description/simpl-21309-a-provider-uploads-a-deployment-script-to-a-new-infrastructu.md)<br>
  *As a Provider end user, I want to add a deployment script to a new infrastructure resource description via UI* · 2 user stories · status Defined
- [Epic: Management of scripts and their metadata](./bp5b2-provider-creates-new-resource-description/simpl-23719-management-of-scripts-and-their-metadata.md)<br>
  no user stories yet · status In Preparation
- [Epic: A Provider links a deployment script to a new infrastructure Resource description via UI](./bp5b2-provider-creates-new-resource-description/simpl-24800-a-provider-links-a-deployment-script-to-a-new-infrastructure.md)<br>
  *As a Provider end user, I want to link a deployment script to a new infrastructure resource description via UI* · 1 user story · status Defined
- [Epic: Provisioner API URL Configuration via Environment Variables (Remove Manual UI Property) [Improvement]](./bp5b2-provider-creates-new-resource-description/simpl-29946-provisioner-api-url-configuration-via-environment-variables.md)<br>
  *As a Provider end user, I want the Provisioner API URL to be managed through deployment configuration instead of manual UI input* · no user stories yet · status New
- [Epic: A Provider sets the VPN configuration for Agent-to-Agent data transfer via UI](./bp5b2-provider-creates-new-resource-description/simpl-30016-a-provider-sets-the-vpn-configuration-for-agent-to-agent-dat.md)<br>
  *As a Provider, I want to indicate in a data resource description if the transfer should use a secured VPN tunnel* · no user stories yet · status Defined

### 5B.2.4 A Provider assigns access policies to a new Resource description

**Status:** In Progress (backlog item SIMPL-14733)<br>
**Process step:** [BP05B.03 Add policy and contract related metadata](./README.md#bp05b03-add-policy-and-contract-related-metadata)<br>
**Public text:** [5B.2.4 on the requirements site](https://simpl-programme.ec.europa.eu/book-page/tbp-5b24-provider-assigns-access-policies-new-resource-description)

Simpl-Open shall allow a Provider via UI and API

- to assign access policies to a new Resource description.

**Delivered through 5 Epics.** Each Epic page has the user stories and their acceptance criteria.

- [Epic: A Provider is supported in defining an access policy by a using policy template via API](./bp5b2-provider-creates-new-resource-description/simpl-935-a-provider-is-supported-in-defining-an-access-policy-by-a-us.md)<br>
  *As a Provider, I want to be supported in defining an access policy through policy templates, by retrieving via API* · 4 user stories · status In Progress
- [Epic: A Provider assigns access policies to a new resource description by using policy template via UI](./bp5b2-provider-creates-new-resource-description/simpl-936-a-provider-assigns-access-policies-to-a-new-resource-descrip.md)<br>
  *As a Provider, I want to use, via UI, an access policy template to assign an access policy to a new resource description* · 8 user stories · status In Progress
- [Epic: A Provider adds a new access policy template](./bp5b2-provider-creates-new-resource-description/simpl-29315-a-provider-adds-a-new-access-policy-template.md)<br>
  *As a Provider, I want to add a new access policy template* · no user stories yet · status In Progress
- [Epic: A Provider assigns access policies related to resource consent to a new resource description via UI](./bp5b2-provider-creates-new-resource-description/simpl-29973-a-provider-assigns-access-policies-related-to-resource-conse.md)<br>
  *As a Provider, I want to assign access policies related to resource consent to a new resource description via UI* · 1 user story · status Defined
- [Epic: A Provider assigns access policies related to contract consent to a new resource description via UI](./bp5b2-provider-creates-new-resource-description/simpl-29974-a-provider-assigns-access-policies-related-to-contract-conse.md)<br>
  *As a Provider, I want to assign access policies related to contract consent to a new resource description via UI* · 1 user story · status Defined

### 5B.2.5 A Provider assigns usage policies to a new Resource description

**Status:** In Progress (backlog item SIMPL-14734)<br>
**Process step:** [BP05B.03 Add policy and contract related metadata](./README.md#bp05b03-add-policy-and-contract-related-metadata)<br>
**Public text:** [5B.2.5 on the requirements site](https://simpl-programme.ec.europa.eu/book-page/tbp-5b25-provider-assigns-usage-policies-new-resource-description)

Simpl-Open shall allow a Provider via UI and API

- to assign usage policies to a new Resource description.

**Delivered through 4 Epics.** Each Epic page has the user stories and their acceptance criteria.

- [Epic: A Provider is supported in defining a usage policy by using a policy template via API](./bp5b2-provider-creates-new-resource-description/simpl-960-a-provider-is-supported-in-defining-a-usage-policy-by-using.md)<br>
  *As a Provider, I want to be supported in defining a usage policy through policy templates, by retrieving via API* · 3 user stories · status In Progress
- [Epic: A Provider assigns usage policies to a new resource description by using policy template via UI](./bp5b2-provider-creates-new-resource-description/simpl-961-a-provider-assigns-usage-policies-to-a-new-resource-descript.md)<br>
  *As a Provider, I want to use, via UI, an usage policy template to assign an usage policy to a new resource description* · 6 user stories · status In Progress
- [Epic: A Provider defines a policy information point to monitor usage policies](./bp5b2-provider-creates-new-resource-description/simpl-28901-a-provider-defines-a-policy-information-point-to-monitor-usa.md)<br>
  *As a Provider, I want to define a policy information point based on the consumption logs of my resources* · no user stories yet · status Defined
- [Epic: A Provider adds a new usage policy template](./bp5b2-provider-creates-new-resource-description/simpl-29331-a-provider-adds-a-new-usage-policy-template.md)<br>
  *As a Provider, I want to add a new usage policy template* · no user stories yet · status In Progress

### 5B.2.6 A Provider adds a set of contract clauses to a new resource description

**Status:** In Progress (backlog item SIMPL-14736)<br>
**Process step:** [BP05B.03 Add policy and contract related metadata](./README.md#bp05b03-add-policy-and-contract-related-metadata)<br>
**Public text:** [5B.2.6 on the requirements site](https://simpl-programme.ec.europa.eu/book-page/tbp-5b26-provider-adds-set-contract-clauses-new-resource-description)

Simpl-Open shall allow a Provider, via UI and API,

- to add a set of contract clauses to a new resource Description

**Delivered through 9 Epics.** Each Epic page has the user stories and their acceptance criteria.

- [Epic: A Provider adds contract clauses to a new Resource description via UI](./bp5b2-provider-creates-new-resource-description/simpl-16251-a-provider-adds-contract-clauses-to-a-new-resource-descripti.md)<br>
  *As a Provider, I want to add contract clauses to a new resource description* · 9 user stories · status In Progress
- [Epic: A Provider defines an endpoint that serves as an information point to calculate the billing data](./bp5b2-provider-creates-new-resource-description/simpl-28461-a-provider-defines-an-endpoint-that-serves-as-an-information.md)<br>
  *As a Provider, I want to define a billing information point based on the consumption logs of my resources and any other relevant logs* · 1 user story · status Defined
- [Epic: A Provider defines an endpoint that serves as an information point to check the credit status of their usage contract](./bp5b2-provider-creates-new-resource-description/simpl-28462-a-provider-defines-an-endpoint-that-serves-as-an-information.md)<br>
  *As a Provider, I want to define a payment information point* · 1 user story · status Defined
- [Epic: A Provider manages contract clauses [MVP]](./bp5b2-provider-creates-new-resource-description/simpl-28989-a-provider-manages-contract-clauses-mvp.md)<br>
  6 user stories · status For DG Connect Review
- [Epic: A Provider defines an information point to monitor SLAs](./bp5b2-provider-creates-new-resource-description/simpl-29121-a-provider-defines-an-information-point-to-monitor-slas.md)<br>
  *As a Provider, I want to define an SLA information point based on the consumption logs of my resources and other relevant logs* · no user stories yet · status Defined
- [Epic: A Provider adds a contract clauses related resource consent of the data resource to a new resource description via UI](./bp5b2-provider-creates-new-resource-description/simpl-29975-a-provider-adds-a-contract-clauses-related-resource-consent.md)<br>
  *As a Provider, I want to add a contract clause related to resource consent of the data resource to a new resource description via UI* · 2 user stories · status Defined
- [Epic: A Provider adds a contract clauses related to the intended purpose of the data resource to a new resource description via UI](./bp5b2-provider-creates-new-resource-description/simpl-29976-a-provider-adds-a-contract-clauses-related-to-the-intended-p.md)<br>
  *As a Provider, I want to add a contract clause related to the intended purpose of the data resource to a new resource description via UI* · 2 user stories · status Defined
- [Epic: A Provider adds contract clauses related to contract consent to a new resource description via UI](./bp5b2-provider-creates-new-resource-description/simpl-29977-a-provider-adds-contract-clauses-related-to-contract-consent.md)<br>
  *As a Provider, I want to add a contract clause related to contract consent of the data resource to a new resource description via UI* · 2 user stories · status Defined
- [Epic: A Provider adds contract clauses to a new Resource description via UI [Extension]](./bp5b2-provider-creates-new-resource-description/simpl-30655-a-provider-adds-contract-clauses-to-a-new-resource-descripti.md)<br>
  1 user story · status Defined
