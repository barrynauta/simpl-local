> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP13](../../../../../README.md) / [L0-SIMPL-10783](../../../../README.md) / [L1-SIMPL-13574](../../../README.md) / [L2-SIMPL-12044](../../README.md) / [Epic-SIMPL-12046](../README.md) / **Story-SIMPL-27802**

# SIMPL-27802: Configure landing page tiles via UI

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP13](../../../../../README.md) › [SIMPL-10783](../../../../README.md) (L0) › [SIMPL-13574](../../../README.md) (L1) › [SIMPL-12044](../../README.md) (L2) › [SIMPL-12046](../README.md) (Epic) › **SIMPL-27802** (User story)

## Text

**As** a Participant,\
**I** **want** to configure my landing page,\
**So** **that** I can personalise the displayed functionalities.

###### **Acceptance Criteria**

**AC1 - Hide a landing page tile**

- **Given** that I am authenticated as a Participant
- **And** I access my landing page configuration
- **When** I choose to hide a tile
- **Then** the selected tile is hidden from the landing page

**AC2 - Show a landing page tile**

- **Given** that I am authenticated as a Participant
- **And** I access my landing page configuration
- **When** I choose to show a tile
- **Then** the selected tile is displayed on the landing page

**AC3 - Reorder landing page tiles**

- **Given** that I am authenticated as a Participant
- **And** I access my landing page configuration
- **When** I reorder the tiles using drag and drop
- **Then** the new tile order is displayed on the landing page

**AC4 - Restore default landing page configuration**

- **Given** that I modified my landing page configuration
- **When** I click on the reset or restore default configuration action
- **Then** the default landing page configuration is restored
- **And** all tiles are displayed in the default order.

**AC5 - Persist landing page configuration locally**

- **Given** that I customized my landing page configuration
- **When** I leave the Simpl-Open UI
- **Then** my landing page preferences are stored locally.

**AC6 - Restore saved landing page configuration**

- **Given** that my landing page preferences were previously stored locally
- **When** I reopen the Simpl-Open UI
- **Then** my saved landing page configuration is restored.

**Technical Info:**

- **Configuration persistence:** Local storage in browser
- **UI interaction:** Drag and drop support for tile reordering
- **Landing page:** Rendering updates dynamically after configuration changes

**Security & Compliance:**

- Authentication: Participant must be authenticated before accessing landing page configuration
- **Data scope:** Configuration is stored locally per user/browser session
- **Data privacy:** No sensitive information is persisted in local storage

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-12046](../README.md) (Epic) A Participant uses the Simpl-Open landing page adapted to their Participant type |
