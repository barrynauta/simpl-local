"""Generate COVERAGE.md from the feature tags, input/ids.json and input/decisions/decisions.json.

Usage: python3 tools/coverage.py [path-to-input-dir]
"""
import json
import re
import sys
from collections import defaultdict
from pathlib import Path

HERE = Path(__file__).resolve().parents[1]
INPUT = Path(sys.argv[1]) if len(sys.argv) > 1 else HERE.parent / "input"

# Why a criterion has no runnable API scenario (UI ones still have @ui Gherkin).
AC_NOTES = {
    "SIMPL-18162/AC4": "Not testable from outside: a black-box test cannot make the component fail with 500 on demand; D44 maps an unreachable store to 503, tested in store_down.feature. Kept as a @wip scenario.",
}
OUT_OF_SCOPE_STORIES = {
    "SIMPL-393": "No criteria; D1: met by the generic upload (scenario 'A CPSV-like vocabulary ... (no seed data)', tag @D1).",
    "SIMPL-395": "No criteria; D1: replaced by the SIMPL-181xx stories.",
    "SIMPL-28680": "Out of scope (D1, D61): its criteria left the live set.",
}
DECISION_NOTES = {
    "D2": "CDM snapshot to work from; no observable behaviour.",
    "D31": "Terms and UI routes; used in the scenario wording. UI routes are checked in the @ui scenarios.",
    "D39": "Records that the decisions win over the OpenAPI file; the points it lists are tested under their own decisions.",
    "D40": "Security scheme: the suite gets tokens from Keycloak with the password grant and scope openid (vmtest/client.py); the token checks are under D17, E5, E7.",
    "D45": "Pull only, no events: absence of events is not observable through the API. Consumers read state through search, tested under D38.",
    "D61": "Scope of the inventory; nothing to test.",
    "D79": "List of contract changes; each change is tested under its own decision.",
    "D80": "Logs: not observable through the API.",
    "D81": "Withdrawn by D82.",
    "D82": "Store-side concurrency; the suite keeps the D43 outcomes (tagged @D82). The two-instance test is the build's own.",
    "D83": "Store users and network: not reachable from the API; checked by review and the build's own tests.",
    "D85": "Query bounds inside the store: not observable; the pageSize limit (D22) is tested.",
    "D87": "Documents delivered by the build; checked by review.",
    "E1": "Test users and realm; used by every authenticated scenario.",
    "E2": "Default ports and URLs; the suite reads them from the environment (E4).",
    "E3": "Store choice; tests do not reset it (E4).",
    "E4": "Run environment: every scenario makes its own data with a unique id; configuration only through environment variables.",
    "E6": "Reuse choices; review.",
    "Q4": "Design load: checked by the load test (load/locustfile.py), not by an API scenario.",
    "Q3": "Availability, backup and restore: not testable with API scenarios; step 70 or the build's backup round trip measures it.",
    "Q5": "Coverage, Trivy and query count: build and review checks, not API scenarios.",
}
Q_CHECKS = {
    "Q1": "load/locustfile.py: one task per read operation (p95 <= 300 ms) and per write (createVocabulary, createVocabularyVersion p95 <= 3 s up to 1 MB; createVocabulary5MB <= 30 s), 20 reads/s and 1 write/s, error rate 0, written to reports/load.json. Also the API scenarios 'A file of exactly 5 MB is accepted within 30 seconds' and 'A new version with a file of exactly 5 MB is accepted'.",
    "Q2": "Scenarios tagged @Q2: at the limit and above for D15/D76 (5 MB file, 6 MB request), D22 (pageSize 100/101), D37 (name 3/64 and 2/65, description and changelog 1000/1001), D38 (100/101 references and URIs), D56 (20/21 aliases, 1000/1001 description), D71 (5 MB within 30 s).",
    "Q3": "None: availability and backup/restore are operational targets (see the decision table).",
    "Q4": "load/locustfile.py: 20 users (19 readers, 1 writer), seeds 1000 vocabularies and one vocabulary with 20 versions, 10-reference searches. Files up to 5 MB.",
    "Q5": "None in this suite: build coverage, Trivy and the per-upload query count are build checks (step 50/70).",
    "Q6": "@ui scenario 'Pages have no axe violations and every action is reachable by keyboard' tagged @a11y: axe-core WCAG 2.1 AA (tags wcag2a, wcag2aa, wcag21a, wcag21aa) on the list, details, create and new version pages, results per page in reports/a11y/, plus a Tab walk over every visible action.",
}

