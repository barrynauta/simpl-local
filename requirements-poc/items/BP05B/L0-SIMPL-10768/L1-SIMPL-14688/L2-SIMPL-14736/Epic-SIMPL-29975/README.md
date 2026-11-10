> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14688](../../README.md) / [L2-SIMPL-14736](../README.md) / **Epic-SIMPL-29975**

# SIMPL-29975: A Provider adds a contract clauses related resource consent of the data resource to a new resource description via UI

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14688](../../README.md) (L1) › [SIMPL-14736](../README.md) (L2) › **SIMPL-29975** (Epic)

## Text

**As a** Provider\
**I want** to add a contract clause related to resource consent of the data resource to a new resource description via UI\
**So that** the Consumer agrees that resource consent (and thus access to the resource) can be withdrawn

**Acceptance criteria**

**Given** I am authenticated as a Provider\
**and** I have a role authorising the management of resource descriptions\
**and** I am creating a new resource description\
**When** I reach the step "Access policies - resource consent" in the wizard\
**and** I create an access policy related to resource consent\
**and** I upload a document related to the resource consent status changes\
**Then** Simpl-Open adds a contract clause of the type consent to the resource description linked to the uploaded document

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14736](../README.md) (L2) A Provider adds a set of contract clauses to a new resource description |
| **Children** | [SIMPL-34168](Story-SIMPL-34168/README.md) (User story) [UI] Add Resource Consent Contract Clause to Resource Description<br>[SIMPL-34485](Story-SIMPL-34485/README.md) (User story) [UI] Enable Consent Management Contract Clauses Document Download in SDTooling UI |
