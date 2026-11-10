> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14729](../../../README.md) / [L2-SIMPL-14745](../../README.md) / [Epic-SIMPL-2292](../README.md) / **Story-SIMPL-24346**

# SIMPL-24346: [BE] Automatically populate the version number of a new version of a resource description

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14729](../../../README.md) (L1) › [SIMPL-14745](../../README.md) (L2) › [SIMPL-2292](../README.md) (Epic) › **SIMPL-24346** (User story)

## Text

#### Storycard

**As a** Provider\
**I want** Simpl to automatically assign the first version number, based on the applicable versioning algorithm, to a new resource description\
**So that** I can uniquely identify the resource description ensuring traceability.

#### Acceptance criteria

**Scenario:** A Provider automatically assigns a version numnber to a new resource desciption - Positive case

1. **Given** I am authenticated as a provider\
   **And** I create a new resource description in the UI\
   **When** I submit the filled resource description to the backend to be enriched and validated\
   **Then** Simpl-open assigns the initial version number, based on the applicable versioning algorithm for resouce desciptions.

**Note on negative AC:**\
The scope of this story is merely to set the initial version number. Thus negative AC should only cover issues with the versioning algorithm itself, not general scenarios, such as not authorised or a general internal server error.

We can assume thast the versioning algorithm has been developed and tested itself outside of the resource description management components. This implies that we also can assume, that the algorithm will be generally able to provide an initial version number and negative scenarios are not necessary in this story.

#### Technical Information

- The versioning algorithm is identical to the one used in schema management.
- The versioning algorithm is contained in the same library as used in schema management.
- The related story in schema management is SIMPL-20534

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-2292](../README.md) (Epic) A Provider signs and  submits a new Resource description or a new version of a Resource description |
