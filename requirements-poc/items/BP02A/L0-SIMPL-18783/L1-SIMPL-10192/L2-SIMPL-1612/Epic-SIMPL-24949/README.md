> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP02A](../../../../README.md) / [L0-SIMPL-18783](../../../README.md) / [L1-SIMPL-10192](../../README.md) / [L2-SIMPL-1612](../README.md) / **Epic-SIMPL-24949**

# SIMPL-24949: Governance Authority marks identity attributes as public

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP02A](../../../../README.md) › [SIMPL-18783](../../../README.md) (L0) › [SIMPL-10192](../../README.md) (L1) › [SIMPL-1612](../README.md) (L2) › **SIMPL-24949** (Epic)

## Text

**As a**

Governance Authority

**I want to**

mark each of my identity attributes as public or non-public

**So that**

I can control which attributes are visible to external Governance Authorities and therefore available for federation mapping.

**Acceptance Criteria**

1. **GIVEN** I am a Governance Authority user with the appropriate roles\
   **WHEN** I mark a custom identity attribute as public or non-public\
   **THEN** the *public* property is updated accordingly
1. **GIVEN** I am a Governance Authority user with the appropriate roles\
   **WHEN** I access the public visibility setting of a built-in identity attribute\
   **THEN** the public attribute is already defined\
   **AND** the *public* property cannot be modified
1. **GIVEN** I am a Governance Authority user with the appropriate roles\
   **WHEN** I list the built-in identity attributes\
   **THEN** only identity attributes related to search and consumption are marked as public

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-1612](../README.md) (L2) Tier 2 identity attributes configuration |
| **Children** | [SIMPL-25007](Story-SIMPL-25007/README.md) (User story) [API] Governance authority manages public identity attributes<br>[SIMPL-25008](Story-SIMPL-25008/README.md) (User story) [UI] Governance authority manages public identity attributes<br>[SIMPL-25086](Story-SIMPL-25086/README.md) (User story) Governance Authority to have search and consumption identity attributes marked as public |
