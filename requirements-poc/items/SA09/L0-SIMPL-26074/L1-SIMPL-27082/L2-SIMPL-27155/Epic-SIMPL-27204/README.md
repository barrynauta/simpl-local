> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [SA09](../../../../README.md) / [L0-SIMPL-26074](../../../README.md) / [L1-SIMPL-27082](../../README.md) / [L2-SIMPL-27155](../README.md) / **Epic-SIMPL-27204**

# SIMPL-27204: Data Provider creates a contract consent assertion

**Epic (deliverable, testable feature)**

**Trace:** [SA09](../../../../README.md) › [SIMPL-26074](../../../README.md) (L0) › [SIMPL-27082](../../README.md) (L1) › [SIMPL-27155](../README.md) (L2) › **SIMPL-27204** (Epic)

## Text

**As a** Data Provider\
**I want to** create a contract consent assertion providing its name, description, metadata, and the Consumer party bound by that contract\
**So that** the assertion can be linked to a specific contract and its status can be used to enforce access control decisions exclusively for the bound Consumer

**Acceptance Criteria:**

1. **Given** I am a Data Provider, wanting to create contract consent assertion during contract establishment\
   **When** I create a consent assertion of type CONTRACT\
   **And** specify the Consumer party bound by the contract\
   **Then** the assertion is saved with the provided attributes, metadata, and party information\
   **And** its status is set to ACTIVE

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA09](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions)) |
| **Parent** | [SIMPL-27155](../README.md) (L2) Data Provider creates contract consent assertions for shared data resources |
| **Children** | [SIMPL-28590](Story-SIMPL-28590/README.md) (User story) [API] Data Provider creates a contract consent assertion |
