> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10192](../../../README.md) / [L2-SIMPL-1612](../../README.md) / [Epic-SIMPL-24949](../README.md) / **Story-SIMPL-25008**

# SIMPL-25008: [UI] Governance authority manages public identity attributes

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10192](../../../README.md) (L1) › [SIMPL-1612](../../README.md) (L2) › [SIMPL-24949](../README.md) (Epic) › **SIMPL-25008** (User story)

## Text

**As a** user with *Tier 2 identity attributes manager* (T2IAA_M) role in the Dataspace Governance Authority, viewing the list of identity attributes of my dataspace\
**I want to** view and mark each of my custom identity attributes as public or non-public\
**So that** I can control which attributes are visible to external Governance Authorities and therefore available for federation mapping.

**Acceptance Criteria**

1. **Given** that I am logged in with the T2IAA_M\
   **When** I open the page that shows the list of the identity attributes\
   **Then** I see a new column with header "Public" (next to the Built-in column)\
   **And** in each row I can see Yes or No depending on the value of the public property

2. **Given** that I am logged in with the T2IAA_M\
   **When** I open the page that shows the list of the identity attributes\
   **And** click on the button to update an identity attribute\
   **Then** I see a toggle that allows me to change the value of the "public" property

3. **Given** that I am logged in with the T2IAA_M\
   **When** I open the page that shows the list of the identity attributes\
   **And** click on the button to save an updated identity attribute\
   **Then** the identity attribute is saved\
   **And** the public property updated accordingly

4. **Given** that I am logged in with the T2IAA_M\
   **When** I mark the identity attribute as public in the identity attribute creation page\
   **And** save the new identity attribute\
   **Then** the identity attribute is saved and marked as public

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-24949](../README.md) (Epic) Governance Authority marks identity attributes as public |
