# Run 14 follow-up checks (26 Nov 2026)

Two checks against the run 14 stack, still running after the run, to settle the open items of `REPORT.md` section 3. The run's own report files are unchanged.

## 1. D158 with a raw malformed escape

The failing scenario (`http_rules.feature:18`) never sent `%ZZ`: the Python test client re-encoded it as `%25ZZ`. Sent raw with curl to `GET /v1/vocabularies`:

| Query | Answer | D158 |
|---|---|---|
| `name=%ZZ` | 400 `belgif:badRequest`, no issues | met |
| `name=%G1` | 400 `belgif:badRequest`, no issues | met |
| `name=ab%` | 400 `belgif:badRequest`, no issues | met |
| `name=%25ZZ` (what the test sent) | 400 with one `schemaViolation` issue on `name` | correct under D19 |
| `name=abc` (control) | 200 | |

Verdict: test bug confirmed. Run 14 has one build bug (D209 dialog focus, 3 cases); measured as intended, 665 of 668 cases pass.

## 2. Fuzzing with a renewed token

Same settings as step 70 (Schemathesis 4.33.4, `--checks all`, 30 examples per operation, seed 1, 300 s), with a Schemathesis auth hook that requests a new token from Keycloak every 240 s instead of one token for the whole run.

- 19 operations tested, 57 999 cases generated.
- 14 failed checks, all explained by `fuzz-known.yaml` (D9, D19, D20, D22, D37, D49, D52): the same 14 as in the run.
- 0 answers with 401, 0 server errors, 0 findings.

Verdict: the 18 findings of the run all came from the expired token. Factory fix: renew the token during fuzzing (an auth hook as above) in `pipeline/fuzz_api.py`, and list the D17 401 in `fuzz-known.yaml` for the case where it still expires.
