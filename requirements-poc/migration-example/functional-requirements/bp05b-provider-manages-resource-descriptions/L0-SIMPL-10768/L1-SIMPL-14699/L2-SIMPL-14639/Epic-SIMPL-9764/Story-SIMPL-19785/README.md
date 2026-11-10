> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../../README.md) · [What the levels mean](../../../../../../../../levels.md) · [All items](../../../../../../../../all-items.md) · [By business process](../../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14699](../../../README.md) / [L2-SIMPL-14639](../../README.md) / [Epic-SIMPL-9764](../README.md) / **Story-SIMPL-19785**

# SIMPL-19785: [API] Provider requests the revocation of a Resource description via API (SD tooling)

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14699](../../../README.md) (L1) › [SIMPL-14639](../../README.md) (L2) › [SIMPL-9764](../README.md) (Epic) › **SIMPL-19785** (User story)

## Text

#### Storycard

| As a|Provider |
| --- |

| I want|to be able to revoke a Resource description via API |
| --- |

| so that|the Resource description cannot be used any more. |
| --- |

#### Acceptance criteria

| Scenario | A Provider requests the revocation of a Resource description via API - positive case |
| --- | --- |

| Given|I am authenticated as a Provider |
| --- |

| and|I have a role authorising the management of Resource descriptions |
| --- |

| when|I want to revoke a Resource description |
| --- |

| then|a revocation request is sent to the Governance authority. |
| --- |
|  |  |

| Scenario | A Provider requests the revocation of a Resource description via API - not an own resource description |
| --- | --- |

| Given|I am authenticated as a Provider |
| --- |

| and|I have a role authorising the management of Resource descriptions |
| --- |

| when|I want to revoke a resource description that is not published by me |
| --- |

| then|a message is returned that I am not authorised. |
| --- |
|  |  |

| Scenario | A Provider requests the revocation of a Resource description via API - not existing resource description |
| --- | --- |

| Given|I am authenticated as a Provider |
| --- |

| and|I have a role authorising the management of Resource descriptions |
| --- |

| when|I want to revoke an inexistent resource description |
| --- |

| then|a message is returned that the resource description cannot be found. |
| --- |
|  |  |

| Scenario | A Provider requests the revocation of a Resource description via API - bad request |
| --- | --- |

| Given|I am authenticated as a Provider |
| --- |

| and|I have a role authorising the management of Resource descriptions |
| --- |

| when|I want to revoke a Resource description but using an invalid resource description ID |
| --- |

| then|a message is returned that the request is bad. |
| --- |
|  |  |

| Scenario | A Provider requests the revocation of a Resource description via API - wrong role |
| --- | --- |

| Given|I am authenticated as a Provider |
| --- |

| and|I do **not** have a role authorising the management of Resource descriptions |
| --- |

| when|I want to revoke a Resource description |
| --- |

| then|a message is returned that I am not authorised. |
| --- |
|  |  |

| Scenario | A Provider requests the revocation of a Resource description via API - internal server error |
| --- | --- |

| Given|I am authenticated as a Provider |
| --- |

| and|I have a role authorising the management of Resource descriptions |
| --- |

| when|I want to revoke a Resource description |
| --- |

| and|there is an internal error |
| --- |

| then|a message is returned that the request cannot be fulfilled. |
| --- |

#### Technical information

- API of the SD tooling component.
- Endpoint defined in SIMPL-20726 (internal page) POST [/resourceDescriptions/{resourceDescriptionId}/revoke](https://code.europa.eu/-/sandbox/swagger#/Resource%20Description/revokeResourceDescription)

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-9764](../README.md) (Epic) A Provider requests the revocation of a resource description via UI or API |
