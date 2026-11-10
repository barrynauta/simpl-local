> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10171](../../../README.md) / [L2-SIMPL-10173](../../README.md) / [Epic-SIMPL-10508](../README.md) / **Story-SIMPL-8500**

# SIMPL-8500: [UI] Governance Authority manages composite validation rules on onboarding procedure template

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10171](../../../README.md) (L1) › [SIMPL-10173](../../README.md) (L2) › [SIMPL-10508](../README.md) (Epic) › **SIMPL-8500** (User story)

## Text

**As a** dataspace governance authority user with the _Tier 2 authorization manager (T2IAA_M)_ role, viewing details of an onboarding template for a given participant type.\
**I want to** via UI, manage the list of COMPOSITE validation rules for each onboarding procedure template\
**So that** I can define a hierarchical tree structure combining content check and presence check rules to automate the validation of a document's content. This structure also supports users with the NOTARY role during the approval or rejection process of onboarding requests.

**Acceptance Criteria**

1. **Given** I am logged in as Tier 2 authorization Manager (T2IAA_M) **and** I navigate to the read only screen of an onboarding procedure template **and** composite rules have been created **when** the Rules tab is opened, **then** I should see the list of the already created rules and the composite rules can be expanded to see child rules

2. **Given** I am logged in as Tier 2 authorization Manager (T2IAA_M) **and** I navigate to the read only screen of an onboarding procedure template **when** I click on a composite rule preview title **then** I should see the composite rule detail preview along the child rules

3. **Given** I am logged in as Tier 2 authorization Manager (T2IAA_M) **and** I navigate to the read only screen of an onboarding procedure template **when** I click on the "Edit rules" button **then** I should be able to modify the heading of the composite rule by modifying the rule name, rule description, enabled flag, required flag, autoapprove flag

4. **Given** I am logged in as Tier 2 authorization Manager (T2IAA_M) **and** I am editing a composite rule **when** I click the edit icon of a child rule **then** I should see the child rule in edit mode and the rule menu on the side highlights the currently selected rule

5. **Given** I am logged in as Tier 2 authorization Manager (T2IAA_M) **and** I am editing a composite rule **when** I click the add child rule **then** I should be able to add a child rule of type PRESENCE, CONTENT or COMPOSITE{}

6. **Given** I am logged in as Tier 2 authorization Manager (T2IAA_M) **and** I am editing a composite rule **when** I click on the trash icon **then** I should be prompted to confirm the removal of the child rule

7. **Given** I am logged in as Tier 2 authorization Manager (T2IAA_M) **and** I am editing a composite rule **when** any validation error is detected in a presence check rule at any level (due to a missing or invalid parameter such as name, description, validation interval, document template) **then** the error should be highlighted and the save button disabled

8. **Given** I am logged in as Tier 2 authorization Manager (T2IAA_M) **and** I am editing a composite rule **when** any validation error is detected in a content check rule at any level (due to a missing or invalid parameter such as name, description, validation interval, document template, external validation endpoint) **then** the error should be highlighted and the save button disabled

9. **Given** I am logged in as Tier 2 authorization Manager (T2IAA_M) **and** I am editing a composite rule **when** any validation error is detected in a composite rule at any level (due to a missing or invalid parameter such as name, description, validation interval, document template or due to less than two child rules defined) **then** the error should be highlighted and the save button disabled{}

10. **Given** I am logged in as Tier 2 authorization Manager (T2IAA_M) **and** I am editing a composite rule **when** the nesting level has reached the configured maximum **then** I should NOT be able to add a child rule

11. **Given** I am logged in as Tier 2 authorization Manager (T2IAA_M) **and** I am editing a composite rule and no validation error are detected in the rule at any level **when** I Click on the Save Changes button **then** the composite rule is persistent and the UI is back to the read only state

12. **Given** I am logged in as Tier 2 authorization Manager (T2IAA_M), navigating to the Rules tab in edit mode, **when** I click on the button to create a COMPOSITE rule, **then** I should see a section that allows me to input the rule name, rule description, enabled flag, required flag and the autoapprove flag

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10508](../README.md) (Epic) Governance Authority creates composite rules with custom logical conditions for document validation |
