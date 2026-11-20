"""Writes COVERAGE.md from the tags of the feature files (run: .venv/bin/python tools/coverage.py)."""
import collections
import glob
import json
import os
import re

from behave.parser import parse_file

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
known = json.load(open(os.path.join(HERE, "known_ids.json")))

NOT_COVERED_CRITERIA = {
    "SIMPL-18162/AC4": "Not testable from outside: no black-box request can make the service fail with a 500 internal error. "
                       "A store that is down gives 503 (D44), covered in store_down.feature; a store error that is not a "
                       "timeout stays 500 (D44) but cannot be provoked through the API.",
}

NOT_COVERED_DECISIONS = {
    "D2": "Input pinning of the CDM snapshot; no runtime behaviour.",
    "D39": None, "D79": None,
    "D40": None,
    "D61": "SIMPL-28680 leaves the live set; its criteria are not in ids.json, so nothing to test (D1).",
    "D80": "Log content (no e-mail or name in logs) is not observable through the API.",
    "D81": "Withdrawn by D82.",
    "D83": "Store users, passwords and network placement are deployment facts, not observable through the API.",
    "D87": "Build deliverables (logical and physical model, openapi-decided.yaml); checked by review, not by the suite.",
    "D88": "Realm key refetch timing needs control over Keycloak keys; the decision itself says the identity "
           "provider cases are unit tests in the code (D44).",
    "D89": "Backup and restore procedure; not an API behaviour (Q3 restore test runs in the build).",
    "D92": "A fifth parallel upload on one instance gets 503: the outcome depends on timing and on how many "
           "backend instances serve the API port (D102), so a black-box test cannot force it reliably. "
           "D119 keeps the suite's parallel test at 4.",
    "D93": "Choice of request validator, recorded in REUSE.md; build review.",
    "D94": "Library choices; build review.",
    "D95": "Lint, format and UI component tests on the finish line; build tooling.",
    "D96": "Log line format; not observable through the API.",
    "D97": "Build inputs and deliverables (CDM, LICENSE, glossary); build review.",
    "D98": "Local stack hardening (ports, networks, limits); deployment review.",
    "D99": "Load test shape: checked by the load test (load/), not by a scenario.",
    "D102": "Two backend instances on the finish line; the suite runs against whatever API_URL serves, "
            "and the D43 parallel scenario holds for any number of instances (D82).",
    "D103": "Circuit breaker timing (half of the last 20 calls, 10 s open) cannot be observed reliably from outside; "
            "the store-down scenarios check the 503 outcome.",
    "D104": "Exception model inside the code; the problem types it produces are checked through D48.",
    "D105": "Log format and shipping; not observable through the API.",
    "D106": "Where the file bytes live; the byte-for-byte download scenarios check the effect (D29).",
    "D107": "No host port for the store; deployment review.",
    "D108": None,
    "D117": "CDM findings and the logical model; build review.",
    "D120": "Management port placement; deployment review.",
    "D124": "The base IRI of external files is not observable through the API: registration checks syntax only and "
            "returns 201 whatever base is used. The scenario 'Relative IRIs in an external file ...' only shows that "
            "relative IRIs are accepted (tagged D41).",
    "D129": "Unknown key id after a failed key fetch needs control over Keycloak availability; unit tests per D44.",
    "D131": "Start command of the stack; used by the orchestrator, not by the suite.",
    "D132": None,
    "D133": "Framework routing and the build's own order test; the observable order (401, 403, 406, 415, 413, 400, "
            "404, 409) is checked through D51 and D63.",
    "D134": "Breaker failure counting and the internal behaviour of GET /health; the health outcomes are checked (D44).",
    "D135": "Code structure rule; build review.",
    "D136": "Q1 per operation: checked by the load test, which reports every operation apart.",
    "E1": None, "E2": None, "E4": None,
    "E3": "Store choice and stubs; refined by E4 (no reset) and D83. No scenario of its own.",
    "E5": None, "E6": "Reuse choices; build review.", "E7": None,
    "Q3": "Availability, recovery and the backup round trip are measured in the build and step 70, not through the API.",
    "Q4": "Design load and seed: used by the load test (load/seed.py, 1000 vocabularies x 20 versions).",
    "Q5": "Coverage, Trivy and query bounds are build measurements (step 70).",
    "Q6": None,
}

