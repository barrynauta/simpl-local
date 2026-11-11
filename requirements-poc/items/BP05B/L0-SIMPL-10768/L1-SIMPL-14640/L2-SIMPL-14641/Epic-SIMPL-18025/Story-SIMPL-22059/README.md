> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14640](../../../README.md) / [L2-SIMPL-14641](../../README.md) / [Epic-SIMPL-18025](../README.md) / **Story-SIMPL-22059**

# SIMPL-22059: [API] The Governance Authority revokes a Resource Description (Catalogue)

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14640](../../../README.md) (L1) › [SIMPL-14641](../../README.md) (L2) › [SIMPL-18025](../README.md) (Epic) › **SIMPL-22059** (User story)

## Text

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

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-18025](../README.md) (Epic) The Governance Authority revokes a resource description |