UI_SECTION = """## UI scenarios (step 60)

Run with `./run.sh ui`: Playwright for Python (Chromium, headless), behave JSON in `reports/ui.json`, axe results in `reports/a11y/`. Every scenario opens a new browser context with no cookies, no storage state and no user data directory, and logs in through the Keycloak login form of E1. Setup data goes through the API as ga-admin.

How elements are found: only roles plus names that a decision gives. Tables by their column headers (D23, D35), row menus by 'Actions for <Name>' and 'Actions for version <n>' (D75), regions 'Version <n>', 'Validation report', 'Validation warnings' (D75), messages by role status and alert (D75), form fields by the labels of D60, filters by the labels of D72, routes of D31 and D74. Stubs (D24, E3) match the browser's API calls by method and the path suffix /v1<path> of E2, so the UI's own API base does not matter. No test ids, CSS classes or build-only names are used.

@manual scenarios: none. Every @ui scenario is automated.

Readings and assumptions of the test writer:

- 'Button' in a step ('New Vocabulary +', 'Submit') accepts a button or a link with that exact name; the decisions name the action, not its role.
- A row menu entry is a menu item, button or link with the decided label.
- The list is shared with other runs (E4, more than 1000 vocabularies). When a scenario's row is not on the first page, the step narrows the list with the decided 'Name contains' filter (D72) on the scenario uid. This does not change what the scenario checks.
- 'The field shows an error' (D37, D62): the field has aria-invalid=true or an error text tied to it with aria-describedby or aria-errormessage.
- Selecting a version (D35): a click on the control in the Version cell of its row, or on the cell.
- 'Links to version m' (D36): a link in the row named 'Latest ACTIVE version: m' or 'm'; after the click the region 'Version m' is shown.
- 'Sorted by' checks the order of the scenario's own rows and, when present, aria-sort. 'Not sortable' means no control in the header, no aria-sort, and a click does not change the order.
- Issue and warning lists: list items of the region (or table rows). Each issue holds a D9 or D47 detail and a location; each warning holds a D65 location (prefix '<p>' -> '<ns>') and a description.
- The keyboard check tabs through the page and fails when a visible, enabled action is never focused.

Differences between the build and the decisions (last run, 19 Nov 2026: 46 UI cases, 45 passed, 1 failed):

| Scenario | Difference | Class |
|---|---|---|
| Semantic errors on submit appear in the validation report and the form stays open (`features/ui/vocabulary_ui.feature:264`) | The validation report is rendered as role alert named 'Validation report', not as a region named 'Validation report' as D75 decides. The content is right: three list items with the D9 details and D65 locations. | build differs from D75 |

Accessibility result: axe-core found 0 WCAG 2.1 AA violations on /vocabularies, /vocabularies/<Name>, /vocabularies/new and /vocabularies/<Name>/versions/new, and every action on those pages was reachable with Tab.
"""


ASSUMPTIONS = [
    "SIMPL-18184/AC2 says a reference to a deprecated version fails; D11 and D47 make it a warning with 201. The scenarios tagged AC2 test the decided warning.",
    "D21 says every GET operation rejects unknown query parameters. The outline covers the 18 secured operations; GET /health is left out because D44 and D67 define it apart from the access rules.",
    "S1 location (D65): 'the literal as written' does not fix quotes, so the suite checks the subject and predicate part exactly and that the location contains the literal text.",
    "Syntax error location (D9, D60, D65): checked as starting with 'line <n>'; the column is optional.",
    "Paging links (D22, D78): page and pageSize may be left out of a link when they have their default value; every other request parameter must be kept. A link with value null counts as absent.",
    "Status sort (D23): ACTIVE sorts before DEPRECATED (value order); ties by name ascending for vocabularies, version descending for versions.",
    "D4: the older version that a new version deprecates gets exactly the new version's creationDate as lastUpdateDate (both to the second, D58).",
    "D86: when the request carries X-Correlation-Id the response returns the same value; without one the response carries a generated value.",
    "Problem texts the decisions leave free are not checked: external name conflict detail, 405 and unknown-path type and detail, field issue details (D55).",
    "An unknown property inside a search item (D52, D68) may be named 'latest' or 'items[0].latest'; both are accepted.",
    "The problem body member 'status' is optional (RFC 9457); when present it must equal the HTTP status.",
    "totalPages is 0 for an empty list, as in the OpenAPI example 'empty' of GET /vocabularies (D22 'correct totalPages').",
    "Date sorts and date-time filters wait for the next second between creations, because times are to the second (D58).",
    "GET /health on a healthy stack (store and Keycloak up) answers 200 UP (D44).",
    "ExternalVocabulary has no validationWarnings field; 'no warnings' on registration (D30) is checked as absent or empty.",
    "Scenarios with names of 3 characters cannot carry the unique id; the step retries with another random name when an earlier run took it (409).",
]


def parse_features():
    scenarios = []
    for path in sorted((HERE / "features").rglob("*.feature")):
        feature_tags, pending = [], []
        lines = path.read_text(encoding="utf-8").splitlines()
        for i, line in enumerate(lines):
            s = line.strip()
            if s.startswith("@"):
                pending.extend(t for t in s.split() if t.startswith("@"))
            elif s.startswith("Feature:"):
                feature_tags, pending = pending, []
            elif s.startswith(("Scenario:", "Scenario Outline:")):
                name = s.split(":", 1)[1].strip()
                tags = set(feature_tags + pending)
                scenarios.append({"file": str(path.relative_to(HERE)), "line": i + 1, "name": name, "tags": tags})
                pending = []
            elif s and not s.startswith("#"):
                pending = []
    return scenarios


