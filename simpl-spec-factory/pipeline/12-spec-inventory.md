# Step 12: spec inventory

Gives every piece of the specification a stable id, so the later steps can point at it: the validators (20 to 22) report per id, the decisions (30) cite ids, and the verification (70) builds the traceability matrix and the Gherkin coverage from them. This step **extracts**. It does not judge quality; that is step 20.

Two parts:

| Part | Who | Reads | Writes |
|---|---|---|---|
| 12a | script [`extract_inventory.py`](./extract_inventory.py) | `<component>/specs/` | `<component>/inventory/inventory.json`, `inventory.md` |
| 12b | fresh agent, brief below | `<component>/specs/`, `<component>/inventory/` | `<component>/inventory/inventory-review.md`, `links.json` |

## 12a: the script

`python3 pipeline/extract_inventory.py <component>` (for the Vocabulary Manager, the root candidate: `.`). It prints the totals. Rerun it whenever `specs/` changes; never edit its output by hand.

Ids:

| Item | Id | Example |
|---|---|---|
| Story | Jira key | `SIMPL-31053` |
| Acceptance criterion | key, `/AC`, the number the story gives | `SIMPL-31053/AC2` |
| AC without a number in the story | the next free number, in order, marked *derived* | `SIMPL-13345/AC3` (derived) |
| AC number used twice in a story | suffix | `SIMPL-31076/AC4.1`, `AC4.2` |
| API operation | API folder, method, path | `contract-manager-be:PATCH /contractOffers/{contractNegotiationId}` |
| Business process requirement | `BP` plus the number in the process | `BP7.2.1` |

What the script does: splits each story into its user story (As a, I want, So that) and its acceptance criteria with their Given/When/Then clauses, word for word. Headings like "Negative Scenarios:" become a group label, not a criterion. Criteria struck through in Jira (`-text-`) are kept but left out of the live count. It reads every OpenAPI operation with its responses and security, every detailed requirement of the business processes, and the story keys and API paths that the texts name literally.

What it flags as extraction issues: a story without criteria, a criterion without a Given, When or Then, a criterion number used twice, a struck criterion, an operation without an operationId or responses, a process without detailed requirements, a gap in requirement numbers.

Operations are counted only in the files listed as `entry` in `sources.yaml`, when given; composed OpenAPI layouts (a root file with references, `-itb` copies, older versions) would otherwise count operations several times. Catalogue: 39 operations, not 103.

Known limits: derived ids depend on the order in the story, so if a story owner inserts a scenario, the derived ids after it shift. Single-file business processes (one markdown file, no folder) are not split into requirements.

## 12b: the review (brief for a fresh agent)

---

You review a **spec inventory** for a spec-driven software factory experiment. A script extracted it; you check it and add what a script cannot. You do not judge whether the specs are good. That comes later.

### Input (read-only)

`{FACTORY}/{COMPONENT}/specs/` (the stories, OpenAPI files, business processes and `README.md`) and `{FACTORY}/{COMPONENT}/inventory/` (`inventory.json`, `inventory.md`). Read nothing else. No web access.

### What to do

1. **Extraction issues.** For each issue in `inventory.json`, look at the source and say whether it is a parser error (the source is fine, the script misread it) or a real property of the spec (for example an AC with no Then). For parser errors, say what the script should do instead.
2. **Spot check.** Pick 10 stories at random and compare their criteria in `inventory.json` with the source, clause by clause. Report any criterion that is missing, split wrongly, merged, or has changed words.
3. **Links.** For each live acceptance criterion, propose the API operations and the business process requirements it relates to, and for each operation and requirement the criteria that cover it. Write them to `links.json` as `{"from": "<id>", "to": "<id>", "basis": "named" | "inferred", "reason": "<one line>"}`. `named`: the text names it (a path, a key). `inferred`: you matched by meaning; give the words that made you link them. Do not link on a vague topic match. When unsure, leave the link out.
4. **Vocabulary.** List the actors and roles, the statuses (for example of a contract or a vocabulary) and the main data objects the specs use, each with the ids where they appear. Where two words seem to mean the same thing (for example "usage contract offer" and "contract offer"), list them as a pair. Do not decide which is right.

### What to write

`{FACTORY}/{COMPONENT}/inventory/inventory-review.md`:

- **Extraction issues:** a table: issue, parser error or real, what to change.
- **Spot check:** the 10 stories, and per story OK or the difference.
- **Coverage from the links:** criteria with no operation and no requirement; operations with no criterion; requirements with no criterion. Just the lists; no verdict.
- **Vocabulary:** actors, statuses, data objects, and the pairs of words that may mean the same.
- Totals: links named and inferred.

`{FACTORY}/{COMPONENT}/inventory/links.json`: the links of point 3.

### Rules

- Extract and link, do not evaluate. "AC2 has no Then" is a fact; "AC2 is badly written" is not for this step.
- Cite ids, not page numbers or line numbers.
- Positive tone. Roles, not names.
- Plain English, short sentences, no em-dashes or en-dashes.

### Final report (under 250 words)

How many extraction issues were parser errors and which, the spot check result, the coverage lists in numbers, and the main word pairs.
