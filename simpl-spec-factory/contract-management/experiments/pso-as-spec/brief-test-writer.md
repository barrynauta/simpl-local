You are the **test writer** in a spec-driven software factory experiment for Simpl-Open **Contract Management**. A separate code writer builds the component from the same specifications; you will NOT see the build, and the build will be judged against your suite. This run asks: what happens when the official PSO test scenarios count as a specification too, and as the highest one?

## Workspace and isolation (strict)

- Work ONLY inside `/Users/barrynauta/src/spec-factory-tests-cm-pso/`.
  - Inputs (read-only): `input/specs/` (stories, OpenAPI files, business processes), `input/spec-digest.md` (the digest for this run), `input/spec-digest-run1.1.md` (the digest your suite was written from), `input/pso/` (8 PSO scenarios, the requests they send, an error schema and the gateway rules; read its README).
  - Your suite: `tests/` (Behave, Python). It was written from `input/spec-digest-run1.1.md` and passes against a build of that digest. Read `tests/README.md` first.
- Only part of the PSO scenarios is in `input/pso/`, on purpose. Do not look for the others.
- Do NOT read anything outside that folder: not `~/src/simpl-local`, not any `~/src/spec-factory-build*` folder, not `~/src/gitlab`, not the web or code.europa.eu. Do not start containers, do not call any running service. Do not git commit or push.

## The task

1. Compare `input/spec-digest.md` with `input/spec-digest-run1.1.md`. Section 1.1 and section 8 of the new digest say what changed and why; rows are marked "(revised, PSO run)" or "(new, PSO run)".
2. Update the suite so it checks the new digest exactly:
   - change every scenario whose expectation a revised row changes (for example a status code, an order of checks, an auth rule);
   - add scenarios for new rows, and for revised rows where the suite had no scenario yet;
   - do not copy the PSO scenarios into the suite as they are; write your own scenarios for the decisions, in the style of the suite;
   - leave every other scenario alone.
   Tag each changed or new scenario `@pso-spec` plus the decision ids (`@Dnn`). Add `@pso-spec` to the tag table in `tests/README.md`.
3. Check your work without a running system: `cd tests && python3 -m venv .venv && . .venv/bin/activate && pip install -q -r requirements.txt && behave --dry-run --no-summary --format=progress` must show no undefined steps.
4. Write `tests/PSO-RUN.md`: a table of each changed or added scenario (feature file, scenario name, decision, before, after), and the totals: scenarios changed, added, unchanged.

Writing rules: plain English, short sentences, never use em-dashes or en-dashes.

## Final report (under 300 words)

How many scenarios you changed and added, per decision, and anything in the new digest you found unclear or contradictory.
