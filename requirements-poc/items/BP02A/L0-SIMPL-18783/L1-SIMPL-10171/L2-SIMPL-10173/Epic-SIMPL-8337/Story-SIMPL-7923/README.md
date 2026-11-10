> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10171](../../../README.md) / [L2-SIMPL-10173](../../README.md) / [Epic-SIMPL-8337](../README.md) / **Story-SIMPL-7923**

# SIMPL-7923: [UI] Governance Authority manages content check validation rules on onboarding procedure template

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10171](../../../README.md) (L1) › [SIMPL-10173](../../README.md) (L2) › [SIMPL-8337](../README.md) (Epic) › **SIMPL-7923** (User story)

## Text

**As a** dataspace governance authority user with the _Tier 2 authorization manager (T2IAA_M)_ role, viewing details of an onboarding template for a given participant type.\
**I want to** via UI, manage the list of CONTENT CHECK validation rules for each onboarding procedure template\
**So that** I can define the parameters supporting the automatic validation of the content of a document and support the users with NOTARY role during the approval or rejection process of onboarding requests

**Acceptance Criteria**

1. **Given** I am logged in as Tier 2 authorization Manager (T2IAA_M) **and** I navigate to the read only screen of an onboarding procedure template **and** no rules have been created **when** the Rules tab is opened, **then** a message is displayed saying that "No rules have been created".

2. **Given** I am logged in as Tier 2 authorization Manager (T2IAA_M) **and** I navigate to the read only screen of an onboarding procedure template **and** rules have been created **when** the Rules tab is opened, **then** I should see the list of the already created rules

3. **Given** I am logged in as Tier 2 authorization Manager (T2IAA_M) **and** I navigate to the read only screen of an onboarding procedure template **and** no document templates have been defined, **when** the Rules tab is opened, **then** I should see a message saying that no rules can be created if there are no documents

4. **Given** I am logged in as Tier 2 authorization Manager (T2IAA_M) **and** I navigate to the read only screen of an onboarding procedure template **and** a document template has been defined, **when** the Rules tab is opened, **then** I should see an "Edit Rules" button that allows me to enter the rule editor mode.

5. **Given** I am logged in as Tier 2 authorization Manager (T2IAA_M) **and** I navigate to rule tab of an onboarding procedure template **when** I click the Edit Rules button **then** I should see a button that allows me to create a new *Content Check Rule*

6. **Given** I am logged in as Tier 2 authorization Manager (T2IAA_M), navigating to the Rules tab in edit mode, **when** I click on the button to create a CONTENT CHECK rule, **then** I should see a section that allows me to input the rule name, rule description, document template, enabled flag, required flag, autoapprove flag and the HTTP endpoint of the external validation service

7. **Given** I am logged in as Tier 2 Authorisation Manager (T2IAA_M) navigating to the Rules tab of an onboarding procedure template **and** at least one rule has been defined **when** I click the "Edit Rules" button **then** the form to update the rule is shown and I can modify all the fields except from the rule type

8. **Given** I am logged in as Tier 2 Authorisation Manager (T2IAA_M) navigating to the Rules tab of an onboarding procedure template **and** I clicked the Edit rules button **when** I click the "Save Changes" button **then** the existing rules are updated and the new ones are created.{}

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-8337](../README.md) (Epic) Governance authority creates content check rules for automated document validation |
