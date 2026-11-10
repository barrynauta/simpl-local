> **Simpl-Open requirements, proof of concept.** One business process and the items under it. New here? [Start here](../../README.md) · [What the levels mean](../../levels.md) · [All items](../../all-items.md) · [By business process](../../processes.md)

Folder: [items](../README.md) / **BP02A**

# BP02A - Configure ID/Trust security solution

In BP02A the Governance Authority prepares its data space for identification and trust. It does this before the first participants join, and it can change the settings later.

First, the Governance Authority decides which identity attributes participants can hold, next to the attributes that Simpl-Open has built in. Simpl-Open uses these attributes for attribute-based access control between agents. Then it sets up an onboarding procedure template for each participant type: which documents a candidate must provide, how long they stay valid, and which validation rules help to review an onboarding request. Last, it configures the security settings of the data space, for example the encryption methods and how long a token stays valid.

When BP02A is done, the data space is ready to onboard participants, and participants can communicate securely.

Read more on the [public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) and the [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution).

## Who takes part

- Governance Authority

## Before it starts

- The Governance Authority has installed the Simpl-Open agent with the default identity attributes (BP02).
- The representative of the Governance Authority is logged in and has the right role (BP03B).

## Steps

The steps as the public process page names them. The items in this folder have no field for the step, so the tree below is ordered by item, not by step.

- **BP02A.01 Configure data space identity attributes**: add custom identity attributes next to the built-in ones.
- **BP02A.02 Configure onboarding procedure templates**: for each participant type, the documents, how long they stay valid, and the validation rules.
- **BP02A.03 Configure the data space security settings**: for example encryption methods and token expiry.

## What this folder holds

The L0 item [SIMPL-18783](L0-SIMPL-18783/README.md) stands for the business process. Under it are 4 high-level requirements (L1). Each has its detailed requirements (L2), the Epics that deliver them and the user stories that build them. In total **120 items:** 1 L0, 4 L1, 8 L2, 35 Epic, 72 User story.

| L1 | High-level requirement | Status | Items under it |
| --- | --- | --- | --- |
| [SIMPL-10171](L0-SIMPL-18783/L1-SIMPL-10171/README.md) | Configure Onboarding Procedure Templates | In Progress | 55 |
| [SIMPL-10176](L0-SIMPL-18783/L1-SIMPL-10176/README.md) | Configure Communication Security Settings *(not published)* | In Preparation | 3 |
| [SIMPL-10192](L0-SIMPL-18783/L1-SIMPL-10192/README.md) | Configure Dataspace Identity Attributes | Defined | 54 |
| [SIMPL-31409](L0-SIMPL-18783/L1-SIMPL-31409/README.md) | Governance Authority configures onboarding terms and conditions *(not published)* | In Progress | 3 |

## Tree

Every item, with its level, title and status.

