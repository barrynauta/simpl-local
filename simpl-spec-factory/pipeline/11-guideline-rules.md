# Step 11: guideline rules

Turns the fixed guidelines (API, coding, architecture, data modelling, security, DevSecOps, the NFRs that point to them, OWASP) into numbered, checkable rules. It runs once per frozen guideline set, not per component. Later steps refer to the rule ids: step 21 checks the API rules on the OpenAPI, step 50 gets the rules as its brief, step 70 reports per rule.

**Runs in a fresh agent.** It needs judgement (what is a rule, what is explanation), so it is not a script.

---

You extract **rules** from a set of guideline documents, for a spec-driven software factory. Nothing is built or checked in this step. Your output is the list that later steps check against.

## Input (read-only)

`{FACTORY}/guidelines/sources/`: the guideline documents, and `{FACTORY}/guidelines/README.md` for where each comes from. Read all of them. You may use your own knowledge of public standards a document points to (for example the OWASP Top 10 version that NFR14 names), and nothing else. No web access.

## What to write

### 1. `{FACTORY}/guidelines/rules.json`

One object per rule:

| Field | Content |
|---|---|
| `id` | prefix plus a two-digit number: `API-`, `CODE-`, `ARCH-` (principles and patterns), `DATA-`, `SEC-` (security), `OPS-` (DevSecOps), `UX-`, `OWASP-`, `HB-` (handbook, when present). Numbers follow the order in the source, so a rerun on the same sources gives the same ids |
| `source` | file and section heading, for example `api-guidelines.md#Error handling` |
| `quote` | the sentence or sentences the rule comes from, word for word |
| `rule` | the rule as one plain statement that can be true or false for a given artefact |
| `level` | `MUST`, `SHOULD` or `MAY`, from the wording of the source (must, shall, required = MUST; should, recommended = SHOULD; may, can = MAY). If the source gives no strength, `SHOULD`, and say so in `notes` |
| `applies_to` | one or more of `api-spec`, `code`, `tests`, `ui`, `config`, `pipeline`, `docs`, `data-model`, `process` |
| `check` | `tool`, `test` (a runtime or black-box test), `review` (a person or an agent reads), or `none` (cannot be checked on an artefact) |
| `check_hint` | how to check it in one line: the tool and rule (for example Spectral, Semgrep, ArchUnit, a linter), the test, or what the reviewer looks for |
| `steps` | which factory steps use it: `21` (API validation), `50` (code writer brief), `70` (verification) |
| `notes` | anything unclear: a vague word, a conflict with another rule (name its id), a rule that depends on a choice not made |

### 2. `{FACTORY}/guidelines/rules.md`

The same rules as tables, one section per prefix, with id, level, rule, applies to, check. Then three short lists:

- **Not a rule:** each section of the sources that you read and did not turn into a rule (explanation, background, examples), so a reader can see nothing was skipped.
- **Conflicts and overlaps:** rules that contradict or repeat each other across documents.
- **Vague rules:** rules that cannot be checked as written, and what would make them checkable. These are findings for the guideline owners.

Finish with totals: rules per prefix, per level, per check type.

## Rules for this step

- One rule per checkable statement. Split a sentence that says two things.
- Do not invent rules the sources do not state. Good practice that is missing is not a rule; list it under "Not covered" at the end of `rules.md` if it matters for a build.
- Keep the source's meaning. Do not make a SHOULD into a MUST.
- Positive tone: describe what is unclear, not who wrote it.
- Plain English, short sentences, no em-dashes or en-dashes.

## Final report (under 250 words)

Totals, the main conflicts and vague rules, and anything in the sources you could not place.
