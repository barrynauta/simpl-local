# What the levels mean

> Part of the [Simpl-Open requirements proof of concept](README.md).

Simpl-Open requirements are written top-down in five levels. Each level has its own purpose and its own way of writing. The definitions below are summarised from the Simpl Development Handbook V1.8, chapter "Requirements elicitation and management".

```mermaid
graph TD
    BP["Business process or supporting activity<br/>e.g. BP02A, SA09"] --> L0
    L0["L0 requirement<br/>the business process as a requirement"] --> L1
    L1["L1 requirement<br/>high-level requirement: WHAT"] --> L2
    L2["L2 requirement<br/>detailed requirement: WHAT"] --> EP
    EP["Epic<br/>deliverable, testable feature: WHAT"] --> US
    US["User story<br/>implementable work: HOW"]
```

## The levels

| Level | Describes | In one sentence | Written as |
| --- | --- | --- | --- |
| **Business process (BP) or supporting activity (SA)** | A business process | What the actors of a data space do, step by step, for example BP02A "Configure ID/Trust security solution". | A process page with steps such as BP02A.02. |
| **L0** | The business process as a requirement | The item that stands for one BP or SA, so lower levels can hang under it. | The BP name, for example "BP02A - Configure ID/Trust security solution". |
| **L1** | A high-level requirement | What Simpl-Open must allow an actor to do in one step, or a few related steps, of a business process. The public requirements site uses the same term, for example high-level requirement 2A.1. | Title: actor, verb, object. Text: "Simpl-Open shall allow ...". |
| **L2** | A detailed requirement | A concrete and testable function that makes part of an L1 real. The public requirements site uses the same term, for example detailed requirement 2A.1.1. | "Simpl-Open shall ..." for one actor and one function. |
| **Epic** | A deliverable feature | A testable feature seen from a business actor. It bridges the requirements (L2) and the implementation work (stories). | "As a ... I want ... so that ...", with acceptance criteria in Given / When / Then. |
| **User story** | Implementable work | A piece of work a team can build and test in a sprint, often split into a UI story and an API story. | "As a ... I want ... so that ...", with acceptance criteria. |

## Example from the handbook

| Level | Title |
| --- | --- |
| BP | Resource Publishing and Access Control |
| L1 | Provider defines and manages access policies for catalogue resources |
| L2 | Provider defines initial access policy for catalogue resources |
| L2 | Provider updates existing access policy for catalogue resources |
| Epic | Provider manages access policies for catalogue resources |
| User story | [UI] Provider creates access policy for catalogue resources |
| User story | [API] Provider creates access policy for catalogue resources |

## Linking rules

The handbook sets these rules. The [trace gaps](trace-gaps.md) page checks the items in this folder against them.

- An **L1** must be linked to **exactly one** business process.
- An **L2** must be linked to **exactly one** parent L1.
- An **Epic** must be linked to **at least one** L2.
- A **user story** sits under an Epic (level 4 under level 3 in the handbook hierarchy). The handbook does not state the rule as a "must".

An item that does not meet its rule is tagged **⚠ Trace gap** on its page.

## How user stories and acceptance criteria are written

The source writes them in many ways. This folder shows them all in the same layout. The words are kept, only the layout changes.

A user story has three lines:

**As a** Provider end user\
**I want to** remove contract clauses from a resource description via UI\
**So that** the new version of the resource description has the updated set of contract clauses

Acceptance criteria are a numbered list, one number per scenario. A scenario starts with **Given**. Each next step is on its own line:

1. **Given** I am authenticated as a Provider end user\
   **And** I am creating a new version of a resource description\
   **When** I reach the step 'Contract Clauses' in the wizard\
   **Then** the UI shows the contract clauses of the previous version

The keywords Given, When, Then, And and But are in bold, with a capital first letter. When the source writes a scenario as one sentence, it stays one sentence, with the keywords in bold.

## Status

Status shows where an item is in its life cycle. Cancelled items are left out of this folder. Some of the statuses you see:

| Status | Meaning |
| --- | --- |
| Defined, PBI Defined | Written and approved after elicitation. The handbook keeps the key fields stable from PBI Defined on. |
| In Progress | Being worked on. |
| Internal QA check | An internal review step. |
