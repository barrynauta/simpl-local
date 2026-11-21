"""Generate COVERAGE.md from the feature tags, input/ids.json and input/decisions/decisions.json.

Run: .venv/bin/python tools/coverage.py   (from work/). Reasons for gaps are kept below,
so the table stays right after scenarios change.
"""
import collections
import json
import os
import re
import sys

from behave.parser import parse_file

WORK = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
INPUT = os.path.join(os.path.dirname(WORK), "input")

AC_GAPS = {}

DECISION_GAPS = {
    "D2": "CDM snapshot for the model; no observable behaviour.",
    "D13": "Browser-side syntax check; covered by @ui scenarios only (./run.sh ui).",
    "D31": "Naming rule; applied in all scenario texts, no own behaviour.",
    "D39": "Records that decisions win over the OpenAPI; the suite tests the decided behaviour.",
    "D40": "Security scheme setup; tokens without scopes are used in every scenario.",
    "D45": "Pull only, no events: absence of events is not observable through the API.",
    "D79": "List of contract changes; each is tested under its own decision.",
    "D61": "Scope: SIMPL-28680 has no scenarios, as decided.",
    "D99": "Load test shape: load/run_load.py (seed 1000 x 20, 20 reads/s and 1 write/s for 5 minutes, 5 MB writes apart).",
    "D80": "Log content is not observable from outside.",
    "D81": "Withdrawn by D82.",
    "D83": "Store users and network: deployment, not API behaviour.",
    "D85": "Store query shape (LIMIT, COUNT): internal; paging results are tested under D22.",
    "D87": "Build deliverables in docs/.",
    "D88": "Realm key refetch timing: D44 assigns the identity provider cases to unit tests.",
    "D89": "Backup and restore procedure: operations, step 70.",
    "D92": "Fifth parallel upload gets 503: needs exactly timed 5 MB uploads; flaky as a black-box test, left to step 70.",
    "D93": "Build choice recorded in REUSE.md.",
    "D94": "Library choice.",
    "D95": "Build finish line (lint, format, UI unit tests).",
    "D96": "Log format is not observable from outside.",
    "D97": "Build deliverables.",
    "D98": "Local stack hardening: deployment.",
    "D102": "Two backend instances on the finish line: deployment; the D43 test runs against whatever stack is up.",
    "D103": "Circuit breaker: needs a failing then recovering store; step 70.",
    "D104": "Exception model inside the code; the problem types are tested under D48.",
    "D105": "Log format.",
    "D106": "Where bytes are stored: internal; content round trip tested under D29.",
    "D107": "No store host port: deployment.",
    "D108": "Lazy key fetch and health DEGRADED: needs Keycloak down; D44 assigns this to unit tests.",
    "D117": "CDM findings for the model.",
    "D120": "Management port placement: deployment.",
    "D122": "Front proxy limits: the suite talks to API_URL; proxy answers above 7 MB are step 70.",
    "D123": "Content-Security-Policy of the UI: axe runs with the browser CSP bypass D123 allows; the header itself is not checked.",
    "D124": "External files get no semantic validation, so the base IRI has no observable effect.",
    "D129": "Key fetch failure window: unit tests (D44).",
    "D131": "Start command of the stack.",
    "D132": "Claim checks by library; the outcomes are tested under E5 and E7 (wrong audience, issuer, expiry).",
    "D133": "Routing implementation; the outcomes 404, 405, 406, 415 are tested under their decisions.",
    "D134": "Breaker failure counting: step 70.",
    "D135": "Code structure.",
    "D136": "Per-operation p95 at the full seed: load test (load/run_load.py).",
    "D152": "Container health check uses the management port: deployment.",
    "D156": "Compaction script: operations.",
    "D157": "Retry backoff timing: internal.",
    "D164": "Failed-token limiting belongs to the gateway; recorded deviation.",
    "D165": "createdBy as personal data: model marking and logs; createdBy content is tested under D27.",
    "D167": "UI component library.",
    "E1": "Test users and realm; used by every scenario.",
    "E2": "Default ports; the build profile and environment variables replace them (E4).",
    "E3": "Store choice (Fuseki): internal.",
    "E4": "Run environment: the suite reads every URL from the environment and never resets the store.",
    "E6": "Reuse choices.",
    "Q3": "Availability, backup and restore: not observable in an API run; step 70 and the build's own round-trip test.",
    "Q4": "Design load: used as the seed and offered load of load/run_load.py (D99).",
    "Q5": "Coverage, Trivy and query bounds: build checks, not black-box.",
}

