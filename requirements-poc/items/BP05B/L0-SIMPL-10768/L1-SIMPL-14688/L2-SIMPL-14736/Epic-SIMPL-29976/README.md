> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14688](../../README.md) / [L2-SIMPL-14736](../README.md) / **Epic-SIMPL-29976**

# SIMPL-29976: A Provider adds a contract clauses related to the intended purpose of the data resource to a new resource description via UI

**Epic (deliverable, testable feature)**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14688](../../README.md) (L1) › [SIMPL-14736](../README.md) (L2) › **SIMPL-29976** (Epic)

## Text

**As a** Provider\
**I want to** add a contract clause related to the intended purpose of the data resource to a new resource description via UI\
**So that** I can ensure that the Consumer agrees that they will only use the data resource for the intended purpose

**Acceptance criteria**

1. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new resource description\
   **When** I reach the step "Access policies - resource consent" in the wizard\
   **And** I create an access policy related to resource consent\
   **And** I upload a document related to the intended purpose of the data resource\
   **Then** Simpl-Open adds a contract clause of the type consent to the resource description linked to the uploaded document

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14736](../README.md) (L2) A Provider adds a set of contract clauses to a new resource description |
| **Children** | [SIMPL-34169](Story-SIMPL-34169/README.md) (User story) [UI] Add Intended Purpose Contract Clause to Resource Description |
