> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10171](../../../README.md) / [L2-SIMPL-3995](../../README.md) / [Epic-SIMPL-2175](../README.md) / **Story-SIMPL-8410**

# SIMPL-8410: [API] Add the "name" attribute to document templates

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10171](../../../README.md) (L1) › [SIMPL-3995](../../README.md) (L2) › [SIMPL-2175](../README.md) (Epic) › **SIMPL-8410** (User story)

## Text

**Description**\
_When creating a document validation rule (see (internal page)) the user must select the document associated with the rule (see the foreign key between the validation_rule and the document_template table in the Data Model) (internal page)._

_Currently, the only readable attribute of a document template is the "Description" (see the document_template table in the Physical Data Model (internal page) of Onboarding Microservice), which may be too long to be rendered in a dropdown menu for the document selection during the creation of an onboarding rule._

*To help the user read a short meaningful name in the dropdown menu of a rule creation section, add the required "name" attribute to the Document Template entity so that the document template name is shown to the user instead of the document template description.*

*(image not shown)*

**As a** Dataspace governance authority user with the _Tier 2 authorization manager (T2IAA_M)_ role, viewing details of an onboarding template for a given participant type.\
**I want to** via API, be able to add a document name to a document template\
**So that** a short descriptive name can be read and used whenever a pointer to a document must be shown

**Acceptance Criteria**

1. **Given** that I am logged in as Dataspace governance authority user with the _Tier 2 authorization manager (T2IAA_M)_ I want to create an onboarding procedure template via PUT /onboarding-template/{participantType} and be able to define a name for each document template and the name is stored in the document template data along the description

2. **Given** that I am logged in as Dataspace governance authority user with the _Tier 2 authorization manager (T2IAA_M)_ I want to update an onboarding procedure template via PUT /onboarding-template/{participantType} and be able to update a name for each document template and the name is stored in the document template data along the description

   - \Given that I am logged in as Dataspace governance authority user with the _Tier 2 authorization manager (T2IAA_M)_ I want to GET /onboarding-template/{participantType} and be able to read a name for each document template attached to the onboarding procedure template

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-2175](../README.md) (Epic) Onboarding Process Governance Framework - resources/documents |
