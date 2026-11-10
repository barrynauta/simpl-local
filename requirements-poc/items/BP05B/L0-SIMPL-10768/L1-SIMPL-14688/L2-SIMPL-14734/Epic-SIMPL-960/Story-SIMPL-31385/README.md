> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14734](../../README.md) / [Epic-SIMPL-960](../README.md) / **Story-SIMPL-31385**

# SIMPL-31385: [API] Provider retrieves the list of usage policy templates

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14734](../../README.md) (L2) › [SIMPL-960](../README.md) (Epic) › **SIMPL-31385** (User story)

## Text

#### Story card

| As|an authorised Provider managing Resource Descriptions |
| --- |

| I want|to retrieve the usage-policy templates |
| --- |

| So that|I can select a template when adding a usage policy to a Resource Description |
| --- |

#### Acceptance criteria

##### Provider retrieves the usage-policy templates

| Given|I am authenticated as a Provider with the required role |
| --- |

| When|I request the policy templates using the usage-policy filter |
| --- |

| Then|the service responds with `200 OK` |
| --- |

| And|returns all usage-policy templates |
| --- |

| And|each item contains a stable identifier, name, and description |
| --- |

##### No usage-policy templates are available

| Given|I am authenticated as a Provider with the required role |
| --- |

| And|no usage-policy templates are available |
| --- |

| When|I request the policy templates using the usage-policy filter |
| --- |

| Then|the service responds with `200 OK` |
| --- |

| And|returns an empty array `[]` |
| --- |

##### Unauthenticated client is rejected

| Given|I am not authenticated |
| --- |

| When|I request the policy templates using the usage-policy filter |
| --- |

| Then|the service responds with `401 Unauthorized` |
| --- |

| And|returns no template data |
| --- |

##### Provider without the required role is rejected

| Given|I am authenticated without a role that allows me to manage Resource Descriptions |
| --- |

| When|I request the policy templates using the usage-policy filter |
| --- |

| Then|the service responds with `403 Forbidden` |
| --- |

| And|returns no template data |
| --- |

#### Technical details

- This is a read-only operation in the shared policy-template API defined by SIMPL-31345.
- Access and usage templates use the same API and are distinguished through policy-type filtering.
- The usage filter returns all usage-policy templates; visibility is not Provider-specific.
- The stable identifier is used by SIMPL-31386 to retrieve the full template details.
- The direct OpenAPI operation link will be added to this story when available.

#### Additional notes

- Shared policy-template API - SIMPL-31345 (internal page)
- Usage-policy template details - SIMPL-31386 (internal page)
- AI-supported implementation expedite - SIMPL-32078 (internal page)
- Policy Definition (internal page)
- [W3C ODRL Information Model](https://www.w3.org/TR/odrl-model/)

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-960](../README.md) (Epic) A Provider is supported in defining a usage policy by using a policy template via API |