Q_TARGETS = {
    "Q1": "Load test (load/locustfile.py): one Locust task per read operation (GET /vocabularies, GET /vocabularies/{vocabularyName}, "
          "GET .../versions, GET .../versions/{version}, POST /vocabularies/searches, GET /content/{vocabularyName}, "
          "GET /content/{vocabularyName}/versions/{version}, GET /external-vocabularies, GET /external-vocabularies/{name}, "
          "GET /external-vocabularies/{name}/content, POST /external-vocabulary/searches, POST /external-vocabularies/content) "
          "at 20 reads per second in total, and the write task (POST /vocabularies, POST .../versions, files up to 1 MB) at "
          "1 write per second, for 5 minutes; p95 at most 300 ms per read operation and 3 s per write operation, achieved "
          "rate at least 95 % of the offered rate, no errors (D99, D136). The 5 MB writes run apart (load/big_writes.py, at "
          "most 30 s, D71); the scenarios 'A vocabulary file of exactly 5 MB is accepted within 30 seconds' and its new "
          "version twin check the 30 s budget on every API run too.",
    "Q2": "limits.feature (5 MB file at and over the limit, 6 MB and 7 MB requests, 1 MB JSON, name 64 and 65 and 2 "
          "characters, description and changelog 1000 and 1001 characters on create and new version), "
          "list_vocabularies.feature (pageSize 100 and 101), search.feature (100 and 101 references, 100 and 101 URIs), "
          "external_vocabularies.feature (20 and 21 aliases, description 1000 and 1001), validation.feature (1000 and 1001 "
          "issues, D125). D71 is checked in limits.feature. D92 (4 parallel uploads) is not tested, see D92 above.",
    "Q3": "Not measured by the suite: availability, recovery time and the backup and restore round trip are measured "
          "in the build and in step 70 (NFR-13: listed as not measured here, reason: not an API behaviour).",
    "Q4": "Used as the condition of the load test: 20 concurrent read users, 1000 vocabularies with 20 versions each, "
          "10 external vocabularies for the consuming services (load/seed.py). Scaling is not measured by the suite.",
    "Q5": "Not measured by the suite: coverage, Trivy findings and query bounds are build measurements (step 70).",
    "Q6": "UI scenarios tagged @Q6 and @a11y (axe-core with the WCAG 2.1 AA tags wcag2a, wcag2aa, wcag21a and "
          "wcag21aa on the list, details and both form pages, plus keyboard reachability). Run with `./run.sh ui`; "
          "each page's violations are written to reports/a11y/<page>.json.",
}


def collect():
    crit = collections.defaultdict(lambda: collections.Counter())
    dec = collections.defaultdict(lambda: collections.Counter())
    rules = collections.defaultdict(lambda: collections.Counter())
    for path in sorted(glob.glob(os.path.join(ROOT, "features", "*.feature"))):
        feature = parse_file(path)
        fname = os.path.basename(path)
        for sc in feature.walk_scenarios():
            tags = set(sc.effective_tags)
            for t in tags:
                if t.startswith("AC_"):
                    crit[t[3:].replace("_AC", "/AC")][fname] += 1
                elif re.fullmatch(r"[DEQ]\d+", t):
                    dec[t][fname] += 1
                elif t not in ("negative", "ui", "wip", "store_down", "a11y", "manual"):
                    rules[t][fname] += 1
    return crit, dec, rules


def fmt(counter):
    return ", ".join(f"{f.replace('.feature', '')} ({n})" for f, n in sorted(counter.items()))