def main():
    ids = json.loads((INPUT / "ids.json").read_text())
    decisions = json.loads((INPUT / "decisions" / "decisions.json").read_text())
    scenarios = parse_features()
    by_tag = defaultdict(list)
    for sc in scenarios:
        for t in sc["tags"]:
            by_tag[t[1:]].append(sc)

    def kind(sc):
        if "@wip" in sc["tags"]:
            return "wip"
        if "@ui" in sc["tags"]:
            return "ui"
        if "@store_down" in sc["tags"]:
            return "store-down"
        return "api"

    def cell(scs):
        return "<br>".join("%s (`%s:%d`, %s)" % (s["name"], s["file"], s["line"], kind(s)) for s in scs)

    out = []
    out.append("# Coverage of the Vocabulary Manager suite\n")
    out.append("Generated by `python3 tools/coverage.py` from the feature tags. Kinds: api (./run.sh api), "
               "store-down (./run.sh store-down), ui (./run.sh ui, Playwright browser steps), wip (excluded from every run).\n")
    out.append("Totals: %d scenarios (%d api, %d store-down, %d ui, %d wip); Scenario Outlines count once here.\n" % (
        len(scenarios), sum(kind(s) == "api" for s in scenarios), sum(kind(s) == "store-down" for s in scenarios),
        sum(kind(s) == "ui" for s in scenarios), sum(kind(s) == "wip" for s in scenarios)))

    out.append("## Acceptance criteria\n")
    out.append("| Criterion | Heading | Scenarios |")
    out.append("|---|---|---|")
    uncovered = []
    for story in ids["stories"]:
        if story["key"] in OUT_OF_SCOPE_STORIES:
            continue
        for ac in story["acs"]:
            if ac.get("struck"):
                continue
            tag = "AC_" + ac["id"].replace("/", "_")
            scs = by_tag.get(tag, [])
            runnable = [s for s in scs if kind(s) in ("api", "store-down")]
            out.append("| %s | %s | %s |" % (ac["id"], ac["heading"] or story["title"], cell(scs) or "none"))
            if not runnable:
                uncovered.append((ac["id"], scs))
    out.append("")
    out.append("### Criteria without a runnable API scenario\n")
    out.append("| Criterion | Why |")
    out.append("|---|---|")
    for ac_id, scs in uncovered:
        if ac_id in AC_NOTES:
            why = AC_NOTES[ac_id]
        elif scs and all(kind(s) == "ui" for s in scs):
            why = "UI only: covered by a @ui browser scenario (./run.sh ui)."
        else:
            why = "No scenario."
        out.append("| %s | %s |" % (ac_id, why))
    out.append("")
    out.append("### Stories without live criteria\n")
    for key, why in OUT_OF_SCOPE_STORIES.items():
        out.append("- %s: %s" % (key, why))
    out.append("")

    out.append("## Decisions\n")
    out.append("| Decision | Topic | Scenarios | Note |")
    out.append("|---|---|---|---|")
    none = []
    for d in decisions:
        scs = by_tag.get(d["id"], [])
        note = DECISION_NOTES.get(d["id"], "")
        n_api = sum(kind(s) in ("api", "store-down") for s in scs)
        n_ui = sum(kind(s) == "ui" for s in scs)
        summary = "%d api, %d ui" % (n_api, n_ui) if scs else "none"
        out.append("| %s | %s | %s | %s |" % (d["id"], d["topic"], summary, note))
        if not scs:
            none.append(d["id"])
    out.append("")
    out.append("### Decisions with no scenario\n")
    for d_id in none:
        out.append("- %s: %s" % (d_id, DECISION_NOTES.get(d_id, "No scenario.")))
    out.append("")
    out.append("Scenario names per decision: run `grep -rn '@<id>' features`.\n")

    out.append("## Quality targets (NFR-13)\n")
    out.append("| Target | Checked by |")
    out.append("|---|---|")
    for d in decisions:
        if d["id"].startswith("Q"):
            out.append("| %s %s | %s |" % (d["id"], d["topic"], Q_CHECKS.get(d["id"], "None.")))
    out.append("")

    rules = sorted({t for sc in scenarios for t in sc["tags"]
                    if re.match(r"@(OWA|OWASP|OWT|API|BLG|NFR|SEC|MACH)-", t)})
    out.append("## Rules checked\n")
    out.append("| Rule | Scenarios |")
    out.append("|---|---|")
    for r in rules:
        out.append("| %s | %d |" % (r[1:], len(by_tag[r[1:]])))
    out.append("")
    out.append(UI_SECTION)
    out.append("## Assumptions and readings of the decisions\n")
    for line in ASSUMPTIONS:
        out.append("- " + line)
    out.append("")
    (HERE / "COVERAGE.md").write_text("\n".join(out) + "\n", encoding="utf-8")
    print("COVERAGE.md written: %d scenarios, %d criteria without runnable API scenario, %d decisions without scenario"
          % (len(scenarios), len(uncovered), len(none)))


if __name__ == "__main__":
    main()
