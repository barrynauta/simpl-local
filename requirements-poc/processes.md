# By business process

> Part of the [Simpl-Open requirements proof of concept](README.md). New here? [Start here](README.md) · [What the levels mean](levels.md)

Every requirement should serve a business process (BP) or supporting activity (SA). This page starts from the process and shows the items under it. Items on the public process page that are not in this folder are listed too, so you can see what is missing from the export.

## BP02A - Configure ID/Trust security solution

The Governance Authority sets up identification and trust for its data space: identity attributes, onboarding procedure templates with validation rules, and security settings.

Public pages: [business-processes repository](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)

- [SIMPL-18783](items/BP02A/SIMPL-18783/README.md) L0 · BP02A - Configure ID/Trust security solution · In Progress
  - [SIMPL-10171](items/BP02A/SIMPL-18783/SIMPL-10171/README.md) L1 · Configure Onboarding Procedure Templates · In Progress · matches step BP02A.02 by title
    - *Not in this folder:* 2A.1.1, 2A.1.2, 2A.1.3 (detailed requirements)
  - *Not in this folder:* SIMPL-10192 L1 · Configure Dataspace Identity Attributes (2A.2)

## SA09 - Data Provider manages consent assertions

A Data Provider creates, withdraws and reactivates consent assertions on its data resources. Consumers are notified when the status changes.

Public pages: [business-processes repository](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions)

- [SIMPL-26074](items/SA09/SIMPL-26074/README.md) L0 · SA09 - Data Provider manages consent assertions · Defined
  - *Not in this folder:* SIMPL-26083 L1 · Data Provider manages consent assertion status changes (9.1)
  - *Not in this folder:* SIMPL-27082 L1 · Data Provider manages consent assertions for shared data resources (9.2)

## SA01 - Data orchestration used by a Participant

Providers and Consumers design, run and monitor repeatable data processing workflows.

Public pages: [business-processes repository](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa01-data-orchestration/README.md)

- *Not in this folder:* the L0 item for SA01
  - [SIMPL-30560](items/SA01/SIMPL-30560/README.md) L1 · Participant executes workflow on saved distribution configuration · Defined · ⚠ linked from the public SA01 page only, no recorded parent
    - *Not in this folder:* 1.6.1 (detailed requirement)

## ⚠ No business process

These items do not trace to any business process. See [trace gaps](trace-gaps.md).

- [SIMPL-31424](items/no-business-process/SIMPL-31424/README.md) L1 · Governance Authority maintains a contact email for each participant · Internal QA check
  - [SIMPL-31425](items/no-business-process/SIMPL-31424/SIMPL-31425/README.md) L2 · Governance authority registers participants' contact email · In Progress
    - [SIMPL-31426](items/no-business-process/SIMPL-31424/SIMPL-31425/SIMPL-31426/README.md) Epic · Applicant provides participant contact email during onboarding · In Progress
- [SIMPL-31853](items/no-business-process/SIMPL-31853/README.md) Epic · A Provider triggers the decommissioning of an infrastructure resource when revocation request is accepted · Defined
- [SIMPL-30641](items/no-business-process/SIMPL-30641/README.md) User story · A Provider removes contract clauses from a resource description via UI, resulting in a new version of the resource description · PBI Defined
- [SIMPL-31429](items/no-business-process/SIMPL-31429/README.md) Epic · Participant updates contact email after onboarding · 🔴 Cancelled
