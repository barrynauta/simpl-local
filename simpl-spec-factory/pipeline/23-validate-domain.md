# Step 23: validate the domain language (CDM, API, stories)

Checks that a component speaks one language: the CDM that belongs to each API, the API schemas and enums, and the words of the stories and the business process. The rules are `DDD-01` to `DDD-15` (a factory addition, see [`../guidelines/sources/factory/ddd.md`](../guidelines/sources/factory/ddd.md)) and the CDM rules `DATA-` and `API-01` from the Simpl guidelines. The main output is a **draft glossary** per component: one term per concept, its definition, its CDM class and API schema, and the synonyms to retire. Step 30 turns open terms into decisions; steps 40 to 60 use the glossary as the names to build and test with.

## Where the CDMs come from (step 10 and 12)

The CDMs are not part of a component's frozen `specs/`: one snapshot of the CDM register serves all components.

| Part | Who | Writes |
|---|---|---|
| Collect | [`collect_cdms.py`](./collect_cdms.py), config [`../cdm/register.yaml`](../cdm/register.yaml) | `cdm/cdm-index.md` (the register), one `cdm/<service-repo>.md` per CDM, `cdm/SOURCES.json` (branch, pinned commit, sha256, or the error) |
| Extract | [`extract_cdm.py`](./extract_cdm.py) | `cdm/inventory.json`, `inventory.md`: classes, attributes, relations, value sets; ids `CDM:<file>:<Class>.<attribute>` |
| Map | each component's `sources.yaml`, key `cdm:` | which CDM describes which API, plus `context:` (CDMs of components it talks to) |

## Two parts

| Part | Who | Reads | Writes |
|---|---|---|---|
| 23a | script [`check_domain.py`](./check_domain.py) | `sources.yaml`, `specs/api/`, `inventory/inventory.json`, `../cdm/inventory.json`, `../guidelines/rules.json` | `validation/domain-checks.json`, `domain-checks.md` |
| 23b | fresh agent, brief below | the above, plus the CDM files, `inventory-review.md` and `validation/ac-findings.json` | `validation/domain-findings.json`, `domain-review.md`, `glossary.md` |

23a checks, per API: is there a CDM and is it approved (DATA-01); does the CDM point to this API (DDD-06); API schemas against CDM classes, with wrappers set apart (DDD-06, DDD-07); properties against attributes (DDD-08); one name per identity (DDD-09); enum values against CDM value sets and against the statuses the ACs quote (DDD-04); CDM class names in the stories and the process (DDD-01); naming inside the CDM (DATA-09). Health and error schemas are left out. A hit is a pointer.

**Gate:** like step 20, every blocker goes to step 30. A term with two live names on a main path is a blocker: the test writer and the code writer will each pick one.

## 23b: brief for a fresh agent

---

You validate the **domain language** of one component for a spec-driven software factory experiment: does it use one name per concept across the stories, the business process, the API and the CDM, and do the API's entities match the CDM? A script has done the name matching. You judge, and you draft the glossary. You do not decide which term wins: you propose, with reasons, and leave the choice to the spec owners.

### Input (read-only)

- `{FACTORY}/{COMPONENT}/sources.yaml` (which CDM belongs to which API), `{FACTORY}/{COMPONENT}/specs/` (stories, OpenAPI, business process).
- `{FACTORY}/{COMPONENT}/inventory/`: `inventory.json`, `links.json`, `inventory-review.md` (vocabulary and word pairs).
- `{FACTORY}/{COMPONENT}/validation/`: `domain-checks.json` and `.md` (step 23a), `ac-findings.json` (step 20b; do not repeat its findings, refer to them by id).
- `{FACTORY}/cdm/`: the CDM files named in `sources.yaml` (under `cdm:` and `context:`), `inventory.json`, `SOURCES.json`, `cdm-index.md`.
- `{FACTORY}/guidelines/rules.json`: the `DDD-` and `DATA-` rules, and `API-01`.
- This brief, with the text above it in `{FACTORY}/pipeline/23-validate-domain.md`.

Read nothing else: no spec digest, no tests, no builds, no findings of earlier runs, no other repositories, no web.

### What to do

1. **Triage 23a.** For each 23a hit: `real` or `harmless`, with a reason for harmless ones.
2. **Rule by rule.** For each `DDD-` rule and for `DATA-01`, `DATA-04`, `DATA-09` and `API-01`: met, partly met or not met for this component, with evidence (ids, quotes).
3. **Entities.** For each business object the APIs expose: its CDM class (or none), whether the CDM marks it as owned or external and whether that is right, its identity and the names it has, its states and the operations and ACs that change them (DDD-12).
4. **Boundaries.** Which concepts belong to other components (signer, wallet, issuer, connector, catalogue, ...)? Does the component's API use their terms instead of its own (DDD-14)? Are the external classes in the CDM the same as the classes in the context CDMs?
5. **Glossary.** Draft it: one row per concept.

### What to write

`{FACTORY}/{COMPONENT}/validation/glossary.md`: a table with term (the proposed one), definition (one line, from the specs, not invented), CDM class id, API schema(s) and field(s), story and AC ids that use it, synonyms found (with where), status (`agreed` when all sources already use one term, `proposed` when you chose among synonyms, `open` when the sources disagree on the meaning). Then the value sets: per entity, the states, and per state the name in the CDM, each API enum, the ACs and the UI.

`{FACTORY}/{COMPONENT}/validation/domain-findings.json`: findings in the format of step 20b (`id` D-001, `target`, `rule` instead of `check`, `severity` blocker, major or minor, `finding`, `evidence`, `question`, `options`, `from_23a`, and `see` for related 20b finding ids).

`{FACTORY}/{COMPONENT}/validation/domain-review.md`: summary (totals per rule and severity, the 5 findings with most impact), blockers with their questions, the rule-by-rule table, the entity table, boundaries, and the 23a triage in numbers.

### Rules

- Evidence first: quote and cite ids. No finding without evidence.
- Propose terms, do not decide them. Prefer the term the business process and the stories use most, then the CDM, then the API; say why.
- Judge the specification, not the people. Positive tone. Roles, not names.
- Plain English, short sentences, no em-dashes or en-dashes.

### Final report (under 300 words)

Totals per rule and severity, blockers, the number of glossary terms by status, the top 5 findings, and the 23a triage in numbers.
