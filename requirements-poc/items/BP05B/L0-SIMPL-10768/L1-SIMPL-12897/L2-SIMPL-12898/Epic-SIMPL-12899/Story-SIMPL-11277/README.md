> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-12897](../../../README.md) / [L2-SIMPL-12898](../../README.md) / [Epic-SIMPL-12899](../README.md) / **Story-SIMPL-11277**

# SIMPL-11277: API to provide a list of a provider's own resource descriptions

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-12897](../../../README.md) (L1) › [SIMPL-12898](../../README.md) (L2) › [SIMPL-12899](../README.md) (Epic) › **SIMPL-11277** (User story)

## Text

#### Storycard

**As a** Provider\
**I want to** receive via an API a sorted (descendingly by publication date or alphabetically by Resource type) list of the resource descriptions summaries which I have published\
**So that** I can locate a specific resource description that I want to update.

#### Acceptance criteria

1. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** there are resource descriptions published by a provider\
   **When** the provider requests the own resource descriptions via API\
   **Then** a list of own resource descriptions published by the provider is returned.

2. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** there are no resource descriptions published by a provider\
   **When** the provider requests the own resource descriptions via API\
   **Then** an empty list of own resource descriptions published by the provider is returned.

3. **Given** I am authenticated as Provider\
   **And** I do not have a role authorising the management of Resource Descriptions\
   **When** the provider requests the own resource descriptions via API\
   **Then** message is returned that the user is not authorised.

4. **Given** I am authenticated as Provider\
   **And** there is an internal error\
   **When** the provider requests the own resource descriptions via API\
   **Then** message is returned that the request cannot be fulfilled.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-12899](../README.md) (Epic) A Provider consults an overview of its Resource descriptions via UI and API |
