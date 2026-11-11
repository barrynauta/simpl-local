> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10171](../../../README.md) / [L2-SIMPL-3995](../../README.md) / [Epic-SIMPL-2175](../README.md) / **Story-SIMPL-20532**

# SIMPL-20532: [API][IMPROVEMENT] Governance Authority Manages different document template types on onboarding procedure templates

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10171](../../../README.md) (L1) › [SIMPL-3995](../../README.md) (L2) › [SIMPL-2175](../README.md) (Epic) › **SIMPL-20532** (User story)

## Text

*Epic for this US is SIMPL-2175. Reviewers to update it due to lack of permissions by the Author.*

*This User Story improves SIMPL-5114 by introducing the concept of* document *type* *into the API of the onboarding procedure templates.  Given the introduction of the eIDAS document in* *SIMPL-20516, there is a need to disambiguate between the current available documents that can be uploaded by users (document type UPLOAD), and the eIDAS documents that are fetched by the onboarding application (EIDAS) without requiring the user to upload them.*

*This user story contains only the acceptance criteria related to the new document type field.*\
----

**As a** Dataspace governance authority user with the _Tier 2 authorization manager (T2IAA_M)_ role\
**I want** via an API, specify a type when creating a document template to an onboarding procedure template\
**So that** I can eventually handle multiple document types within an onboarding procedure template, depending on which features are enabled in the deployment configuration of the onboarding component ^(1)^

**Acceptance Criteria**

1. **Given** that I am authenticated with the T2IAA_M role\
   **When** I call *POST /onboardingProcedureTemplates*\
   **And** for each documentTemplate in the *documentTemplates field* I specify a document type ^(1)^\
   **Then** I receive a 201 Created response, and the document template is created with the appropriate type

2. **Given** that I am authenticated with the T2IAA_M role\
   **When** I call *POST /onboardingProcedureTemplates*\
   **And** a documentTemplate in the *documentTemplates* list has the *type* set to null\
   **Then** I receive a 201 Created, and the document is created with the UPLOAD type (default for retrocompatibility)

3. **Given** that I am authenticated with the T2IAA_M role\
   **When** I call *POST /onboardingProcedureTemplates*\
   **And** a documentTemplate in the *documentTemplates* list has the *type* set to an invalid value ^(1)^\
   **Then** I receive a 400 Bad Request with a message stating that the document template type is invalid

4. **Given** that I am authenticated with the T2IAA_M role\
   **When** I call *PUT* */onboardingProcedureTemplates/{onboardingProcedureTemplateId}*\
   **And** new documentTemplates in the *documentTemplates* field are added with document type ^(1)^\
   **Then** I receive a 200 OK response and the document template is added to the onboarding template{}

5. **Given** that I am authenticated with the T2IAA_M role\
   **When** I call *PUT /onboardingProcedureTemplates/{onboardingProcedureTemplateId}*\
   **And** for existing documentTemplates in the *documentTemplates* field I change the document type\
   **Then** I receive a 400 Bad Request with a message stating that document template type cannot be updated

6. **Given** that I am authenticated with the T2IAA_M role\
   **When** I call *PUT /onboardingProcedureTemplates/{onboardingProcedureTemplateId}*\
   **And** a documentTemplate in the *documentTemplates* list has the *type* set to null\
   **Then** I receive a 400 Bad Request with a message stating that the document template type is missing

7. **Given** that I am authenticated with the T2IAA_M role\
   **When** I call *PUT /onboardingProcedureTemplates/{onboardingProcedureTemplateId}*\
   **And** a documentTemplate in the *documentTemplates* list has the *type* set to an invalid value\
   **Then** I receive a 400 Bad Request with a message stating that the document template type is invalid

*^(1)^ At the moment, only UPLOAD (always enabled) and EIDAS (enabled only if the feature is enabled at deployment time) are allowed as document types. Please refer to the Onboarding OpenAPI definition for the list of valid document template type values. More document types may be added in the future.*

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-2175](../README.md) (Epic) Onboarding Process Governance Framework - resources/documents |
