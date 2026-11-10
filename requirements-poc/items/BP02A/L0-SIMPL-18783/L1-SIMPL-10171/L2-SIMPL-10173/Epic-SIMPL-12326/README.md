> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP02A](../../../../README.md) / [L0-SIMPL-18783](../../../README.md) / [L1-SIMPL-10171](../../README.md) / [L2-SIMPL-10173](../README.md) / **Epic-SIMPL-12326**

# SIMPL-12326: Governance authority deletes a validation rule

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP02A](../../../../README.md) › [SIMPL-18783](../../../README.md) (L0) › [SIMPL-10171](../../README.md) (L1) › [SIMPL-10173](../README.md) (L2) › **SIMPL-12326** (Epic)

## Text

**As a** dataspace governance authority user with the _Tier 2 authorization manager (T2IAA_M)_ role\
**I want** to delete or disable document validation rules\
**So that** I can prevent using rules that are no longer relevant for future onboarding procedures

**Acceptance Criteria**

1. **Given** that I have defined a validation rule for an onboarding procedure template, and the rule has never been evaluated and is not linked to any onboarding request **when** I issue a delete request **then** the rule is permanently deleted from the onboarding procedure template.

2. **Given** that I have defined a validation rule for an onboarding procedure template, and the rule has been previously evaluated or is linked to one or more existing onboarding requests **when** I issue a delete request **then** the deletion of the rule is rejected.

3. **Given** that I have defined a validation rule for an onboarding procedure template, and the rule is linked to at least one onboarding request **when** I attempt to delete the rule **then** the system guides me to disable the rule instead, preventing its deletion{}

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10173](../README.md) (L2) Configure a ruleset for the automatic validation of onboarding request documents |
| **Children** | [SIMPL-11766](Story-SIMPL-11766/README.md) (User story) [UI] Governance Authority deletes a validation rule<br>[SIMPL-11767](Story-SIMPL-11767/README.md) (User story) [API] Governance Authority deletes a validation rule |
