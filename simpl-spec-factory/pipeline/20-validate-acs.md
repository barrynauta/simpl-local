# Step 20: validate the acceptance criteria

Checks every live acceptance criterion from the step 12 inventory, per AC id. The output is two things: findings for the spec owners (collected in step 29), and the list of **blockers** that step 30 must decide before anything is built. A blocker left open is a guess by the test writer and the code writer, and two writers guess differently.

Two parts:

| Part | Who | Reads | Writes |
|---|---|---|---|
| 20a | script [`check_acs.py`](./check_acs.py) | `<component>/inventory/inventory.json` | `<component>/validation/ac-checks.json`, `ac-checks.md` |
| 20b | fresh agent, brief below | `<component>/specs/`, `<component>/inventory/`, `<component>/validation/ac-checks.*` | `<component>/validation/ac-findings.json`, `ac-review.md` |

**Gate:** step 30 starts only when every blocker of 20b has a decision row or is marked as accepted risk.

## The checks

### 20a: mechanical (script)

| Code | Check |
|---|---|
| AC-S01, S02, S03 | no Given, no When, no Then |
| AC-S04 | more than one action in the When part (an And between When and Then, or two Whens) |
| AC-S05 | more than 5 outcomes in the Then part |
| AC-S06 | struck clauses inside a live criterion |
| AC-S07 | an AC number used twice in one story |
| AC-S08 | a story without criteria |
| AC-S09 | a criterion without a number in the story (its id depends on the order) |
| AC-S10 | text inside a criterion outside the clauses (notes, tables, lists) |
| AC-S11 | a story marked [Replaced] that is still live |
| AC-S12 | two criteria in one story with nearly the same text |
| AC-W01 | a vague word (appropriate, properly, securely, quickly, relevant, ...) |
| AC-W02 | an open placeholder (TBD, TBC, to be defined, a question mark) |
| AC-W03 | optional or open wording outside Given (and/or, may, might, should, either) |
| AC-W05 | a limit or time without a value (timeout, within, maximum, retention, ...) |

A 20a hit is a pointer, not a verdict. "Securely" in a Then is a real gap; "valid" with the rules next to it is not. 20b decides.

### 20b: judgement (agent)

| Code | Check | Question |
|---|---|---|
| AC-C01 | **Completeness** | Does the story cover the error and edge cases a builder needs: wrong or missing input, no token (401), no role (403), not found, conflict, an unavailable dependency, empty results, limits? Use the links: an operation with error responses but no criterion for them is a gap. |
| AC-C02 | **Testability** | Is the outcome observable from outside (a status, a field, a screen element, a message), and does it say what counts as a pass? "The contract is stored securely" has no oracle. |
| AC-C03 | **Ambiguity** | Can two careful readers build two different things from it? Name both readings. |
| AC-C04 | **Consistency** | Does it use the same names for the same things as the other stories, the API and the business process (actors, statuses, objects; see the word pairs in `inventory-review.md`)? Does it contradict another criterion? |
| AC-C05 | **Correctness against the API and the process** | Does it ask for something the OpenAPI does not offer or forbids (a method, a status code, a field), or that the business process orders differently? |
| AC-C06 | **Scope** | Is it about this component? Does a replaced story still add a requirement its replacement lacks? |
| AC-C07 | **Cross-cutting** (once per component) | Questions no single criterion raises, but every build must answer: the order of the checks when several errors combine (no token, no role, wrong media type, bad body, not found, conflict); what happens when each dependency is down or slow; content negotiation (406, 415); the formats of signatures, credentials and keys; how content from another party is made safe before it is shown or stored; limits (sizes, page sizes, timeouts). Raise a finding for each one the specs leave open. (Added 17 Nov 2026: these were most of what step 30c found missing.) |

Severity:

- **blocker:** a builder or tester cannot proceed without a decision (two readings with different behaviour, a contradiction, an outcome with no oracle on a main path);
- **major:** the build can proceed on a sensible default, but the default should be confirmed (missing error case, unnamed limit);
- **minor:** wording, numbering, duplicates, no effect on behaviour.

## 20b: brief for a fresh agent

---

You validate the **acceptance criteria** of one component for a spec-driven software factory experiment. A script has already done the mechanical checks. You do the checks that need judgement. You do not fix the criteria and you do not decide: you state what is wrong or open, with evidence, and the question a spec owner has to answer.

### Input (read-only)

- `{FACTORY}/{COMPONENT}/specs/`: stories, OpenAPI files, business processes, `README.md`.
- `{FACTORY}/{COMPONENT}/inventory/`: `inventory.json` (every AC with its id and clauses), `links.json` (AC to operation and to process requirement), `inventory-review.md` (coverage lists and the vocabulary with word pairs).
- `{FACTORY}/{COMPONENT}/validation/ac-checks.json` and `ac-checks.md`: the mechanical findings of step 20a.
- This brief, including the table of checks above it in `{FACTORY}/pipeline/20-validate-acs.md`.

Read nothing else: no spec digest, no tests, no builds, no findings of earlier runs, no other repositories, no web.

### What to do

1. Go through the stories one by one. For each live AC, apply AC-C02, AC-C03 and AC-C05. For each story as a whole, apply AC-C01 and AC-C06. Across stories, apply AC-C04. For the component as a whole, apply AC-C07.
2. For each 20a hit, say in one word whether it matters: `real` (a finding of its own or part of one of yours) or `harmless` (and why, in a few words). Harmless hits do not go into the findings.
3. Write each finding once, at the right level: an AC id, a story key, or `cross` with the ids involved.

### What to write

`{FACTORY}/{COMPONENT}/validation/ac-findings.json`: a list of

```json
{"id": "F-001", "target": "SIMPL-31053/AC2", "check": "AC-C03", "severity": "blocker",
 "finding": "one or two sentences", "evidence": "the words from the source, with their ids",
 "question": "the question for the spec owner", "options": ["reading A", "reading B"],
 "from_20a": ["AC-W01"]}
```

`options` only when there are readings to choose from. `from_20a` lists the 20a codes this finding confirms, if any.

`{FACTORY}/{COMPONENT}/validation/ac-review.md`:

- **Summary:** totals per check and severity, and the 5 findings with the most impact on a build.
- **Blockers:** every blocker, one line each, with its question. This is the list step 30 has to close.
- **Per story:** a short table per story: AC id, findings (ids), severity.
- **Cross-story:** consistency findings (AC-C04) together.
- **20a triage:** per 20a code, how many hits were real and how many harmless, with one example of a harmless one.
- **Coverage:** the stories with the most and the fewest findings, and the operations or process requirements that no AC covers (from the links), as a reminder, without repeating step 12b.

### Rules

- Evidence first: quote the words and cite the ids. No finding without evidence.
- Judge the specification, not the people. Positive tone: say what a builder needs, not what someone missed. Roles, not names.
- Do not invent requirements. A missing error case is a finding only if a builder would have to guess the behaviour.
- Plain English, short sentences, no em-dashes or en-dashes.

### Final report (under 300 words)

Totals per check and severity, the number of blockers, the top 5 findings, and the 20a triage in numbers.
