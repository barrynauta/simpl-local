> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10192](../../../README.md) / [L2-SIMPL-1612](../../README.md) / [Epic-SIMPL-28810](../README.md) / **Story-SIMPL-28146**

# SIMPL-28146: [UI][IMPROVEMENT] Governance authority manages description of identity attributes

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10192](../../../README.md) (L1) › [SIMPL-1612](../../README.md) (L2) › [SIMPL-28810](../README.md) (Epic) › **SIMPL-28146** (User story)

## Text

*This user story improves the UI related to the creation of an identity attribute. Refer to SIMPL-1052 for an overview of the initial implementation.*\
----

**As a** user with Tier 2 identity attributes manager (T2IAA_M) role in the Dataspace Governance Authority, creating a new identity attribute of my dataspace\
**I want to** specify the description of the identity attribute I'm creating\
**So that** each identity attribute can include a human-readable description, making it easier to understand the purpose and usage of every attribute

**Acceptance Criteria**

1. **Given** that I am logged in with the T2IAA_M\
   **When** I enter the description (maxLength: 255 - not mandatory) of the identity attribute on the identity attribute creation page\
   **And** I save the new identity attribute\
   **Then** the identity attribute is saved with the description specified

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-28810](../README.md) (Epic) Governance authority manages the description of identity attributes |
