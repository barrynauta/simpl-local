> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-12897](../../../README.md) / [L2-SIMPL-12903](../../README.md) / [Epic-SIMPL-12905](../README.md) / **Story-SIMPL-11293**

# SIMPL-11293: API to provide the full content of a publisher's resource description

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-12897](../../../README.md) (L1) › [SIMPL-12903](../../README.md) (L2) › [SIMPL-12905](../README.md) (Epic) › **SIMPL-11293** (User story)

## Text

#### Storycard

**As a** provider\
**I want to** receive the details of one of my self-descriptions via an API\
**So that** I can inform myself about the details.

#### Acceptance criteria

1. **Given** there is a resource description published by a provider\
   **When** the provider requests the details of an own resource description via API\
   **Then** all details of the resource description are returned.

2. **Given** the provider is authorised\
   **When** a provider requests a resource description with a syntactically valid ID\
   **And** the ID refers to a not existing Resource description\
   **Then** an message is returned that the resource is not found.

3. **Given** a user is a provider\
   **When** the user requests a resource description not created by themself\
   **Then** an message is returned that the user is not authorised.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-12905](../README.md) (Epic) A Provider consults the full contents of one of its own Resource descriptions via UI and API |
