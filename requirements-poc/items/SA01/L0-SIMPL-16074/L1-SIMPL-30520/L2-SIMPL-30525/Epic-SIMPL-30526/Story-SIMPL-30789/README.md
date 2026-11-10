> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA01](../../../../../README.md) / [L0-SIMPL-16074](../../../../README.md) / [L1-SIMPL-30520](../../../README.md) / [L2-SIMPL-30525](../../README.md) / [Epic-SIMPL-30526](../README.md) / **Story-SIMPL-30789**

# SIMPL-30789: A Participant defines the service distribution model of a data orchestration workflow

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [SA01](../../../../../README.md) › [SIMPL-16074](../../../../README.md) (L0) › [SIMPL-30520](../../../README.md) (L1) › [SIMPL-30525](../../README.md) (L2) › [SIMPL-30526](../README.md) (Epic) › **SIMPL-30789** (User story)

## Text

**As a** Participant with a role authorising the definition of data orchestration workflows,\
**I want to** consult a user guide that explains the service-level distribution model and its interaction with each integration pattern,\
**so that** I can decide when step-level distribution is the right choice and understand setup implications.\
**Documentation context:** The service distribution model uses a step-level Kubernetes executor for dispatch. It is compatible with both integration patterns: tightly coupled setup relies on direct runtime connectivity for native execution-state handling, while loosely coupled setup can offload selected compute actions to external pods and still return runtime telemetry.

**Acceptance Criteria**

*Note: This is a documentation ticket. The following criteria describe the expected content and completeness of the user guide and its accompanying example implementations, not automated test conditions. Linking a service distribution model to a workflow is done via configuration in code; this ticket covers both the documentation and the delivery of working example implementations.*

**AC1 - Guide explains service-level distribution behavior**

- The guide describes what step-level dispatch means in the context of Dagster and Kubernetes
- The guide explains dependency-aware execution and clarifies expected behavior for parallel and sequential branches
- The guide states which integration patterns (tightly coupled / loosely coupled) are compatible and how each behaves

**AC2 - Guide covers data handoff and known constraints**

- The guide explains storage and IO prerequisites required when step-level distribution crosses pod boundaries
- The guide lists failure modes that occur when those prerequisites are not met

**AC3 - Guide provides practical setup examples**

- The guide includes one annotated example for a tightly coupled setup and one for a loosely coupled setup
- Each example documents prerequisites, configuration steps, validation checks, and expected runtime telemetry output

**AC4 - Guide shows how to link a distribution model to a workflow via code configuration**

- The guide explains which configuration construct (e.g. executor definition, resource binding, or run-config block) is the mechanism for attaching a distribution model to a specific workflow
- The guide provides a worked example showing a workflow with the distribution model applied through its configuration
- The `distributed_execution` service delivers at least one reference implementation, a runnable workflow with the distribution model that participants can inspect and adapt
- The guide clarifies that the distribution model selection is persisted as part of the workflow configuration (code), not as a separate runtime setting

**Technical Details**

- Service-level distribution relies on a step-level Kubernetes executor model.
- Cross-step data handoff depends on shared IO/storage accessible across pod boundaries.
- For loosely coupled setup variants, runtime telemetry should include logs and custom messages from external execution.
- The user guide and supporting material will be hosted in a dedicated service called distributed_execution, which serves as the canonical location for all documentation, example workflows, and reference service implementations related to distributed execution patterns. It should be located here: [https://code.europa.eu/simpl/simpl-open/development/data-services](https://code.europa.eu/simpl/simpl-open/development/data-services)
- The **distributed_execution** service will include: (1) the user guide, (2) annotated example workflows, and (3) reference service implementations that teams can inspect and adapt.
- The user guide will include screenshots of relevant UI surfaces and diagrams illustrating both integration patterns. The platform architecture document will be updated to reflect the distributed_execution service and its relationship to the orchestration runtime and external clusters.
- configuration files will be included as working examples
- The guide will provide links to official upstream documentation for Dagster, Kubernetes, and dagster-pipes where relevant.

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-30526](../README.md) (Epic) A Participant defines the service distribution model of a data orchestration workflow |
