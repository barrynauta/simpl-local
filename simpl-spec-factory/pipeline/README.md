# Pipeline

The prompts that drive the factory, one file per step, so a run can be repeated. Each step runs in a **fresh agent** (Claude Code, general-purpose agent) that knows only its prompt and the files it may read.

| Step | File | Who | Reads | Writes |
|---|---|---|---|---|
| 1 | [`01-spec-digest.md`](./01-spec-digest.md) | main session | `specs/` | `spec-digest.md` |
| 2 | [`02-test-writer-api.md`](./02-test-writer-api.md) | test writer | `specs/`, digest | `tests/` (API scenarios, UI Gherkin) |
| 3 | [`03-code-writer.md`](./03-code-writer.md) | code writer | its own copy of `specs/` and the digest | the build, in its own workspace |
| 4 | [`04-test-writer-ui.md`](./04-test-writer-ui.md) | test writer | `specs/`, digest, `tests/`, the running UIs | UI browser steps |

After step 3: copy the build into `generated/runN/` (without `node_modules`, `target`, `dist`), run the suite against it, and do the two checks below. Record everything in `findings.md`.

## Placeholders

| Placeholder | Meaning |
|---|---|
| `{FACTORY}` | this `simpl-spec-factory` folder |
| `{WORKSPACE}` | a separate folder **outside** this repository for the code writer, with `input/specs/` and `input/spec-digest.md` copied in and an empty `build/` |
| `{UPSTREAM}` | the local upstream stack used for the baseline (`../simpl-vocabulary-manager`) |

The code writer gets its own workspace so it cannot see the tests or upstream, even by accident.

## Checks after the code writer

- **Isolation:** go through the code writer's tool calls (the agent transcript) and list every file path and web request. Run 1: no access outside `{WORKSPACE}`, no web access.
- **Contamination:** upstream is public, so the model may have seen it. Compare class names, method names, package structure and size with upstream. Shared names that come from the OpenAPI schemas or are generic do not count.

## How run 1 differs from these files

Stated so a rerun can be compared fairly:

- **Steps 1 and 2 were done in the main session,** not by separate agents. Their files are reconstructed briefs, not verbatim prompts. Steps 3 and 4 are the prompts as used.
- **Paths** are replaced by the placeholders above.
- **One framing sentence** in the code writer prompt named an intended audience, from before this became a personal test project. It is removed.
- **Input:** in run 1 the code writer's copy of the stories also held the Jira review comments. The public `specs/stories/` holds description and acceptance criteria only. A rerun from this repository therefore starts with a little less, and may find more gaps.
- **Ports** in the prompts are the ones free on the machine used for run 1. Change them if needed, and set the same values in the test configuration.
