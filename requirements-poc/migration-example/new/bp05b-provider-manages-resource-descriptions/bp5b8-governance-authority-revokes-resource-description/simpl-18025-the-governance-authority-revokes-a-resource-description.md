<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.8 - The Governance Authority revokes a resource description](../bp5b8-governance-authority-revokes-resource-description.md) / [5B.8.1 The Governance Authority revokes a resource description](../bp5b8-governance-authority-revokes-resource-description.md#5b81-the-governance-authority-revokes-a-resource-description) / Epic
</div>

# Epic: The Governance Authority revokes a resource description

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.8.1 The Governance Authority revokes a resource description](../bp5b8-governance-authority-revokes-resource-description.md#5b81-the-governance-authority-revokes-a-resource-description), under high-level requirement [5B.8 The Governance Authority revokes a resource description](../bp5b8-governance-authority-revokes-resource-description.md). In the process it belongs to step [BP05B.14 Revoke a resource description](../README.md#bp05b14-revoke-a-resource-description).

In the backlog (Jira) it is item SIMPL-18025. It has 2 user stories, each with acceptance criteria that say when the work is done.

## What this Epic delivers

#### Storycard

**As** Governance Authority\
**I want to** revoke a resource description upon request of a provider\
**So that** the Provider can manage their resource descriptions.

#### Acceptance criteria

**Scenario:** The Governance Authority revokes a resource description\
When I, the Governance Authority, receive a request to revoke a resource description\
and the Provider is authorised with the identity attributes to manage resource descriptions\
Then the resource description is revoked.

## User stories

1. [[API] The Governance Authority revokes a Resource Description (Query Mapper)](#user-story-1-api-the-governance-authority-revokes-a-resource-description-query-mapper)
2. [[API] The Governance Authority revokes a Resource Description (Catalogue)](#user-story-2-api-the-governance-authority-revokes-a-resource-description-catalogue)

### User story 1: [API] The Governance Authority revokes a Resource Description (Query Mapper)

Backlog item SIMPL-19789

#### Storycard

**As** Governance Authority\
**I want to** revoke a resource description upon request of a provider\
**So that** the Provider can manage their resource descriptions.

#### Acceptance criteria

**Scenario:** The Governance Authority revokes a resource description - positive case\
When I, the Governance Authority, receive a request to revoke a resource description\
and the Provider is authorised with the identity attributes to manage resource descriptions (one of DATA_PROVIDER_PUBLISHER, APP_PROVIDER_PUBLISHER, INFRA_PROVIDER_PUBLISHER)\
and the resource description to be revoked was published by the revoking Provider\
and the resource description is published\
Then the resource description is revoked.

**Scenario:** A Provider requests the revocation of a Resource description via API - negative case: not an own resource description\
When I, the Governance Authority, receive a request to revoke a resource description\
and the Provider is authorised with the identity attributes to manage resource descriptions\
and the resource description to be revoked was not published by the revoking Provider\
Then a message is returned that the Provider is not authorised.

**Scenario:** The Governance Authority revokes a resource description - negative case: resource description not existing\
When I, the Governance Authority, receive a request to revoke a resource description\
and the Provider is authorised with the identity attributes to manage resource descriptions\
and the resource description does not exist\
Then an error message is returned.

**Scenario:** The Governance Authority revokes a resource description - negative case: insufficient permission\
When I, the Governance Authority, receive a request to revoke a resource description\
and the Provider is not authorised with the identity attributes to manage resource descriptions\
Then an error message is returned.

**Scenario:** The Governance Authority revokes a resource description - negative case: internal server error\
When I, the Governance Authority, receive a request to revoke a resource description\
and the Provider is authorised with the identity attributes to manage resource descriptions\
and there is an internal error\
Then an error message is returned.

#### Technical information:

- API of the Query Mapper component
- Endpoint defined in (internal page)
- The resource descriptions published by a Provider contain the provider ID as attribute. This provider ID in the resource description should be compared to the provider ID from the token to establish whether resource description was published by the same provider who wants to revoke it now.

### User story 2: [API] The Governance Authority revokes a Resource Description (Catalogue)

Backlog item SIMPL-22059

#### Storycard

**As** Governance Authority\
**I want to** revoke a resource description upon request of a provider\
**So that** the Provider can manage their resource descriptions.

#### Acceptance criteria

**Scenario:** The Governance Authority revokes a resource description - positive case\
When I, the Governance Authority, receive a request to revoke a resource description\
and the Provider is authorised with the identity attributes to manage resource descriptions\
and the resource description is published\
Then the resource description is revoked.

**Scenario:** The Governance Authority revokes a resource description - negative case: resource description not in published status\
When I, the Governance Authority, receive a request to revoke a resource description\
and the Provider is authorised with the identity attributes to manage resource descriptions\
and the resource description exists but is not published\
Then the resource description is not revoked\
and an error message is returned.

**Scenario:** The Governance Authority revokes a resource description - negative case: resource description not existing\
When I, the Governance Authority, receive a request to revoke a resource description\
and the Provider is authorised with the identity attributes to manage resource descriptions\
and the resource description does not exist\
Then an error message is returned.

**Scenario:** The Governance Authority revokes a resource description - negative case: insufficient permission\
When I, the Governance Authority, receive a request to revoke a resource description\
and the Provider is not authorised with the identity attributes to manage resource descriptions\
Then an error message is returned.

**Scenario:** The Governance Authority revokes a resource description - bad request\
When I, the Governance Authority, receive a request to revoke a resource description\
and the Provider is authorised with the identity attributes to manage resource descriptions\
and the request is invalid\
Then an error message is returned.

**Scenario:** The Governance Authority revokes a resource description - negative case: internal server error\
When I, the Governance Authority, receive a request to revoke a resource description\
and the Provider is authorised with the identity attributes to manage resource descriptions\
and there is an internal error\
Then an error message is returned.

#### Technical information

- Endpoint defined in SIMPL-22587 [API definition] API to request the revocation of a resource description (Catalogue)
- Change from using the resource description hash to using the ID.

---

Back to [5B.8.1 The Governance Authority revokes a resource description](../bp5b8-governance-authority-revokes-resource-description.md#5b81-the-governance-authority-revokes-a-resource-description) · [5B.8 The Governance Authority revokes a resource description](../bp5b8-governance-authority-revokes-resource-description.md) · [BP05B process page](../README.md)
