<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/tree/main/bp05b-provider-manages-resource-descriptions?ref_type=heads) / 5B.1 - A Provider consults its own Resource descriptions
</div>

# 5B.1 - A Provider consults its own Resource descriptions

## Canonical source
[https://simpl-programme.ec.europa.eu/book-page/tbp-5b1-provider-consults-its-own-resource-descriptions](https://simpl-programme.ec.europa.eu/book-page/tbp-5b1-provider-consults-its-own-resource-descriptions)

## About this requirement

This is high-level requirement 5B.1 of business process [BP05B - Provider manages resource descriptions](./README.md). In the backlog (Jira) it is item SIMPL-12897.

It has no process step of its own: a Provider consults its descriptions before deciding what to do, so it belongs to the [Trigger (resource description management)](./README.md#trigger--resource-description-management) of the process. The dynamic view shows it in [Retrieve SD metadata](bp05b-dynamic-view.md#retrieve-sd-metadata) and [Retrieve full self-description](bp05b-dynamic-view.md#retrieve-full-self-description).

It is split into 3 detailed requirements, delivered through 5 Epics with 11 user stories in total. Each detailed requirement below says what Simpl-Open must do. Under it are its Epics: open one to read its user stories and their acceptance criteria.

## Description

Simpl-Open shall allow a Provider

- to consult its own resource descriptions.

## Detailed requirements

Select a title to jump to its section below.

| ID | Detailed requirement | Process step | Epics | User stories |
| --- | --- | --- | --- | --- |
| 5B.1.1 | [A Provider consults an overview of its Resource descriptions](#5b11-a-provider-consults-an-overview-of-its-resource-descriptions) | Trigger | 1 | 4 |
| 5B.1.2 | [A Provider consults the details of one of its own resource descriptions](#5b12-a-provider-consults-the-details-of-one-of-its-own-resource-descriptions) | Trigger | 3 | 4 |
| 5B.1.3 | [A Provider consults the version history of one of its own resource descriptions](#5b13-a-provider-consults-the-version-history-of-one-of-its-own-resource-descriptions) | Trigger | 1 | 3 |

### 5B.1.1 A Provider consults an overview of its Resource descriptions

**Process step:** [Trigger (resource description management)](./README.md#trigger--resource-description-management)<br>
**Public text:** [5B.1.1 on the requirements site](https://simpl-programme.ec.europa.eu/book-page/tbp-5b11-provider-consults-overview-its-resource-descriptions)

Simpl-Open shall allow a Provider via UI and API

- to consult the inventory of its own Resource descriptions.

**Delivered through 1 Epic.** Each Epic page has the user stories and their acceptance criteria.

- [Epic: A Provider consults an overview of its Resource descriptions via UI and API](./bp5b1-provider-consults-its-own-resource-descriptions/simpl-12899-a-provider-consults-an-overview-of-its-resource-descriptions.md)<br>
  *As a Provider, I want to retrieve an overview of my own Resource descriptions via UI or API* · 4 user stories

### 5B.1.2 A Provider consults the details of one of its own resource descriptions

**Process step:** [Trigger (resource description management)](./README.md#trigger--resource-description-management)<br>
**Public text:** [5B.1.2 on the requirements site](https://simpl-programme.ec.europa.eu/book-page/tbp-5b12-provider-consults-details-one-its-own-resource-descriptions)

Simpl-Open shall allow a Provider via UI and API

- to consult the details of one of its own Resource descriptions.

**Delivered through 3 Epics.** Each Epic page has the user stories and their acceptance criteria.

- [Epic: A Provider consults the full contents of one of its own Resource descriptions via UI and API](./bp5b1-provider-consults-its-own-resource-descriptions/simpl-12905-a-provider-consults-the-full-contents-of-one-of-its-own-reso.md)<br>
  *As a Provider, I want to retrieve the full contents of one of my own Resource descriptions via UI or API* · 3 user stories
- [Epic: A Provider consults the policies of one of its own resource descriptions via UI](./bp5b1-provider-consults-its-own-resource-descriptions/simpl-25590-a-provider-consults-the-policies-of-one-of-its-own-resource.md)<br>
  *As a Provider, I want to retrieve the policies of one of my own Resource descriptions via UI* · no user stories yet
- [Epic: A Provider triggers the update of the resource consent assertion related to a selected resource description via UI](./bp5b1-provider-consults-its-own-resource-descriptions/simpl-30292-a-provider-triggers-the-update-of-the-resource-consent-asser.md)<br>
  *As a Provider, I want to trigger the update of the resource consent assertion related to a selected resource description via UI* · 1 user story

### 5B.1.3 A Provider consults the version history of one of its own resource descriptions

**Process step:** [Trigger (resource description management)](./README.md#trigger--resource-description-management)<br>
**Public text:** [5B.1.3 on the requirements site](https://simpl-programme.ec.europa.eu/book-page/tbp-5b13-provider-consults-version-history-one-its-own-resource-descriptions)

Simpl-Open shall allow a Provider via UI and API

- to consult the version history of one of its own Resource descriptions.

**Delivered through 1 Epic.** Each Epic page has the user stories and their acceptance criteria.

- [Epic: A Provider consults the version history of one of its own resource descriptions](./bp5b1-provider-consults-its-own-resource-descriptions/simpl-13658-a-provider-consults-the-version-history-of-one-of-its-own-re.md)<br>
  *As a Provider, I want to retrieve the version history of one of my own resource descriptions via UI or API* · 3 user stories