UI_AC_REASON = "UI only: @ui scenario, Playwright steps (./run.sh ui)."


def scenarios():
    result = []
    for root, _dirs, files in os.walk(os.path.join(WORK, "features")):
        for fname in sorted(files):
            if not fname.endswith(".feature"):
                continue
            feature = parse_file(os.path.join(root, fname))
            for sc in feature.walk_scenarios():
                tags = set(feature.tags) | set(sc.tags)
                result.append((fname, sc.name, tags))
    return result


def main():
    ids = json.load(open(os.path.join(INPUT, "ids.json")))
    decisions = json.load(open(os.path.join(INPUT, "decisions", "decisions.json")))
    all_sc = scenarios()
    by_tag = collections.defaultdict(list)
    for fname, name, tags in all_sc:
        for tag in tags:
            by_tag[tag].append((fname, name, tags))

    def describe(tag):
        hits = by_tag.get(tag, [])
        api = [h for h in hits if "ui" not in h[2] and "wip" not in h[2]]
        ui = [h for h in hits if "ui" in h[2]]
        wip = [h for h in hits if "wip" in h[2]]
        files = sorted({h[0] for h in hits})
        return len(api), len(ui), len(wip), files

    out = ["# Coverage", "",
           "Generated by `tools/coverage.py` from the scenario tags. Counts are scenarios after outline",
           "expansion: API (default run and store-down), UI (@ui, ./run.sh ui), WIP (@wip).", ""]

    # per feature and per tag kind
    per_feature = collections.Counter(f for f, _n, _t in all_sc)
    out += ["## Scenarios per feature", "", "| Feature file | Scenarios |", "|---|---|"]
    for f, n in sorted(per_feature.items()):
        out.append(f"| {f} | {n} |")
    out.append(f"| total | {len(all_sc)} |")
    kinds = collections.Counter()
    for _f, _n, tags in all_sc:
        for t in tags:
            if t.startswith("AC_"):
                kinds["criterion (@AC_)"] += 1
            elif re.fullmatch(r"[DEQ]\d+", t):
                kinds["decision (@D, @E, @Q)"] += 1
            elif t in ("ui", "wip", "negative", "store_down"):
                kinds["@" + t] += 1
            else:
                kinds["rule"] += 1
    out += ["", "| Tag kind | Tag uses |", "|---|---|"]
    for k, v in sorted(kinds.items()):
        out.append(f"| {k} | {v} |")

    # acceptance criteria
    out += ["", "## Acceptance criteria", "",
            "Live criteria from `input/ids.json` (SIMPL-28680 left the inventory, D61).", "",
            "| Criterion | API | UI | WIP | Feature files | Note |", "|---|---|---|---|---|---|"]
    gaps = []
    for story in ids["stories"]:
        for ac in story["acs"]:
            if ac.get("struck"):
                continue
            tag = "AC_" + ac["id"].replace("/", "_")
            api, ui, wip, files = describe(tag)
            note = ""
            if api == 0 and ui > 0:
                note = UI_AC_REASON
            elif api == 0 and wip:
                note = "Only a @wip scenario: no black-box way to cause a 500 (D44 gives 503 for a store outage)."
            elif api == 0 and ui == 0:
                note = AC_GAPS.get(ac["id"], "NOT COVERED")
                gaps.append((ac["id"], note))
            elif api == 0 and wip:
                note = "Only a @wip scenario."
            out.append(f"| {ac['id']} | {api} | {ui} | {wip} | {', '.join(files)} | {note} |")
    for story in ids["stories"]:
        if not story["acs"]:
            out.append(f"| {story['key']} (no criteria) | - | - | - | - | No Given/When/Then criteria; "
                       f"D1 settles it (SIMPL-393 by the generic upload, see create_vocabulary.feature; SIMPL-395 replaced). |")
    out += ["", "Criteria without an executable API scenario and why:", ""]
    out.append("- SIMPL-18162/AC4 (internal error 500): no black-box way to cause a 500; a store outage gives 503 (D44). "
               "A @wip scenario is kept for fault injection in step 70.")
    out.append("- UI criteria (SIMPL-18161, 18163, 18165, 18167, 18169, 18172, 18174, 18176, 18178, 18180, 18182, 18184/AC4, "
               "18186, 18189, 18191, 18193, 18195, 18452, 18559): @ui Gherkin scenarios; the API part of several is also "
               "tested (for example 18184/AC1 to AC3 and 18180/AC7 through the API).")
    for ac_id, note in gaps:
        out.append(f"- {ac_id}: {note}")

    # decisions
    out += ["", "## Decisions", "", "| Decision | Topic | Status | API | UI | Note |", "|---|---|---|---|---|---|"]
    for d in decisions:
        if d["id"].startswith("Q"):
            continue
        api, ui, wip, _files = describe(d["id"])
        note = ""
        if api == 0 and ui == 0:
            note = DECISION_GAPS.get(d["id"], "NOT COVERED")
        elif api == 0:
            note = "UI only (./run.sh ui)."
        out.append(f"| {d['id']} | {d.get('topic','')} | {d.get('status','')} | {api} | {ui} | {note} |")

    # quality targets
    out += ["", "## Quality targets (NFR-13)", "", "| Target | Checked by | Note |", "|---|---|---|"]
    q_checks = {
        "Q1": "load/run_load.py: one Locust task per read operation and per write operation, 20 reads/s and "
              "1 write/s offered for 5 minutes after warm-up (D99), p50/p95/p99 and error rate per operation "
              "in reports/load.json; separate 5 MB write phase (D71). Also two API scenarios that a 5 MB "
              "file is accepted (tagged @Q1).",
        "Q2": "Scenarios tagged @Q2 and @NFR-04, at the limit and one above: name 64/65 characters, description "
              "and changelog 1000/1001, file 5 MB (internal create, new version, external), multipart request "
              "6 MB, JSON body 1 MB, pageSize 100/101, search items 100/101, external URIs 100/101, aliases 20/21, "
              "external description 1000/1001, report 1000/1001 issues. Not tested at the lower limit: a 3-character "
              "name is accepted (a 3-character name cannot hold the run id; names are unique case-insensitively, "
              "so a second run would get 409, E4); the 2-character rejection is tested. The 64-part limit (D110) "
              "is not proven on its own: the filler parts are unknown fields that D52 already rejects with 400. "
              "D92 (4 parallel uploads) is not tested (see D92).",
        "Q3": "Not measured here: availability and recovery are operations targets; step 70 and the build's "
              "own backup and restore round trip.",
        "Q4": "Used as the load shape: seed of 1000 vocabularies x 20 versions (D99) and 20 concurrent users "
              "in load/run_load.py.",
        "Q5": "Not measured here: coverage, Trivy and query bounds are build checks (step 50/70).",
        "Q6": "@ui @a11y scenarios run axe (WCAG 2.1 AA tags) on the list, details and both form pages; @Q6 keyboard scenarios Tab through every action on the same pages.",
    }
    for d in decisions:
        if d["id"].startswith("Q"):
            api, ui, _w, _f = describe(d["id"])
            out.append(f"| {d['id']} {d.get('topic','')} | {q_checks.get(d['id'], 'NOT COVERED')} | "
                       f"{api} API and {ui} UI scenarios tagged |")

    # rules
    out += ["", "## Rules checked", "", "| Rule | Scenarios |", "|---|---|"]
    rule_tags = sorted({t for _f, _n, tags in all_sc for t in tags
                        if not t.startswith("AC_") and not re.fullmatch(r"[DEQ]\d+", t)
                        and t not in ("ui", "wip", "negative", "store_down", "a11y", "manual")})
    for t in rule_tags:
        out.append(f"| {t} | {len(by_tag[t])} |")
    out += ["", "Rules a black-box test cannot observe here, or that do not apply:", "",
            "- OWA-API4 (call-rate limit): applied by the Tier 1 gateway, outside this build (D46).",
            "- OWASP-08 / D132 expiry: the `edited-expiry` token is refused by the signature check; a validly "
            "signed expired token needs a Keycloak client with a short token lifespan, which E1 and E7 do not "
            "provide. The expiry check is not proven on its own.",
            "- OWT-A07 failed-token limiting: gateway, recorded deviation (D164); token validation itself is tested.",
            "- API-39 (Vary: Accept): no operation negotiates between several success media types; each serves one.",
            "- API-46 is checked only on the simpl problem types (401, 403, 406, 409, 413, 415), as D48 decides.",
            "- API-25 (HTTPS only): the local stack runs plain HTTP; TLS is a deployment matter for step 70.",
            "- API-65 to API-75 (CloudEvents, Kafka): no events in this build (D45).",
            "- NFR-03: the load test in load/ (see Q1)."]
    out += ["", "## UI automation (step 60)", "",
            "`./run.sh ui` runs every @ui scenario in Chromium with Playwright for Python "
            "(`features/steps/ui_steps.py`), behave JSON to `reports/ui.json`, axe results to "
            "`reports/axe/`, screenshots of failures to `reports/ui-screens/`. Each scenario gets a new "
            "browser context, so no stored session or cookie is reused; sign-in goes through the "
            "Keycloak login form of E1.", "",
            "Elements are found only by decided texts and names: labels and texts of D60, D72, D115; "
            "column headers of D23, D35, D159; 'Actions for <Name>' and 'Actions for version <n>', the regions "
            "'Version <n>', 'Validation report' and 'Validation warnings' (D75); roles status and alert "
            "(D75, D90, D114); aria-current and aria-sort (D163); routes of D31 and D74; the /v1 API path of E2 "
            "for stubs. Nothing is taken from the build.", "",
            "Interpretations of step texts (assumptions):", "",
            "- 'the field X shows an error': the field has aria-invalid=\"true\" and a non-empty error text "
            "tied to it by aria-errormessage or aria-describedby.",
            "- 'no request was sent to the API': no POST, PATCH, PUT or DELETE to /v1/ after the click.",
            "- 'the API answers <code>': a Playwright route on the browser context answers the matching "
            "method and /v1 path with a problem+json body (D24); 200 on GET /vocabularies gives the empty "
            "example of the OpenAPI.",
            "- 'every action ... with the keyboard only': every visible, enabled focusable element is reached "
            "with Tab, and the first actions menu opens with Enter and closes with Escape.",
            "- 'the page has no axe violations': axe-core rules tagged wcag2a, wcag2aa, wcag21a, wcag21aa.", "",
            "@manual scenarios: none. Every @ui scenario could be automated with the decided names.", ""]
    with open(os.path.join(WORK, "COVERAGE.md"), "w") as fh:
        fh.write("\n".join(out) + "\n")
    missing = [l for l in out if "NOT COVERED" in l]
    print("\n".join(missing) if missing else "no gaps without a reason")
    return 0


if __name__ == "__main__":
    sys.exit(main())
