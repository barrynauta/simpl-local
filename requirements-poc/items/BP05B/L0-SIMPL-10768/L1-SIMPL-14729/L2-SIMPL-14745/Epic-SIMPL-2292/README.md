> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14729](../../README.md) / [L2-SIMPL-14745](../README.md) / **Epic-SIMPL-2292**

# SIMPL-2292: A Provider signs and  submits a new Resource description or a new version of a Resource description

**Epic (deliverable, testable feature)**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14729](../../README.md) (L1) › [SIMPL-14745](../README.md) (L2) › **SIMPL-2292** (Epic)

## Text

#### Storycard

**As a** Provider\
**I want to** sign and submit a new resource description or a new version of a resource description to the Governance Authority using an API\
**So that** the resource description can be published in the Catalogue .

#### Acceptance criteria

**Scenario:** A Provider signs and submits a new Resource description or a new version of a Resource description

1. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions\
   **And** I have finished editing or updating a new Resource description or a new version of a Resource description\
   **When** I request the publication of the new Resource description or new version of a Resource description\
   **Then** the new Resource description or new version of the Resource description is validated\
   **And** the elements of the resource description, required to later support consumption of the resource, are stored in my agent\
   **And** the new Resource description or new version of the Resource description is signed\
   **And** the signed new Resource description or new version of the Resource description is submitted to the Governance Authority for publication.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14745](../README.md) (L2) A Provider signs and submits a new Resource description or a new version of a Resource description |
| **Children** | [SIMPL-110](Story-SIMPL-110/README.md) (User story) Implement credential management logic<br>[SIMPL-226](Story-SIMPL-226/README.md) (User story) Sign created json-ld document with a signer<br>[SIMPL-228](Story-SIMPL-228/README.md) (User story) [UI] Create UI frontend for XFSC catalogue - Dialog add resource<br>[SIMPL-233](Story-SIMPL-233/README.md) (User story) Interactively Create a Service Self-Description<br>[SIMPL-241](Story-SIMPL-241/README.md) (User story) Deploy signer service<br>[SIMPL-314](Story-SIMPL-314/README.md) (User story) [UI] User Interface for Sign created JSON-LD document<br>[SIMPL-489](Story-SIMPL-489/README.md) (User story) Syntax Validation of Self-Description<br>[SIMPL-1083](Story-SIMPL-1083/README.md) (User story) Simpl Self-Description Frontend<br>[SIMPL-6287](Story-SIMPL-6287/README.md) (User story) Integrating the asset registering of the connector in to the published process of SD<br>[SIMPL-15289](Story-SIMPL-15289/README.md) (User story) Automatically populate the provider id on the resource description<br>[SIMPL-16825](Story-SIMPL-16825/README.md) (User story) [BE] Automatically populate the version number of a new resource description<br>[SIMPL-24346](Story-SIMPL-24346/README.md) (User story) [BE] Automatically populate the version number of a new version of a resource description |
