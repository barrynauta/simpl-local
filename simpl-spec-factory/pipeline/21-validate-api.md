# Step 21: validate the API definitions

Checks each OpenAPI file of a component against the API guidelines (the `API-` rules of step 11), and checks the coverage between operations and stories in both directions. Step 20 looked at the API through the acceptance criteria; this step looks at the API itself.

| Part | Who | Reads | Writes |
|---|---|---|---|
| 21a | script [`check_api.py`](./check_api.py) | `sources.yaml`, the OpenAPI entry files, `inventory/`, `../guidelines/rules.json` | `validation/api-checks.json`, `api-checks.md` |
| 21b | fresh agent, brief below | the above, the OpenAPI files, the findings of 20b and 23b, the decisions of step 30 | `validation/api-findings.json`, `api-review.md` |

21a implements every rule with check `tool` that applies to an API specification (37 rules: naming of paths, parameters, properties and headers; servers and versions; info, license and contact; security schemes; problem+json for errors; examples; parameter completeness), one function per rule id. A Spectral ruleset would do the same; a script keeps the rule ids in the findings without a translation layer and adds no npm dependency. Two coverage checks: **COV-01**, an operation no acceptance criterion covers (from the step 12b links; health and context-only APIs left out), and **COV-02**, an operation a story names that no OpenAPI entry file has.

In the pipeline order, step 21 comes before step 30. In the first run (17 Nov 2026) it ran after step 30; its blockers then go back to step 30 as a short addition, and the gate reads `api-findings.json` too.

## 21b: brief for a fresh agent

---

You validate the **API definitions** of one component for a spec-driven software factory experiment. A script has checked the OpenAPI files against the tool-checkable API guideline rules and listed coverage gaps. You judge those hits, check the rules a script cannot, and look at what the API means for a builder.

### Input (read-only)

- `{FACTORY}/{COMPONENT}/sources.yaml` and the OpenAPI files under `{FACTORY}/{COMPONENT}/specs/api/` (or the file named under `dest:`).
- `{FACTORY}/{COMPONENT}/validation/api-checks.json` and `.md` (step 21a), `ac-findings.json` (20b), `domain-findings.json` (23b).
- `{FACTORY}/{COMPONENT}/decisions/decisions.json` (step 30).
- `{FACTORY}/{COMPONENT}/inventory/inventory.json` and `links.json`.
- `{FACTORY}/guidelines/rules.json`: the `API-` rules, and the `SEC-` and `OWASP-` rules that apply to `api-spec`.
- This brief, in `{FACTORY}/pipeline/21-validate-api.md`.

Read nothing else: no spec digest, no tests, no builds, no other components, no web.

### What to do

1. **Triage 21a.** For each finding: `real` or `harmless`, with a reason for harmless ones (for example a JSON-LD `@id` that the standard requires). A real one becomes a finding of yours, or part of one.
2. **Review rules.** Check each `API-`, `SEC-` and `OWASP-` rule with check `review` that applies to `api-spec`, for each API: met, partly met, not met, or not applicable, with evidence.
3. **Coverage.** For each COV-01 operation: is it an operation the stories forgot (a story is missing), an operation that should not be there (retire it), or internal plumbing (fine)? For each COV-02: is it already a finding of 20b or decided in step 30? Only new points become findings.
4. **For a builder.** Anything in the API that two builders would implement differently and that 20b and 23b did not raise: missing response codes for cases the decisions define, schemas that allow what the decisions forbid, a security scheme that does not fit the decided roles. Refer to the decision ids where they apply.

### Severity

- **blocker:** a builder cannot implement or a tester cannot test without a decision, and no existing finding or decision covers it.
- **major:** a MUST rule not met, or a SHOULD rule not met in a way that affects clients (error format, security, versioning), or an uncovered operation that needs a story.
- **minor:** naming, examples, descriptions, other SHOULD rules.

### What to write

`{FACTORY}/{COMPONENT}/validation/api-findings.json`: findings in the format of step 20b, with ids `P-001` and up, `rule` instead of `check`, and `from_21a` (the A- ids it confirms) and `see` (related F-, D- and decision ids).

`{FACTORY}/{COMPONENT}/validation/api-review.md`: summary (totals per rule and severity, the 5 findings with most impact), blockers with their questions, the rule-by-rule table per API, the coverage table (each COV-01 and COV-02 with its class), and the 21a triage in numbers.

### Rules

Evidence first: cite file, path and schema names, and rule ids. Do not repeat findings of 20b or 23b; refer to them. Positive tone, roles not names. Plain English, short sentences, no em-dashes or en-dashes.

### Final report (under 250 words)

Totals per severity, blockers, the coverage classes in numbers, the review rules not met, and the 21a triage in numbers.

---
