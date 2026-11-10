> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-12897](../../../README.md) / [L2-SIMPL-12898](../../README.md) / [Epic-SIMPL-12899](../README.md) / **Story-SIMPL-2295**

# SIMPL-2295: Displaying a list of a provider's own self-descriptions

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-12897](../../../README.md) (L1) › [SIMPL-12898](../../README.md) (L2) › [SIMPL-12899](../README.md) (Epic) › **SIMPL-2295** (User story)

## Text

#### Storycard

**As a** Provider end user\
**I want to** use a UI to see the self-descriptions I have published\
**So that** I can locate a specific self-description that I want to update.

#### Acceptance criteria

1. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** there are resource descriptions published by a provider\
   **When** the provider end user opens the dialog of a list of self-descriptions\
   **Then** the list of resource descriptions previously published by the provider is displayed.

2. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** there are no self-descriptions of a provider\
   **When** the provider end user opens the dialog of a list of self-descriptions\
   **Then** a message saying that there are no self-descriptions is displayed.

#### Technical details

UI Mockup:

(image not shown)

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-12899](../README.md) (Epic) A Provider consults an overview of its Resource descriptions via UI and API |