- [SIMPL-18783](L0-SIMPL-18783/README.md) L0 · BP02A - Configure ID/Trust security solution · In Progress
  - [SIMPL-10171](L0-SIMPL-18783/L1-SIMPL-10171/README.md) L1 · Configure Onboarding Procedure Templates · In Progress
    - [SIMPL-3995](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-3995/README.md) L2 · Define the onboarding process documentation · In Progress
      - [SIMPL-2175](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-3995/Epic-SIMPL-2175/README.md) Epic · Onboarding Process Governance Framework - resources/documents · In Progress
        - [SIMPL-246](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-3995/Epic-SIMPL-2175/Story-SIMPL-246/README.md) User story · [UI] Onboarding administration dashboard - access granted · Ready for DG Acceptance
        - [SIMPL-255](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-3995/Epic-SIMPL-2175/Story-SIMPL-255/README.md) User story · [UI] Onboarding administration dashboard - access denied · Ready for DG Acceptance
        - [SIMPL-4746](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-3995/Epic-SIMPL-2175/Story-SIMPL-4746/README.md) User story · [UI] Dataspace Governance Authority onboarding procedure templates summary page · Ready for DG Acceptance
        - [SIMPL-4747](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-3995/Epic-SIMPL-2175/Story-SIMPL-4747/README.md) User story · [UI] Dataspace Governance Authority creates an onboarding procedure template for a given partipant type · Ready for DG Acceptance
        - [SIMPL-5114](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-3995/Epic-SIMPL-2175/Story-SIMPL-5114/README.md) User story · [API] Management API for Onboarding procedure template · Ready for DG Acceptance
        - [SIMPL-5115](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-3995/Epic-SIMPL-2175/Story-SIMPL-5115/README.md) User story · [API] Management API for mime types · Ready for DG Acceptance
        - [SIMPL-8410](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-3995/Epic-SIMPL-2175/Story-SIMPL-8410/README.md) User story · [API] Add the "name" attribute to document templates · Ready for DG Acceptance
        - [SIMPL-8887](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-3995/Epic-SIMPL-2175/Story-SIMPL-8887/README.md) User story · [UI] Add the "name" attribute to document templates · Ready for DG Acceptance
        - [SIMPL-18234](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-3995/Epic-SIMPL-2175/Story-SIMPL-18234/README.md) User story · [UI] [IMPROVEMENT] Governance Authority fills application instructions on an onboarding procedure template · Ready for DG Acceptance
        - [SIMPL-18235](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-3995/Epic-SIMPL-2175/Story-SIMPL-18235/README.md) User story · [API] [IMPROVEMENT] Governance Authority fills application instructions on an onboarding procedure template · Ready for DG Acceptance
        - [SIMPL-20517](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-3995/Epic-SIMPL-2175/Story-SIMPL-20517/README.md) User story · [UI][IMPROVEMENT] Governance Authority Manages document template types on onboarding procedure templates · Ready for DG Acceptance
        - [SIMPL-20532](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-3995/Epic-SIMPL-2175/Story-SIMPL-20532/README.md) User story · [API][IMPROVEMENT] Governance Authority Manages different document template types on onboarding procedure templates · Ready for DG Acceptance
        - [SIMPL-22405](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-3995/Epic-SIMPL-2175/Story-SIMPL-22405/README.md) User story · [API][IMPROVEMENT] Dataspace Governance Authority updates onboarding expirationTimeframe and credentialId · DevTeam PBI is ready for Implementation check
      - [SIMPL-2176](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-3995/Epic-SIMPL-2176/README.md) Epic · Onboarding Process Governance Framework - mandatory resources/documents - UI · Cancelled · *🔴 cancelled*
      - [SIMPL-2177](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-3995/Epic-SIMPL-2177/README.md) Epic · Onboarding Process Governance Framework - mandatory resources/documents for automatic onboarding - UI · Cancelled · *🔴 cancelled*
      - [SIMPL-2180](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-3995/Epic-SIMPL-2180/README.md) Epic · API for onboarding Process Governance Framework - resources/documents · Cancelled · *🔴 cancelled*
      - [SIMPL-2181](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-3995/Epic-SIMPL-2181/README.md) Epic · API for onboarding Process Governance Framework - mandatory resources/documents · Cancelled · *🔴 cancelled*
        - [SIMPL-5114](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-3995/Epic-SIMPL-2175/Story-SIMPL-5114/README.md) User story · [API] Management API for Onboarding procedure template · *shown under SIMPL-2175*
        - [SIMPL-5115](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-3995/Epic-SIMPL-2175/Story-SIMPL-5115/README.md) User story · [API] Management API for mime types · *shown under SIMPL-2175*
      - [SIMPL-2182](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-3995/Epic-SIMPL-2182/README.md) Epic · API for onboarding Process Governance Framework - mandatory resources/documents for automatic onboarding · Cancelled · *🔴 cancelled*
    - [SIMPL-10173](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10173/README.md) L2 · Configure a ruleset for the automatic validation of onboarding request documents · Defined
      - [SIMPL-8337](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10173/Epic-SIMPL-8337/README.md) Epic · Governance authority creates content check rules for automated document validation · Defined
        - [SIMPL-6364](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10173/Epic-SIMPL-8337/Story-SIMPL-6364/README.md) User story · [API] Governance Authority manages content check validation rules on onboarding procedure template · Ready for DG Acceptance
        - [SIMPL-7923](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10173/Epic-SIMPL-8337/Story-SIMPL-7923/README.md) User story · [UI] Governance Authority manages content check validation rules on onboarding procedure template · Ready for DG Acceptance
        - [SIMPL-18944](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10173/Epic-SIMPL-8337/Story-SIMPL-18944/README.md) User story · [API][IMPROVEMENT] Allow users to create and update invalid content check onboarding validation rules when disabled · PBI Cancelled · *🔴 cancelled*
        - [SIMPL-18945](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10173/Epic-SIMPL-8337/Story-SIMPL-18945/README.md) User story · [UI][IMPROVEMENT] Allow users to create and update invalid content check onboarding validation rules when disabled · PBI Cancelled · *🔴 cancelled*
      - [SIMPL-10507](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10173/Epic-SIMPL-10507/README.md) Epic · Governance authority creates presence check rules to check if a document is uploaded · Defined
        - [SIMPL-10510](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10173/Epic-SIMPL-10507/Story-SIMPL-10510/README.md) User story · [API] Governance Authority manages presence check validation rules on onboarding procedure template · Ready for DG Acceptance
        - [SIMPL-10511](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10173/Epic-SIMPL-10507/Story-SIMPL-10511/README.md) User story · [UI] Governance Authority manages presence check validation rules on onboarding procedure template · Ready for DG Acceptance
        - [SIMPL-18946](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10173/Epic-SIMPL-10507/Story-SIMPL-18946/README.md) User story · [API][IMPROVEMENT] Allow users to create and update invalid presence check onboarding validation rules when disabled · PBI Cancelled · *🔴 cancelled*
        - [SIMPL-18947](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10173/Epic-SIMPL-10507/Story-SIMPL-18947/README.md) User story · [UI][IMPROVEMENT] Allow users to create and update invalid presence check onboarding validation rules when disabled · PBI Cancelled · *🔴 cancelled*
      - [SIMPL-10508](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10173/Epic-SIMPL-10508/README.md) Epic · Governance Authority creates composite rules with custom logical conditions for document validation · Defined
        - [SIMPL-8499](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10173/Epic-SIMPL-10508/Story-SIMPL-8499/README.md) User story · [API] Governance Authority manages composite validation rules on onboarding procedure template · Ready for DG Acceptance
        - [SIMPL-8500](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10173/Epic-SIMPL-10508/Story-SIMPL-8500/README.md) User story · [UI] Governance Authority manages composite validation rules on onboarding procedure template · Ready for DG Acceptance
        - [SIMPL-18913](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10173/Epic-SIMPL-10508/Story-SIMPL-18913/README.md) User story · [API][IMPROVEMENT] Allow users to create and update invalid composite onboarding validation rules when disabled · PBI Cancelled · *🔴 cancelled*
        - [SIMPL-18914](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10173/Epic-SIMPL-10508/Story-SIMPL-18914/README.md) User story · [UI][IMPROVEMENT] Allow users to create and update invalid composite onboarding validation rules when disabled · PBI Cancelled · *🔴 cancelled*
      - [SIMPL-12326](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10173/Epic-SIMPL-12326/README.md) Epic · Governance authority deletes a validation rule · Defined
        - [SIMPL-11766](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10173/Epic-SIMPL-12326/Story-SIMPL-11766/README.md) User story · [UI] Governance Authority deletes a validation rule · Ready for DG Acceptance
        - [SIMPL-11767](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10173/Epic-SIMPL-12326/Story-SIMPL-11767/README.md) User story · [API] Governance Authority deletes a validation rule · Ready for DG Acceptance
      - [SIMPL-19153](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10173/Epic-SIMPL-19153/README.md) Epic · Governance authority creates eIDAS rules for automated onboarding request validation · Defined
        - [SIMPL-20250](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10173/Epic-SIMPL-19153/Story-SIMPL-20250/README.md) User story · [UI] Governance Authority creates and updates eIDAS rules on onboarding procedure templates · Ready for DG Acceptance
        - [SIMPL-20251](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10173/Epic-SIMPL-19153/Story-SIMPL-20251/README.md) User story · [API] Governance Authority manages eIDAS rules on onboarding procedure templates · Ready for DG Acceptance
        - [SIMPL-20516](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10173/Epic-SIMPL-19153/Story-SIMPL-20516/README.md) User story · [UI] Governance Authority Manages eIDAS document on onboarding procedure templates · Ready for DG Acceptance
        - [SIMPL-20519](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10173/Epic-SIMPL-19153/Story-SIMPL-20519/README.md) User story · [API] Governance Authority Manages eIDAS document on onboarding procedure templates · Ready for DG Acceptance
        - [SIMPL-20637](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10173/Epic-SIMPL-19153/Story-SIMPL-20637/README.md) User story · [API] Governance Authority retrieves the list of eIDAS attributes · Ready for DG Acceptance
      - [SIMPL-24264](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10173/Epic-SIMPL-24264/README.md) Epic · Governance authority deletes validation rules used in existing onboarding requests · Defined
        - [SIMPL-18911](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10173/Epic-SIMPL-24264/Story-SIMPL-18911/README.md) User story · [UI][IMPROVEMENT] Prevent the deletion of a document template linked to an onboarding validation rule · Ready for DG Acceptance
        - [SIMPL-24176](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10173/Epic-SIMPL-24264/Story-SIMPL-24176/README.md) User story · [API][IMPROVEMENT] Prevent the deletion of a document template linked to an onboarding validation rule · Ready for DG Acceptance
        - [SIMPL-24262](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10173/Epic-SIMPL-24264/Story-SIMPL-24262/README.md) User story · [UI][IMPROVEMENT] Allow the deletion of a validation rule even if used in an existing onboarding request · Ready for DG Acceptance
        - [SIMPL-24263](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10173/Epic-SIMPL-24264/Story-SIMPL-24263/README.md) User story · [API][IMPROVEMENT] Allow the deletion of a validation rule even if used in an existing onboarding request · Ready for DG Acceptance
    - [SIMPL-10174](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10174/README.md) L2 · Define identity attributes for an Onboarding Procedure Template · Defined
      - [SIMPL-10175](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10174/Epic-SIMPL-10175/README.md) Epic · Dataspace Governance authority links a subset of identity attributes to an onboarding procedure template · Defined
        - [SIMPL-7928](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10174/Epic-SIMPL-10175/Story-SIMPL-7928/README.md) User story · [API] Governance Authority selects Security Identity Attributes for an Onboarding Procedure Template · Ready for DG Acceptance
        - [SIMPL-8227](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10174/Epic-SIMPL-10175/Story-SIMPL-8227/README.md) User story · [UI] Governance Authority selects Security Identity Attributes for an Onboarding Procedure Template · Ready for DG Acceptance
        - [SIMPL-18671](L0-SIMPL-18783/L1-SIMPL-10171/L2-SIMPL-10174/Epic-SIMPL-10175/Story-SIMPL-18671/README.md) User story · [UI] [IMPROVEMENT] Participant detail page - Assign attributes · Ready for DG Acceptance
  - [SIMPL-10176](L0-SIMPL-18783/L1-SIMPL-10176/README.md) L1 · Configure Communication Security Settings · In Preparation · *not published*
    - [SIMPL-10177](L0-SIMPL-18783/L1-SIMPL-10176/L2-SIMPL-10177/README.md) L2 · Configure Ephemeral Proof creation and renewal settings · New · *not published*
    - [SIMPL-10178](L0-SIMPL-18783/L1-SIMPL-10176/L2-SIMPL-10178/README.md) L2 · Configure tier 2 communication mode · New · *not published*
      - [SIMPL-8496](L0-SIMPL-18783/L1-SIMPL-10176/L2-SIMPL-10178/Epic-SIMPL-8496/README.md) Epic · Governance Authority users use a dedicated interface to oversee and manage agent configurations · Cancelled · *not published, 🔴 cancelled*
  - [SIMPL-10192](L0-SIMPL-18783/L1-SIMPL-10192/README.md) L1 · Configure Dataspace Identity Attributes · Defined
    - [SIMPL-1610](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1610/README.md) L2 · Defining preconfigured attributes · Defined
      - [SIMPL-10194](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1610/Epic-SIMPL-10194/README.md) Epic · Setup default identity attributes in the Governance Authority Agent · Defined
        - [SIMPL-13919](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1610/Epic-SIMPL-10194/Story-SIMPL-13919/README.md) User story · Governance Authority intialization with built-in identity attributes · DevTeam PBI is ready for Implementation check
        - [SIMPL-13920](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1610/Epic-SIMPL-10194/Story-SIMPL-13920/README.md) User story · [UI] Prevent Governance Authority from updating or creating built-in identity attributes · Ready for DG Acceptance
        - [SIMPL-13998](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1610/Epic-SIMPL-10194/Story-SIMPL-13998/README.md) User story · [API] Prevent Governance Authority from updating or creating built-in identity attributes · Ready for DG Acceptance
    - [SIMPL-1612](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/README.md) L2 · Tier 2 identity attributes configuration · Defined
      - [SIMPL-465](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-465/README.md) Epic · UI for listing identity attributes · Cancelled · *🔴 cancelled*
      - [SIMPL-854](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-854/README.md) Epic · API for listing identity attributes · Cancelled · *🔴 cancelled*
      - [SIMPL-865](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-865/README.md) Epic · API for creating identity attributes · Cancelled · *🔴 cancelled*
      - [SIMPL-866](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-866/README.md) Epic · API for modify identitiy attributes · Cancelled · *🔴 cancelled*
      - [SIMPL-867](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-867/README.md) Epic · API for disabling identity attributes · Cancelled · *🔴 cancelled*
      - [SIMPL-868](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-868/README.md) Epic · API for enabling identity attributes · Cancelled · *🔴 cancelled*
      - [SIMPL-873](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-873/README.md) Epic · API for removing identity attributes · Cancelled · *🔴 cancelled*
      - [SIMPL-876](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-876/README.md) Epic · API for making identity attributes assignable on Tier1 roles · Cancelled · *🔴 cancelled*
      - [SIMPL-977](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-977/README.md) Epic · UI for making identity attributes assignable on Tier1 roles · Cancelled · *🔴 cancelled*
      - [SIMPL-1013](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-1013/README.md) Epic · UI for creating identity attributes · Cancelled · *🔴 cancelled*
      - [SIMPL-1014](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-1014/README.md) Epic · UI for modify identitiy attributes · Cancelled · *🔴 cancelled*
      - [SIMPL-1015](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-1015/README.md) Epic · UI for disabling identity attributes · Cancelled · *🔴 cancelled*
      - [SIMPL-1016](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-1016/README.md) Epic · UI for enabling identity attributes · Cancelled · *🔴 cancelled*
      - [SIMPL-1017](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-1017/README.md) Epic · UI for removing identity attributes · Cancelled · *🔴 cancelled*
      - [SIMPL-10199](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-10199/README.md) Epic · Managing identity attributes · In Preparation
        - [SIMPL-482](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-10199/Story-SIMPL-482/README.md) User story · Expose Identity Attributes via API · Ready for DG Acceptance
        - [SIMPL-483](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-10199/Story-SIMPL-483/README.md) User story · Retrieve Identity Attributes via Dataspace Governace Authority API · Ready for DG Acceptance
        - [SIMPL-485](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-10199/Story-SIMPL-485/README.md) User story · Retrieve Paginated List of Identity Attributes via API · Ready for DG Acceptance
        - [SIMPL-720](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-10199/Story-SIMPL-720/README.md) User story · API to store the dataspace identity attributes · DevTeam PBI is ready for Implementation check
        - [SIMPL-773](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-10199/Story-SIMPL-773/README.md) User story · Management API for Identity Attributes · Ready for DG Acceptance
        - [SIMPL-869](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-10199/Story-SIMPL-869/README.md) User story · Disable Identity Attribute via API · Ready for DG Acceptance
        - [SIMPL-870](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-10199/Story-SIMPL-870/README.md) User story · Enable Identity Attribute via API · Ready for DG Acceptance
        - [SIMPL-875](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-10199/Story-SIMPL-875/README.md) User story · Attempt to Delete Assigned Identity Attribute via API · Ready for DG Acceptance
        - [SIMPL-908](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-10199/Story-SIMPL-908/README.md) User story · [UI] Identity Attributes Management page · Ready for DG Acceptance
        - [SIMPL-909](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-10199/Story-SIMPL-909/README.md) User story · [UI] Identity Attribute detail page · Ready for DG Acceptance
        - [SIMPL-915](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-10199/Story-SIMPL-915/README.md) User story · [UI] Identity Attributes Management page - filtering · Ready for DG Acceptance
        - [SIMPL-918](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-10199/Story-SIMPL-918/README.md) User story · [UI] Identity Attributes Management page - ordering · Ready for DG Acceptance
        - [SIMPL-1051](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-10199/Story-SIMPL-1051/README.md) User story · [UI] Identity Attributes Management page - creating Identity Attribute · Ready for DG Acceptance
        - [SIMPL-1052](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-10199/Story-SIMPL-1052/README.md) User story · [UI] Identity Attribute creation page · Ready for DG Acceptance
        - [SIMPL-1053](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-10199/Story-SIMPL-1053/README.md) User story · [UI] Identity Attribute edit page · Ready for DG Acceptance
        - [SIMPL-1054](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-10199/Story-SIMPL-1054/README.md) User story · [UI] Editing Identity Attributes · Ready for DG Acceptance
        - [SIMPL-1055](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-10199/Story-SIMPL-1055/README.md) User story · [UI] Removing an Identity Attribute · Ready for DG Acceptance
        - [SIMPL-17306](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-10199/Story-SIMPL-17306/README.md) User story · [API] Dataspace Governance Authority retrieves the list of dataspace identity attributes · Ready for DG Acceptance
      - [SIMPL-10200](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-10200/README.md) Epic · Making identity attributes assignable on Tier1 roles · For DG Connect Review
        - [SIMPL-800](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-10200/Story-SIMPL-800/README.md) User story · Management API for Identity Attributes and Participant Types · Ready for DG Acceptance
        - [SIMPL-881](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-10200/Story-SIMPL-881/README.md) User story · Define Assignable Identity Attributes for Tier 1 User Roles via API · Ready for DG Acceptance
        - [SIMPL-1010](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-10200/Story-SIMPL-1010/README.md) User story · [UI] Identity Attribute assignability flag editing · Ready for DG Acceptance
      - [SIMPL-24949](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-24949/README.md) Epic · Governance Authority marks identity attributes as public · Defined
        - [SIMPL-25007](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-24949/Story-SIMPL-25007/README.md) User story · [API] Governance authority manages public identity attributes · Ready for DG Acceptance
        - [SIMPL-25008](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-24949/Story-SIMPL-25008/README.md) User story · [UI] Governance authority manages public identity attributes · Ready for DG Acceptance
        - [SIMPL-25086](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-24949/Story-SIMPL-25086/README.md) User story · Governance Authority to have search and consumption identity attributes marked as public · Ready for DG Acceptance
      - [SIMPL-24950](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-24950/README.md) Epic · Governance Authority marks identity attributes as federable · Defined
        - [SIMPL-25009](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-24950/Story-SIMPL-25009/README.md) User story · [API] Governance authority manages federable identity attributes · Ready for DG Acceptance
        - [SIMPL-25010](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-24950/Story-SIMPL-25010/README.md) User story · [UI] Governance authority manages federable identity attributes · Ready for DG Acceptance
        - [SIMPL-25087](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-24950/Story-SIMPL-25087/README.md) User story · Governance Authority to have search and consumption identity attributes marked as federable · Ready for DG Acceptance
      - [SIMPL-28810](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-28810/README.md) Epic · Governance authority manages the description of identity attributes · For DG Connect Review
        - [SIMPL-28146](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-28810/Story-SIMPL-28146/README.md) User story · [UI][IMPROVEMENT] Governance authority manages description of identity attributes · DevTeam PBI is ready for Implementation check
        - [SIMPL-28148](L0-SIMPL-18783/L1-SIMPL-10192/L2-SIMPL-1612/Epic-SIMPL-28810/Story-SIMPL-28148/README.md) User story · [UI][IMPROVEMENT] Governance authority updates an identity attributes · DevTeam PBI is ready for Implementation check
  - [SIMPL-31409](L0-SIMPL-18783/L1-SIMPL-31409/README.md) L1 · Governance Authority configures onboarding terms and conditions · In Progress · *not published*
    - [SIMPL-31410](L0-SIMPL-18783/L1-SIMPL-31409/L2-SIMPL-31410/README.md) L2 · Governance Authority configures onboarding terms and conditions · In Progress · *not published*
      - [SIMPL-31411](L0-SIMPL-18783/L1-SIMPL-31409/L2-SIMPL-31410/Epic-SIMPL-31411/README.md) Epic · Governance Authority configures terms and conditions for Applicants · In Progress · *not published*
        - [SIMPL-34233](L0-SIMPL-18783/L1-SIMPL-31409/L2-SIMPL-31410/Epic-SIMPL-31411/Story-SIMPL-34233/README.md) User story · Applicant accepts changed Terms and Conditions · DevTeam PBI is ready for Implementation check · *not published*
