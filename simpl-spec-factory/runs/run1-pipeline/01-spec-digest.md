# Step 1: spec digest

**Reconstructed.** In run 1 this step was done in the main session, not by a separate agent, so there is no verbatim prompt. This is the brief to use for a rerun; it describes what was done.

---

You write the **spec digest** for a spec-driven software factory experiment. Nothing is built yet. Your output is the single document that both the test writer and the code writer will work from, next to the specs.

## Input (read-only)

`{FACTORY}/specs/`: the OpenAPI, the user stories with acceptance criteria, the functional description and the index `specs/README.md`. Read all of it once.

## What to write: `{FACTORY}/spec-digest.md`

1. **Source hierarchy:** which source wins when they disagree. Run 1 used: OpenAPI > user stories > technical description > business process, because the OpenAPI is the newest and the stories defer status codes and payloads to it.
2. **Scope:** which stories are in, which are out and why, and what is in scope without a story (OpenAPI only).
3. **Decisions:** one numbered row (D1, D2, ...) for every point where the specs are silent, vague or contradict each other. Each row: the topic, what each source says (with story keys), and the decision for this run. Group them (API and access, lifecycle, validation, UI, other). Include the environment choices the writers need to agree on, such as the identity provider contract for the tests (users, passwords, client, where the roles are in the token) and any naming convention the tests depend on.
4. **Open questions for the spec owners,** ordered by impact on buildability.
5. **Next steps** for the test writer and the code writer.

## Rules

- Decide, do not defer: every row gets a decision, even a provisional one, so two writers resolve each gap the same way.
- Every decision is also a finding: phrase it so a spec owner can act on it.
- Positive tone: the spec writers did their best; describe what is missing, not who missed it.
- Roles, not names. No quotes from review comments that identify people.
- Plain English, short sentences, no em-dashes or en-dashes.
