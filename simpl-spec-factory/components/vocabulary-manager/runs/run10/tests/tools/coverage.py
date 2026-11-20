"""Generate COVERAGE.md from the feature tags, ids.json and decisions.json.

Usage: python3 tools/coverage.py <input dir> > COVERAGE.md
"""

import json
import re
import sys
from collections import defaultdict
from pathlib import Path

from behave.parser import parse_file

ROOT = Path(__file__).resolve().parents[1]
INPUT = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT.parent / "input"

AC_REASONS = {
    "SIMPL-18162/AC4": "Not testable from outside: a black-box suite cannot force an internal error other than a store "
                       "outage, which D44 turns into 503, not 500. Kept as a @wip scenario.",
}

DECISION_REASONS = {
    "D2": "Model snapshot for the writers; no observable behaviour.",
    "D39": "Records that the decisions win over the OpenAPI file; the decided points are tested under their own decisions.",
    "D40": "Security scheme: the suite gets tokens from Keycloak with scope openid only (vmclient.fetch_token); the 401/403 "
           "behaviour is tested under D17.",
    "D45": "Pull only, no events: absence of events is not observable through the API; the pull path (searches) is "
           "tested under D38.",
    "D61": "Scope: SIMPL-28680 is not in the live set; nothing to test.",
    "D79": "List of contract changes; each change is tested under its own decision (D17, D22, D37, D52, D70, D7, D38, "
           "D56, D73, D9, D49, D63, D67, D76, D19, D27, D10, D5).",
    "D80": "Log content is not observable through the API (step 70 reads logs).",
    "D81": "Withdrawn by D82.",
    "D82": "Store-side conditional update and several instances: the build proves it with its own test (D82); the suite's "
           "D43 test covers the observable outcome and carries @D82.",
    "D83": "Store access users and network: deployment property, checked by review in step 70.",
    "D84": "",
    "D85": "Bounded store queries: internal; the observable page sizes are tested under D22.",
    "D87": "Build deliverables in docs/; checked by review.",
    "D89": "Restore procedure: step 70 runs the build's backup scripts.",
    "D93": "Build deliverable REUSE.md and its request validator; checked by review.",
    "D94": "Library choices; checked by review.",
    "D95": "Lint, format and UI test configuration of the build; checked by review.",
    "D96": "Log format is not observable through the API.",
    "D97": "Build deliverables (LICENSE, glossary, logical model); checked by review.",
    "D98": "Local stack hardening (ports, networks, limits); checked by review in step 70.",
    "D102": "Two backend instances on the finish line: run by the build's smoke test; the suite runs unchanged against "
            "either set-up.",
    "D104": "Exception model inside the code; the resulting problem types are tested under D48.",
    "D105": "Log format out of scope; not observable through the API.",
    "D106": "File bytes in the store: storage design; the observable byte-exact download is tested under D29 and D70.",
    "D107": "No host port for the store: deployment property; checked in step 70.",
    "D108": "Library and lazy first key fetch: internal; health UP while the store answers is tested (protocol.feature).",
    "D116": "Formats in openapi-decided.yaml; the behaviour (date filters, version minimum) is tested under D20 and D49.",
    "D117": "CDM findings for the model owner; no behaviour.",
    "D1": "",
    "D31": "",
    "E1": "Test users and roles: used by every scenario through the token steps (ga-admin, ga-viewer, ga-norole).",
    "E2": "Ports and base URLs: configuration through API_URL and the contentBase discovered from contentHref.",
    "E3": "Store choice; the reset sentence is withdrawn by E4.",
    "E4": "Applied to the whole suite: unique run id per scenario, no store reset, checks through name filters.",
    "E6": "Reuse choices for the build; checked by review.",
    "D103": "",
    "D44": "",
    "D92": "Fifth simultaneous upload gets 503: timing dependent with fast uploads and two instances (D102); kept as @wip.",
    "D99": "",
    "Q3": "Availability, RTO, RPO and the backup and restore round trip are measured in step 70 with the build's scripts, "
          "not through the API (@wip scenario kept).",
    "Q5": "Coverage, Trivy findings and query bounds are measured on the build in step 70 (NFR-09), not by this suite.",
}

