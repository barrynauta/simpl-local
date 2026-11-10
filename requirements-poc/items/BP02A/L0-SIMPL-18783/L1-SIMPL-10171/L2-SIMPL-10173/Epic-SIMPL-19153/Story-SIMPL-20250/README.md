> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10171](../../../README.md) / [L2-SIMPL-10173](../../README.md) / [Epic-SIMPL-19153](../README.md) / **Story-SIMPL-20250**

# SIMPL-20250: [UI] Governance Authority creates and updates eIDAS rules on onboarding procedure templates

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10171](../../../README.md) (L1) › [SIMPL-10173](../../README.md) (L2) › [SIMPL-19153](../README.md) (Epic) › **SIMPL-20250** (User story)

## Text

*The primary objective of an eIDAS rule is to verify whether a specific eIDAS attribute is present and/or contains a defined value. A combination of conditions on multiple eIDAS attributes can be achieved by the combination of multiple eIDAS rules.*

*Rules disablement and cancellation are covered in Epic SIMPL-12326.*\
----\
**As A**

Dataspace governance authority user with the _Tier 2 authorization manager (T2IAA_M)_ role, viewing details of an onboarding procedure template

**I want**

via a UI, to creates and updates ** rules of type eIDAS

**So that**

I can automate the evaluation of eIDAS attributes during the onboarding of a participant

**Acceptance Criteria**

1. **GIVEN** that I am authenticated with the T2IAA_M role\
   **AND** the eIDAS login is enabled in the deployment configurations of the onboarding frontend component  ^(1)^\
   **WHEN** I navigate to the Rules tab\
   **AND** I click on the "Edit Rules" and then the "Add Rules" button\
   **THEN** I see a dropdown list that contains the item "eIDAS rule"
1. **GIVEN** that I am authenticated with the T2IAA_M role\
   **AND** the eIDAS login is enabled in the deployment configurations of the onboarding frontend component  ^(1)^\
   **AND** the eIDAS document template hasn't been created in the documents section of the page (see SIMPL-20516)\
   **WHEN** I click on the "Add Rules" button\
   **THEN** the item "eIDAS rule" list item is not clickable\
   **AND** below the "eIDAS document" label, I see the message "Please create an eIDAS document template to enable eIDAS rules"
1. **GIVEN** that I am authenticated with the T2IAA_M role\
   **AND** the eIDAS login is enabled in the deployment configurations of the onboarding frontend component  ^(1)^\
   **AND** the eIDAS document template has been created in the documents section of the page (see SIMPL-20516)\
   **WHEN** I click on the "Add Rules" button\
   **THEN** the item "eIDAS rule" item is clickable
1. **GIVEN** that I am authenticated with the T2IAA_M role\
   **AND** the eIDAS login is enabled in the deployment configurations of the onboarding frontend component  (1)\
   **WHEN** I click the button to create a new eIDAS rule\
   **THEN** a form to create the rule is shown\
   **AND** the form contains the following fields (only underlined fields are new for the eIDAS rule)\
   a required "Rule Name" input text field to enter the rule name (max 255 chars)\
   a required "Rule Description" input text field to enter the rule description (max 500 characters)\
   a required  "Document" select field prepopulated to the eIDAS document template name\
   a required "Valid From" (required) and "Valid To" (non required) fields to indicate the date validity range of the rule\
   +a required "Applicable To" radio button with "Natural Person" and "Legal Person" options+\
   +a required "eIDAS Attribute" dropdown field that allows to chose one eIDAS identity attribute (filtered by Natural or Legal person)+\
   +a required "Condition" dropdown field that allows to chose between the "IS PRESENT" or "CONTAINS" options+\
   +a "Value" field that is enabled and required only if the "condition" field is set to "CONTAINS"+\
   a "enabled" checkbox that allows activation or deactivation of the rule\
   a "Autoapprove" checkbox that links the rule to the automatic approval of the onboarding request\
   a "Required" checkbox to indicate that the rule is required for the request to pass the automatic validation
1. **GIVEN** that I am authenticated with the T2IAA_M role\
   **WHEN** I fill all required fields and any optional fields with valid data\
   **THEN** the "Save Changes" button becomes active
1. **GIVEN** that I am authenticated with the T2IAA_M role\
   **AND** I click on the button to edit the rules\
   **AND** an eIDAS rule has already been defined\
   **WHEN**  I update all required fields and any optional fields with valid data\
   **THEN** the "Save Changes" button is enabled for the update
1. **GIVEN** that I am authenticated with the T2IAA_M role\
   **AND** the "Save Changes" button is enabled\
   **WHEN** I click the button\
   **THEN** the rule is saved
1. **GIVEN** that I am authenticated with the T2IAA_M role\
   **AND** the eIDAS login is disabled in the deployment configurations of the onboarding frontend component  ^(1)^\
   **WHEN** I navigate to the Rules tab\
   **AND** I click on the "Edit Rules" and then the "Add Rules" button\
   **THEN** I see a dropdown list that does not contain the item "eIDAS rule"
1. **GIVEN** that I am an IT system administrator, responsible for installing the onboarding frontend component ^(1)^\
   **WHEN** I read the [deployment documentation](https://code.europa.eu/simpl/simpl-open/development/iaa/fe-onboarding/-/blob/main/documents/deployment-guide/README.md?ref_type=heads) of the component\
   **THEN** I see the property to use to enable or disable the eIDAS login feature\
   **AND** I see that by default the feature is disabled

*(1) By default, this feature is disabled on the component, and should be enabled at deployment time by the IT Administrators installing the component into the agent (see last AC).*

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-19153](../README.md) (Epic) Governance authority creates eIDAS rules for automated onboarding request validation |