def main():
    crit, dec, rules = collect()
    out = ["# Coverage of the Vocabulary Manager API suite", "",
           "Generated by `tools/coverage.py` from the scenario tags. Numbers in brackets are scenarios per feature "
           "file (outline rows count one each). Feature files starting with `ui_` hold `@ui` scenarios, which run "
           "in a browser with `./run.sh ui` (step 60); `store_down` runs with `./run.sh store-down`.", "",
           "## Acceptance criteria", "", "| Criterion | Story | Scenarios |", "|---|---|---|"]
    for c in known["criteria"]:
        story = c.split("/")[0]
        out.append(f"| {c} | {known['stories'][story]} | {fmt(crit[c]) if c in crit else 'none'} |")
    out += ["", "### Criteria with no scenario", ""]
    missing = [c for c in known["criteria"] if c not in crit]
    for c in missing:
        out.append(f"- **{c}**: {NOT_COVERED_CRITERIA.get(c, 'no reason recorded')}")
    if not missing:
        out.append("None.")
    out += ["", "Stories without criteria: SIMPL-393 (met by the generic upload, D1; scenario 'A CPSV-like vocabulary can "
            "be uploaded and found in the list') and SIMPL-395 (replaced by the SIMPL-181xx stories, D1). SIMPL-28680 is "
            "out of scope (D1, D61) and not in ids.json.", "",
            "Criteria of UI stories whose outcome the server decides (SIMPL-18182, SIMPL-18186, SIMPL-18189 AC1, "
            "SIMPL-18191, SIMPL-18176 AC1) are also tagged on API scenarios.", ""]
    out += ["## Decisions", "", "| Decision | Topic | Status | Scenarios |", "|---|---|---|---|"]
    for d, info in known["decisions"].items():
        out.append(f"| {d} | {info['topic']} | {info['status']} | {fmt(dec[d]) if d in dec else 'none'} |")
    out += ["", "### Decisions with no scenario", ""]
    for d in known["decisions"]:
        if d not in dec:
            out.append(f"- **{d}** ({known['decisions'][d]['topic']}): {NOT_COVERED_DECISIONS.get(d) or 'no reason recorded'}")
    out += ["", "## Quality targets (NFR-13)", "", "| Target | Checked by |", "|---|---|"]
    for q, text in Q_TARGETS.items():
        out.append(f"| {q} | {text} |")
    out += ["", "## Rules checked by the suite", "", "| Rule | Scenarios |", "|---|---|"]
    for r in sorted(rules):
        out.append(f"| {r} | {fmt(rules[r])} |")
    out += ["", "Rules a black-box test could observe but the suite does not check:", "",
            "- **API-39** (Vary: Accept): no operation negotiates between several response media types; each serves one "
            "type (JSON or Turtle) and answers 406 otherwise (D63), so the rule does not apply.",
            "- **API-25** (HTTPS only): the build profile serves the API over plain HTTP on localhost; TLS is terminated "
            "outside this build.",
            "- **OWA-API4** (call-rate limit): applied by the Tier 1 gateway in production, outside this build (D46).",
            "- **NFR-07** graceful shutdown: needs control over the process; only the health outcomes are checked.",
            "- **NFR-03**: the load test (`./run.sh load`) writes p50, p95, p99 and the error rate per operation."]
    out += ["", "## UI scenarios (step 60)", "",
            "All 62 `@ui` scenarios are automated with Playwright for Python (`features/steps/ui.py`, "
            "`support/ui_hooks.py`) and run with `./run.sh ui` (behave JSON in `reports/ui.json`). No scenario is "
            "tagged `@manual`.", "",
            "- Elements are found only by decided texts, labels, column headers, routes (D31, D74), the menu button "
            "names of D75, roles status and alert (D75, D90, D114, D115) and the regions of D75 and D90. Where a "
            "decision names an action but not its element type ('New Vocabulary +', 'Cancel'), a button or a link "
            "is accepted.",
            "- The login uses Keycloak's own form (E1). Every scenario gets a new browser context, so no cookie, "
            "storage or Keycloak session is reused within or across runs.",
            "- Error cases stub the API in the browser (D24, E3): only fetch and XHR calls whose path ends with the "
            "decided API path are answered by the stub.",
            "- 'Selecting a version' (D35) is done by activating the version number in its row; no decision says how a "
            "version is selected, so the step accepts a link or button named with the number, or the first cell.",
            "- Sortable columns (D23) are recognised by aria-sort or a button in the column header; no decision says "
            "how sortability is shown.",
            "- axe runs with the browser's CSP bypass, as D123 asks.",
            "- The four `@a11y` scenarios cover the main pages: list, details, create form and new version form. "
            "Keycloak's login page is not part of the build and is not checked.", "",
            "Result against the run 11 build (20 Nov 2026): 61 of 62 passed; axe found 0 WCAG 2.1 AA violations on "
            "the four pages. One failure, a difference between the build and the decisions (not a test bug):", "",
            "- `ui_status_access.feature:83` 'A user without a vocabulary role sees the authorisation message on the "
            "list' (D18, SIMPL-18195/AC2): for ga-norole the list shows 'The vocabulary list could not be loaded.' "
            "with the API detail of the 403. D18 says any 403 from the API shows 'You are not authorized to perform "
            "this action.'. D72 fixes the load error text only for 500 and 503."]
    with open(os.path.join(ROOT, "COVERAGE.md"), "w") as fh:
        fh.write("\n".join(out) + "\n")
    print("COVERAGE.md written")


if __name__ == "__main__":
    main()
