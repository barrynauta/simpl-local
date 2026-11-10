> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14728](../../../README.md) / [L2-SIMPL-14743](../../README.md) / [Epic-SIMPL-30635](../README.md) / **Story-SIMPL-30641**

# SIMPL-30641: A Provider removes contract clauses from a resource description via UI, resulting in a new version of the resource description

**User story (implementable work)** · Status **PBI Defined**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14728](../../../README.md) (L1) › [SIMPL-14743](../../README.md) (L2) › [SIMPL-30635](../README.md) (Epic) › **SIMPL-30641** (User story)

## What this item is

This user story covers one step of the resource description wizard: when a Provider makes a new version of a resource description, it can drop contract clauses that the previous version had.

## Text

**As a** Provider end user\
**I want** to remove contract clauses from a resource description via UI\
**so that** I can ensure that the new version of the resource description includes the updated set of contract clauses

**Acceptance Criteria**

**AC1 - A Provider removes contract clauses from a resource description via UI**

**Given** I am authenticated as a Provider end user\
**and** I have a role authorising the management of resource descriptions\
**and** I am creating a new version of a resource description\
**When** I reach the step 'Contract Clauses' in the wizard\
**Then** the UI shows the properties of the contract clauses prefilled with the values from the previous version\
**and** I can remove one or more of the previously defined contract clauses

**AC2 - A Provider finishes removing contract clauses via UI, resulting in a new version of a resource description**

**Given** I am authenticated as a Provider end user\
**and** I have a role authorising the management of resource descriptions\
**and** I am creating a new version of a resource description\
**And** I reach the step 'Contract Clauses' in the wizard\
**When** I choose to remove one or more of the previously defined contract clauses in the UI wizard,\
**Then** those contract clauses are not associated anymore in the new version of the resource description\
**And** I can navigate to the next step in the wizard

**References / Supporting Material / Documentation / Validation rules:** [Contract Clauses management|an internal page]

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-30635](../README.md) (Epic) A Provider removes contract clauses from a resource description via UI, resulting in a new version of the resource description |

## Notes on the source

Problems found in the source and in PSO's export of 10 November 2026. They are listed here, not fixed in the text above.

- PSO's export showed no parent Epic. In the source it sits under Epic [SIMPL-30635](../README.md), in BP05B.
- The reference is only the title of an internal page.
