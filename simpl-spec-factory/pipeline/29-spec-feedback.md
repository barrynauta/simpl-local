# Step 29: spec feedback report

One report per component for the owners of the stories, the API definitions, the CDMs and the business process. It is assembled, not written: [`build_feedback.py`](./build_feedback.py) collects the findings of steps 12b, 20 and 23 and orders them, so a rerun after new findings gives the same structure. Nothing new is judged in this step.

Run: `python3 pipeline/build_feedback.py <component>`. Output: `<component>/validation/spec-feedback.md` and `spec-feedback.json` (every finding with its step, code, theme and severity).

## What the report holds

1. **In short:** the input in numbers, the findings by severity, the mechanical hits, the coverage gaps.
2. **Decide first:** every blocker, grouped by theme (behaviour and interactions, one language, entities and states, boundaries, error cases and testability, data model governance, scope), each with where, the question and the options.
3. **Terms to agree:** the draft glossary in numbers, and the open terms.
4. **To confirm:** the majors as a table.
5. **Coverage:** operations and process requirements with no criterion.
6. **The data models:** the CDM of each API, its register status and branch.
7. **How this was made**, and the caveat that agent findings are well-argued questions, not verdicts.

## Use

The blockers are also the gate for step 30: each needs a decision row (or an accepted risk) before the writers start. The report is the version of that list that a spec owner can read without knowing the factory.

Positive tone is built in: the report states what a builder needs, cites ids, and names no people.
