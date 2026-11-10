> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [SA09](../../../../README.md) / [L0-SIMPL-26074](../../../README.md) / [L1-SIMPL-27082](../../README.md) / [L2-SIMPL-27154](../README.md) / **Epic-SIMPL-27203**

# SIMPL-27203: Data Provider creates a resource consent assertion

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [SA09](../../../../README.md) › [SIMPL-26074](../../../README.md) (L0) › [SIMPL-27082](../../README.md) (L1) › [SIMPL-27154](../README.md) (L2) › **SIMPL-27203** (Epic)

## Text

**As a**

Data Provider

**I want to**

create a resource consent assertion providing its name, description, and metadata

**So that**

the assertion can be linked to a data resource description and its status can be used to enforce access control decisions for all Consumers

**Acceptance Criteria:**

1. **GIVEN** I am a Data Provider user with the appropriate roles\
   **WHEN** I create a consent assertion of type RESOURCE\
   **THEN** the assertion is saved with the provided attributes and metadata\
   **AND** its status is set to ACTIVE

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA09](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions)) |
| **Parent** | [SIMPL-27154](../README.md) (L2) Data Provider creates resource consent assertions for shared data resources |
| **Children** | [SIMPL-28589](Story-SIMPL-28589/README.md) (User story) [API] Data Provider creates a resource consent assertion<br>[SIMPL-28598](Story-SIMPL-28598/README.md) (User story) [UI] Data Provider creates a resource consent assertion<br>[SIMPL-28808](Story-SIMPL-28808/README.md) (User story) [API][IMPROVEMENT] Simpl-Open user to retrieve participant details |
