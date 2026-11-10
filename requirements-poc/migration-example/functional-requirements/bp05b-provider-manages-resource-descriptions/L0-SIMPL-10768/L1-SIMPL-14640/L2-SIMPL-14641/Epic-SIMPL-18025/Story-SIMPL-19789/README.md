> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../../README.md) · [What the levels mean](../../../../../../../../levels.md) · [All items](../../../../../../../../all-items.md) · [By business process](../../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14640](../../../README.md) / [L2-SIMPL-14641](../../README.md) / [Epic-SIMPL-18025](../README.md) / **Story-SIMPL-19789**

# SIMPL-19789: [API] The Governance Authority revokes a Resource Description (Query Mapper)

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14640](../../../README.md) (L1) › [SIMPL-14641](../../README.md) (L2) › [SIMPL-18025](../README.md) (Epic) › **SIMPL-19789** (User story)

## Text

#### Storycard

| As|Governance Authority |
| --- |

| I want|to revoke a resource description upon request of a provider |
| --- |

| so that|the Provider can manage their resource descriptions. |
| --- |

#### Acceptance criteria

| Scenario | The Governance Authority revokes a resource description - positive case |
| --- | --- |

| When|I, the Governance Authority, receive a request to revoke a resource description |
| --- |

| and|the Provider is authorised with the identity attributes to manage resource descriptions (one of DATA_PROVIDER_PUBLISHER, APP_PROVIDER_PUBLISHER, INFRA_PROVIDER_PUBLISHER) |
| --- |

| and |the resource description to be revoked was published by the revoking Provider |
| --- |

| and |the resource description is published |
| --- |

| Then|the resource description is revoked. |
| --- |
|  |  |

| Scenario | A Provider requests the revocation of a Resource description via API - negative case: not an own resource description |
| --- | --- |

| When|I, the Governance Authority, receive a request to revoke a resource description |
| --- |

| and|the Provider is authorised with the identity attributes to manage resource descriptions |
| --- |

| and |the resource description to be revoked was not published by the revoking Provider |
| --- |

| Then|a message is returned that the Provider is not authorised. |
| --- |
|  |  |

| Scenario | The Governance Authority revokes a resource description - negative case: resource description not existing |
| --- | --- |

| When|I, the Governance Authority, receive a request to revoke a resource description |
| --- |

| and|the Provider is authorised with the identity attributes to manage resource descriptions |
| --- |

| and |the resource description does not exist |
| --- |

| Then|an error message is returned. |
| --- |
|  |  |

| Scenario | The Governance Authority revokes a resource description - negative case: insufficient permission |
| --- | --- |

| When|I, the Governance Authority, receive a request to revoke a resource description |
| --- |

| and|the Provider is not authorised with the identity attributes to manage resource descriptions |
| --- |

| Then|an error message is returned. |
| --- |
|  |  |

| Scenario | The Governance Authority revokes a resource description - negative case: internal server error |
| --- | --- |

| When|I, the Governance Authority, receive a request to revoke a resource description |
| --- |

| and|the Provider is authorised with the identity attributes to manage resource descriptions |
| --- |

| and|there is an internal error |
| --- |

| Then|an error message is returned. |
| --- |

#### Technical information:

- API of the Query Mapper component
- Endpoint defined in (internal page)
- The resource descriptions published by a Provider contain the provider ID as attribute. This provider ID in the resource description should be compared to the provider ID from the token to establish whether resource description was published by the same provider who wants to revoke it now.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-18025](../README.md) (Epic) The Governance Authority revokes a resource description |
