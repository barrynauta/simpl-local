> **Simpl-Open requirements, proof of concept.** One business process and the items under it. New here? [Start here](../../README.md) · [What the levels mean](../../levels.md) · [All items](../../all-items.md) · [By business process](../../processes.md)

Folder: [items](../README.md) / **BP05B**

# BP05B - Provider manages resource descriptions

In BP05B a Provider manages the descriptions of its resources in the data space catalogue, and the Governance Authority checks and publishes them. A resource description tells Consumers what a data set, an application or an infrastructure service is, and under which policies and contract terms they can use it.

The Provider chooses the resource type and fills in the common metadata, the metadata for that type, and the access and usage policies. It signs the description with its own credentials and submits it. The Governance Authority validates it: the syntax, the schema and vocabulary, and the quality rules. If the description is valid, it is published. If not, the Provider fixes it and submits it again.

A published description cannot be changed in place. To update it, the Provider creates a new version, and the Governance Authority revokes the old one. A Provider can also ask to revoke a description. In both cases the Consumers of the resource are informed. Usage contracts that are based on an old or a revoked description stay valid; only new contracts are no longer possible.

Read more on the [public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) and the [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions).

## Who takes part

- Provider
- Governance Authority

## Before it starts

- The Governance Authority has configured the catalogue with its vocabulary and schemas (BP02).
- The Provider is onboarded (BP03A).
- The Provider has stored at least one set of contract clauses (BP05A).

## Steps

The steps as the public process page names them. The items in this folder have no field for the step, so the tree below is ordered by item, not by step.

- **BP05B.01 Create a new resource description**: choose the resource type and enter the common metadata.
- **BP05B.02 Add resource type specific metadata**
- **BP05B.03 Add policy and contract related metadata**: access and usage policies, and the contract terms.
- **BP05B.04 Sign and submit the resource description**: with the Provider's credentials, to the Governance Authority.
- **BP05B.05 Validate the resource description**: syntax, schema and vocabulary, quality rules.
- **BP05B.06 Remediate validation issues**: the Provider fixes the description and submits it again.
- **BP05B.07 Publish the resource description**
- **BP05B.08 Create a new version of a resource description**
- **BP05B.09 Update resource type specific metadata**
- **BP05B.10 Update policies and contract related metadata**
- **BP05B.11 Revoke the previous version of the resource description**
- **BP05B.12 Inform about new version**: Consumers of the old version are informed.
- **BP05B.13 Request resource description revocation**
- **BP05B.14 Revoke a resource description**
- **BP05B.15 Inform about revocation**: Consumers of the resource are informed.

## What this folder holds

The L0 item [SIMPL-10768](L0-SIMPL-10768/README.md) stands for the business process. Under it are 9 high-level requirements (L1). Each has its detailed requirements (L2), the Epics that deliver them and the user stories that build them. In total **258 items:** 1 L0, 9 L1, 32 L2, 69 Epic, 147 User story.

| L1 | High-level requirement | Status | Items under it |
| --- | --- | --- | --- |
| [SIMPL-12897](L0-SIMPL-10768/L1-SIMPL-12897/README.md) | A Provider consults its own Resource descriptions | Defined | 20 |
| [SIMPL-14640](L0-SIMPL-10768/L1-SIMPL-14640/README.md) | The Governance Authority revokes a resource description | Defined | 4 |
| [SIMPL-14687](L0-SIMPL-10768/L1-SIMPL-14687/README.md) | The Governance Authority publishes a Resource description | In Progress | 48 |
| [SIMPL-14688](L0-SIMPL-10768/L1-SIMPL-14688/README.md) | A Provider creates a new Resource description | In Progress | 108 |
| [SIMPL-14699](L0-SIMPL-10768/L1-SIMPL-14699/README.md) | A Provider requests the revocation of a Resource description | Defined | 4 |
| [SIMPL-14728](L0-SIMPL-10768/L1-SIMPL-14728/README.md) | A Provider creates a new version of a Resource description | In Progress | 40 |
| [SIMPL-14729](L0-SIMPL-10768/L1-SIMPL-14729/README.md) | A Provider signs and submits a new Resource description or a new version of a Resource description | Defined | 17 |
| [SIMPL-21117](L0-SIMPL-10768/L1-SIMPL-21117/README.md) | The Provider notifies Consumers about the revocation of a resource description | In Progress | 4 |
| [SIMPL-21118](L0-SIMPL-10768/L1-SIMPL-21118/README.md) | A Provider notifies Consumers about a new version of a Resource description | In Progress | 3 |

## Tree

Every item, with its level, title and status.

