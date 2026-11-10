> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP02A](../../../../README.md) / [L0-SIMPL-18783](../../../README.md) / [L1-SIMPL-10171](../../README.md) / [L2-SIMPL-10174](../README.md) / **Epic-SIMPL-10175**

# SIMPL-10175: Dataspace Governance authority links a subset of identity attributes to an onboarding procedure template

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP02A](../../../../README.md) › [SIMPL-18783](../../../README.md) (L0) › [SIMPL-10171](../../README.md) (L1) › [SIMPL-10174](../README.md) (L2) › **SIMPL-10175** (Epic)

## Text

*Context: currently the mapping between a participant type and the identity attributes is implemented in the Security Attributes Provider (SAP) component (SIMPL-2611). This mapping is then used in the onboarding to preselect some identity attributes based on applicant participant type. This epic describes that the mapping feature has to be moved into the onboarding component.*

*Motivation:*

1. *User Experience on Identity Attributes Definition*
   1. *The mapping page is separate from the onboarding procedure template definition, leading to a fragmented user experience.*
   1. *Setting up an onboarding procedure requires navigating between two distinct sections of the IAA frontend (onboarding and SAP), making the process less intuitive.*
1. *Domain Model Misalignment*
   1. *The current implementation mixes Onboarding Domain concepts (Onboarding Procedure Templates, Participant Types) with Identity Domain concepts (Identity Attributes, Participant).*
   1. *The participant type is merely a logical grouping of identity attributes for onboarding purposes. Once onboarding is complete, the participant is identified solely by their identity attributes, making the participant type unnecessary in the security model.*

*For more details and modeling information refer to: [an internal page]*\
----\
**As a**\
dataspace governance authority managing the onboarding procedure templates, which are linked to participant types,

**I want**

for each onboarding procedure template, to select identity attributes relevant to the type of participant

**So that**\
when a participant applies by selecting a participant type, the corresponding identity attributes are suggested to the onboarding request approvers.

**Acceptance Criteria**

1. **GIVEN that I am defining an onboarding procedure template for a participant type WHEN I open the Identity Attributes section THEN I should see the list of all the identity attributes of the data space**
1. **GIVEN that I am defining an onboarding procedure template WHEN I open the Identity Attributes section THEN I should be able to select one or more identity attributes and mark them as default for all the onboarding requests referencing the template**

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10174](../README.md) (L2) Define identity attributes for an Onboarding Procedure Template |
| **Children** | [SIMPL-7928](Story-SIMPL-7928/README.md) (User story) [API] Governance Authority selects Security Identity Attributes for an Onboarding Procedure Template<br>[SIMPL-8227](Story-SIMPL-8227/README.md) (User story) [UI] Governance Authority selects Security Identity Attributes for an Onboarding Procedure Template<br>[SIMPL-18671](Story-SIMPL-18671/README.md) (User story) [UI] [IMPROVEMENT] Participant detail page - Assign attributes |
