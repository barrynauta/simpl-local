> **Simpl-Open requirements, proof of concept.** One business process and the items under it. New here? [Start here](../../README.md) · [What the levels mean](../../levels.md) · [All items](../../all-items.md) · [By business process](../../processes.md)

Folder: [items](../README.md) / **SA01**

# SA01 - Data orchestration used by a Participant

Providers and Consumers design, run and monitor repeatable data processing workflows.

Public pages: [public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa01-data-orchestration-used-participant)

**67 items:** 1 L0, 8 L1, 18 L2, 24 Epic, 16 User story.

## Tree

- [SIMPL-16074](L0-SIMPL-16074/README.md) L0 · SA01 - Data orchestration used by a Participant · Defined
  - [SIMPL-19287](L0-SIMPL-16074/L1-SIMPL-19287/README.md) L1 · Participant creates a new data orchestration workflow · Defined
    - [SIMPL-19311](L0-SIMPL-16074/L1-SIMPL-19287/L2-SIMPL-19311/README.md) L2 · Participant creates a new data orchestration workflow · Defined
      - [SIMPL-19323](L0-SIMPL-16074/L1-SIMPL-19287/L2-SIMPL-19311/Epic-SIMPL-19323/README.md) Epic · A Participant defines a new data orchestration workflow · Defined
  - [SIMPL-19288](L0-SIMPL-16074/L1-SIMPL-19288/README.md) L1 · Participant consults their own data orchestration workflows · Defined
    - [SIMPL-19312](L0-SIMPL-16074/L1-SIMPL-19288/L2-SIMPL-19312/README.md) L2 · Participant consults an overview of their own data orchestration workflows · Defined
      - [SIMPL-19328](L0-SIMPL-16074/L1-SIMPL-19288/L2-SIMPL-19312/Epic-SIMPL-19328/README.md) Epic · A Participant gets the list of existing data orchestration workflows · Defined
    - [SIMPL-19313](L0-SIMPL-16074/L1-SIMPL-19288/L2-SIMPL-19313/README.md) L2 · Participant consults the details of a data orchestration workflow · Defined
      - [SIMPL-19329](L0-SIMPL-16074/L1-SIMPL-19288/L2-SIMPL-19313/Epic-SIMPL-19329/README.md) Epic · A Participant gets details of the existing data orchestration workflow · Defined
  - [SIMPL-19289](L0-SIMPL-16074/L1-SIMPL-19289/README.md) L1 · Participant creates a new version of a data orchestration workflow · Defined
    - [SIMPL-19315](L0-SIMPL-16074/L1-SIMPL-19289/L2-SIMPL-19315/README.md) L2 · Participant creates a new version of a data orchestration workflow · Defined
      - [SIMPL-19330](L0-SIMPL-16074/L1-SIMPL-19289/L2-SIMPL-19315/Epic-SIMPL-19330/README.md) Epic · A Participant updates the data orchestration workflow · Defined
  - [SIMPL-19290](L0-SIMPL-16074/L1-SIMPL-19290/README.md) L1 · Participant deletes a data orchestration workflow · Defined
    - [SIMPL-19317](L0-SIMPL-16074/L1-SIMPL-19290/L2-SIMPL-19317/README.md) L2 · Participant deletes a data orchestration workflow · Defined
      - [SIMPL-19332](L0-SIMPL-16074/L1-SIMPL-19290/L2-SIMPL-19317/Epic-SIMPL-19332/README.md) Epic · A Participant deletes a data orchestration workflow that is not referenced by any published dataset · Defined
      - [SIMPL-19333](L0-SIMPL-16074/L1-SIMPL-19290/L2-SIMPL-19317/Epic-SIMPL-19333/README.md) Epic · A Participant is prevented from deleting a data orchestration workflow with dependent datasets · Defined
        - [SIMPL-23647](L0-SIMPL-16074/L1-SIMPL-19290/L2-SIMPL-19317/Epic-SIMPL-19333/Story-SIMPL-23647/README.md) User story · Check Whether a Workflow Has Associated Resource Descriptions · Ready for DG Acceptance
  - [SIMPL-19291](L0-SIMPL-16074/L1-SIMPL-19291/README.md) L1 · Participant executes a data orchestration workflow · Defined
    - [SIMPL-1620](L0-SIMPL-16074/L1-SIMPL-19291/L2-SIMPL-1620/README.md) L2 · Participant tracks data lineage for processed datasets · Defined
      - [SIMPL-2068](L0-SIMPL-16074/L1-SIMPL-19291/L2-SIMPL-1620/Epic-SIMPL-2068/README.md) Epic · Participant captures dataset lineage for processed datasets · Defined
        - [SIMPL-25746](L0-SIMPL-16074/L1-SIMPL-19291/L2-SIMPL-1620/Epic-SIMPL-2068/Story-SIMPL-25746/README.md) User story · Provider configures lineage metadata at job and service level · DevTeam PBI is ready for Implementation check
        - [SIMPL-25747](L0-SIMPL-16074/L1-SIMPL-19291/L2-SIMPL-1620/Epic-SIMPL-2068/Story-SIMPL-25747/README.md) User story · Participant captures lineage within workflows · DevTeam PBI is ready for Implementation check
        - [SIMPL-27497](L0-SIMPL-16074/L1-SIMPL-19291/L2-SIMPL-1620/Epic-SIMPL-2068/Story-SIMPL-27497/README.md) User story · Provider generates PROV-O lineage report for processed datasets · DevTeam PBI is ready for Implementation check
      - [SIMPL-2069](L0-SIMPL-16074/L1-SIMPL-19291/L2-SIMPL-1620/Epic-SIMPL-2069/README.md) Epic · Participant visualises dataset lineage · Cancelled · *🔴 cancelled*
    - [SIMPL-19318](L0-SIMPL-16074/L1-SIMPL-19291/L2-SIMPL-19318/README.md) L2 · Participant manually executes a data orchestration workflow · Defined
      - [SIMPL-19334](L0-SIMPL-16074/L1-SIMPL-19291/L2-SIMPL-19318/Epic-SIMPL-19334/README.md) Epic · A Participant manually starts the execution of a data orchestration workflow · Defined
    - [SIMPL-19319](L0-SIMPL-16074/L1-SIMPL-19291/L2-SIMPL-19319/README.md) L2 · Participant executes data orchestration workflows automatically based on events · Defined
      - [SIMPL-19335](L0-SIMPL-16074/L1-SIMPL-19291/L2-SIMPL-19319/Epic-SIMPL-19335/README.md) Epic · A Participant automatically executes an event-triggered data orchestration workflow execution · Defined
        - [SIMPL-21844](L0-SIMPL-16074/L1-SIMPL-19291/L2-SIMPL-19319/Epic-SIMPL-19335/Story-SIMPL-21844/README.md) User story · API for Workflows and Default Config Retrieval · PBI Cancelled · *🔴 cancelled*
        - [SIMPL-22926](L0-SIMPL-16074/L1-SIMPL-19291/L2-SIMPL-19319/Epic-SIMPL-19335/Story-SIMPL-22926/README.md) User story · API for Workflow and Default Config Retrieval · Ready for DG Acceptance
      - [SIMPL-22371](L0-SIMPL-16074/L1-SIMPL-19291/L2-SIMPL-19319/Epic-SIMPL-22371/README.md) Epic · A Participant associates an event-triggered data orchestration workflow to a resource description · Defined
        - [SIMPL-20783](L0-SIMPL-16074/L1-SIMPL-19291/L2-SIMPL-19319/Epic-SIMPL-22371/Story-SIMPL-20783/README.md) User story · API for Resource - Workflow Association and Configuration Validation · Ready for DG Acceptance
        - [SIMPL-23617](L0-SIMPL-16074/L1-SIMPL-19291/L2-SIMPL-19319/Epic-SIMPL-22371/Story-SIMPL-23617/README.md) User story · API for Workflow Execution and Execution Status · Ready for DG Acceptance
        - [SIMPL-24647](L0-SIMPL-16074/L1-SIMPL-19291/L2-SIMPL-19319/Epic-SIMPL-22371/Story-SIMPL-24647/README.md) User story · [BE] Integrate SD‑Tooling‑BE With AssetOrchestrator for Workflow Association · DevTeam PBI is ready for Implementation check
        - [SIMPL-24652](L0-SIMPL-16074/L1-SIMPL-19291/L2-SIMPL-19319/Epic-SIMPL-22371/Story-SIMPL-24652/README.md) User story · [UI] Retrieve Available Workflows and Render Workflow‑Enabled Template (Data Orchestration) · DevTeam PBI is ready for Implementation check
        - [SIMPL-26071](L0-SIMPL-16074/L1-SIMPL-19291/L2-SIMPL-19319/Epic-SIMPL-22371/Story-SIMPL-26071/README.md) User story · [UI] Retrieve workflow default from Asset Orchestrator (Data Orchestration) · Ready for DG Acceptance
        - [SIMPL-26909](L0-SIMPL-16074/L1-SIMPL-19291/L2-SIMPL-19319/Epic-SIMPL-22371/Story-SIMPL-26909/README.md) User story · [UI] Expose app sharing method templates with workflow configuration support (App Orchestration) · Ready for DG Acceptance
    - [SIMPL-19320](L0-SIMPL-16074/L1-SIMPL-19291/L2-SIMPL-19320/README.md) L2 · Participant executes data orchestration workflows automatically based on time · Defined
      - [SIMPL-19336](L0-SIMPL-16074/L1-SIMPL-19291/L2-SIMPL-19320/Epic-SIMPL-19336/README.md) Epic · Simpl-Open automatically executes a time-triggered data orchestration workflow · Defined
  - [SIMPL-19292](L0-SIMPL-16074/L1-SIMPL-19292/README.md) L1 · Participant monitors and accesses data orchestration workflow execution logs · Defined
    - [SIMPL-19321](L0-SIMPL-16074/L1-SIMPL-19292/L2-SIMPL-19321/README.md) L2 · Participant captures data orchestration workflow execution logs · Defined
      - [SIMPL-19337](L0-SIMPL-16074/L1-SIMPL-19292/L2-SIMPL-19321/Epic-SIMPL-19337/README.md) Epic · A Participant captures data orchestration workflow execution logs · Defined
    - [SIMPL-19322](L0-SIMPL-16074/L1-SIMPL-19292/L2-SIMPL-19322/README.md) L2 · Participant monitors a data orchestration workflow · Defined
      - [SIMPL-19338](L0-SIMPL-16074/L1-SIMPL-19292/L2-SIMPL-19322/Epic-SIMPL-19338/README.md) Epic · A Participant views data orchestration workflow logs · Defined
      - [SIMPL-19339](L0-SIMPL-16074/L1-SIMPL-19292/L2-SIMPL-19322/Epic-SIMPL-19339/README.md) Epic · A Participant views the status indicator of a data orchestration workflow · Defined
      - [SIMPL-19340](L0-SIMPL-16074/L1-SIMPL-19292/L2-SIMPL-19322/Epic-SIMPL-19340/README.md) Epic · A Participant views historical data orchestration workflow logs · Defined
  - [SIMPL-30520](L0-SIMPL-16074/L1-SIMPL-30520/README.md) L1 · Participant defines used distribution model · Defined
    - [SIMPL-30521](L0-SIMPL-16074/L1-SIMPL-30520/L2-SIMPL-30521/README.md) L2 · Participant configures the distributed execution · Defined
      - [SIMPL-30522](L0-SIMPL-16074/L1-SIMPL-30520/L2-SIMPL-30521/Epic-SIMPL-30522/README.md) Epic · A Participant configures the execution target used for distributed workflow runs · Defined
        - [SIMPL-30451](L0-SIMPL-16074/L1-SIMPL-30520/L2-SIMPL-30521/Epic-SIMPL-30522/Story-SIMPL-30451/README.md) User story · A Participant configures the execution target used for distributed workflow runs · DevTeam PBI is ready for Implementation check
    - [SIMPL-30523](L0-SIMPL-16074/L1-SIMPL-30520/L2-SIMPL-30523/README.md) L2 · Participant defines workflow distribution model · Defined
      - [SIMPL-30524](L0-SIMPL-16074/L1-SIMPL-30520/L2-SIMPL-30523/Epic-SIMPL-30524/README.md) Epic · A Participant defines the workflow distribution model of a data orchestration workflow · Defined
        - [SIMPL-30788](L0-SIMPL-16074/L1-SIMPL-30520/L2-SIMPL-30523/Epic-SIMPL-30524/Story-SIMPL-30788/README.md) User story · A Participant defines the workflow distribution model of a data orchestration workflow · DevTeam PBI is ready for Implementation check
    - [SIMPL-30525](L0-SIMPL-16074/L1-SIMPL-30520/L2-SIMPL-30525/README.md) L2 · Participant defines service distribution model · Defined
      - [SIMPL-30526](L0-SIMPL-16074/L1-SIMPL-30520/L2-SIMPL-30525/Epic-SIMPL-30526/README.md) Epic · A Participant defines the service distribution model of a data orchestration workflow · Defined
        - [SIMPL-30789](L0-SIMPL-16074/L1-SIMPL-30520/L2-SIMPL-30525/Epic-SIMPL-30526/Story-SIMPL-30789/README.md) User story · A Participant defines the service distribution model of a data orchestration workflow · DevTeam PBI is ready for Implementation check
    - [SIMPL-30527](L0-SIMPL-16074/L1-SIMPL-30520/L2-SIMPL-30527/README.md) L2 · Participant defines sub-service distribution model · Defined
      - [SIMPL-30528](L0-SIMPL-16074/L1-SIMPL-30520/L2-SIMPL-30527/Epic-SIMPL-30528/README.md) Epic · A Participant defines the sub-service distribution model of a data orchestration workflow · Defined
    - [SIMPL-30529](L0-SIMPL-16074/L1-SIMPL-30520/L2-SIMPL-30529/README.md) L2 · Participant defines used execution container cluster · Defined
      - [SIMPL-30530](L0-SIMPL-16074/L1-SIMPL-30520/L2-SIMPL-30529/Epic-SIMPL-30530/README.md) Epic · A Participant defines the execution container cluster of the distribution model of a data orchestration workflow · Defined
        - [SIMPL-30787](L0-SIMPL-16074/L1-SIMPL-30520/L2-SIMPL-30529/Epic-SIMPL-30530/Story-SIMPL-30787/README.md) User story · A Participant defines the execution container cluster of the distribution model of a data orchestration workflow · DevTeam PBI is ready for Implementation check
    - [SIMPL-30531](L0-SIMPL-16074/L1-SIMPL-30520/L2-SIMPL-30531/README.md) L2 · Participant defines used sub-service distribution cluster · Defined
      - [SIMPL-30532](L0-SIMPL-16074/L1-SIMPL-30520/L2-SIMPL-30531/Epic-SIMPL-30532/README.md) Epic · A Participant defines the distributed processing clusters of the sub-service distribution model of a data orchestration workflow · Defined
  - [SIMPL-30560](L0-SIMPL-16074/L1-SIMPL-30560/README.md) L1 · Participant executes workflow on saved distribution configuration · Defined
    - [SIMPL-30562](L0-SIMPL-16074/L1-SIMPL-30560/L2-SIMPL-30562/README.md) L2 · Participant executes workflow on saved distribution configuration with pre-dispatch validation · Defined
      - [SIMPL-30563](L0-SIMPL-16074/L1-SIMPL-30560/L2-SIMPL-30562/Epic-SIMPL-30563/README.md) Epic · Simpl-Open executes a data orchestration workflow using the configured distribution model and execution cluster · Defined
      - [SIMPL-30564](L0-SIMPL-16074/L1-SIMPL-30560/L2-SIMPL-30562/Epic-SIMPL-30564/README.md) Epic · Simpl-Open enforces authorization before cluster selection or use · Defined
