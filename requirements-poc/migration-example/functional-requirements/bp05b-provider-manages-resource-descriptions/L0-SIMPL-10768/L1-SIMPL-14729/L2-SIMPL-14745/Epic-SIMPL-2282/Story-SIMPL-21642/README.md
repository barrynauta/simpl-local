> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../../README.md) · [What the levels mean](../../../../../../../../levels.md) · [All items](../../../../../../../../all-items.md) · [By business process](../../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14729](../../../README.md) / [L2-SIMPL-14745](../../README.md) / [Epic-SIMPL-2282](../README.md) / **Story-SIMPL-21642**

# SIMPL-21642: [UI] A Provider views notifications about validation errors

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14729](../../../README.md) (L1) › [SIMPL-14745](../../README.md) (L2) › [SIMPL-2282](../README.md) (Epic) › **SIMPL-21642** (User story)

## Text

#### Storycard

**As a** Provider end user\
**I want to** view notifications about errors during validation on Governance Authority side\
**So that** I can correct the Resource description and resubmit it for publication.

#### Acceptance criteria

**Scenario:** A provider views notifications about validation errors

1. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of Resource descriptions\
   **And** I have created a Resource description or a new version of a Resource description\
   **And** there are errors in the Resource description or new version of a Resource description\
   **When** I submit the new Resource description or new version of a Resource description\
   **And** the Governance Authority validates the resource description or new version of a Resource description\
   **Then** the validation errors are shown in the UI

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-2282](../README.md) (Epic) A Provider receives notifications about validation errors via UI or API |
