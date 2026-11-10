> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP13](../../../../../README.md) / [L0-SIMPL-10783](../../../../README.md) / [L1-SIMPL-1275](../../../README.md) / [L2-SIMPL-1613](../../README.md) / [Epic-SIMPL-467](../README.md) / **Story-SIMPL-20512**

# SIMPL-20512: [UI] Participant triggers manual synchronisation of identity attributes

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP13](../../../../../README.md) › [SIMPL-10783](../../../../README.md) (L0) › [SIMPL-1275](../../../README.md) (L1) › [SIMPL-1613](../../README.md) (L2) › [SIMPL-467](../README.md) (Epic) › **SIMPL-20512** (User story)

## Text

**As a** Participant user of the Tier 1 user and role manager role (T1UAR_M)\
**I want to** via UI, trigger the update of the local copy of identity attributes\
**So that** I can manage my participant's agent with up-to-date identity attributes provided by the Governance Authority.

**Acceptance Criteria:**

1. **Given** I am an authenticated user with T1UAR_M role\
   **When** I navigate to the identity attributes info page,\
   **Then** a button with the "Synchronise Identity Attribute" label is displayed

2. **Given** I am an authenticated user with T1UAR_M role, navigating to the identity attributes info page\
   **When** I click the "Synchronise Identity Attribute" button\
   **Then** the page calls POST /tier1/v2/identityAttributes/synchronize\
   **And** an information message is displayed to confirm that the local copy of identity attributes has been updated

3. **Given** I am an authenticated user with T1UAR_M role, updating the local copy of identity attributes,\
   **When** the backend operation fails\
   **Then** an error message is displayed\
   **And** the local copy is not updated until the action is retried successfully.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-467](../README.md) (Epic) Synchronisation process for local identity attribute list update |
