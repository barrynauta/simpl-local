> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP02A](../../../../README.md) / [L0-SIMPL-18783](../../../README.md) / [L1-SIMPL-10171](../../README.md) / [L2-SIMPL-10173](../README.md) / **Epic-SIMPL-24264**

# SIMPL-24264: Governance authority deletes validation rules used in existing onboarding requests

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP02A](../../../../README.md) › [SIMPL-18783](../../../README.md) (L0) › [SIMPL-10171](../../README.md) (L1) › [SIMPL-10173](../README.md) (L2) › **SIMPL-24264** (Epic)

## Text

*This epics improves SIMPL-12326, by allowing the deletion of validation rules even if they are linked to an onboarding request.*\
----

**As a** dataspace governance authority user\
**I want** to delete document validation rules even when they are linked to onboarding requests\
**So that** I can delete rules that are no longer relevant for future onboarding procedures, by also preserving a snapshot of them to be used for past onboarding requests

**Acceptance Criteria**

1. [Deprecates AC1 of SIMPL-12326]

1. **Given** that I have defined a validation rule for an onboarding procedure template\
   **When** I issue a delete request\
   **Then** the rule is permanently deleted from the onboarding procedure template\
   **And** existing onboarding requests continue to use a snapshot of the rule at the time it was applied

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10173](../README.md) (L2) Configure a ruleset for the automatic validation of onboarding request documents |
| **Children** | [SIMPL-18911](Story-SIMPL-18911/README.md) (User story) [UI][IMPROVEMENT] Prevent the deletion of a document template linked to an onboarding validation rule<br>[SIMPL-24176](Story-SIMPL-24176/README.md) (User story) [API][IMPROVEMENT] Prevent the deletion of a document template linked to an onboarding validation rule<br>[SIMPL-24262](Story-SIMPL-24262/README.md) (User story) [UI][IMPROVEMENT] Allow the deletion of a validation rule even if used in an existing onboarding request<br>[SIMPL-24263](Story-SIMPL-24263/README.md) (User story) [API][IMPROVEMENT] Allow the deletion of a validation rule even if used in an existing onboarding request |
