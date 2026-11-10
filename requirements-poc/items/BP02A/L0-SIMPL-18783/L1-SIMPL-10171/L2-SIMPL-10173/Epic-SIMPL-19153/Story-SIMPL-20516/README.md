> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10171](../../../README.md) / [L2-SIMPL-10173](../../README.md) / [Epic-SIMPL-19153](../README.md) / **Story-SIMPL-20516**

# SIMPL-20516: [UI] Governance Authority Manages eIDAS document on onboarding procedure templates

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10171](../../../README.md) (L1) › [SIMPL-10173](../../README.md) (L2) › [SIMPL-19153](../README.md) (Epic) › **SIMPL-20516** (User story)

## Text

**As a** Dataspace governance authority user with the _Tier 2 authorization manager (T2IAA_M)_ role, viewing details of an onboarding procedure template\
**I want** via a UI, to add a document template of type eIDAS to the list of documents of the onboarding template\
**So that** I can enable the submission and evaluation of eIDAS attributes during the onboarding of a participant

**Acceptance Criteria**

1. **Given** that I am authenticated with the T2IAA_M role\
   **And** eIDAS login is enabled in the deployment configurations of the onboarding frontend component ^(1)^\
   **When** I click on the "Edit Documents" and then the "Add Documents" button\
   **Then** I see a dropdown list that contains the item "eIDAS document"

2. **Given** that I am authenticated with the T2IAA_M role\
   **And** I click on the item to create an "eIDAS Document"\
   **Then** I see a document template creation form that contains\
   a "Document Name" input text prepopulated with the "eIDAS Document" text\
   a "Document Description" input text prepopulated with the following description "eIDAS Document. Upload your eID identity"\
   a "Mandatory" toggle default to false\
   a "Document Type" read-only field with the value of "eIDAS"

3. **Given** that I am authenticated with the T2IAA_M role\
   **And** the document template of type eIDAS has already been added to the onboarding template via the "Save Changes" button\
   **When** I click on the "Add Documents" button\
   **Then** I see that the "eIDAS Document" item is disabled\
   **And** below the "eIDAS Document" label, I see the message "Only one eIDAS document is allowed for each onboarding procedure template"

4. **Given** that eIDAS login is disabled in the deployment configurations of the onboarding frontend component ^(1)^\
   **When** viewing the onboarding procedure template page\
   **Then** the "eIDAS document" item is not visible in the "Add Documents" dropdown list

5. **Given** that I am an IT system administrator, responsible for installing the onboarding frontend component ^(1)^\
   **When** I read the [deployment documentation](https://code.europa.eu/simpl/simpl-open/development/iaa/fe-onboarding/-/blob/main/documents/deployment-guide/README.md?ref_type=heads) of the component\
   **Then** I see the property to use to enable or disable the eIDAS login feature\
   **And** I see that by default the feature is disabled

*(1) By default, this feature is disabled on the component, and should be enabled at deployment time by the IT Administrators installing the component into the agent (see last AC).*

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-19153](../README.md) (Epic) Governance authority creates eIDAS rules for automated onboarding request validation |
