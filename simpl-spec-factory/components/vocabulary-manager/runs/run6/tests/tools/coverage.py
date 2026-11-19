"""Builds COVERAGE.md from the feature files, ids.json and decisions.json.

Usage: python3 tools/coverage.py [path/to/input]   (default: ../input next to work/)
"""
import collections
import json
import re
import sys
from pathlib import Path

WORK = Path(__file__).resolve().parent.parent
INPUT = Path(sys.argv[1]) if len(sys.argv) > 1 else WORK.parent / "input"

# Why a decision has no scenario of its own.
DECISION_REASONS = {
    "D2": "Working rule for the writers (pinned CDM snapshot); no observable behaviour.",
    "D31": "Naming rule. The suite uses the glossary terms in step text; the observable parts (wire names, UI labels) are checked under D25 to D27 and D60.",
    "D39": "Records that the decisions win over the OpenAPI file; each listed point is tested under its own decision (D5, D7, D10, D17, D19, D21, D22, D27, D37, D38).",
    "D40": "Security scheme: the suite gets tokens with the password grant and scope openid only, so every scenario relies on it; no separate check that scopes are ignored is possible from outside.",
    "D45": "Pull only, no events: nothing to observe on the API beyond the search and content operations, which are tested under D38 and D29. Tagged on the search scenario.",
    "D61": "Inventory rule: SIMPL-28680 is out of the live set, so it has no scenario.",
    "E1": "Test users and realm; used by every scenario through the token step.",
    "E2": "Defaults only (refined by E4); contentBase is learnt from contentHref.",
    "E3": "Store choice; the reset sentence is withdrawn by E4. Not observable from outside.",
    "E4": "Run rule: every scenario creates its own data with a run id and filters on its own names. Followed by the whole suite.",
}

AC_REASONS = {}

# Step 60: how the UI scenarios are automated and what the run against the build showed.
UI_NOTES = [
    "## UI run (step 60)",
    "",
    "`./run.sh ui` runs every `@ui` scenario with Playwright for Python (Chromium, headless) and writes "
    "`reports/ui.json`. Each scenario gets a new browser context (no stored session) and signs in on the "
    "Keycloak login page of E1. Elements are found by the texts, labels and column names of the decisions "
    "(D31 to D37, D46, D60) and criteria; labels no decision fixes match case-insensitively as a whole word "
    "('Name' matches 'Name contains', 'Download' matches 'Download vocabulary'). Only the routes of D31 are "
    "written in the scenarios. No CSS class, test id or build route is used.",
    "",
    "Run of 19 Nov 2026 against the run 6 build: 41 cases (37 UI plus 4 accessibility), 40 pass, 1 fails. No scenario is `@manual`.",
    "",
    "### Failures",
    "",
    "- ui_create_and_version.feature 'A file above 5 MB is refused in the browser' (D13, D15, D37). "
    "Build differs from the decisions. The UI refuses the file and disables Submit, but shows the refusal "
    "as a message above the form ('Uploaded vocabulary file is too large...'), not as a field error under "
    "'Vocabulary file'. D37 puts errors found before submit under the field and only server errors (400, 409, "
    "413) above the form. The scenario is kept. Question for the owners: is the browser size check a field error "
    "(D37) or the 413 message of D15?",
    "",
    "### Test-writer fixes made in step 60 (meaning and tags unchanged)",
    "",
    "- 'A new version is created from the form with the assigned version number': the scenario resubmitted the "
    "latest version unchanged, which D46 refuses with 409 (the next scenario tests exactly that). It now also "
    "changes the description, a change D33 allows.",
    "- 'The list shows each vocabulary with its metadata': the scenario looked for its row on the unfiltered first "
    "page, which E4 does not allow (the store is never reset, the list pages by 20, D34). It now sets the name "
    "filter to its own vocabulary before checking the row.",
    "",
    "### Points that rely on a reading rather than a decision",
    "",
    "- 'I open the URL of the create / new version form directly' (D18): no decision gives these routes. The step "
    "learns the URL at run time by opening the form as ga-admin through its decided control ('New Vocabulary +', "
    "'Update') and then opens that URL as the scenario's user. A build whose forms have no URL of their own fails "
    "this step. Question for the owners: name the routes of the create and new version forms in D31.",
    "- Row actions: when a row action is not visible, the step opens the row's menu through its ARIA state "
    "(aria-haspopup or aria-expanded), not through a name.",
    "- Field errors (D37): proven by aria-invalid, an aria-describedby or aria-errormessage text, or text under the "
    "field beyond its label. The build puts the error text inside the field's label.",
    "- The version panel (D35): proven by the text being visible outside the version list table.",
    "- API stubs (E3, D24): Playwright answers the UI's calls to `<API base path><path>` with the OpenAPI example "
    "problem bodies (500 InternalServerErrorResponse, 404 VocabularyNotFoundResponse) or an empty list page.",
    "",
    "### Accessibility (NFR-10)",
    "",
    "`ui_accessibility.feature` runs axe-core (axe-playwright-python 0.1.8, rules tagged wcag2a, wcag2aa, wcag21a, "
    "wcag21aa) on the Vocabulary list, Vocabulary details, create form and new version form. Result: 0 violations "
    "on all four pages. axe marks 2 checks incomplete on the list page (color-contrast on 20 nodes, "
    "aria-prohibited-attr on 1 node); they need a manual check (NFR-10 check hint). Full results in `reports/a11y/`. "
    "The Keycloak login page is not checked: it is the identity provider's page, not the build's.",
    "",
]