LOAD_COVERED = {"Q1", "Q4", "D99"}

UI_ONLY = {"SIMPL-18161", "SIMPL-18163", "SIMPL-18165", "SIMPL-18167", "SIMPL-18169", "SIMPL-18172", "SIMPL-18174",
           "SIMPL-18176", "SIMPL-18178", "SIMPL-18180", "SIMPL-18182", "SIMPL-18186", "SIMPL-18189", "SIMPL-18191",
           "SIMPL-18193", "SIMPL-18195", "SIMPL-18452", "SIMPL-18559"}


def scenarios():
    out = []
    for path in sorted((ROOT / "features").glob("*.feature")):
        feature = parse_file(str(path))
        ftags = set(feature.tags)
        for sc in feature.scenarios:
            tags = ftags | set(sc.tags)
            examples = getattr(sc, "examples", None)
            if examples:
                for ex in examples:
                    rows = len(ex.table.rows) if ex.table else 0
                    out.append({"file": path.name, "line": sc.line, "name": sc.name,
                                "tags": tags | set(ex.tags), "count": rows})
            else:
                out.append({"file": path.name, "line": sc.line, "name": sc.name, "tags": tags, "count": 1})
    return out


def ref(s):
    kind = " (ui)" if "ui" in s["tags"] else (" (wip)" if "wip" in s["tags"] else "")
    rows = f" x{s['count']}" if s["count"] > 1 else ""
    return f"`{s['file']}:{s['line']}`{rows}{kind}"


