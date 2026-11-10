> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10171](../../../README.md) / [L2-SIMPL-10173](../../README.md) / [Epic-SIMPL-10508](../README.md) / **Story-SIMPL-18914**

# SIMPL-18914: [UI][IMPROVEMENT] Allow users to create and update invalid composite onboarding validation rules when disabled

**User story (implementable work)** · Status **PBI Cancelled**

> 🔴 **Cancelled.** Kept here for history. It is not part of the current requirements.

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10171](../../../README.md) (L1) › [SIMPL-10173](../../README.md) (L2) › [SIMPL-10508](../README.md) (Epic) › **SIMPL-18914** (User story)

## Text

*This story invalidates AC7, AC8, and AC9 of SIMPL-8500. It has been created to solve SIMPL-16360.*

**As a** Dataspace Governance Authority end user with the T2IAA_M (Tier 2 authorization manager) role, viewing the rules tab of an onboarding procedure template\
**I want to** via UI, save invalid COMPOSITE onboarding validation rules in a disabled state\
**So that** I can draft validation rules without being blocked, while ensuring only valid rules can be enabled

**Acceptance Criteria**

1. **Given** I am logged in as Tier 2 authorization Manager (T2IAA_M)\
   **And** I am editing a composite rule\
   **When** any validation error is detected in a presence check rule at any level (due to a missing or invalid parameter such as name, description, validation interval, document template)\
   **Then** the error should be highlighted\
   **And** the enabled flag remains disabled\
   **And** the rule can be saved as disabled

2. **Given** I am logged in as Tier 2 authorization Manager (T2IAA_M)\
   **And** I am editing a composite rule\
   **When** any validation error is detected in a content check rule at any level (due to a missing or invalid parameter such as name, description, validation interval, document template, external validation endpoint)\
   **Then** the error should be highlighted\
   **And** the enabled flag remains disabled\
   **And** the rule can be saved as disabled

3. **Given** I am logged in as Tier 2 authorization Manager (T2IAA_M)\
   **And** I am editing a composite rule\
   **When** any validation error is detected in a composite rule at any level (due to a missing or invalid parameter such as name, description, validation interval, document template or due to less than two child rules defined)\
   **Then** the error should be highlighted\
   **And** the enabled flag remains disabled\
   **And** the rule can be saved as disabled

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10508](../README.md) (Epic) Governance Authority creates composite rules with custom logical conditions for document validation |
