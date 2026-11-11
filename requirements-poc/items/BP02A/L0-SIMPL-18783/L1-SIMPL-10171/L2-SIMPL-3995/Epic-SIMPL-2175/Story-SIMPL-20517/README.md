> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10171](../../../README.md) / [L2-SIMPL-3995](../../README.md) / [Epic-SIMPL-2175](../README.md) / **Story-SIMPL-20517**

# SIMPL-20517: [UI][IMPROVEMENT] Governance Authority Manages document template types on onboarding procedure templates

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10171](../../../README.md) (L1) › [SIMPL-3995](../../README.md) (L2) › [SIMPL-2175](../README.md) (Epic) › **SIMPL-20517** (User story)

## Text

*Epic for this US is SIMPL-2175. Reviewers to update it due to lack of permissions by the Author.*

*This User Story improves SIMPL-4747 by introducing the concept of **type** into the UI of the onboarding procedure templates.* _Given the introduction of the eIDAS document in SIMPL-20516, there is a need to disambiguate between the current available documents that can be uploaded by users (document type UPLOAD), and the eIDAS documents that are fetched by the onboarding application (EIDAS_REMOTE_FETCH) without requiring the user to upload them._

*The add document button will open a dropdown list instead of directly showing the document template creation form (see attached image).*\
----

**As a** Dataspace governance authority user with the _Tier 2 authorization manager (T2IAA_M)_ role, viewing details of an onboarding procedure template\
**I want** via a UI, to choose the document template type before adding it to the onboarding procedure template\
**So that** I can eventually handle multiple documents types within an onboarding procedure template, depending on which features are enabled in the deployment configuration of the onboarding component ^(1)^

**Acceptance Criteria**

1. **Given** that I am authenticated with the T2IAA_M role\
   **When** I click on the "Edit Documents" and then the "Add Documents" button\
   **Then** I see a dropdown list that contains at least the default item "User Upload"

_^(1)^ At the moment, only UPLOAD (always enabled) and EIDAS_REMOTE_FETCH (enabled only if the feature is enabled at deployment time) are allowed as document types. Please refer to the Onboarding OpenAPI definition for the list of valid document template type values. More document types may be added in the future._

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-2175](../README.md) (Epic) Onboarding Process Governance Framework - resources/documents |
