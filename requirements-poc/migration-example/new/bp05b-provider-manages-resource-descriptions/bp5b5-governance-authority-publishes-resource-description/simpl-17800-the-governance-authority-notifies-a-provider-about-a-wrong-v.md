<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.5 - The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md) / [5B.5.4 The Governance Authority notifies a Provider about wrong versioning of new versions of a Resource description](../bp5b5-governance-authority-publishes-resource-description.md#5b54-the-governance-authority-notifies-a-provider-about-wrong-versioning-of-new-versions-of-a-resource-description) / Epic
</div>

# Epic: The Governance Authority notifies a Provider about a wrong version number of a new version of a Resource description

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.5.4 The Governance Authority notifies a Provider about wrong versioning of new versions of a Resource description](../bp5b5-governance-authority-publishes-resource-description.md#5b54-the-governance-authority-notifies-a-provider-about-wrong-versioning-of-new-versions-of-a-resource-description), under high-level requirement [5B.5 The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md). In the process it belongs to step [BP05B.06 Remediate validation issues](../README.md#bp05b06-remediate-validation-issues).

In the backlog (Jira) it is item SIMPL-17800. It has 1 user story, each with acceptance criteria that say when the work is done. For each story, the development status shows whether it is Planned, In development or Developed (built, not necessarily released yet).

## What this Epic delivers

#### Storycard

**As** Governance Authority\
**I want to** notify Providers about a wrong version number of a new version of a resource description\
**So that** the Provider can correct the error.

#### Acceptance criteria

1. **Given** an algorithm to calculate and verify version numbers of resource description is defined by me, the Governance Authority\
   **And** I have received a new resource description or a new version of a resource description with an incorrect version number\
   **When** it is checked that the version number of the new version of a Resource description is correct based on the version numbering algorithm\
   **And** version number validation for the new version of a Resource description has the result "invalid".\
   **Then** a notification about an version numbering error is sent to the Provider.

## User stories

1. [[API] The Governance Authority notifies a Provider about a wrong version number of a new version of a Resource description](#user-story-1-api-the-governance-authority-notifies-a-provider-about-a-wrong-version-number-of-a-new-version-of-a-resource-description) · Developed

### User story 1: [API] The Governance Authority notifies a Provider about a wrong version number of a new version of a Resource description

**Development:** Developed · backlog item SIMPL-21594

#### Storycard

**As** Governance Authority\
**I want to** notify Providers about a wrong version number of a new version of a resource description\
**So that** the Provider can correct the error.

#### Acceptance criteria

1. **Given** an algorithm to calculate and verify version numbers of resource description is defined by me, the Governance Authority\
   **And** I have received a new resource description or a new version of a resource description with an incorrect version number\
   **When** it is checked that the version number of the new version of a Resource description is correct based on the version numbering algorithm\
   **And** version number validation for the new version of a Resource description has the result "invalid".\
   **Then** a notification about an version numbering error is sent to the Provider.

#### Technical details

The version validation mechanism shall be implemented in a configurable and extensible manner so that the versioning strategy can be changed or replaced.

Approach is described here:

(internal page)

A http response with a suitable error code and a meaningful error description is sufficient.

API: [https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-fc-service/-/blob/main/openapi/fc_openapi-itb.yaml?ref_type=heads](https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-fc-service/-/blob/main/openapi/fc_openapi-itb.yaml?ref_type=heads)

Endpoint: POST /selfDescriptions/resource

Validations:

- incremental versioning - if latest version is 5 and I get 7 - REJECT
- numeric versioning - reject strings or other formats
- reject negative numbers

HTTP Status Code for successful validation: 201

HTTP Status Code for negative scenarios: 422

---

Back to [5B.5.4 The Governance Authority notifies a Provider about wrong versioning of new versions of a Resource description](../bp5b5-governance-authority-publishes-resource-description.md#5b54-the-governance-authority-notifies-a-provider-about-wrong-versioning-of-new-versions-of-a-resource-description) · [5B.5 The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md) · [BP05B process page](../README.md)
