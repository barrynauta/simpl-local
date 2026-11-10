> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA01](../../../../../README.md) / [L0-SIMPL-16074](../../../../README.md) / [L1-SIMPL-19291](../../../README.md) / [L2-SIMPL-1620](../../README.md) / [Epic-SIMPL-2068](../README.md) / **Story-SIMPL-25746**

# SIMPL-25746: Provider configures lineage metadata at job and service level

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [SA01](../../../../../README.md) › [SIMPL-16074](../../../../README.md) (L0) › [SIMPL-19291](../../../README.md) (L1) › [SIMPL-1620](../../README.md) (L2) › [SIMPL-2068](../README.md) (Epic) › **SIMPL-25746** (User story)

## Text

**As a** Data Provider or Consumer,\
**I want to** configure lineage dataset references and upstream input references in workflow run config,\
**so that** lineage generation can build a semantically linked provenance graph.

###### Technical Details:

- Lineage configuration is provided via a lineage resource implementing AbstractLineageConfig.
- Current job wiring uses LocalLineageConfig configured at launch through resources.lineage.config.file_path.
- Optional generate_dataset may be provided in resource config.
- If generate_dataset is omitted, run_id is used as dataset suffix.
- Service-level upstream references are read from lineage configuration (string or list of strings).
- URI values are validated in the sensor before report generation.

###### Definitions

Lineage-enabled workflow: A workflow where lineage is activated through a lineage tag.

Lineage configuration: The workflow configuration section created for lineage when lineage is enabled. It contains:\
1. one service-level subsection per service in the workflow, and\
2. one global lineage subsection.

Input services: Services that run as part of the workflow and emit references used to build lineage relations.

##### Acceptance Criteria

###### **AC1**

**Given** a workflow run configuration includes a -valid flag- valid lineage configuration to activate the lineage generation\
**When** the workflow run completes succesfully\
**Then** the lineage output is built

###### **AC2**

**Given** a dataset name is provided in the lineage configuration\
**When** lineage output is built\
**Then** the system generates a URI using the uri_prefix and dataset name as an identifier for the dataset\
**And** that identifier is used in the lineage report

###### **AC3**

**Given** one or more input services (services that read data, e.g., from S3 or other data sources) provide valid references to other URI\
**When** lineage output is built\
**Then** those URIs are accepted as upstream references\
**And** they are available for prov:used and derived linkage

###### **AC4**

**Given** lineage configuration is invalid\
**When** lineage output is built\
**Then** lineage generation is skipped\
**And** the skip reason includes a stable non-sensitive error code

###### **AC5**

**Given** no dataset name is provided in the config\
**When** lineage output is built\
**Then** the system generates a URI using the uri_prefix and run_id as an identifier for the dataset\
**And** that identifier is used in the lineage report

###### Context:

Technical Description: [an internal page]

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-2068](../README.md) (Epic) Participant captures dataset lineage for processed datasets |
