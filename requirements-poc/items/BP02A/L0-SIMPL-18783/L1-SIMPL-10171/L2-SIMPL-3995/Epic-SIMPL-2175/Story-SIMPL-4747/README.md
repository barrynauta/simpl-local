> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10171](../../../README.md) / [L2-SIMPL-3995](../../README.md) / [Epic-SIMPL-2175](../README.md) / **Story-SIMPL-4747**

# SIMPL-4747: [UI] Dataspace Governance Authority creates an onboarding procedure template for a given partipant type

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10171](../../../README.md) (L1) › [SIMPL-3995](../../README.md) (L2) › [SIMPL-2175](../README.md) (Epic) › **SIMPL-4747** (User story)

## Text

**As a** dataspace governance authority user with the _Tier 2 authorization manager (T2IAA_M)_ role, viewing the onboarding procedures summary page, selecting a specific participant type with no onboarding procedure defined\
**I want to** create an onboarding procedure template (Onboarding procedure template (internal page)), defining all the documents needed to onboard that participant type\
**So that** there is an onboarding procedure defined for that specific participant type

**Acceptance Criteria:**

1. **Given** that I am a dataspace governance authority user with the T2IAA_M role, **when** I view the onboarding procedures summary page and select a specific participant type with no onboarding procedure defined, the page should provide a creation interface, allowing me to create a new onboarding procedure for the selected participant type.

2. **Given** that I am a dataspace governance authority user with the T2IAA_M role, **when** I view the onboarding procedures summary page and select a specific participant type with no onboarding procedure defined, the creation interface should allow me to define one or more documents that are required for onboarding the selected participant type.

3. **Given** that I am a dataspace governance authority user with the T2IAA_M role, **when** I view the onboarding procedures summary page and select a specific participant type with no onboarding procedure defined, for each document, I should be able to enter a description that explains what the document must contain (e.g., "Business License", "Proof of Identity", etc.).

4. **Given** that I am a dataspace governance authority user with the T2IAA_M role, **when** I view the onboarding procedures summary page and select a specific participant type with no onboarding procedure defined, for each document, I should be able to select a MIME type as defined in RFC2046 (e.g., "application/pdf", "image/jpeg", etc.).

5. **Given** that I am a dataspace governance authority user with the T2IAA_M role, **when** I view the onboarding procedures summary page and select a specific participant type with no onboarding procedure defined, after defining the mandatory documents, the system should save the onboarding procedure successfully, associating it with the selected participant type.

6. **Given** that I am a dataspace governance authority user with the T2IAA_M role, **when** I view the onboarding procedures summary page and select a specific participant type with no onboarding procedure defined, after creating the onboarding procedure, the system should display it on the onboarding procedures summary page, under the corresponding participant type tab.

7. **Given** that I am a dataspace governance authority user with the T2IAA_M role, **when** I view the onboarding procedures summary page and select a specific participant type with no onboarding procedure defined, for each document, the summary page should display the document description and MIME type.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-2175](../README.md) (Epic) Onboarding Process Governance Framework - resources/documents |
