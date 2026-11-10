> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10171](../../../README.md) / [L2-SIMPL-3995](../../README.md) / [Epic-SIMPL-2175](../README.md) / **Story-SIMPL-18234**

# SIMPL-18234: [UI] [IMPROVEMENT] Governance Authority fills application instructions on an onboarding procedure template

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10171](../../../README.md) (L1) › [SIMPL-3995](../../README.md) (L2) › [SIMPL-2175](../README.md) (Epic) › **SIMPL-18234** (User story)

## Text

*Note for the reviewers: Epic is SIMPL-2175. Not inserted due to lack of the backlog permissions.*

*This story is an improvement of (internal page) and covers a new field "application instructions" to be added to the fields requested upon onboarding procedure template creation and update.*

1. **Given** the new eIDAS rule that is about to be introduced (see (internal page)) there is the need to help users to understand which documents to upload or which actions to take during their onboarding. This new field, aims to help the GA to outline these instructions and make them visible to the applicants._\
   ----

**As a** Dataspace governance authority user with the _Tier 2 authorization manager (T2IAA_M)_ role, viewing details of an onboarding template\
**I want to** via UI, be able to add a descriptive, human-readable "application instructions"\
**So that** Applicants can read these instructions during the onboarding process and understand which documents to upload and which rules to follow.

**Acceptance Criteria**

2. **Given** that I am logged in with the _Tier 2 authorization manager (T2IAA_M)_ role\
   **When** I open the Onboarding Procedure Template creation page\
   **Then** I want to see a non-mandatory textarea field where I can insert multiline application instructions

3. **Given** I am creating an Onboarding Procedure Template with application instructions\
   **When** I click the button to save changes\
   **Then** the template is successfully saved with the application instructions included

4. **Given** that I am logged in with the _Tier 2 authorization manager (T2IAA_M)_ role\
   **When** I open the Onboarding Procedure Template update page\
   **Then** I want to see a non-mandatory textarea field where I can update multiline application instructions

5. **Given** I am updating an Onboarding Procedure Template with application instructions\
   **When** I click the button to save changes\
   **Then** the template is successfully saved with the application instructions included

6. **Given** that I am logged in with the _Tier 2 authorization manager (T2IAA_M)_ role\
   **When** I open the Onboarding Procedure Template update page\
   **And** navigate to the documents or rules tab\
   **Then** I want to see a warning that suggests "Make sure the application instructions info accurately explains the required documents and rules defined in this section."

7. **Given** that I am logged in with the _Tier 2 authorization manager (T2IAA_M)_ role\
   **When** I open the Onboarding Procedure Template detail page\
   **And** the application instruction field has been filled\
   **Then** I want to see the application instructions for that onboarding template

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-2175](../README.md) (Epic) Onboarding Process Governance Framework - resources/documents |
