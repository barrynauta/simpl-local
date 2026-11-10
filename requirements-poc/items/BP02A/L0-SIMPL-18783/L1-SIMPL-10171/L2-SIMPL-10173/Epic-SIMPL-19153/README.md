> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP02A](../../../../README.md) / [L0-SIMPL-18783](../../../README.md) / [L1-SIMPL-10171](../../README.md) / [L2-SIMPL-10173](../README.md) / **Epic-SIMPL-19153**

# SIMPL-19153: Governance authority creates eIDAS rules for automated onboarding request validation

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP02A](../../../../README.md) › [SIMPL-18783](../../../README.md) (L0) › [SIMPL-10171](../../README.md) (L1) › [SIMPL-10173](../README.md) (L2) › **SIMPL-19153** (Epic)

## Text

**As a** Dataspace Governance Authority,\
**I want to** define an eIDAS rule for an onboarding procedure template by specifying an eIDAS attribute and optional expected values for the attribute\
**So that** onboarding requests submitted by users authenticated via eIDAS can be automatically validated

**Acceptance criteria**

1. **Given** that I am defining an onboarding procedure template **when** I add a rule of type eIDAS **then** I want to insert a name, a description, a validity interval for the rule, a flag to state if the rule is enabled or not

2. **Given** that I am defining an onboarding procedure template **when** I add a rule of type eIDAS **then** I want to flag an "auto-approved" field that states that if the rule evaluator evaluates the rule positively **then** the onboarding request can be automatically approved

3. **Given** that I am defining an onboarding procedure template **when** I add a rule of type eIDAS **then** I want to flag a "required" field that states that the rule evaluator evaluates the rule negatively, then the onboarding request must be automatically temporarily rejected and resubmitted to the applicant

4. **Given** that I am defining an onboarding procedure template, **when** I add a rule of type eIDAS, **then** I want to specify an eIDAS attribute name and either a flag indicating that I only need the attribute to be present or a specific expected value for that attribute.

_* If multiple eIDAS attributes are requested, multiple eIDAS rules must be created and then combined using COMPOSITE rules (see SIMPL-10508). See [eIDAS attrbute profile](https://ec.europa.eu/digital-building-blocks/sites/display/DIGITAL/eID+Profile?preview=/467109280/817168535/eIDAS%20SAML%20Attribute%20Profile%20v1.4.1_final.pdf)for an overview of the eIDAS attributes. See [eIDAS dashboard](https://eidas.ec.europa.eu/efda/browse/notification/eid-chapter-contacts) for an overview of the attributes supported by member states._

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10173](../README.md) (L2) Configure a ruleset for the automatic validation of onboarding request documents |
| **Children** | [SIMPL-20250](Story-SIMPL-20250/README.md) (User story) [UI] Governance Authority creates and updates eIDAS rules on onboarding procedure templates<br>[SIMPL-20251](Story-SIMPL-20251/README.md) (User story) [API] Governance Authority manages eIDAS rules on onboarding procedure templates<br>[SIMPL-20516](Story-SIMPL-20516/README.md) (User story) [UI] Governance Authority Manages eIDAS document on onboarding procedure templates<br>[SIMPL-20519](Story-SIMPL-20519/README.md) (User story) [API] Governance Authority Manages eIDAS document on onboarding procedure templates<br>[SIMPL-20637](Story-SIMPL-20637/README.md) (User story) [API] Governance Authority retrieves the list of eIDAS attributes |
