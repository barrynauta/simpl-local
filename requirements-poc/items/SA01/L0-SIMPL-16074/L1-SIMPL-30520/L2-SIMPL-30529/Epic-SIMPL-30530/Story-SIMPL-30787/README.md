> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA01](../../../../../README.md) / [L0-SIMPL-16074](../../../../README.md) / [L1-SIMPL-30520](../../../README.md) / [L2-SIMPL-30529](../../README.md) / [Epic-SIMPL-30530](../README.md) / **Story-SIMPL-30787**

# SIMPL-30787: A Participant defines the execution container cluster of the distribution model of a data orchestration workflow

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [SA01](../../../../../README.md) › [SIMPL-16074](../../../../README.md) (L0) › [SIMPL-30520](../../../README.md) (L1) › [SIMPL-30529](../../README.md) (L2) › [SIMPL-30530](../README.md) (Epic) › **SIMPL-30787** (User story)

## Text

**As a** Participant with a role authorising the definition of data orchestration workflows,\
**I want to** consult user-guide documentation that explains how to document and prepare container-cluster settings per pattern,\
**so that** I can align workflow distribution choices with my operational environment.\
**Documentation context:** The container cluster is where run-worker pods or external execution pods are spawned. In a tightly coupled setup, cluster runtime connectivity to orchestration dependencies is required for native runtime-state handling. In a loosely coupled setup, external pods are launched through cluster APIs and return runtime information through a decoupled reporting channel, so direct metadata database connectivity is not required from execution pods.

**Acceptance Criteria**

*Note: This is a documentation ticket. The criteria below describe expected content completeness of the user guide and supporting reference material, not automated test conditions. The linkage between distribution pattern choice and workflow execution setup is done through configuration in code; this ticket covers both documentation and working reference implementations.*

**AC1 - Guide explains cluster setup options**

- The guide describes prerequisites for tightly coupled and loosely coupled setups.
- The guide explains what must be documented before execution for each setup.
- The guide states where cluster-runtime connectivity is required and where decoupled reporting is expected.

**AC2 - Guide clarifies required inputs**

- The cluster-configuration section clearly distinguishes mandatory and optional inputs per setup.
- The guide provides examples for both setups without enforcing one fixed config naming model.
- The guide includes a minimum input set that can be reused as a baseline by participating teams.

**AC3 - Guide includes validation checklist**

- The validation checklist distinguishes tightly coupled setup checks from loosely coupled setup checks.
- Each checklist item is mapped to expected evidence.

**AC4 - Guide shows how pattern linkage is configured in code**

- The guide explains which configuration constructs are used to attach the selected distribution pattern to a workflow or reference service implementation.
- The guide provides a worked before-and-after example showing how configuration changes when switching between tightly coupled and loosely coupled setups.
- The distributed_execution service delivers at least one runnable reference implementation for each setup with pattern selection wired through code configuration.
- The guide clarifies that setup selection is persisted as part of workflow or service configuration artifacts, not as a separate runtime-only setting.

**Technical Details**

- The user guide and supporting material will be hosted in a dedicated service called distributed_execution, which serves as the canonical location for all documentation, example workflows, and reference service implementations related to distributed execution patterns. It should be located here: [https://code.europa.eu/simpl/simpl-open/data/supporting-data-services/distributed-execution/distributed-execution](https://code.europa.eu/simpl/simpl-open/data/supporting-data-services/distributed-execution/distributed-execution)
- The **distributed_execution** service will include: (1) the user guide, (2) annotated example workflows, and (3) reference service implementations that teams can inspect and adapt.
- The user guide will include screenshots of relevant UI surfaces and diagrams illustrating both integration patterns. The platform architecture document will be updated to reflect the distributed_execution service and its relationship to the orchestration runtime and external clusters.
- configuration files will be included as working examples
- The guide will provide links to official upstream documentation for Dagster, Kubernetes, and dagster-pipes where relevant.

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-30530](../README.md) (Epic) A Participant defines the execution container cluster of the distribution model of a data orchestration workflow |
