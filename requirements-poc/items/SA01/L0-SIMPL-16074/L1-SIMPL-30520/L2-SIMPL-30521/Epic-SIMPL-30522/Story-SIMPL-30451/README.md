> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA01](../../../../../README.md) / [L0-SIMPL-16074](../../../../README.md) / [L1-SIMPL-30520](../../../README.md) / [L2-SIMPL-30521](../../README.md) / [Epic-SIMPL-30522](../README.md) / **Story-SIMPL-30451**

# SIMPL-30451: A Participant configures the execution target used for distributed workflow runs

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [SA01](../../../../../README.md) › [SIMPL-16074](../../../../README.md) (L0) › [SIMPL-30520](../../../README.md) (L1) › [SIMPL-30521](../../README.md) (L2) › [SIMPL-30522](../README.md) (Epic) › **SIMPL-30451** (User story)

## Text

**As a** Participant with permission to configure workflows,\
**I want to** consult user-guide documentation that explains how execution target and integration pattern must be selected and documented for each workflow,\
**so that** I can choose the right runtime model and configuration approach without ambiguity.

bq. **Documentation context:** The execution target binds the workflow to infrastructure and defines how runtime information is shared with the control plane. In a tightly coupled setup, runtime workers require direct connectivity to orchestration runtime dependencies. In the loosely coupled setup, execution is dispatched to an external cluster and runtime information returns through a decoupled reporting channel, without direct metadata database connectivity from execution pods.

**Acceptance Criteria**

*Note: This is a documentation story. The criteria below describe expected content completeness of the user guide and supporting reference material, not automated test conditions. Execution-target and integration-pattern linkage are configured in code; this story covers both documentation and runnable reference implementations.*

**AC1 - Guide explains execution-target choices**

- The execution-target section explains when to choose tightly coupled setup and when to choose loosely coupled setup.
- The guide states prerequisites for each setup, including connectivity and control-plane reporting expectations.
- The guide clarifies what must be documented at workflow level before first execution.

**AC2 - Guide supports decision making**

- The guide provides a side-by-side comparison of coupling, connectivity, and runtime-information flow across both setups.
- The guide includes environment-fit indicators that help participants select the appropriate pattern.
- The guide lists key trade-offs, including operational dependencies and observability implications.

**AC3 - Guide includes readiness checks**

- The checklist provides pre-run validation items per setup.
- The checklist maps each readiness check to expected evidence.
- The guide includes common misconfiguration symptoms and corrective actions for each setup.

**AC4 - Guide shows how execution-target linkage is configured in code**

- The guide explains which configuration constructs are used to bind execution target and integration pattern to a workflow.
- The guide provides a worked before-and-after example showing configuration changes when switching between tightly coupled and loosely coupled setups.
- The distributed_execution service includes runnable reference implementations for both setups with linkage defined through code configuration.
- The guide clarifies that setup selection is persisted in workflow/service configuration artifacts, not as a separate runtime-only setting.

**Technical Details**

- Tightly coupled setup (implementation reference): Dagster launchers/executors schedule run workers/step jobs in Kubernetes and execution pods must reach orchestration runtime dependencies.
- Loosely coupled setup (implementation reference): Dagster dispatches external execution through Kubernetes API and receives logs/custom messages via `dagster-pipes`.
- Loosely coupled setup does not require direct metadata database connectivity from execution pods.
- Validation checklist should include target availability, launcher setup, and message-path verification.
- The user guide and supporting material will be hosted in a dedicated service called **distributed_execution**, which serves as the canonical location for all documentation, example workflows, and reference service implementations related to distributed execution patterns. It should be located here: https://code.europa.eu/simpl/simpl-open/data/supporting-data-services/distributed-execution
- The **distributed_execution** service will include: (1) the user guide, (2) annotated example workflows, and (3) reference service implementations that teams can inspect and adapt.
- The user guide will include screenshots of relevant UI surfaces and diagrams illustrating both integration patterns. The platform architecture document will be updated to reflect the distributed_execution service and its relationship to the orchestration runtime and external clusters.
- configuration files will be included as working examples
- The guide will provide links to official upstream documentation for Dagster, Kubernetes, and dagster-pipes where relevant.

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-30522](../README.md) (Epic) A Participant configures the execution target used for distributed workflow runs |