def main():
    ids = json.loads((INPUT / "ids.json").read_text())
    decisions = json.loads((INPUT / "decisions" / "decisions.json").read_text())
    scs = scenarios()
    by_tag = defaultdict(list)
    for s in scs:
        for t in s["tags"]:
            by_tag[t].append(s)

    total = sum(s["count"] for s in scs)
    p = print
    p("# Coverage of the Vocabulary Manager test suite\n")
    p("Generated by `tools/coverage.py` from the feature tags, `input/ids.json` and "
      "`input/decisions/decisions.json`. `xN` is the number of example rows of an outline; (ui) scenarios run in a "
      "browser with `./run.sh ui` (step 60); (wip) scenarios never run.\n")
    p(f"Scenario count: {total} (scenario outlines expanded), in {len(scs)} scenario definitions.\n")

    p("## Acceptance criteria\n")
    p("| Criterion | Heading | Scenarios |")
    p("|---|---|---|")
    uncovered = []
    live = 0
    for story in ids["stories"]:
        for ac in story["acs"]:
            if ac["struck"]:
                continue
            live += 1
            tag = "AC_" + ac["id"].replace("/", "_")
            refs = by_tag.get(tag, [])
            heading = (ac["heading"] or story["title"]).replace("|", "/").replace("\u2013", "-").replace("\u2014", "-")
            if refs:
                api = [r for r in refs if not ({"ui", "wip"} & r["tags"])]
                note = "" if api or story["key"] in UI_ONLY else " (no runnable API scenario)"
                p(f"| {ac['id']} | {heading} | {', '.join(ref(r) for r in refs)}{note} |")
            else:
                uncovered.append(ac["id"])
                p(f"| {ac['id']} | {heading} | none: {AC_REASONS.get(ac['id'], 'NO REASON GIVEN')} |")
    p(f"\nLive criteria: {live}. Stories without criteria: SIMPL-393 and SIMPL-395 (D1: met by the generic upload, "
      "tested in `create_vocabulary.feature`, and replaced by the SIMPL-181xx stories). SIMPL-28680 is out of scope "
      "(D1, D61) and not in the live set.\n")
    p("### Criteria with no runnable API scenario\n")
    p("- UI stories (" + ", ".join(sorted(UI_ONLY)) + "): their criteria have @ui scenarios, run in a browser by "
      "`./run.sh ui`. Where a UI criterion has an API counterpart (validation, versioning, publication) that is "
      "tested in the API features under the API story's tags.")
    for ac in uncovered:
        p(f"- {ac}: {AC_REASONS.get(ac, 'NO REASON GIVEN')}")
    p("")

    p("## Decisions\n")
    p("| Decision | Topic | Status | Scenarios |")
    p("|---|---|---|---|")
    none = []
    for d in decisions:
        refs = by_tag.get(d["id"], [])
        status = d.get("status", "")
        if refs:
            n = sum(r["count"] for r in refs)
            files = sorted({r["file"] for r in refs})
            p(f"| {d['id']} | {d['topic']} | {status} | {n} in {', '.join(files)} |")
        elif d["id"] in LOAD_COVERED:
            p(f"| {d['id']} | {d['topic']} | {status} | load test `load/` (`./run.sh load`), see Quality targets |")
        else:
            reason = DECISION_REASONS.get(d["id"]) or "NO REASON GIVEN"
            none.append((d["id"], reason))
            p(f"| {d['id']} | {d['topic']} | {status} | none: {reason} |")
    p("\n### Decisions with no scenario\n")
    for i, r in none:
        p(f"- {i}: {r}")
    p("")

    p("## Quality targets (NFR-13)\n")
    p("| Target | Checked by |")
    p("|---|---|")
    q_text = {
        "Q1": "Load test `load/` (Locust, `./run.sh load`): one task per operation Q1 names (9 read operations, "
              "createVocabulary, createVocabularyVersion) at the offered rate of D99 (20 reads/s, 1 write/s, files up "
              "to 1 MB, 5 minutes, after seeding 1000 x 20 versions of about 10 KB); p50, p95, p99 and error rate per "
              "operation in `reports/load.json`, plus the separate 5 MB write test (D71, target p95 30 s). "
              "Functional at-limit 5 MB scenarios also run in the API suite.",
        "Q2": "Every limit has a scenario at the limit and one above, tagged @Q2 @NFR-04: file 5 242 880 / 5 242 881 "
              "bytes (create, version, external), request over 6 MB, name 3 (201 or 409) / 2, 64 / 65, description "
              "and changelog 1000 / 1001, pageSize 100 / 101, page 2147483647 / 2147483648, search 100 / 101 items, "
              "external search 100 / 101 URIs, aliases 20 / 21, external file 5 242 880 / 5 242 881 bytes, "
              "multipart request exactly 6 MB (answers the field error, not 413) / over 6 MB, JSON body exactly "
              "1 MB / over 1 MB, more than 64 parts. The 64-part limit (D110) cannot be isolated through the API: "
              "a request with 64 parts cannot be valid, and the parts above the limit are repeated fields, so a "
              "build that rejects repeated fields but has no part limit also passes (only the case above the limit "
              "exists). "
              "The 4-upload limit of D92 is @wip (timing).",
        "Q3": DECISION_REASONS["Q3"],
        "Q4": "Design load used as the load test shape (seed 1000 vocabularies x 20 versions, 20 reader users, "
              "1 writer); the D43 parallel scenario covers concurrent writers on one vocabulary.",
        "Q5": DECISION_REASONS["Q5"],
        "Q6": "@ui @a11y scenarios `ui_list_and_details.feature` (axe WCAG 2.1 AA on list, details and both forms; "
              "keyboard reach), run by `./run.sh ui` (NFR-10). Result per page in `reports/a11y.json`.",
    }
    for q in ["Q1", "Q2", "Q3", "Q4", "Q5", "Q6"]:
        n = sum(r["count"] for r in by_tag.get(q, []))
        extra = f" Tagged scenarios: {n}." if n else ""
        p(f"| {q} | {q_text[q]}{extra} |")
    p("")

    p("## Rules checked\n")
    p("| Rule | Scenarios |")
    p("|---|---|")
    rule_tags = sorted({t for s in scs for t in s["tags"]
                        if re.match(r"^(OWA|OWASP|NFR|API|BLG|SEC|MACH|ARCH)-", t)})
    for t in rule_tags:
        p(f"| {t} | {sum(r['count'] for r in by_tag[t])} |")
    p("\nAPI-46 and API-47 are checked only on the simpl problem types (401, 403, 406, 409, 413, 415), because D48 "
      "keeps the belgif types for 400, 404, 500 and 503. API-39 (Vary: Accept) is not checked: no decision fixes "
      "it for this API. OWA-API4 (call-rate limit) is outside this build (D46). NFR-03 is the load test.")


