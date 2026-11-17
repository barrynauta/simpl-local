# Step 30: decisions, with the gate

Closes what steps 20 to 23 leave open, so the test writer and the code writer resolve every gap the same way. It replaces the spec digest of run 1 (`01-spec-digest.md`): that step found the gaps and decided them in one go; now the validators find them, and this step only decides.

Three parts:

| Part | Who | Reads | Writes |
|---|---|---|---|
| 30a | fresh agent, brief below | specs, inventory, the findings of 20b and 23b, the glossary, the rules | `<component>/decisions/decisions.json`, `decisions.md`, `glossary.md` |
| 30b | script [`check_gate.py`](./check_gate.py) | the findings and `decisions.json` | `<component>/decisions/gate.md`; exit code 1 if the gate is not passed |
| 30c | fresh agent, brief below | the old spec digest of run 1 and the outputs of 20, 23 and 30a | `<component>/decisions/compare-run1.md` |

**The gate:** every blocker of 20b and 23b is closed by a decision or marked as an accepted risk, and every proposed or open glossary term has a chosen term. Majors should get a default; the gate lists the ones without one but does not fail on them. It also warns when the decisions mix error body formats, or when two decisions close the same finding (added 17 Nov 2026, after the Catalogue decisions used two error formats). Steps 40 to 60 start only when the gate passes.

30a must not read the old digest, so 30c can measure what the validators found on their own.

## 30a: brief for a fresh agent

---

You take the **decisions** for one component of a spec-driven software factory experiment. The specs have been read and checked: you get the findings (blockers, majors, minors) and a draft glossary. A test writer and a code writer will work from the specs plus your decisions, separately, and must build and test the same thing. Every gap you leave open, they fill differently.

The spec owners are not available in this experiment. So you decide, provisionally, and say for each decision what the owner should confirm. Prefer the reading the sources support best; where they are silent, prefer what is simplest to build and test, and say so.

### Input (read-only)

- `{FACTORY}/{COMPONENT}/specs/` (stories, OpenAPI, business process), `sources.yaml`.
- `{FACTORY}/{COMPONENT}/inventory/` (`inventory.json`, `links.json`, `inventory-review.md`).
- `{FACTORY}/{COMPONENT}/validation/`: `ac-findings.json`, `domain-findings.json`, `glossary.md`, `spec-feedback.md`, and the review files.
- `{FACTORY}/cdm/`: the CDM files named in `sources.yaml`.
- `{FACTORY}/guidelines/rules.json` (the `API-`, `DDD-` and `SEC-` rules apply to the decisions).
- This brief, in `{FACTORY}/pipeline/30-decisions.md`.

Read nothing else. In particular, never read any `spec-digest*.md`, `findings.md`, `tests/`, `generated/` or `experiments/`: this step must be independent of the earlier runs.

### What to do

1. **Source hierarchy.** Write down which source wins when they disagree (for example: decisions, then acceptance criteria for behaviour, then the OpenAPI for the wire format, then technical notes, then the business process), and why. Apply it everywhere.
2. **Close every blocker** of `ac-findings.json` and `domain-findings.json`: one decision may close several findings. If you cannot decide (for example it needs a fact nobody wrote down), mark it `accepted risk` and say what the writers should assume.
3. **Give a default for every major** where a builder would otherwise guess. Majors that need no decision (for example a wording issue) get none.
4. **Decide the glossary:** for each term with status `proposed` or `open`, the term to use in the build and the tests, and the synonyms to retire. Include the states of the main entities with one name per state, and the mapping where the API must keep another value.
5. **Environment choices** the two writers must share and the specs do not fix (identity provider users and roles, ports, stubs for external services). Keep them few.
6. **Consistency.** Read your decisions together before you finish: one error body format for the whole component, the same status code for the same situation, one name per identity and per state, no two decisions that close the same finding in different ways. Fix what clashes.

### What to write

`{FACTORY}/{COMPONENT}/decisions/decisions.json`: a list of

```json
{"id": "D1", "topic": "short title", "closes": ["F-001", "D-004"], "decision": "what the writers do",
 "options": ["the options that were open"], "basis": "why: which source, which rule", "status": "follows the specs | provisional | accepted risk",
 "owner_question": "what the spec owner should confirm", "affects": ["api", "ui", "tests", "data", "events"]}
```

`follows the specs`: the sources already settle it once read correctly. `provisional`: a real choice between readings. `accepted risk`: not decidable, with the assumption to use.

`{FACTORY}/{COMPONENT}/decisions/decisions.md`: the source hierarchy, then the decisions grouped by theme as short tables (id, topic, decision, status, closes), then the environment choices, then the questions for the owners ordered by impact.

