> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP02A](../../../../README.md) / [L0-SIMPL-18783](../../../README.md) / [L1-SIMPL-10192](../../README.md) / [L2-SIMPL-1610](../README.md) / **Epic-SIMPL-10194**

# SIMPL-10194: Setup default identity attributes in the Governance Authority Agent

**Epic (deliverable, testable feature)**

**Trace:** [BP02A](../../../../README.md) › [SIMPL-18783](../../../README.md) (L0) › [SIMPL-10192](../../README.md) (L1) › [SIMPL-1610](../README.md) (L2) › **SIMPL-10194** (Epic)

## Text

**As a** Dataspace Governance Authority\
**I want to** Have a startup procedure that populates the list of built-in identity attributes for Simpl-Open data spaces\
**So that** I can assign these identity attributes to onboarding procedures and participants, but I cannot change these identity attributes in any way.

**Acceptance Criteria:**

1. **Given** that I am a Dataspace Governance authority\
   **When** I install my agent\
   **Then** I want the Simpl-Open built-in identity attributes to be automatically uploaded\
   **And** I want these built-in identity attributes to have the properties (a code, a name, a description and an assignable flag) and a flag indicating they are built-in

2. **Given** that I am a Dataspace Governance authority and I have installed my agent without further actions on the identity attributes\
   **When** I retrieve the list of identity attributes\
   **Then** the list contains the built-in identity attributes with their predefined properties and a flag indicating that they are built in

3. **Given** that I am a Dataspace Governance authority\
   **When** I attempt to update or delete a built-in identity attribute\
   **Then** I get an error message stating built-in identity attributes cannot be modified

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-1610](../README.md) (L2) Defining preconfigured attributes |
| **Children** | [SIMPL-13919](Story-SIMPL-13919/README.md) (User story) Governance Authority intialization with built-in identity attributes<br>[SIMPL-13920](Story-SIMPL-13920/README.md) (User story) [UI] Prevent Governance Authority from updating or creating built-in identity attributes<br>[SIMPL-13998](Story-SIMPL-13998/README.md) (User story) [API] Prevent Governance Authority from updating or creating built-in identity attributes |
