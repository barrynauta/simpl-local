> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14729](../../README.md) / [L2-SIMPL-14745](../README.md) / **Epic-SIMPL-2282**

# SIMPL-2282: A Provider receives notifications about validation errors via UI or API

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14729](../../README.md) (L1) › [SIMPL-14745](../README.md) (L2) › **SIMPL-2282** (Epic)

## Text

#### Storycard

**As a** Provider\
**I want to** receive notifications about errors during validation on Governance Authority via UI or API\
**So that** I can to correct the Resource description and resubmit it for publication.

#### Acceptance criteria

**Scenario:** A provider receives notifications about validation errors via UI or API

1. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of Resource descriptions\
   **And** I have created a Resource description or a new version of a Resource description\
   **And** there are errors in the Resource description or new version of a Resource description\
   **When** I submit the new Resource description or new version of a Resource description\
   **And** the Governance Authority validates the resource description or new version of a Resource description\
   **Then** the validation errors are provided via UI or API

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14745](../README.md) (L2) A Provider signs and submits a new Resource description or a new version of a Resource description |
| **Children** | [SIMPL-21642](Story-SIMPL-21642/README.md) (User story) [UI] A Provider views notifications about validation errors |