`{FACTORY}/{COMPONENT}/decisions/glossary.md`: the final glossary (term, definition, CDM class, API names, retired synonyms), and the state tables.

### Rules

- Every decision cites the finding ids it closes and its basis. No decision without a finding, except environment choices.
- Decide, do not defer: two writers must be able to act on it without asking.
- Positive tone, roles not names. Plain English, short sentences, no em-dashes or en-dashes.

### Final report (under 250 words)

Number of decisions by status, blockers closed and accepted as risk, majors with and without a default, glossary terms decided, and the 5 decisions with the most impact.

---

## 30c: brief for a fresh agent

---

You compare two ways of preparing the same specs for a build, for one component. **Run 1** (late October 2026): one person-led step read the specs and wrote a spec digest with numbered decisions (`D1`, `D2`, ...). **This run**: validators found the gaps (steps 20 and 23), and a separate step decided them (step 30a). The question: how much of the run 1 decision work did the validators find on their own, what did they miss, and what did they find that run 1 did not?

### Input (read-only)

- The run 1 digest: `{DIGEST}` (rows or headings `D1`, `D2`, ...). Decisions added in later runs are marked (for example "PSO run", "run 1.2", "added 4 Nov 2026"); count those separately.
- This run: `{FACTORY}/{COMPONENT}/validation/ac-findings.json`, `domain-findings.json`, `{FACTORY}/{COMPONENT}/decisions/decisions.json`.
- `{FACTORY}/{COMPONENT}/inventory/inventory.json` for ids.

Read nothing else.

### What to do

For each run 1 decision: is the same gap present in this run's findings (`found`: cite the finding ids), only in a step 30a decision without a finding (`decided only`), or absent (`missed`)? If found, do the two decisions agree, differ, or is the new one still open (`accepted risk`)? Some run 1 decisions are environment choices (store, identity provider, ports) or scope statements, not gaps: mark them `environment` or `scope` and leave them out of the found/missed count.

Then the reverse: findings of this run (blockers and majors) that match no run 1 decision (`new`).

### What to write

`{FACTORY}/{COMPONENT}/decisions/compare-run1.md`: a summary table (run 1 decisions by class; found, decided only, missed; agree, differ; new blockers and majors), the per-decision table (run 1 id, topic, class, this run's finding ids and decision id, agree or differ, one line why), the missed ones with a line on why the validators may have missed each, and the new ones.

### Rules

Evidence first: cite ids. Do not judge which run was right where they differ; say what each decided. Plain English, short sentences, no em-dashes or en-dashes.

### Final report (under 200 words)

The counts, and the 3 most telling misses and the 3 most telling new findings.

---

## 30a addendum: deciding findings that came later

When a step that belongs before step 30 runs after it (step 21 on 17 Nov 2026), its blockers and majors reopen the gate. A fresh agent then decides only those, with this brief:

---

You add decisions for one component of a spec-driven software factory experiment. The decisions in `{FACTORY}/{COMPONENT}/decisions/decisions.json` stand; new findings arrived in `{FACTORY}/{COMPONENT}/validation/api-findings.json` (step 21b). Run `python3 {FACTORY}/pipeline/check_gate.py {COMPONENT}` and read `decisions/gate.md`: it lists the blockers without a decision and the majors without a default.

Read the 30a brief above for the rules, the format and the statuses. Read `decisions.json`, `decisions.md`, `glossary.md`, `api-findings.json`, `api-review.md`, the OpenAPI files and `{FACTORY}/guidelines/rules.json`; nothing else (no spec digest, tests, builds or other components). Then:

1. Add a decision for each open blocker and a default for each major without one. Continue the numbering of `decisions.json`. Where an existing decision already settles the point, add the finding id to that decision's `closes` instead of a new decision.
2. Where a finding says the OpenAPI does not yet show a decision, the decision stands: record it as a change the API owner should make (`owner_question`), not as a new decision.
3. Add the new rows to `decisions.md` under a heading "Added after step 21 (17 Nov 2026)", and update the questions for the owners.
4. Run the gate again until it passes. Do not change the text of earlier decisions; if a new one overrides or refines an earlier one, say so in your report. The pipeline then marks the earlier one with `overridden_by` or `refined_by` (and `later_change`), so the writers read both together; the gate ignores overridden decisions in its consistency check.

Write only `decisions.json`, `decisions.md` and `gate.md`. Plain English, short sentences, no em-dashes or en-dashes. Report in under 150 words: decisions added, findings closed, conflicts with earlier decisions, gate result.

---
