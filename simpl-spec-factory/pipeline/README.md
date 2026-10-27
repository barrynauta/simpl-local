# Pipeline

The prompts that drive the factory, one file per step, so a run can be repeated. Each step runs in a **fresh agent** (Claude Code, general-purpose agent) that knows only its prompt and the files it may read.

| Step | File | Who | Reads | Writes |
|---|---|---|---|---|
| 0 | [`collect_stories.py`](./collect_stories.py) | script, local model | Jira (personal access token) | `specs/stories/` |
| 1 | [`01-spec-digest.md`](./01-spec-digest.md) | main session | `specs/` | `spec-digest.md` |
| 2 | [`02-test-writer-api.md`](./02-test-writer-api.md) | test writer | `specs/`, digest | `tests/` (API scenarios, UI Gherkin) |
| 3 | [`03-code-writer.md`](./03-code-writer.md) | code writer | its own copy of `specs/` and the digest | the build, in its own workspace |
| 4 | [`04-test-writer-ui.md`](./04-test-writer-ui.md) | test writer | `specs/`, digest, `tests/`, the running UIs | UI browser steps |

After step 3: copy the build into `generated/runN/` (without `node_modules`, `target`, `dist`), run the suite against it, and do the two checks below. Record everything in `findings.md`.

## Step 0: collecting the stories locally

`collect_stories.py` fetches the stories with a Jira personal access token and converts them with a local model through Ollama (default Mistral Small 3.2: open weights, Apache 2.0, from the French company Mistral AI; any Ollama model can be set with `OLLAMA_MODEL`), so the story text does not pass through any external service. A word-by-word check against the source catches any change the model makes; on a mismatch it retries once and then falls back to a plain conversion without the model. It prints counts only. Run: `JIRA_PAT=... python3 pipeline/collect_stories.py --out specs/stories --keys-from specs/stories`.

The split is deliberate: fetching runs locally, so no Jira access goes through an external service. The later steps (1 to 4) run on Claude. They work from the specs only, and the stories will be published with the open source documentation. A local model for those steps was tried for step 1 (below) and is not good enough yet.

## Step 1 on a local model

[`run_local.py`](./run_local.py) runs step 1 (the spec digest) on a local model through Ollama, with the same input as run 1 in one prompt. The output goes to `local-run/` (gitignored) until a person has read it. It prints counts and timings only. Run: `python3 pipeline/run_local.py --step 1`.

- **Mistral Small 3.2 (24B): not sufficient.** Input of 73k tokens, about 7 minutes. The digest repeated the brief as 12 generic "decisions" and found none of the gaps or contradictions of run 1. It also skipped the tech description and put an out-of-scope story in scope. Not usable as input for the writers.
- **Qwen3 30B A3B Instruct 2507:** the default now. Same input, about 6 minutes. A usable start, not yet a replacement for run 1: 10 decisions and 5 open questions with real content (the draft API in the tech description against the OpenAPI, external vocabularies without stories, roles, error format, versioning, file format). Together they cover about a third of the 30 decisions of run 1. Missing among others: public download, 401 against 403, vocabulary against version status, duplicate names, "published" against "active", and most UI details (columns, filter, page size, upload limit). Some decisions repeat the spec instead of deciding, it chose a mock identity provider where the tests need a real one, and it did not mark SIMPL-28680 as out of scope.

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
