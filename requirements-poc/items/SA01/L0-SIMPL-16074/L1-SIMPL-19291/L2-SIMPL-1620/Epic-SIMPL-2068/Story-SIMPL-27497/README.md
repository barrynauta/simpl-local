> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA01](../../../../../README.md) / [L0-SIMPL-16074](../../../../README.md) / [L1-SIMPL-19291](../../../README.md) / [L2-SIMPL-1620](../../README.md) / [Epic-SIMPL-2068](../README.md) / **Story-SIMPL-27497**

# SIMPL-27497: Provider generates PROV-O lineage report for processed datasets

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [SA01](../../../../../README.md) › [SIMPL-16074](../../../../README.md) (L0) › [SIMPL-19291](../../../README.md) (L1) › [SIMPL-1620](../../README.md) (L2) › [SIMPL-2068](../README.md) (Epic) › **SIMPL-27497** (User story)

## Text

**As a** Data Provider,\
**I want to** generate machine-readable lineage for each processed dataset,\
**So that** provenance can be exchanged and interpreted consistently.

###### Technical Details:

- Output format is JSON-LD aligned to W3C PROV-O context.
- Graph includes at minimum:
- prov:Entity for the transformed dataset
- workflow prov:Activity
- one prov:Activity per extracted service
- prov:Agent from LINEAGE_PARTICIPANT_NAME
- Dataset URI is built from URI_PREFIX + generate_dataset, or URI_PREFIX + run_id fallback.
- Upstream used_input values are represented through prov:used and prov:qualifiedUsage on service activities.
- Entity-level derivation is represented through prov:wasDerivedFrom.
- Entity generation linkage is represented through prov:wasGeneratedBy (and prov:qualifiedGeneration when minimal is false).
- Minimal mode excludes runtime time fields while preserving core graph linkage.
- Persistence is performed through lineage_resource.write_lineage_report, currently implemented for LocalLineageConfig.

##### Acceptance Criteria

###### **AC1**

1. **Given** a lineage-enabled successful run with valid prerequisites\
   **When** lineage output is generated\
   **Then** a JSON-LD PROV-O report is created\
   **And** it contains dataset, workflow activity, service activities, and participant agent

###### **AC2**

2. **Given** upstream dataset references are available\
   **When** the report is generated\
   **Then** upstream links are represented with valid absolute URIs\
   **And** semantic stitching across runs is supported through those URIs

###### **AC3**

3. **Given** service-specific lineage label fields are not provided\
   **When** the report is generated\
   **Then** the service op name is used as fallback activity label\
   **And** report generation remains valid

###### **AC4**

4. **Given** required graph inputs cannot be resolved (for example invalid URI or missing required env)\
   **When** report generation is attempted\
   **Then** generation is skipped or fails deterministically with stable reason codes\
   **And** operators receive non-sensitive diagnostic feedback in logs/skip messages

###### **AC5**

5. **Given** multiple upstream inputs are defined across service configs\
   **When** the lineage report is generated\
   **Then** each upstream input is represented as a separate prov:used entry\
   -And qualified usage nodes are emitted per input in non-minimal mode-

###### **AC6**

6. **Given** generate_dataset is not set\
   **When** the lineage report is generated\
   **Then** dataset URI is constructed using URI_PREFIX + run_id\
   **And** the resulting URI is unique per run

###### **AC7**

7. **Given** lineage report persistence is executed in current implementation\
   **When** local lineage config is used\
   **Then** the report is written to the configured local file_path\
   **And** S3 persistence remains a follow-up until S3LineageConfig.write_lineage_report is implemented

###### Context:

- Technical Description: (internal page)

- Relation to SIMPL-25747: business side logic overlapps between those two tickets. The main difference is the technical part\
   -- SIMPL-25747 is about the sensor to catch it\
   -- SIMPL-27497 is about the transformation logic

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-2068](../README.md) (Epic) Participant captures dataset lineage for processed datasets |