def parse_features():
    scenarios = []
    for path in sorted((WORK / "features").glob("*.feature")):
        feature_tags, pending, name = [], [], None
        for raw in path.read_text(encoding="utf-8").splitlines():
            line = raw.strip()
            if line.startswith("@"):
                pending.extend(t for t in line.split() if t.startswith("@"))
                continue
            if line.startswith("Feature:"):
                feature_tags, pending = pending, []
                continue
            m = re.match(r"(Scenario Outline|Scenario):\s*(.*)", line)
            if m:
                rows = 0
                scenarios.append({
                    "file": path.name, "name": m.group(2), "outline": m.group(1) == "Scenario Outline",
                    "tags": set(t[1:] for t in feature_tags + pending), "rows": rows,
                })
                pending = []
                continue
            if line.startswith("|") and scenarios and scenarios[-1]["file"] == path.name and scenarios[-1]["outline"]:
                scenarios[-1]["rows"] += 1
    for s in scenarios:
        if s["outline"]:
            s["rows"] = max(0, s["rows"] - 1)  # header row
            # Outlines with several Examples blocks have one header per block; count blocks.
        s["cases"] = s["rows"] if s["outline"] else 1
    return scenarios


def fix_outline_counts(scenarios):
    """Recount outline rows exactly: one header per Examples block."""
    for path in sorted((WORK / "features").glob("*.feature")):
        lines = path.read_text(encoding="utf-8").splitlines()
        current, in_examples, header_seen = None, False, False
        idx = [s for s in scenarios if s["file"] == path.name]
        k = -1
        for raw in lines:
            line = raw.strip()
            if re.match(r"(Scenario Outline|Scenario):", line):
                k += 1
                current = idx[k]
                current["cases"] = 0 if current["outline"] else 1
                in_examples = False
            elif line.startswith("Examples"):
                in_examples, header_seen = True, False
            elif in_examples and line.startswith("|"):
                if header_seen:
                    current["cases"] += 1
                else:
                    header_seen = True
            elif in_examples and line and not line.startswith("#"):
                in_examples = False


def kind(s):
    if "ui" in s["tags"]:
        return "ui"
    if "wip" in s["tags"]:
        return "wip"
    if "store_down" in s["tags"]:
        return "store-down"
    return "api"


