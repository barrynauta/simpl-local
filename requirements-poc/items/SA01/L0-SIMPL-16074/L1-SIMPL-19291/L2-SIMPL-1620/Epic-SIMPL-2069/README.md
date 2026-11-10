> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [SA01](../../../../README.md) / [L0-SIMPL-16074](../../../README.md) / [L1-SIMPL-19291](../../README.md) / [L2-SIMPL-1620](../README.md) / **Epic-SIMPL-2069**

# SIMPL-2069: Participant visualises dataset lineage

**Epic (deliverable, testable feature)** · Status **Cancelled**

> 🔴 **Cancelled.** Kept here for history. It is not part of the current requirements.

**Trace:** [SA01](../../../../README.md) › [SIMPL-16074](../../../README.md) (L0) › [SIMPL-19291](../../README.md) (L1) › [SIMPL-1620](../README.md) (L2) › **SIMPL-2069** (Epic)

## Text

As a **Participant** (Data Provider or Data Consumer),\
**I want to** visualise dataset lineage through graphical representations,\
**so that** I can inspect how datasets flow through processing steps, understand dependencies, and analyse dataset derivation.

**Acceptance Criteria**

**Scenario 1:** Dataset lineage is visualised for a processed dataset with available lineage metadata

- **Given** lineage metadata exists for a processed dataset,\
   **and** the Data Consumer has read access to the dataset,
- **When** the Participant requests to view dataset lineage via the Simpl-Open UI,
- **Then** Simpl-Open must present a visual representation of dataset sources, transformations, and outputs in an intuitive manner.

Scenario 2: Lineage metadata does not exist for the dataset

- **Given** lineage metadata does not exist for the dataset,\
   **and** the Participant has read access to the dataset,
- **When** the Participant requests to view dataset lineage via the Simpl-Open UI,
- **Then** Simpl-Open must return an error indicating that lineage metadata is not available for the dataset,\
   **and** the UI must indicate that dataset lineage cannot be visualised.

Scenario 3: Access to dataset lineage is denied

- **Given** lineage metadata exists for the dataset,\
   **and** the Participant does not have read access to the dataset,
- **When** the Participant requests to view dataset lineage via the Simpl-Open UI,
- Then Simpl-Open must reject the request due to insufficient permissions,\
   **and** the UI must display an access denied message indicating that the Participant is not authorised to view dataset lineage.

---------------------------------------------

**Possible Solution Implementation / Scenarios:**

**Scenario#1: lineage available**

- User Action: Participant requests to view dataset lineage *via the Simpl-Open UI*
- Expected Result:
   - Simpl-Open displays an interactive SVG graph
   - Each node/edge is clickable to show metadata tooltips
   - The graph supports zoom/pan via mouse controls
   - A "Download PNG" button exports the full graph.

**Scenario#2: No lineage available: lineage metadata does NOT exist for a dataset**

- User Action: Participant requests to view dataset lineage via the Simpl-Open UI
- Expected Result:
   - Simpl-Open displays an error
   - UI shows a disabled graph container with help text.

**Scenario#3 : Access denied**

- User Action: Participant requests to view dataset lineage via the Simpl-Open UI
- Expected Result:
   - Simpl-Open displays an error banner: "Access denied - insufficient permissions for dataset lineage"
   - no graph data loads (empty container).

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-1620](../README.md) (L2) Participant tracks data lineage for processed datasets |
| **Children** | None. |
