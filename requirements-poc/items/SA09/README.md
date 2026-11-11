> **Simpl-Open requirements, proof of concept.** One business process and the items under it. New here? [Start here](../../README.md) · [What the levels mean](../../levels.md) · [All items](../../all-items.md) · [By business process](../../processes.md)

Folder: [items](../README.md) / **SA09**

# SA09 - Data Provider manages consent assertions

SA09 is a supporting activity for data that contains personal data. A consent assertion records whether the consent of the data subjects is still valid for a data set. Its status decides whether Consumers may use the data.

There are two types. A resource assertion applies to all Consumers of a resource description. A contract assertion is created during the contract negotiation and applies to the Consumer of that one contract. Both start as active.

When a data subject withdraws consent, the Data Provider withdraws the assertion, and the Consumers are told that the data set is no longer available. The withdrawal can be temporary, for example while the Provider removes the personal data. When the consent basis is back, the Provider reactivates the assertion and the Consumers are told that a new version is available. Every change needs a reason and is audited. The Provider can do this in the user interface or through an API from its own consent platform.

SA09 is used in BP05B (a resource description can be linked to a consent assertion), BP07 (a contract assertion is created when the contract is established) and BP09A (the status is checked before consumption).

Read more on the [public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) and the [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions).

## Who takes part

- Data Provider, through users with the role to manage consent assertions

## Before it starts

- The Data Provider is onboarded (BP03A).

## Steps

The steps as the public process page names them. The items in this folder have no field for the step, so the tree below is ordered by item, not by step.

- **SA09.01 Create resource consent assertion**
- **SA09.02 Withdraw consent assertion**: the status becomes WITHDRAWN.
- **SA09.03 Reactivate consent assertion**: back to ACTIVE, with a reason.
- **SA09.04 Notify consent assertion status change**: all Consumers bound to the assertion are informed.

## What this folder holds

The L0 item [SIMPL-26074](L0-SIMPL-26074/README.md) stands for the supporting activity. Under it are 2 high-level requirements (L1). Each has its detailed requirements (L2), the Epics that deliver them and the user stories that build them. In total **37 items:** 1 L0, 2 L1, 4 L2, 6 Epic, 24 User story.

| L1 | High-level requirement | Items under it |
| --- | --- | --- |
| [SIMPL-26083](L0-SIMPL-26074/L1-SIMPL-26083/README.md) | Data Provider manages consent assertion status changes | 7 |
| [SIMPL-27082](L0-SIMPL-26074/L1-SIMPL-27082/README.md) | Data Provider manages consent assertions for shared data resources | 27 |

## Tree

Every item, with its level and title. User stories also show how far their development is: Planned, In development or Developed.

