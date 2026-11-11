> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA01](../../../../../README.md) / [L0-SIMPL-16074](../../../../README.md) / [L1-SIMPL-30520](../../../README.md) / [L2-SIMPL-30523](../../README.md) / [Epic-SIMPL-30524](../README.md) / **Story-SIMPL-30788**

# SIMPL-30788: A Participant defines the workflow distribution model of a data orchestration workflow

**User story (implementable work)** · Development: **Developed**

**Trace:** [SA01](../../../../../README.md) › [SIMPL-16074](../../../../README.md) (L0) › [SIMPL-30520](../../../README.md) (L1) › [SIMPL-30523](../../README.md) (L2) › [SIMPL-30524](../README.md) (Epic) › **SIMPL-30788** (User story)

## Text

**As a** Participant with a role authorising the definition of data orchestration workflows,\
**I want to** consult a user guide that explains the workflow-level distribution model and where it fits among available integration patterns,\
**So that** I can choose workflow-level distribution confidently for my use case.\
Documentation context: The workflow distribution model remains a run-level execution model and is compatible with both integration patterns. In a tightly coupled setup, run workers execute with direct orchestration-runtime connectivity. In a loosely coupled setup, selected workloads can be delegated to external pods while still reporting runtime details back to the control plane.\

**Acceptance Criteria**

*Note: This is a documentation ticket. The criteria below describe expected content completeness of the user guide and supporting reference material, not automated test conditions. The selection and linkage of workflow-level distribution and integration pattern are configured in code; this ticket covers both documentation and working reference implementations.*

**AC1 - Guide defines workflow-level distribution model**

- The workflow-distribution section defines workflow-level distribution in clear operational terms.
- The guide explains expected execution behavior at run level, including how run scheduling and execution boundaries are interpreted.
- The guide states how workflow-level distribution differs from service-level distribution at a conceptual level.

**AC2 - Guide explains when to choose workflow-level distribution**

- The model-selection guidance identifies when workflow-level distribution is recommended.
- The guide explains trade-offs versus service-level distribution, including operational complexity, control boundaries, and deployment prerequisites.
- The guide provides at least one concrete scenario where workflow-level distribution is a preferred choice and one where it is not.

**AC3 - Guide includes pattern mapping and decision flow**

- The decision flow lists required setup checks before first execution for each pattern.
- The guide includes a compact checklist of evidence expected during decisions.

**AC4 - Guide shows how model and pattern linkage is configured in code**

- The guide explains which configuration constructs are used to attach workflow-level distribution and the selected integration pattern to a workflow.
- The guide provides a worked before-and-after example showing how configuration changes when switching from tightly coupled setup to loosely coupled setup (and vice versa).
- The distributed_execution service delivers runnable reference implementations for both setups with linkage defined through code configuration.
- The guide clarifies that model and pattern selections are persisted in workflow/service configuration artifacts, not as separate runtime-only settings.

**Technical Details**

**Technical Description (deployment-specific launcher setup):**
```
run_coordinator:
  module: dagster.core.run_coordinator
  class: QueuedRunCoordinator
  config:
    max_concurrent_runs: 20
    tag_concurrency_limits:
      - key: "participant_id"
        limit: 5
```
Run-launcher and executor wiring is intentionally deployment-specific:

- Tightly coupled setup uses launcher/executor setup that is tightly connected to orchestration runtime dependencies.
- Loosely coupled setup uses external dispatch via Kubernetes API and message exchange for runtime reporting.
- The selected pattern determines the required launcher, credentials, RBAC, network paths, and cluster bootstrap steps.

- The user guide and supporting material will be hosted in a dedicated service called distributed_execution, which serves as the canonical location for all documentation, example workflows, and reference service implementations related to distributed execution patterns. It should be located here: [https://code.europa.eu/simpl/simpl-open/development/data-services](https://code.europa.eu/simpl/simpl-open/development/data-services)
- The **distributed_execution** service will include: (1) the user guide, (2) annotated example workflows, and (3) reference service implementations that teams can inspect and adapt.
- The user guide will include screenshots of relevant UI surfaces and diagrams illustrating both integration patterns. The platform architecture document will be updated to reflect the distributed_execution service and its relationship to the orchestration runtime and external clusters.
- configuration files will be included as working examples
- The guide will provide links to official upstream documentation for Dagster, Kubernetes, and dagster-pipes where relevant.

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA01](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)) |
| **Parent** | [SIMPL-30524](../README.md) (Epic) A Participant defines the workflow distribution model of a data orchestration workflow |
