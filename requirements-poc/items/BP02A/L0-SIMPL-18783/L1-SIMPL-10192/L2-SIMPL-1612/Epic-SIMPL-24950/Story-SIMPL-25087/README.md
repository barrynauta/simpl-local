> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10192](../../../README.md) / [L2-SIMPL-1612](../../README.md) / [Epic-SIMPL-24950](../README.md) / **Story-SIMPL-25087**

# SIMPL-25087: Governance Authority to have search and consumption identity attributes marked as federable

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10192](../../../README.md) (L1) › [SIMPL-1612](../../README.md) (L2) › [SIMPL-24950](../README.md) (Epic) › **SIMPL-25087** (User story)

## Text

**As a** Dataspace Governance authority,\
**I want** the Simpl-Open built-in identity attributes related to searching and consumption to be marked as federable\
**So that** I can ensure that these attributes can be mapped to federated trusted authorities identity attributes to provide these capabilities to trusted participants

**Acceptance Criteria**

1. **Given** that I install or update a Simpl-Open authority agent\
   **When** I fetch the list of the identity attributes\
   **Then** the built-in attributes related to searching and consumption are marked as federable (1)

(1) The built-in identity attributes affected by this change are CATALOGUE_SEARCHER (to be added to the list of built-in attributes) and CONSUMER (already present).

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-24950](../README.md) (Epic) Governance Authority marks identity attributes as federable |