- [SIMPL-26074](L0-SIMPL-26074/README.md) L0 · SA09 - Data Provider manages consent assertions
  - [SIMPL-26083](L0-SIMPL-26074/L1-SIMPL-26083/README.md) L1 · Data Provider manages consent assertion status changes
    - [SIMPL-27195](L0-SIMPL-26074/L1-SIMPL-26083/L2-SIMPL-27195/README.md) L2 · Data Provider withdraws and reactivates consent assertions
      - [SIMPL-27206](L0-SIMPL-26074/L1-SIMPL-26083/L2-SIMPL-27195/Epic-SIMPL-27206/README.md) Epic · Data Provider withdraws and reactivates consent assertions
        - [SIMPL-28591](L0-SIMPL-26074/L1-SIMPL-26083/L2-SIMPL-27195/Epic-SIMPL-27206/Story-SIMPL-28591/README.md) User story · [API] Data Provider updates consent assertion status · Developed
        - [SIMPL-28592](L0-SIMPL-26074/L1-SIMPL-26083/L2-SIMPL-27195/Epic-SIMPL-27206/Story-SIMPL-28592/README.md) User story · [API] Data Provider returns consent assertion status · Developed
        - [SIMPL-28599](L0-SIMPL-26074/L1-SIMPL-26083/L2-SIMPL-27195/Epic-SIMPL-27206/Story-SIMPL-28599/README.md) User story · [UI] Data Provider updates consent assertion status from consent assertion details page · Developed
        - [SIMPL-29116](L0-SIMPL-26074/L1-SIMPL-26083/L2-SIMPL-27195/Epic-SIMPL-27206/Story-SIMPL-29116/README.md) User story · [UI] Data Provider updates consent assertion status from consent assertion list · Developed
      - [SIMPL-27207](L0-SIMPL-26074/L1-SIMPL-26083/L2-SIMPL-27195/Epic-SIMPL-27207/README.md) Epic · Data Provider notifies Consumers upon consent assertion status change
  - [SIMPL-27082](L0-SIMPL-26074/L1-SIMPL-27082/README.md) L1 · Data Provider manages consent assertions for shared data resources
    - [SIMPL-27154](L0-SIMPL-26074/L1-SIMPL-27082/L2-SIMPL-27154/README.md) L2 · Data Provider creates resource consent assertions for shared data resources
      - [SIMPL-27203](L0-SIMPL-26074/L1-SIMPL-27082/L2-SIMPL-27154/Epic-SIMPL-27203/README.md) Epic · Data Provider creates a resource consent assertion
        - [SIMPL-28589](L0-SIMPL-26074/L1-SIMPL-27082/L2-SIMPL-27154/Epic-SIMPL-27203/Story-SIMPL-28589/README.md) User story · [API] Data Provider creates a resource consent assertion · Developed
        - [SIMPL-28598](L0-SIMPL-26074/L1-SIMPL-27082/L2-SIMPL-27154/Epic-SIMPL-27203/Story-SIMPL-28598/README.md) User story · [UI] Data Provider creates a resource consent assertion · Developed
        - [SIMPL-28808](L0-SIMPL-26074/L1-SIMPL-27082/L2-SIMPL-27154/Epic-SIMPL-27203/Story-SIMPL-28808/README.md) User story · [API][IMPROVEMENT] Simpl-Open user to retrieve participant details · Developed
    - [SIMPL-27155](L0-SIMPL-26074/L1-SIMPL-27082/L2-SIMPL-27155/README.md) L2 · Data Provider creates contract consent assertions for shared data resources
      - [SIMPL-27204](L0-SIMPL-26074/L1-SIMPL-27082/L2-SIMPL-27155/Epic-SIMPL-27204/README.md) Epic · Data Provider creates a contract consent assertion
        - [SIMPL-28590](L0-SIMPL-26074/L1-SIMPL-27082/L2-SIMPL-27155/Epic-SIMPL-27204/Story-SIMPL-28590/README.md) User story · [API] Data Provider creates a contract consent assertion · Developed
    - [SIMPL-27156](L0-SIMPL-26074/L1-SIMPL-27082/L2-SIMPL-27156/README.md) L2 · Data Provider consults and updates existing consent assertions
      - [SIMPL-27205](L0-SIMPL-26074/L1-SIMPL-27082/L2-SIMPL-27156/Epic-SIMPL-27205/README.md) Epic · Data Provider consults and updates consent assertions
        - [SIMPL-28593](L0-SIMPL-26074/L1-SIMPL-27082/L2-SIMPL-27156/Epic-SIMPL-27205/Story-SIMPL-28593/README.md) User story · [API] Data provider updates consent assertion · Developed
        - [SIMPL-28594](L0-SIMPL-26074/L1-SIMPL-27082/L2-SIMPL-27156/Epic-SIMPL-27205/Story-SIMPL-28594/README.md) User story · [API] Data Provider lists and filters consent assertions · Developed
        - [SIMPL-28595](L0-SIMPL-26074/L1-SIMPL-27082/L2-SIMPL-27156/Epic-SIMPL-27205/Story-SIMPL-28595/README.md) User story · [API] Data provider retrieve the details of a consent assertion · Developed
        - [SIMPL-28600](L0-SIMPL-26074/L1-SIMPL-27082/L2-SIMPL-27156/Epic-SIMPL-27205/Story-SIMPL-28600/README.md) User story · [UI] Data Provider updates consent assertion · Developed
        - [SIMPL-28601](L0-SIMPL-26074/L1-SIMPL-27082/L2-SIMPL-27156/Epic-SIMPL-27205/Story-SIMPL-28601/README.md) User story · [UI] Data Provider lists and filters consent assertions · Developed
        - [SIMPL-28602](L0-SIMPL-26074/L1-SIMPL-27082/L2-SIMPL-27156/Epic-SIMPL-27205/Story-SIMPL-28602/README.md) User story · [UI] Data provider shows the details of a consent assertion · Developed
        - [SIMPL-29955](L0-SIMPL-26074/L1-SIMPL-27082/L2-SIMPL-27156/Epic-SIMPL-27205/Story-SIMPL-29955/README.md) User story · [API][IMPROVEMENT] Data provider configures the maximum amount of metadata key-value pairs for a consent assertion · Developed
        - [SIMPL-30219](L0-SIMPL-26074/L1-SIMPL-27082/L2-SIMPL-27156/Epic-SIMPL-27205/Story-SIMPL-30219/README.md) User story · [UI][IMPROVEMENT] Add creation date and update date to consent assertion list · Developed
      - [SIMPL-30295](L0-SIMPL-26074/L1-SIMPL-27082/L2-SIMPL-27156/Epic-SIMPL-30295/README.md) Epic · Data provider manages consent assertions - Improvement
        - [SIMPL-30081](L0-SIMPL-26074/L1-SIMPL-27082/L2-SIMPL-27156/Epic-SIMPL-30295/Story-SIMPL-30081/README.md) User story · [API][IMPROVEMENT] Allow user responsible for self description to read and update the status of a resource consent assertion · Developed
        - [SIMPL-30214](L0-SIMPL-26074/L1-SIMPL-27082/L2-SIMPL-27156/Epic-SIMPL-30295/Story-SIMPL-30214/README.md) User story · [UI][IMPROVEMENT] Allow user responsible for self description to read and update the status of a resource consent assertion · Developed
        - [SIMPL-30297](L0-SIMPL-26074/L1-SIMPL-27082/L2-SIMPL-27156/Epic-SIMPL-30295/Story-SIMPL-30297/README.md) User story · [API][IMPROVEMENT] Allow user responsible for contract management to read and update the status of a contract consent assertion · Developed
        - [SIMPL-30298](L0-SIMPL-26074/L1-SIMPL-27082/L2-SIMPL-27156/Epic-SIMPL-30295/Story-SIMPL-30298/README.md) User story · [UI][IMPROVEMENT] Allow user responsible for contract management to read and update the status of a contract consent assertion · Developed
        - [SIMPL-30300](L0-SIMPL-26074/L1-SIMPL-27082/L2-SIMPL-27156/Epic-SIMPL-30295/Story-SIMPL-30300/README.md) User story · [API][IMPROVEMENT] User responsible for self description gets catalogue UI filter url in a resource consent assertion · Developed
        - [SIMPL-30310](L0-SIMPL-26074/L1-SIMPL-27082/L2-SIMPL-27156/Epic-SIMPL-30295/Story-SIMPL-30310/README.md) User story · [UI][IMPROVEMENT] User responsible for self description views catalogue UI link in a resource consent assertion · Developed
        - [SIMPL-30311](L0-SIMPL-26074/L1-SIMPL-27082/L2-SIMPL-27156/Epic-SIMPL-30295/Story-SIMPL-30311/README.md) User story · [API][IMPROVEMENT] User responsible for managing contracts gets contract UI filter url in a contract consent assertion · Developed
        - [SIMPL-30312](L0-SIMPL-26074/L1-SIMPL-27082/L2-SIMPL-27156/Epic-SIMPL-30295/Story-SIMPL-30312/README.md) User story · [UI][IMPROVEMENT] User responsible for managing contracts gets views contract UI filter url in a contract consent assertion · Developed