def main():
    scenarios = parse_features()
    fix_outline_counts(scenarios)
    ids = json.loads((INPUT / "ids.json").read_text())
    decisions = json.loads((INPUT / "decisions" / "decisions.json").read_text())
    rules = {r["id"] for r in json.loads((INPUT / "rules.json").read_text())}

    out = ["# Coverage of the Vocabulary Manager test suite", "",
           "Generated by `tools/coverage.py` from the feature files, `input/ids.json` and "
           "`input/decisions/decisions.json`. A scenario outline counts once per example row (cases).", ""]

    # Totals
    by_kind = collections.Counter()
    by_file = collections.defaultdict(lambda: collections.Counter())
    for s in scenarios:
        by_kind[kind(s)] += s["cases"]
        by_file[s["file"]][kind(s)] += s["cases"]
        by_file[s["file"]]["scenarios"] += 1
        if "negative" in s["tags"]:
            by_file[s["file"]]["negative"] += s["cases"]
    out += ["## Totals", "", "| Feature file | Scenarios | Cases | API | Store-down | UI | WIP | Negative |",
            "|---|---|---|---|---|---|---|---|"]
    for f, c in sorted(by_file.items()):
        cases = c["api"] + c["store-down"] + c["ui"] + c["wip"]
        out.append(f"| {f} | {c['scenarios']} | {cases} | {c['api']} | {c['store-down']} | {c['ui']} | {c['wip']} | {c['negative']} |")
    total_cases = sum(by_kind.values())
    out.append(f"| **total** | {len(scenarios)} | {total_cases} | {by_kind['api']} | {by_kind['store-down']} | {by_kind['ui']} | {by_kind['wip']} | "
               f"{sum(c['negative'] for c in by_file.values())} |")
    out.append("")

    # Acceptance criteria
    live = [(st["key"], ac["id"]) for st in ids["stories"] for ac in st["acs"] if not ac.get("struck")]
    out += ["## Acceptance criteria", "",
            f"{len(live)} live criteria in `ids.json` (SIMPL-28680 is out of the live set by D61; "
            "SIMPL-393 and SIMPL-395 have no criteria, D1).", "",
            "| Criterion | API cases | UI cases | WIP | Scenarios |", "|---|---|---|---|---|"]
    uncovered = []
    for key, ac in live:
        tag = "AC_" + ac.replace("/", "_")
        hits = [s for s in scenarios if tag in s["tags"]]
        api = sum(s["cases"] for s in hits if kind(s) in ("api", "store-down"))
        ui = sum(s["cases"] for s in hits if kind(s) == "ui")
        wip = sum(s["cases"] for s in hits if kind(s) == "wip")
        names = "; ".join(f"{s['file'].replace('.feature', '')}: {s['name']}" for s in hits[:4])
        if len(hits) > 4:
            names += f"; and {len(hits) - 4} more"
        out.append(f"| {ac} | {api} | {ui} | {wip} | {names or 'none'} |")
        if not hits:
            uncovered.append(ac)
    out.append("")
    out += ["### Criteria without a runnable API scenario", ""]
    ui_only = [ac for _, ac in live if not any("AC_" + ac.replace("/", "_") in s["tags"] and kind(s) in ("api", "store-down") for s in scenarios)]
    out.append("Every live criterion has at least one scenario." if not uncovered else
               "No scenario: " + ", ".join(uncovered))
    out.append("")
    out.append("These criteria have only UI or WIP scenarios. The UI scenarios run with `./run.sh ui` "
               "(Playwright, step 60); see 'UI run (step 60)' below.")
    out.append("")
    for ac in ui_only:
        hits = [s for s in scenarios if "AC_" + ac.replace("/", "_") in s["tags"]]
        reason = "UI only (screen behaviour)" if all(kind(s) == "ui" for s in hits) else ""
        if ac == "SIMPL-18162/AC4":
            reason = ("@wip: an internal error (500) cannot be caused from outside. The store-down case "
                      "returns 503 by D44, not 500, and is tested in store_down.feature.")
        out.append(f"- {ac}: {reason}")
    out.append("")

    # Decisions
    out += ["## Decisions", "", "| Decision | Topic | Cases | Kinds |", "|---|---|---|---|"]
    none = []
    for d in decisions:
        hits = [s for s in scenarios if d["id"] in s["tags"]]
        kinds = sorted({kind(s) for s in hits})
        out.append(f"| {d['id']} | {d['topic']} | {sum(s['cases'] for s in hits)} | {', '.join(kinds) or '-'} |")
        if not hits:
            none.append(d)
    out += ["", "### Decisions without a scenario", ""]
    for d in none:
        out.append(f"- {d['id']} ({d['topic']}): {DECISION_REASONS.get(d['id'], 'no reason recorded')}")
    if not none:
        out.append("Every decision has a scenario.")
    out += ["", "Decisions with scenarios that still leave parts unchecked:", "",
            "- D44: the identity provider case (unknown key id while Keycloak is down) is covered by unit tests in the code, as D44 says; GET /health 200 DEGRADED is not reachable from outside.",
            "- D43: the overlap of a version creation with a status PATCH has two valid outcomes, so it is not asserted.",
            "- D46: the call-rate limit is outside this build (Tier 1 gateway, OWA-API4); nothing to test.",
            "- D9: the location of S1 to S5 is checked to be present and not empty; its exact text is left to the OpenAPI example form, so it is not compared.",
            "- D24, D13, D18, D33 to D36, D50, D60: UI behaviour, checked by the @ui scenarios (`./run.sh ui`).",
            ""]

    # Quality targets
    q = [d for d in decisions if re.fullmatch(r"Q\d+", d["id"])]
    out += ["## Quality targets (Q decisions, NFR-03, NFR-13)", ""]
    if not q:
        out += ["No `Q` decision exists in `decisions.json`. No decision gives a response time or throughput "
                "target, so no load test is written. `./run.sh load` writes `reports/load.json` with "
                "`skipped: true` and this reason. NFR-13: there is no NFR with a target to measure in step 70.",
                "", "The limits that are stated (D15 5 MB upload, D22 page size 100, D38 100 search references, "
                "D56 20 aliases, D37 field lengths) are tested just at and just above each limit (NFR-04).", ""]
    else:
        for d in q:
            out.append(f"- {d['id']}: {d['topic']}")

    # Rules
    rule_tags = collections.Counter()
    for s in scenarios:
        for t in s["tags"]:
            if t in rules:
                rule_tags[t] += s["cases"]
    out += ["## Rules checked by the suite", "", "| Rule | Cases |", "|---|---|"]
    for r, n in sorted(rule_tags.items()):
        out.append(f"| {r} | {n} |")
    out += ["", "Rules that a black-box test could observe but the suite does not check:", "",
            "- API-25 (HTTPS only): the local stack of the build profile serves plain HTTP; TLS is the gateway's job.",
            "- API-37 (406 on an Accept header the API cannot serve): the OpenAPI lists 406, but no decision fixes it and every scenario without a criterion needs a decision tag, so it is left to the fuzzing step.",
            "- URI format of namespaceUri, aliasUris and sourceUri: no decision fixes it (suite review question 2), so it is not tested.",
            "- API-39 (Vary: Accept): no operation negotiates between several response media types.",
            "- OWA-API4 (call-rate limit): D46 puts the limit in the Tier 1 gateway, outside this build.",
            "- NFR-07 graceful shutdown: needs stopping the container during a write; outside the API suite. Health is checked.",
            "- NFR-03: no target to load-test (see above).",
            ""]
    out += UI_NOTES
    (WORK / "COVERAGE.md").write_text("\n".join(out) + "\n", encoding="utf-8")
    print(f"{len(scenarios)} scenarios, {total_cases} cases, {len(live)} criteria, uncovered {uncovered}, "
          f"decisions without scenario {[d['id'] for d in none]}")
    print(dict(by_kind))


if __name__ == "__main__":
    main()
