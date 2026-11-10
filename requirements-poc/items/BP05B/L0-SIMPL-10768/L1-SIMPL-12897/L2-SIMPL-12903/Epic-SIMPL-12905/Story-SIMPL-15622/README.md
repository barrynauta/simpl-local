> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-12897](../../../README.md) / [L2-SIMPL-12903](../../README.md) / [Epic-SIMPL-12905](../README.md) / **Story-SIMPL-15622**

# SIMPL-15622: [API] API to provide the full contents of a provider's own resource description to the UI

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-12897](../../../README.md) (L1) › [SIMPL-12903](../../README.md) (L2) › [SIMPL-12905](../README.md) (Epic) › **SIMPL-15622** (User story)

## Text

#### Storycard

| As a|provider |
| --- |

| I want|to receive the details of one of my self-descriptions via an API |
| --- |

| so that|I can inform myself about the details. |
| --- |

#### Acceptance criteria

| Given|there is a resource description published by myself |
| --- |

| when|I request the details of an own resource description with its ID via API |
| --- |

| then|all details of the resource description are returned. |
| --- |

| Given|I authorised as a provider |
| --- |

| when|I requests a resource description with a syntactically valid ID |
| --- |

| and|the ID refers to a not existing Resource description |
| --- |

| then|an message is returned that the resource is not found. |
| --- |

| Given|I am authorised as a provider |
| --- |

| when|I request a resource description not created by myself |
| --- |

| then|an message is returned that I am not authorised. |
| --- |

| Given|I am authorised as a provider |
| --- |

| when|I requests a resource description with an incorrect UUID (e.g. too short or too many characters). |
| --- |

| then|an message is returned that the request is bad. |
| --- |

#### Technical information

The endpoint is to be implemented in the SD Tooling backend.

The API will be accessible externally via the `{}Tier-1 Gateway{`}, making it available to the frontend or other provider applications.

The full contents of a provider's own resource description can be obtained by the SD tooling BE from the Query mapper adapter.

#### Additional notes

- The analogous implementation ticket for the Query Mapper adapter is (internal page)
- The API definition ticket for the SD Tooling backend is (internal page)

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-12905](../README.md) (Epic) A Provider consults the full contents of one of its own Resource descriptions via UI and API |