def ui_section(scs):
    ui = [s for s in scs if "ui" in s["tags"]]
    manual = [s for s in ui if "manual" in s["tags"]]
    p = print
    p("\n## UI run (step 60)\n")
    p(f"`./run.sh ui` runs the @ui scenarios in Chromium with Playwright for Python: {sum(s['count'] for s in ui)} "
      f"scenarios once outlines are expanded, {sum(s['count'] for s in manual)} of them @manual. Every scenario opens "
      "a new browser context (no cookies, no storage, no stored sign-in) and signs in through the Keycloak form of "
      "realm E1. Report: `reports/ui.json` (behave JSON); axe results: `reports/a11y.json`; a screenshot per failed "
      "scenario: `reports/ui-artifacts/`.\n")
    p("Elements are found only by decided texts, labels, column headers, routes and ARIA roles (D60, D72, D74, D75, "
      "D90, D100, D109, D114, D115). No test id, CSS class, element id or generated name of the build is used. "
      "These generic interactions are assumptions, as no decision names them:\n")
    p("- A filter is applied by pressing Enter in a filter field (the Vocabulary status choice is applied with Enter "
      "in Name contains). A build that needs its own apply button would fail the filter scenarios.")
    p("- A field error is the field's accessible description (aria-describedby) plus aria-invalid, or else the "
      "extra text in the field's own container.")
    p("- A sortable column header holds a button or carries aria-sort; a trailing arrow glyph in a header is a sort "
      "indicator, not part of its name.")
    p("- A version row is selected by a click on the control in its first cell, or on the cell itself.")
    p("- 'Required' (D33) is checked as markup (required or aria-required) or else as behaviour: Submit with the "
      "field empty shows an error under it and sends no write.")
    p("- API stubs (D24) intercept browser requests whose path ends in `/v1<path>` (E2) and answer "
      "application/problem+json with the given status and detail.\n")
    p("### @manual scenarios\n")
    if manual:
        for s in manual:
            p(f"- `{s['file']}:{s['line']}` {s['name']}")
    else:
        p("None. Every @ui scenario could be automated with the decided texts and roles.")
    p("\n### Differences between the build and the decisions (run 10)\n")
    for line in UI_DIFFERENCES:
        p(f"- {line}")
    p("\n### Test-writer fixes made while running against the build\n")
    for line in UI_FIXES:
        p(f"- {line}")


UI_DIFFERENCES = [
    "`ui_list_and_details.feature:154` (SIMPL-18559/AC1, D36): the version row shows 'This version is deprecated. "
    "Latest ACTIVE version: 3', but only the number '3' is the link; D36 makes 'Latest ACTIVE version: <n>' the "
    "link. The scenario is kept as written and fails.",
    "Observation, no failing scenario: the Change log field of the new version form is required in behaviour "
    "(error on Submit) but carries no required or aria-required markup, so assistive technology does not announce "
    "it as required before Submit. axe does not flag this; no decision names the markup.",
]

UI_FIXES = [
    "Waits used time.sleep, which stops Playwright from delivering events, so downloads were never seen; waits now "
    "use the page's own timer.",
    "Column header texts carry a sort arrow on the active column; the arrow is stripped before comparing headers.",
    "Selecting a version row clicked the cell centre and missed its control; the step now clicks the control in "
    "the first cell.",
    "'Change log is required' first demanded a required attribute that no decision names; it now accepts the "
    "decided behaviour (D33, D62).",
]


if __name__ == "__main__":
    main()
    ui_section(scenarios())