- [SIMPL-10768](L0-SIMPL-10768/README.md) L0 · BP05B - Provider manages resource descriptions · In Progress
  - [SIMPL-12897](L0-SIMPL-10768/L1-SIMPL-12897/README.md) L1 · A Provider consults its own Resource descriptions · Defined
    - [SIMPL-12898](L0-SIMPL-10768/L1-SIMPL-12897/L2-SIMPL-12898/README.md) L2 · A Provider consults an overview of its Resource descriptions · Defined
      - [SIMPL-12899](L0-SIMPL-10768/L1-SIMPL-12897/L2-SIMPL-12898/Epic-SIMPL-12899/README.md) Epic · A Provider consults an overview of its Resource descriptions via UI and API · Defined
        - [SIMPL-2295](L0-SIMPL-10768/L1-SIMPL-12897/L2-SIMPL-12898/Epic-SIMPL-12899/Story-SIMPL-2295/README.md) User story · Displaying a list of a provider's own self-descriptions · DevTeam PBI is ready for Implementation check
        - [SIMPL-11277](L0-SIMPL-10768/L1-SIMPL-12897/L2-SIMPL-12898/Epic-SIMPL-12899/Story-SIMPL-11277/README.md) User story · API to provide a list of a provider's own resource descriptions · Ready for DG Acceptance
        - [SIMPL-15584](L0-SIMPL-10768/L1-SIMPL-12897/L2-SIMPL-12898/Epic-SIMPL-12899/Story-SIMPL-15584/README.md) User story · [API] API to provide a list of a provider's own resource descriptions (SD tooling) · DevTeam PBI is ready for Implementation check
        - [SIMPL-18524](L0-SIMPL-10768/L1-SIMPL-12897/L2-SIMPL-12898/Epic-SIMPL-12899/Story-SIMPL-18524/README.md) User story · Using identity attributes in the API to provide a list of a provider's own resource descriptions · Ready for DG Acceptance
    - [SIMPL-12903](L0-SIMPL-10768/L1-SIMPL-12897/L2-SIMPL-12903/README.md) L2 · A Provider consults the details of one of its own resource descriptions · Defined
      - [SIMPL-12905](L0-SIMPL-10768/L1-SIMPL-12897/L2-SIMPL-12903/Epic-SIMPL-12905/README.md) Epic · A Provider consults the full contents of one of its own Resource descriptions via UI and API · Defined
        - [SIMPL-9838](L0-SIMPL-10768/L1-SIMPL-12897/L2-SIMPL-12903/Epic-SIMPL-12905/Story-SIMPL-9838/README.md) User story · Display self-description details · Ready for DG Acceptance
        - [SIMPL-11293](L0-SIMPL-10768/L1-SIMPL-12897/L2-SIMPL-12903/Epic-SIMPL-12905/Story-SIMPL-11293/README.md) User story · API to provide the full content of a publisher's resource description · DevTeam PBI is ready for Implementation check
        - [SIMPL-15622](L0-SIMPL-10768/L1-SIMPL-12897/L2-SIMPL-12903/Epic-SIMPL-12905/Story-SIMPL-15622/README.md) User story · [API] API to provide the full contents of a provider's own resource description to the UI · DevTeam PBI is ready for Implementation check
      - [SIMPL-25590](L0-SIMPL-10768/L1-SIMPL-12897/L2-SIMPL-12903/Epic-SIMPL-25590/README.md) Epic · A Provider consults the policies of one of its own resource descriptions via UI · Defined
      - [SIMPL-30292](L0-SIMPL-10768/L1-SIMPL-12897/L2-SIMPL-12903/Epic-SIMPL-30292/README.md) Epic · A Provider triggers the update of the resource consent assertion related to a selected resource description via UI · Defined
        - [SIMPL-36072](L0-SIMPL-10768/L1-SIMPL-12897/L2-SIMPL-12903/Epic-SIMPL-30292/Story-SIMPL-36072/README.md) User story · A Provider selects a resource description and triggers the update of the resource consent status · PBI Defined
    - [SIMPL-12904](L0-SIMPL-10768/L1-SIMPL-12897/L2-SIMPL-12904/README.md) L2 · A Provider consults the version history of one of its own resource descriptions · Defined
      - [SIMPL-13658](L0-SIMPL-10768/L1-SIMPL-12897/L2-SIMPL-12904/Epic-SIMPL-13658/README.md) Epic · A Provider consults the version history of one of its own resource descriptions · Defined
        - [SIMPL-21646](L0-SIMPL-10768/L1-SIMPL-12897/L2-SIMPL-12904/Epic-SIMPL-13658/Story-SIMPL-21646/README.md) User story · [UI] A provider consults the version history of a resource description · DevTeam PBI is ready for Implementation check
        - [SIMPL-21647](L0-SIMPL-10768/L1-SIMPL-12897/L2-SIMPL-12904/Epic-SIMPL-13658/Story-SIMPL-21647/README.md) User story · [API] A provider consults the version history of a resource description via API (SD tooling) · Ready for DG Acceptance
        - [SIMPL-21648](L0-SIMPL-10768/L1-SIMPL-12897/L2-SIMPL-12904/Epic-SIMPL-13658/Story-SIMPL-21648/README.md) User story · [API] The Governance authority provides the version history of a resource description via API (Query mapper) · Ready for DG Acceptance
      - [SIMPL-29371](L0-SIMPL-10768/L1-SIMPL-12897/L2-SIMPL-12904/Epic-SIMPL-29371/README.md) Epic · A Provider consults the version history of one of its own resource descriptions [Improvement] · For DG Connect Review
  - [SIMPL-14640](L0-SIMPL-10768/L1-SIMPL-14640/README.md) L1 · The Governance Authority revokes a resource description · Defined
    - [SIMPL-14641](L0-SIMPL-10768/L1-SIMPL-14640/L2-SIMPL-14641/README.md) L2 · The Governance Authority revokes a resource description · Defined
      - [SIMPL-18025](L0-SIMPL-10768/L1-SIMPL-14640/L2-SIMPL-14641/Epic-SIMPL-18025/README.md) Epic · The Governance Authority revokes a resource description · Defined
        - [SIMPL-19789](L0-SIMPL-10768/L1-SIMPL-14640/L2-SIMPL-14641/Epic-SIMPL-18025/Story-SIMPL-19789/README.md) User story · [API] The Governance Authority revokes a Resource Description (Query Mapper) · Ready for DG Acceptance
        - [SIMPL-22059](L0-SIMPL-10768/L1-SIMPL-14640/L2-SIMPL-14641/Epic-SIMPL-18025/Story-SIMPL-22059/README.md) User story · [API] The Governance Authority revokes a Resource Description (Catalogue) · DevTeam PBI is ready for Implementation check
  - [SIMPL-14687](L0-SIMPL-10768/L1-SIMPL-14687/README.md) L1 · The Governance Authority publishes a Resource description · In Progress
    - [SIMPL-14689](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14689/README.md) L2 · The Governance Authority validates the schema of a new Resource description or a new version of a Resource Description · Defined
      - [SIMPL-17332](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14689/Epic-SIMPL-17332/README.md) Epic · The Governance Authority checks the schema of a new resource description or a new version of a resource description · Defined
        - [SIMPL-21592](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14689/Epic-SIMPL-17332/Story-SIMPL-21592/README.md) User story · [API] The Governance Authority checks the schema of a new resource description or a new version of a resource description · Ready for DG Acceptance
    - [SIMPL-14746](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14746/README.md) L2 · The Governance Authority notifies a Provider about an inactive schema · Defined
      - [SIMPL-17798](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14746/Epic-SIMPL-17798/README.md) Epic · The Governance Authority notifies a Provider about an invalid schema · Defined
        - [SIMPL-21597](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14746/Epic-SIMPL-17798/Story-SIMPL-21597/README.md) User story · [API] The Governance Authority notifies a Provider about an invalid schema · Ready for DG Acceptance
    - [SIMPL-14747](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14747/README.md) L2 · The Governance Authority validates the syntax of a new Resource description or a new version of a Resource Description · In Progress
      - [SIMPL-982](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14747/Epic-SIMPL-982/README.md) Epic · The Governance Authority checks the syntax of a new Resource description or a new version of a Resource Description · Defined
        - [SIMPL-488](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14747/Epic-SIMPL-982/Story-SIMPL-488/README.md) User story · Syntax Validation Engine · DevTeam PBI is ready for Implementation check
      - [SIMPL-18402](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14747/Epic-SIMPL-18402/README.md) Epic · The Governance Authority validates the syntax of the policies assigned to a new Resource description or a new version of a Resource Description · In Progress
        - [SIMPL-31673](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14747/Epic-SIMPL-18402/Story-SIMPL-31673/README.md) User story · [API] GA validates the syntax of access and usage policies of new RD version · FOR SC TO RESPOND
        - [SIMPL-31674](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14747/Epic-SIMPL-18402/Story-SIMPL-31674/README.md) User story · [UI] Provider sees the policy syntax validation outcome in the RD editor for new RD version · PBI INTERNAL QA
    - [SIMPL-14748](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14748/README.md) L2 · The Governance Authority provides feedback to a Provider about syntax validation errors · In Progress
      - [SIMPL-17801](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14748/Epic-SIMPL-17801/README.md) Epic · The Governance Authority notifies a Provider about syntax validation errors · Defined
        - [SIMPL-21591](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14748/Epic-SIMPL-17801/Story-SIMPL-21591/README.md) User story · [API] The Governance Authority notifies a Provider about syntax validation errors · Ready for DG Acceptance
      - [SIMPL-25371](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14748/Epic-SIMPL-25371/README.md) Epic · The Governance Authority provides feedback to a Provider about syntax validation errors of a policy · In Progress
        - [SIMPL-31640](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14748/Epic-SIMPL-25371/Story-SIMPL-31640/README.md) User story · [API] The Governance Authority provides feedback to a Provider about policy syntax validation errors · FOR SC TO RESPOND
        - [SIMPL-31641](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14748/Epic-SIMPL-25371/Story-SIMPL-31641/README.md) User story · [UI] The Provider views policy syntax validation errors in the resource description editor · PBI Cancelled · *🔴 cancelled*
    - [SIMPL-14749](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14749/README.md) L2 · The Governance Authority validates the semantics of a new Resource description or a new version of a Resource Description · Defined
      - [SIMPL-889](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14749/Epic-SIMPL-889/README.md) Epic · The Governance Authority checks the semantics of a new Resource description or a new version of a Resource Description · Defined
        - [SIMPL-490](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14749/Epic-SIMPL-889/Story-SIMPL-490/README.md) User story · Semantic Validation Engine · DevTeam PBI is ready for Implementation check
        - [SIMPL-491](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14749/Epic-SIMPL-889/Story-SIMPL-491/README.md) User story · Semantic Validation of Self-Description · DevTeam PBI is ready for Implementation check
      - [SIMPL-29978](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14749/Epic-SIMPL-29978/README.md) Epic · The Governance Authority checks the semantics of a new Resource description or a new version of a Resource Description [Extension Consent] · Defined
    - [SIMPL-14750](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14750/README.md) L2 · The Governance Authority provides feedback to a Provider about semantic validation errors · Defined
      - [SIMPL-18040](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14750/Epic-SIMPL-18040/README.md) Epic · The Governance Authority provides feedback to a Provider about semantic validation errors · Defined
        - [SIMPL-21593](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14750/Epic-SIMPL-18040/Story-SIMPL-21593/README.md) User story · [API] The Governance Authority notifies a Provider about semantic validation errors · Ready for DG Acceptance
    - [SIMPL-14753](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14753/README.md) L2 · The Governance Authority publishes a new Resource description or a new version of a Resource description · Defined
      - [SIMPL-4890](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14753/Epic-SIMPL-4890/README.md) Epic · The Governance Authority publishes a new resource description or a new version of a resource description · Defined
    - [SIMPL-14754](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14754/README.md) L2 · The Governance Authority revokes the previous version of a Resource description · Defined
      - [SIMPL-18041](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14754/Epic-SIMPL-18041/README.md) Epic · The Governance Authority revokes the previous version of a new version of a resource description · Defined
        - [SIMPL-21644](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14754/Epic-SIMPL-18041/Story-SIMPL-21644/README.md) User story · [API] The Governance Authority revokes the previous version of a new version of a resource description · Ready for DG Acceptance
    - [SIMPL-14864](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14864/README.md) L2 · The Governance Authority validates the versioning of a new version of a Resource description · Defined
      - [SIMPL-17799](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14864/Epic-SIMPL-17799/README.md) Epic · The Governance Authority checks the version number of a new version of a Resource description · Defined
        - [SIMPL-21596](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14864/Epic-SIMPL-17799/Story-SIMPL-21596/README.md) User story · [API] The Governance Authority checks the version number of a new version of a Resource description · Ready for DG Acceptance
    - [SIMPL-14865](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14865/README.md) L2 · The Governance Authority notifies a Provider about wrong versioning of new versions of a Resource description · Defined
      - [SIMPL-17800](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14865/Epic-SIMPL-17800/README.md) Epic · The Governance Authority notifies a Provider about a wrong version number of a new version of a Resource description · Defined
        - [SIMPL-21594](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-14865/Epic-SIMPL-17800/Story-SIMPL-21594/README.md) User story · [API] The Governance Authority notifies a Provider about a wrong version number of a new version of a Resource description · Ready for DG Acceptance
    - [SIMPL-22687](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-22687/README.md) L2 · The Governance Authority evaluates the metadata quality of a new resource description or a new version of a resource description · Defined
      - [SIMPL-24060](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-22687/Epic-SIMPL-24060/README.md) Epic · The Governance Authority evaluates the metadata quality of a Resource description · Defined
        - [SIMPL-27899](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-22687/Epic-SIMPL-24060/Story-SIMPL-27899/README.md) User story · Governance Authority evaluate metadata quality scores for submitted resource description · DevTeam PBI is ready for Implementation check
        - [SIMPL-27901](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-22687/Epic-SIMPL-24060/Story-SIMPL-27901/README.md) User story · Governance Authority assign metadata quality classifications to evaluation results · DevTeam PBI is ready for Implementation check
        - [SIMPL-27904](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-22687/Epic-SIMPL-24060/Story-SIMPL-27904/README.md) User story · Governance Authority store metadata quality assessment as related metadata · DevTeam PBI is ready for Implementation check
    - [SIMPL-22688](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-22688/README.md) L2 · The Governance Authority notifies the Provider about the metadata quality · Defined
      - [SIMPL-24062](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-22688/Epic-SIMPL-24062/README.md) Epic · The Governance Authority notifies the Provider about metadata quality results · Defined
        - [SIMPL-28001](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-22688/Epic-SIMPL-24062/Story-SIMPL-28001/README.md) User story · Governance Authority and Provider access metadata quality assessment outcome through publication response · DevTeam PBI is ready for Implementation check
    - [SIMPL-24049](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-24049/README.md) L2 · The Governance Authority approves/rejects the metadata of a new resource description or a new version of a resource description · Defined
      - [SIMPL-24061](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-24049/Epic-SIMPL-24061/README.md) Epic · The Governance Authority approves or rejects a Resource description based on metadata quality · Defined
        - [SIMPL-27998](L0-SIMPL-10768/L1-SIMPL-14687/L2-SIMPL-24049/Epic-SIMPL-24061/Story-SIMPL-27998/README.md) User story · Catalogue validates against the Quality Assesment Record and decides the publication before storing self-description · DevTeam PBI is ready for Implementation check
  - [SIMPL-14688](L0-SIMPL-10768/L1-SIMPL-14688/README.md) L1 · A Provider creates a new Resource description · In Progress
    - [SIMPL-14730](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14730/README.md) L2 · A provider retrieves a Resource type · Defined
      - [SIMPL-2285](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14730/Epic-SIMPL-2285/README.md) Epic · A Provider retrieves from the Governance Authority a list of published resource types · Defined
        - [SIMPL-330](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14730/Epic-SIMPL-2285/Story-SIMPL-330/README.md) User story · [API] API for the Self-Description Tool · Ready for DG Acceptance
        - [SIMPL-15867](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14730/Epic-SIMPL-2285/Story-SIMPL-15867/README.md) User story · [UI] A provider consults a list of published resource types in the resource description manager UI · DevTeam PBI is ready for Implementation check
        - [SIMPL-16118](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14730/Epic-SIMPL-2285/Story-SIMPL-16118/README.md) User story · [API] A provider resource description manager retrieves a list of active schemas via API for the UI · Ready for DG Acceptance
      - [SIMPL-19176](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14730/Epic-SIMPL-19176/README.md) Epic · A Provider retrieves from the Governance Authority the schema of a published resource type · Defined
        - [SIMPL-20514](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14730/Epic-SIMPL-19176/Story-SIMPL-20514/README.md) User story · API for Schema Managament Webhook Notification and Trigger Schema Update · DevTeam PBI is ready for Implementation check
        - [SIMPL-20613](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14730/Epic-SIMPL-19176/Story-SIMPL-20613/README.md) User story · [UI] A provider selects the Resource type in the UI · DevTeam PBI is ready for Implementation check
        - [SIMPL-23442](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14730/Epic-SIMPL-19176/Story-SIMPL-23442/README.md) User story · [API] API for retrieving a specific schema version (schema-sync-adapter) · DevTeam PBI is ready for Implementation check
        - [SIMPL-24335](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14730/Epic-SIMPL-19176/Story-SIMPL-24335/README.md) User story · [API] API for retrieving a specific schema version (sd-tooling-be) · DevTeam PBI is ready for Implementation check
    - [SIMPL-14731](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14731/README.md) L2 · A Provider adds common metadata to a new Resource description · Defined
      - [SIMPL-8340](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14731/Epic-SIMPL-8340/README.md) Epic · A Provider defines the resource source address for resource sharing · Defined
        - [SIMPL-10301](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14731/Epic-SIMPL-8340/Story-SIMPL-10301/README.md) User story · [UI] Viewing Resource Sharing Methods and Their Templates (Provider) · Ready for DG Acceptance
        - [SIMPL-10562](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14731/Epic-SIMPL-8340/Story-SIMPL-10562/README.md) User story · [API] Extend the SD Tooling API for Retrieving Available Resource Sharing Methods and Templates (Provider) · Ready for DG Acceptance
        - [SIMPL-12291](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14731/Epic-SIMPL-8340/Story-SIMPL-12291/README.md) User story · [API] Resource Source Address Validation Based on Resource Sharing Method and Provider Template · DevTeam PBI is ready for Implementation check
        - [SIMPL-13565](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14731/Epic-SIMPL-8340/Story-SIMPL-13565/README.md) User story · [UI] Selecting, Filling, and Submitting the Resource Source Template (Provider) · Ready for DG Acceptance
        - [SIMPL-13635](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14731/Epic-SIMPL-8340/Story-SIMPL-13635/README.md) User story · [API] Resource Source Address Validation Upon Provider Resource Submission · DevTeam PBI is ready for Implementation check
        - [SIMPL-18360](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14731/Epic-SIMPL-8340/Story-SIMPL-18360/README.md) User story · Create Resource Address Template for eDelivery · Ready for DG Acceptance
      - [SIMPL-15863](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14731/Epic-SIMPL-15863/README.md) Epic · A Provider adds common metadata to a new Resource description via UI · Defined
        - [SIMPL-232](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14731/Epic-SIMPL-15863/Story-SIMPL-232/README.md) User story · [UI] Displaying Self-description Fields in the UI - SD Tool · DevTeam PBI is ready for Implementation check
        - [SIMPL-18343](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14731/Epic-SIMPL-15863/Story-SIMPL-18343/README.md) User story · [UI] Provider adds metadata property groups to a new Resource description · Ready for DG Acceptance
      - [SIMPL-25754](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14731/Epic-SIMPL-25754/README.md) Epic · A Provider defines the resource source address for resource sharing [Improvement] · Defined
    - [SIMPL-14732](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14732/README.md) L2 · A Provider adds resource type specific metadata to a new Resource description · Defined
      - [SIMPL-17409](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14732/Epic-SIMPL-17409/README.md) Epic · A Provider adds resource type specific metadata to a new Resource description via UI · Defined
        - [SIMPL-18344](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14732/Epic-SIMPL-17409/Story-SIMPL-18344/README.md) User story · [UI] Add data resource type specific metadata · Ready for DG Acceptance
        - [SIMPL-20969](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14732/Epic-SIMPL-17409/Story-SIMPL-20969/README.md) User story · [UI] Add application resource type specific metadata · Ready for DG Acceptance
        - [SIMPL-21590](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14732/Epic-SIMPL-17409/Story-SIMPL-21590/README.md) User story · [UI] Add infrastructure resource type specific metadata · Ready for DG Acceptance
      - [SIMPL-21309](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14732/Epic-SIMPL-21309/README.md) Epic · A Provider uploads a deployment script to a new infrastructure Resource description via UI · Defined
        - [SIMPL-10289](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14732/Epic-SIMPL-21309/Story-SIMPL-10289/README.md) User story · [UI] Select Resource Type "Infrastructure" and Open Deployment Script Upload · Ready for DG Acceptance
        - [SIMPL-13605](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14732/Epic-SIMPL-21309/Story-SIMPL-13605/README.md) User story · [UI] Upload Deployment Script in Popup Window · DevTeam PBI is ready for Implementation check
      - [SIMPL-23719](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14732/Epic-SIMPL-23719/README.md) Epic · Management of scripts and their metadata · In Preparation
      - [SIMPL-24800](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14732/Epic-SIMPL-24800/README.md) Epic · A Provider links a deployment script to a new infrastructure Resource description via UI · Defined
        - [SIMPL-28293](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14732/Epic-SIMPL-24800/Story-SIMPL-28293/README.md) User story · [UI] Add infrastructure deployment script link · DevTeam PBI is ready for Implementation check
      - [SIMPL-29946](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14732/Epic-SIMPL-29946/README.md) Epic · Provisioner API URL Configuration via Environment Variables (Remove Manual UI Property) [Improvement] · New
      - [SIMPL-30016](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14732/Epic-SIMPL-30016/README.md) Epic · A Provider sets the VPN configuration for Agent-to-Agent data transfer via UI · Defined
    - [SIMPL-14733](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14733/README.md) L2 · A Provider assigns access policies to a new Resource description · In Progress
      - [SIMPL-935](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14733/Epic-SIMPL-935/README.md) Epic · A Provider is supported in defining an access policy by a using policy template via API · In Progress
        - [SIMPL-2938](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14733/Epic-SIMPL-935/Story-SIMPL-2938/README.md) User story · API for Access Policy · Ready for DG Acceptance
        - [SIMPL-3546](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14733/Epic-SIMPL-935/Story-SIMPL-3546/README.md) User story · [API] Access Policy multiple attributes per Permission · PBI Defined
        - [SIMPL-3549](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14733/Epic-SIMPL-935/Story-SIMPL-3549/README.md) User story · [API] Access Policy possible to define Prohibitions · PBI Cancelled · *🔴 cancelled*
        - [SIMPL-3550](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14733/Epic-SIMPL-935/Story-SIMPL-3550/README.md) User story · [QueryMapper] Access Policy possible to define Prohibitions · PBI Cancelled · *🔴 cancelled*
        - [SIMPL-31346](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14733/Epic-SIMPL-935/Story-SIMPL-31346/README.md) User story · [API] Provider retrieves the list of access policy templates · DevTeam PBI is ready for Implementation check
        - [SIMPL-31347](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14733/Epic-SIMPL-935/Story-SIMPL-31347/README.md) User story · [API] Provider retrieves the details of an access policy template · DevTeam PBI is ready for Implementation check
      - [SIMPL-936](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14733/Epic-SIMPL-936/README.md) Epic · A Provider assigns access policies to a new resource description by using policy template via UI · In Progress
        - [SIMPL-493](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14733/Epic-SIMPL-936/Story-SIMPL-493/README.md) User story · UI for defining access policies · DevTeam PBI is ready for Implementation check
        - [SIMPL-2581](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14733/Epic-SIMPL-936/Story-SIMPL-2581/README.md) User story · Define Access Policy for the SD-Schema · DevTeam PBI is ready for Implementation check
        - [SIMPL-3545](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14733/Epic-SIMPL-936/Story-SIMPL-3545/README.md) User story · [UI] Access Policy multiple attributes per Permission · FOR SC TO RESPOND
        - [SIMPL-3548](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14733/Epic-SIMPL-936/Story-SIMPL-3548/README.md) User story · [UI] Access Policy possible to define Prohibitions · PBI Cancelled · *🔴 cancelled*
        - [SIMPL-18345](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14733/Epic-SIMPL-936/Story-SIMPL-18345/README.md) User story · [UI] Assign access policies to a new Resource description · PBI Cancelled · *🔴 cancelled*
        - [SIMPL-31405](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14733/Epic-SIMPL-936/Story-SIMPL-31405/README.md) User story · [UI] Provider selects an access policy template in the resource description wizard · DevTeam PBI is ready for Implementation check
        - [SIMPL-31406](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14733/Epic-SIMPL-936/Story-SIMPL-31406/README.md) User story · [UI] Provider fills in the access policy template parameters in the resource description wizard · DevTeam PBI is ready for Implementation check
        - [SIMPL-31407](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14733/Epic-SIMPL-936/Story-SIMPL-31407/README.md) User story · [UI] Provider sees inline validation feedback for access policy template parameters · DevTeam PBI is ready for Implementation check
        - [SIMPL-31433](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14733/Epic-SIMPL-936/Story-SIMPL-31433/README.md) User story · [UI] Provider adds an access policy to a new Resource Description · DevTeam PBI is ready for Implementation check
        - [SIMPL-31703](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14733/Epic-SIMPL-936/Story-SIMPL-31703/README.md) User story · [UI] Provider removes an access policy from the new resource description · DevTeam PBI is ready for Implementation check
      - [SIMPL-29315](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14733/Epic-SIMPL-29315/README.md) Epic · A Provider adds a new access policy template · In Progress
      - [SIMPL-29973](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14733/Epic-SIMPL-29973/README.md) Epic · A Provider assigns access policies related to resource consent to a new resource description via UI · Defined
        - [SIMPL-34175](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14733/Epic-SIMPL-29973/Story-SIMPL-34175/README.md) User story · [UI] Configure Resource Consent Access Policy for Resource Descriptions · PBI Defined
      - [SIMPL-29974](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14733/Epic-SIMPL-29974/README.md) Epic · A Provider assigns access policies related to contract consent to a new resource description via UI · Defined
        - [SIMPL-34176](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14733/Epic-SIMPL-29974/Story-SIMPL-34176/README.md) User story · [UI] Configure Contract Consent Access Policy for Resource Descriptions · FOR SC TO RESPOND
    - [SIMPL-14734](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14734/README.md) L2 · A Provider assigns usage policies to a new Resource description · In Progress
      - [SIMPL-960](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14734/Epic-SIMPL-960/README.md) Epic · A Provider is supported in defining a usage policy by using a policy template via API · In Progress
        - [SIMPL-3241](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14734/Epic-SIMPL-960/Story-SIMPL-3241/README.md) User story · API for Usage Policy · Ready for DG Acceptance
        - [SIMPL-31385](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14734/Epic-SIMPL-960/Story-SIMPL-31385/README.md) User story · [API] Provider retrieves the list of usage policy templates · DevTeam PBI is ready for Implementation check
        - [SIMPL-31386](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14734/Epic-SIMPL-960/Story-SIMPL-31386/README.md) User story · [API] Provider retrieves the details of a usage policy template · DevTeam PBI is ready for Implementation check
      - [SIMPL-961](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14734/Epic-SIMPL-961/README.md) Epic · A Provider assigns usage policies to a new resource description by using policy template via UI · In Progress
        - [SIMPL-424](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14734/Epic-SIMPL-961/Story-SIMPL-424/README.md) User story · UI for definition of usage policies · DevTeam PBI is ready for Implementation check
        - [SIMPL-18346](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14734/Epic-SIMPL-961/Story-SIMPL-18346/README.md) User story · [UI] Assign usage policies to a new Resource description · PBI Cancelled · *🔴 cancelled*
        - [SIMPL-31452](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14734/Epic-SIMPL-961/Story-SIMPL-31452/README.md) User story · [UI] Provider selects a usage policy template in the resource description wizard · DevTeam PBI is ready for Implementation check
        - [SIMPL-31453](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14734/Epic-SIMPL-961/Story-SIMPL-31453/README.md) User story · [UI] Provider fills in the usage policy template parameters in the resource description wizard · DevTeam PBI is ready for Implementation check
        - [SIMPL-31454](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14734/Epic-SIMPL-961/Story-SIMPL-31454/README.md) User story · [UI] Provider sees inline validation feedback for usage policy template parameters · DevTeam PBI is ready for Implementation check
        - [SIMPL-31455](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14734/Epic-SIMPL-961/Story-SIMPL-31455/README.md) User story · [UI] Provider adds a usage policy to a new Resource Description · DevTeam PBI is ready for Implementation check
        - [SIMPL-31704](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14734/Epic-SIMPL-961/Story-SIMPL-31704/README.md) User story · [UI] Provider removes a usage policy from the new resource description · DevTeam PBI is ready for Implementation check
      - [SIMPL-28901](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14734/Epic-SIMPL-28901/README.md) Epic · A Provider defines a policy information point to monitor usage policies · Defined
      - [SIMPL-29331](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14734/Epic-SIMPL-29331/README.md) Epic · A Provider adds a new usage policy template · In Progress
    - [SIMPL-14736](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/README.md) L2 · A Provider adds a set of contract clauses to a new resource description · In Progress
      - [SIMPL-16251](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-16251/README.md) Epic · A Provider adds contract clauses to a new Resource description via UI · In Progress
        - [SIMPL-26472](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-16251/Story-SIMPL-26472/README.md) User story · A Provider adds a licence agreement to a new resource description via UI · DevTeam PBI is ready for Implementation check
        - [SIMPL-28815](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-16251/Story-SIMPL-28815/README.md) User story · A Provider adds a billing schema to a new resource description via UI · DevTeam PBI is ready for Implementation check
        - [SIMPL-28816](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-16251/Story-SIMPL-28816/README.md) User story · A Provider adds an SLA to a new resource description via UI · DevTeam PBI is ready for Implementation check
        - [SIMPL-28846](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-16251/Story-SIMPL-28846/README.md) User story · Store uploaded Contract Clauses documents and generate hash metadata · DevTeam PBI is ready for Implementation check
        - [SIMPL-29022](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-16251/Story-SIMPL-29022/README.md) User story · [API] - API to store Contract Clause document · DevTeam PBI is ready for Implementation check
        - [SIMPL-29049](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-16251/Story-SIMPL-29049/README.md) User story · [API] - API to retrieve Contract Clause document (xfsc-adv-search) · DevTeam PBI is ready for Implementation check
        - [SIMPL-29384](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-16251/Story-SIMPL-29384/README.md) User story · A Provider adds a generic contract clause to a new resource description via UI · DevTeam PBI is ready for Implementation check
        - [SIMPL-33065](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-16251/Story-SIMPL-33065/README.md) User story · [UI] Enable Contract Clauses Document Download in SDTooling UI · PBI INTERNAL QA
        - [SIMPL-33084](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-16251/Story-SIMPL-33084/README.md) User story · [UI] Enable Contract Clauses Document Download in CatalogueUI · PBI INTERNAL QA
      - [SIMPL-28461](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-28461/README.md) Epic · A Provider defines an endpoint that serves as an information point to calculate the billing data · Defined
        - [SIMPL-36085](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-28461/Story-SIMPL-36085/README.md) User story · [Documentation][Billing] Create Billing information point definition guide for Providers · New Product Backlog Item
      - [SIMPL-28462](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-28462/README.md) Epic · A Provider defines an endpoint that serves as an information point to check the credit status of their usage contract · Defined
        - [SIMPL-36086](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-28462/Story-SIMPL-36086/README.md) User story · [Documentation][Credit status] Create Payment information point definition guide for Providers · New Product Backlog Item
      - [SIMPL-28989](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-28989/README.md) Epic · A Provider manages contract clauses [MVP] · For DG Connect Review
        - [SIMPL-509](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-28989/Story-SIMPL-509/README.md) User story · UI for assigning a defining billing schemas [Placeholder] · Ready for DG Acceptance
        - [SIMPL-510](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-28989/Story-SIMPL-510/README.md) User story · Support document mode for Billing Schema using source + hash · DevTeam PBI is ready for Implementation check
        - [SIMPL-512](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-28989/Story-SIMPL-512/README.md) User story · UI for assigning a defining SLA Agreements [Placeholder] · Ready for DG Acceptance
        - [SIMPL-513](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-28989/Story-SIMPL-513/README.md) User story · Support document mode for SLA Agreements using source + hash · Ready for DG Acceptance
        - [SIMPL-516](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-28989/Story-SIMPL-516/README.md) User story · UI for assigning a Contract Template to a Resource · Ready for DG Acceptance
        - [SIMPL-517](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-28989/Story-SIMPL-517/README.md) User story · Support document mode for Contract Clauses using source + hash · Ready for DG Acceptance
      - [SIMPL-29121](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-29121/README.md) Epic · A Provider defines an information point to monitor SLAs · Defined
      - [SIMPL-29975](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-29975/README.md) Epic · A Provider adds a contract clauses related resource consent of the data resource to a new resource description via UI · Defined
        - [SIMPL-34168](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-29975/Story-SIMPL-34168/README.md) User story · [UI] Add Resource Consent Contract Clause to Resource Description · FOR SC TO RESPOND
        - [SIMPL-34485](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-29975/Story-SIMPL-34485/README.md) User story · [UI] Enable Consent Management Contract Clauses Document Download in SDTooling UI · FOR SC TO RESPOND
      - [SIMPL-29976](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-29976/README.md) Epic · A Provider adds a contract clauses related to the intended purpose of the data resource to a new resource description via UI · Defined
        - [SIMPL-34169](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-29976/Story-SIMPL-34169/README.md) User story · [UI] Add Intended Purpose Contract Clause to Resource Description · PBI Defined
        - [SIMPL-34486](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-29976/Story-SIMPL-34486/README.md) User story · [UI] Enable Consent Management Intended purpose Contract Clauses Document Download in SDTooling UI · FOR SC TO RESPOND
      - [SIMPL-29977](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-29977/README.md) Epic · A Provider adds contract clauses related to contract consent to a new resource description via UI · Defined
        - [SIMPL-34170](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-29977/Story-SIMPL-34170/README.md) User story · [UI] Add Contract Consent Contract Clause to Resource Description · FOR SC TO RESPOND
        - [SIMPL-34487](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-29977/Story-SIMPL-34487/README.md) User story · [UI] Enable Contract Consent Management Contract Clauses Document Download in SDTooling UI · FOR SC TO RESPOND
      - [SIMPL-30655](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-30655/README.md) Epic · A Provider adds contract clauses to a new Resource description via UI [Extension] · Defined
        - [SIMPL-30657](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14736/Epic-SIMPL-30655/Story-SIMPL-30657/README.md) User story · A Provider removes contract clauses from a resource description via UI, when creating a new resource description · PBI Defined
  - [SIMPL-14699](L0-SIMPL-10768/L1-SIMPL-14699/README.md) L1 · A Provider requests the revocation of a Resource description · Defined
    - [SIMPL-14639](L0-SIMPL-10768/L1-SIMPL-14699/L2-SIMPL-14639/README.md) L2 · A Provider requests the revocation of a resource description · Defined
      - [SIMPL-9764](L0-SIMPL-10768/L1-SIMPL-14699/L2-SIMPL-14639/Epic-SIMPL-9764/README.md) Epic · A Provider requests the revocation of a resource description via UI or API · Defined
        - [SIMPL-19784](L0-SIMPL-10768/L1-SIMPL-14699/L2-SIMPL-14639/Epic-SIMPL-9764/Story-SIMPL-19784/README.md) User story · [UI] A Provider requests the revocation of a Resource description via UI · DevTeam PBI is ready for Implementation check
        - [SIMPL-19785](L0-SIMPL-10768/L1-SIMPL-14699/L2-SIMPL-14639/Epic-SIMPL-9764/Story-SIMPL-19785/README.md) User story · [API] Provider requests the revocation of a Resource description via API (SD tooling) · Ready for DG Acceptance
  - [SIMPL-14728](L0-SIMPL-10768/L1-SIMPL-14728/README.md) L1 · A Provider creates a new version of a Resource description · In Progress
    - [SIMPL-14738](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14738/README.md) L2 · A Provider updates the common metadata of a new version of a Resource description · Defined
      - [SIMPL-12250](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14738/Epic-SIMPL-12250/README.md) Epic · A Provider updates the resource source address for resource sharing · Defined
        - [SIMPL-13565](L0-SIMPL-10768/L1-SIMPL-14688/L2-SIMPL-14731/Epic-SIMPL-8340/Story-SIMPL-13565/README.md) User story · [UI] Selecting, Filling, and Submitting the Resource Source Template (Provider) · *shown under SIMPL-8340*
      - [SIMPL-18028](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14738/Epic-SIMPL-18028/README.md) Epic · A Provider updates common metadata of a new version of a  Resource description via UI · Defined
        - [SIMPL-18683](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14738/Epic-SIMPL-18028/Story-SIMPL-18683/README.md) User story · [UI] Provider updates common metadata to a new version of Resource description · Ready for DG Acceptance
        - [SIMPL-25147](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14738/Epic-SIMPL-18028/Story-SIMPL-25147/README.md) User story · [API] API for retrieving a resourceAddress info (EdcConnectorAdapter) · Ready for DG Acceptance
        - [SIMPL-25148](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14738/Epic-SIMPL-18028/Story-SIMPL-25148/README.md) User story · [API] API for retrieving a resourceAddress info (sd-tooling-be) · Ready for DG Acceptance
        - [SIMPL-25170](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14738/Epic-SIMPL-18028/Story-SIMPL-25170/README.md) User story · [UI] Retrieve resourceAddress info for updating/visualizing resource description · Ready for DG Acceptance
        - [SIMPL-25713](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14738/Epic-SIMPL-18028/Story-SIMPL-25713/README.md) User story · [API] Implement API to atomically enrich and sign a resourceDescription (SD-Tooling)) · Ready for DG Acceptance
      - [SIMPL-19293](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14738/Epic-SIMPL-19293/README.md) Epic · A Provider starts the creation of a new version of a Resource description via UI · Defined
        - [SIMPL-21651](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14738/Epic-SIMPL-19293/Story-SIMPL-21651/README.md) User story · [UI] A Provider starts the creation of a new version of a Resource description via UI · Ready for DG Acceptance
      - [SIMPL-32140](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14738/Epic-SIMPL-32140/README.md) Epic · A Provider updates common metadata of a new version of a  Resource description via UI (Improvement) · For DG Connect Review
    - [SIMPL-14739](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14739/README.md) L2 · A Provider updates the resource type specific metadata of a new version of a Resource description · Defined
      - [SIMPL-18029](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14739/Epic-SIMPL-18029/README.md) Epic · A Provider updates the resource type specific metadata of a new version of a  Resource description via UI · Defined
        - [SIMPL-18685](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14739/Epic-SIMPL-18029/Story-SIMPL-18685/README.md) User story · [UI] Update data resource type specific metadata to new RD version · Ready for DG Acceptance
        - [SIMPL-20970](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14739/Epic-SIMPL-18029/Story-SIMPL-20970/README.md) User story · [UI] Update application resource type specific metadata to new RD version · Ready for DG Acceptance
        - [SIMPL-21641](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14739/Epic-SIMPL-18029/Story-SIMPL-21641/README.md) User story · [UI] Update infrastructure resource type specific metadata to new RD version · Ready for DG Acceptance
      - [SIMPL-21310](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14739/Epic-SIMPL-21310/README.md) Epic · A Provider updates the deployment script of a new version of an infrastructure Resource description via UI · Defined
      - [SIMPL-24801](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14739/Epic-SIMPL-24801/README.md) Epic · A Provider updates the link to the deployment script in a new version of an infrastructure Resource description via UI · Defined
        - [SIMPL-28325](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14739/Epic-SIMPL-24801/Story-SIMPL-28325/README.md) User story · [UI] Update infrastructure deployment script link · DevTeam PBI is ready for Implementation check
    - [SIMPL-14740](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14740/README.md) L2 · A Provider updates the access policies of a new version of a Resource description · In Progress
      - [SIMPL-4887](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14740/Epic-SIMPL-4887/README.md) Epic · A Provider updates the access policies of a new version of a Resource description by using policy templatesvia UI · In Progress
        - [SIMPL-18686](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14740/Epic-SIMPL-4887/Story-SIMPL-18686/README.md) User story · [UI] Asssign access policy to a new version of a resource description · PBI Cancelled · *🔴 cancelled*
        - [SIMPL-31543](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14740/Epic-SIMPL-4887/Story-SIMPL-31543/README.md) User story · [UI] Provider sees the access policies carried over from the previous resource description version · PBI INTERNAL QA
        - [SIMPL-31544](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14740/Epic-SIMPL-4887/Story-SIMPL-31544/README.md) User story · [UI] Provider selects an access policy template in the new resource description version wizard · PBI INTERNAL QA
        - [SIMPL-31545](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14740/Epic-SIMPL-4887/Story-SIMPL-31545/README.md) User story · [UI] Provider fills in the access policy template parameters in the new resource description version wizard · PBI INTERNAL QA
        - [SIMPL-31546](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14740/Epic-SIMPL-4887/Story-SIMPL-31546/README.md) User story · [UI] Provider removes an access policy from the new resource description version · PBI INTERNAL QA
        - [SIMPL-31547](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14740/Epic-SIMPL-4887/Story-SIMPL-31547/README.md) User story · [UI] Provider saves the updated access policies on the new resource description version · PBI Defined
    - [SIMPL-14741](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14741/README.md) L2 · A Provider updates the usage policies of a new version of a Resource description · In Progress
      - [SIMPL-18030](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14741/Epic-SIMPL-18030/README.md) Epic · A Provider updates the usage policies of a new version of a resource description by using policy template via UI · In Progress
        - [SIMPL-18688](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14741/Epic-SIMPL-18030/Story-SIMPL-18688/README.md) User story · [UI] Assign usage policies to the new version of Resource description · PBI Cancelled · *🔴 cancelled*
        - [SIMPL-31575](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14741/Epic-SIMPL-18030/Story-SIMPL-31575/README.md) User story · [UI] Provider sees the usage policies carried over from the previous resource description version · PBI INTERNAL QA
        - [SIMPL-31576](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14741/Epic-SIMPL-18030/Story-SIMPL-31576/README.md) User story · [UI] Provider selects a usage policy template in the new resource description version wizard · PBI INTERNAL QA
        - [SIMPL-31577](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14741/Epic-SIMPL-18030/Story-SIMPL-31577/README.md) User story · [UI] Provider fills in the usage policy template parameters in the new resource description version wizard · PBI INTERNAL QA
        - [SIMPL-31578](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14741/Epic-SIMPL-18030/Story-SIMPL-31578/README.md) User story · [UI] Provider removes a usage policy from the new resource description version · PBI INTERNAL QA
        - [SIMPL-31579](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14741/Epic-SIMPL-18030/Story-SIMPL-31579/README.md) User story · [UI] Provider saves the updated usage policies on the new resource description version · FOR SC TO RESPOND
    - [SIMPL-14743](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14743/README.md) L2 · A Provider updates the set of contract clauses of a new version of a resource description · Defined
      - [SIMPL-16252](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14743/Epic-SIMPL-16252/README.md) Epic · A Provider updates the contract clauses, resulting in a new version of the resource description via UI · Defined
        - [SIMPL-26274](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14743/Epic-SIMPL-16252/Story-SIMPL-26274/README.md) User story · A Provider updates the contract clauses, resulting in a new version of the resource description via UI · DevTeam PBI is ready for Implementation check
      - [SIMPL-30635](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14743/Epic-SIMPL-30635/README.md) Epic · A Provider removes contract clauses from a resource description via UI, resulting in a new version of the resource description · Defined
        - [SIMPL-30641](L0-SIMPL-10768/L1-SIMPL-14728/L2-SIMPL-14743/Epic-SIMPL-30635/Story-SIMPL-30641/README.md) User story · A Provider removes contract clauses from a resource description via UI, resulting in a new version of the resource description · PBI Defined
  - [SIMPL-14729](L0-SIMPL-10768/L1-SIMPL-14729/README.md) L1 · A Provider signs and submits a new Resource description or a new version of a Resource description · Defined
    - [SIMPL-14745](L0-SIMPL-10768/L1-SIMPL-14729/L2-SIMPL-14745/README.md) L2 · A Provider signs and submits a new Resource description or a new version of a Resource description · Defined
      - [SIMPL-2282](L0-SIMPL-10768/L1-SIMPL-14729/L2-SIMPL-14745/Epic-SIMPL-2282/README.md) Epic · A Provider receives notifications about validation errors via UI or API · Defined
        - [SIMPL-21642](L0-SIMPL-10768/L1-SIMPL-14729/L2-SIMPL-14745/Epic-SIMPL-2282/Story-SIMPL-21642/README.md) User story · [UI] A Provider views notifications about validation errors · DevTeam PBI is ready for Implementation check
      - [SIMPL-2292](L0-SIMPL-10768/L1-SIMPL-14729/L2-SIMPL-14745/Epic-SIMPL-2292/README.md) Epic · A Provider signs and  submits a new Resource description or a new version of a Resource description · Defined
        - [SIMPL-110](L0-SIMPL-10768/L1-SIMPL-14729/L2-SIMPL-14745/Epic-SIMPL-2292/Story-SIMPL-110/README.md) User story · Implement credential management logic · DevTeam PBI is ready for Implementation check
        - [SIMPL-226](L0-SIMPL-10768/L1-SIMPL-14729/L2-SIMPL-14745/Epic-SIMPL-2292/Story-SIMPL-226/README.md) User story · Sign created json-ld document with a signer · Ready for DG Acceptance
        - [SIMPL-228](L0-SIMPL-10768/L1-SIMPL-14729/L2-SIMPL-14745/Epic-SIMPL-2292/Story-SIMPL-228/README.md) User story · [UI] Create UI frontend for XFSC catalogue - Dialog add resource · DevTeam PBI is ready for Implementation check
        - [SIMPL-233](L0-SIMPL-10768/L1-SIMPL-14729/L2-SIMPL-14745/Epic-SIMPL-2292/Story-SIMPL-233/README.md) User story · Interactively Create a Service Self-Description · Ready for DG Acceptance
        - [SIMPL-241](L0-SIMPL-10768/L1-SIMPL-14729/L2-SIMPL-14745/Epic-SIMPL-2292/Story-SIMPL-241/README.md) User story · Deploy signer service · DevTeam PBI is ready for Implementation check
        - [SIMPL-314](L0-SIMPL-10768/L1-SIMPL-14729/L2-SIMPL-14745/Epic-SIMPL-2292/Story-SIMPL-314/README.md) User story · [UI] User Interface for Sign created JSON-LD document · DevTeam PBI is ready for Implementation check
        - [SIMPL-489](L0-SIMPL-10768/L1-SIMPL-14729/L2-SIMPL-14745/Epic-SIMPL-2292/Story-SIMPL-489/README.md) User story · Syntax Validation of Self-Description · Ready for DG Acceptance
        - [SIMPL-1083](L0-SIMPL-10768/L1-SIMPL-14729/L2-SIMPL-14745/Epic-SIMPL-2292/Story-SIMPL-1083/README.md) User story · Simpl Self-Description Frontend · Ready for DG Acceptance
        - [SIMPL-6287](L0-SIMPL-10768/L1-SIMPL-14729/L2-SIMPL-14745/Epic-SIMPL-2292/Story-SIMPL-6287/README.md) User story · Integrating the asset registering of the connector in to the published process of SD · Ready for DG Acceptance
        - [SIMPL-15289](L0-SIMPL-10768/L1-SIMPL-14729/L2-SIMPL-14745/Epic-SIMPL-2292/Story-SIMPL-15289/README.md) User story · Automatically populate the provider id on the resource description · DevTeam PBI is ready for Implementation check
        - [SIMPL-16825](L0-SIMPL-10768/L1-SIMPL-14729/L2-SIMPL-14745/Epic-SIMPL-2292/Story-SIMPL-16825/README.md) User story · [BE] Automatically populate the version number of a new resource description · Ready for DG Acceptance
        - [SIMPL-24346](L0-SIMPL-10768/L1-SIMPL-14729/L2-SIMPL-14745/Epic-SIMPL-2292/Story-SIMPL-24346/README.md) User story · [BE] Automatically populate the version number of a new version of a resource description · Ready for DG Acceptance
      - [SIMPL-26078](L0-SIMPL-10768/L1-SIMPL-14729/L2-SIMPL-14745/Epic-SIMPL-26078/README.md) Epic · A Provider signs and  submits a new Resource description or a new version of a Resource description [Improvement] · For DG Connect Review
  - [SIMPL-21117](L0-SIMPL-10768/L1-SIMPL-21117/README.md) L1 · The Provider notifies Consumers about the revocation of a resource description · In Progress
    - [SIMPL-14642](L0-SIMPL-10768/L1-SIMPL-21117/L2-SIMPL-14642/README.md) L2 · A Provider notifies Consumers about the revocation of a resource description · In Progress
      - [SIMPL-18026](L0-SIMPL-10768/L1-SIMPL-21117/L2-SIMPL-14642/Epic-SIMPL-18026/README.md) Epic · A Provider notifies Consumers about the revocation of a resource description · In Progress
        - [SIMPL-19790](L0-SIMPL-10768/L1-SIMPL-21117/L2-SIMPL-14642/Epic-SIMPL-18026/Story-SIMPL-19790/README.md) User story · [API] A Provider notifies Consumers about revocation of Resource Description · DevTeam PBI blocked
        - [SIMPL-22586](L0-SIMPL-10768/L1-SIMPL-21117/L2-SIMPL-14642/Epic-SIMPL-18026/Story-SIMPL-22586/README.md) User story · [UI] Inform end user (provider) about successful revocation · PBI Defined
  - [SIMPL-21118](L0-SIMPL-10768/L1-SIMPL-21118/README.md) L1 · A Provider notifies Consumers about a new version of a Resource description · In Progress
    - [SIMPL-14755](L0-SIMPL-10768/L1-SIMPL-21118/L2-SIMPL-14755/README.md) L2 · A Provider notifies Consumers about a new version of a Resource description · In Progress
      - [SIMPL-17521](L0-SIMPL-10768/L1-SIMPL-21118/L2-SIMPL-14755/Epic-SIMPL-17521/README.md) Epic · A Provider notifies Consumers about a new version of a Resource description · In Progress
        - [SIMPL-21600](L0-SIMPL-10768/L1-SIMPL-21118/L2-SIMPL-14755/Epic-SIMPL-17521/Story-SIMPL-21600/README.md) User story · [BE] A Provider notifies Consumers about a new version of a Resource description · DevTeam PBI blocked
