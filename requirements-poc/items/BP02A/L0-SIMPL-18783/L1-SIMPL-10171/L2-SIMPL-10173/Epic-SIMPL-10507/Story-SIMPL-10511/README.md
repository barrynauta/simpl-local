> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10171](../../../README.md) / [L2-SIMPL-10173](../../README.md) / [Epic-SIMPL-10507](../README.md) / **Story-SIMPL-10511**

# SIMPL-10511: [UI] Governance Authority manages presence check validation rules on onboarding procedure template

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10171](../../../README.md) (L1) › [SIMPL-10173](../../README.md) (L2) › [SIMPL-10507](../README.md) (Epic) › **SIMPL-10511** (User story)

## Text

**As A**

dataspace governance authority user with the _Tier 2 authorization manager (T2IAA_M)_ role, viewing details of an onboarding template for a given participant type.

**I want to**

via API, manage the list of PRESENCE CHECK validation rules for each onboarding procedure template

**So** **that**

I can define the parameters to support the automatic check the presence or the absence of a document and support the approval or rejection process of the users with NOTARY role.

**Acceptance Criteria**

1. **GIVEN I am logged in as Tier 2 authorization Manager (T2IAA_M) AND I navigate to the read only screen of an onboarding procedure template AND no rules have been created WHEN the Rules tab is opened,**  **THEN a message is displayed** **saying that "No rules have been created"**
1. **GIVEN I am logged in as Tier 2 authorization Manager (T2IAA_M) AND I navigate to the read only screen of an onboarding procedure template AND rules have been created WHEN the Rules tab is opened, THEN I should see the list of the already created rules** **
1. **GIVEN I am logged in as Tier 2 authorization Manager (T2IAA_M) AND I navigate to the read only screen of an onboarding procedure template AND no document templates have been defined, WHEN the Rules tab is opened, THEN I should see a message saying that no rules can be created if there are no documents**
1. **GIVEN I am logged in as Tier 2 authorization Manager (T2IAA_M) AND I navigate to the read only screen of an onboarding procedure template** **AND a document template has been defined,** **WHEN the Rules tab is opened, THEN I should see an "Edit Rules" button that allows me to enter the rule editor mode.**
1. **GIVEN I am logged in as Tier 2 authorization Manager (T2IAA_M) AND I navigate to rule tab of an onboarding procedure template WHEN I click the Edit Rules button THEN I should see a button that allows me to create a new *Presence Check Rule***
1. **GIVEN I am logged in as Tier 2 authorization Manager (T2IAA_M), navigating to the Rules tab in edit mode, WHEN I click on the button to create a PRESENCE CHECK rule, THEN I should see a section that allows me to input the rule name, rule description, document template, enabled flag, required flag and the autoapprove flag**
1. **GIVEN I am logged in as Tier 2 Authorisation Manager (T2IAA_M) navigating to the Rules tab of an onboarding procedure template AND at least one rule has been defined WHEN I click the "Edit Rules" button THEN the form to update the rule is shown and I can modify all the fields except from the rule type**
1. GIVEN I am logged in as Tier 2 Authorisation Manager (T2IAA_M) navigating to the Rules tab of an onboarding procedure template AND I clicked the Edit rules button WHEN I click the "Save Changes" button THEN the existing rules are updated and the new ones are created.{}

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10507](../README.md) (Epic) Governance authority creates presence check rules to check if a document is uploaded |
