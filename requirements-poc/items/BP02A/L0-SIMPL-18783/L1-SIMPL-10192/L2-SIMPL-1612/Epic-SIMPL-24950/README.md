> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP02A](../../../../README.md) / [L0-SIMPL-18783](../../../README.md) / [L1-SIMPL-10192](../../README.md) / [L2-SIMPL-1612](../README.md) / **Epic-SIMPL-24950**

# SIMPL-24950: Governance Authority marks identity attributes as federable

**Epic (deliverable, testable feature)**

**Trace:** [BP02A](../../../../README.md) › [SIMPL-18783](../../../README.md) (L0) › [SIMPL-10192](../../README.md) (L1) › [SIMPL-1612](../README.md) (L2) › **SIMPL-24950** (Epic)

## Text

**As a** Governance Authority\
**I want to** mark each of my identity attributes as federable or non-federable\
**So that** I can control which capabilities can be extended to participants of federated data spaces through attribute mapping.

**Acceptance Criteria**

1. **Given** I am a Governance Authority user with the appropriate roles\
   **When** I mark a custom identity attribute as federable or non-federable\
   **Then** the *federable* property of that attribute is updated accordingly

2. **Given** I am a Governance Authority user with the appropriate roles\
   **When** I access the federable setting of a built-in identity attribute\
   **Then** the federable attribute is already defined\
   **And** the *federable* property cannot be modified

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-1612](../README.md) (L2) Tier 2 identity attributes configuration |
| **Children** | [SIMPL-25009](Story-SIMPL-25009/README.md) (User story) [API] Governance authority manages federable identity attributes<br>[SIMPL-25010](Story-SIMPL-25010/README.md) (User story) [UI] Governance authority manages federable identity attributes<br>[SIMPL-25087](Story-SIMPL-25087/README.md) (User story) Governance Authority to have search and consumption identity attributes marked as federable |
