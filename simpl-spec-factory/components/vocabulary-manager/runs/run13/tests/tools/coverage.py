"""Write COVERAGE.md from the feature tags, ids.json and decisions.json.

Usage: .venv/bin/python tools/coverage.py [path to the input folder, default ../input]
Also checks that every scenario has a criterion tag or a decision tag.
"""
import collections
import glob
import json
import os
import sys

from behave.parser import parse_file

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
INPUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "..", "input")

# Why a criterion has no scenario, or only UI scenarios.
AC_NOTES = {
    "SIMPL-18162/AC4": "500 cannot be caused from outside; kept as a @wip scenario. The UI tests stub 500 (D24); the store-down 503 is D44.",
}

# Why a decision has no scenario.
DECISION_NOTES = {
    "D2": "Model snapshot for the build; no observable behaviour.",
    "D61": "SIMPL-28680 is out of the live set; nothing to test.",
    "E3": "Store choice; withdrawn reset sentence (E4). The store itself is not visible through the API.",
    "E6": "Reuse choices of the build; not observable through the API.",
    "D80": "Log content is not observable through the API.",
    "D81": "Withdrawn by D82.",
    "Q3": "Availability, backup and restore are measured by the build and step 70, not by API scenarios.",
    "Q4": "Design load: checked by the load test (load/run_load.py), not by a scenario.",
    "Q5": "Coverage, Trivy and the bounded reference query are build-side measurements.",
    "D83": "Store users and network are deployment facts; the store has no host port in the build profile, so the suite cannot reach it.",
    "D87": "Documents delivered by the build (docs/); reviewed, not tested.",
    "D88": "Key refetch timing: D44 says the identity provider cases are unit tests in the code.",
    "D89": "Restore procedure; operations task, not observable through the API.",
    "D92": "A fifth parallel upload on one instance gets 503: not testable reliably from outside (timing, and D102 runs two instances behind a proxy). The D43 test keeps to 4 writes (D119).",
    "D93": "How the build validates requests (REUSE.md); the outcomes are tested through D21, D37, D52.",
    "D94": "Library choices; not observable.",
    "D95": "Lint, format and UI unit tests of the build.",
    "D96": "Log format; not observable through the API.",
    "D97": "Build deliverables (LICENSE, glossary, LDM).",
    "D99": "Load shape: implemented by the load test (load/run_load.py), not by a scenario.",
    "D102": "Two backend instances on the finish line; the suite runs against whatever API_URL serves.",
    "D103": "Circuit breaker timing; with the store down every call is 503 anyway (store_down.feature).",
    "D104": "Exception model of the code; its outcome (problem types) is tested under D48.",
    "D105": "Log format scope.",
    "D106": "Where the file bytes are stored; byte-exact downloads are tested under D29.",
    "D107": "No host port for the store; not in the build profile, so not reachable by the suite.",
    "D117": "CDM findings; documentation.",
    "D120": "Management port placement; covered from outside by the /actuator scenarios of D67.",
    "D124": "External registration runs no semantic validation (D30), so the base IRI of external files is not observable.",
    "D129": "Key fetch after a failure: unit tests in the code (D44).",
    "D131": "Start command of the stack.",
    "D132": "Library used for claim checks; the outcomes (issuer, audience) are tested under E5 and E7.",
    "D133": "Framework routing; the order 404, 405, 406, 415 is tested through D63, D67 and D139 scenarios.",
    "D134": "Breaker and health internals; health without keys is not reachable from outside.",
    "D135": "Code structure.",
    "D136": "Q1 per operation: the load test reports p95 per operation.",
    "D152": "Container liveness probe on the management port; not exposed.",
    "D156": "Compaction script; operations.",
    "D157": "Retry backoff inside the service; not observable.",
    "D164": "Failed-token limiting belongs to the gateway; nothing to test in the build.",
    "D167": "UI component library; step 60 tests behaviour, not the library.",
    "E1": None, "E2": None, "E4": None,
}

LOAD_DECISIONS = {"Q1": "load task per read and write operation, plus the 5 MB write test",
                  "Q4": "20 read users and 1 write user on the D99 seed", "D99": "seed, offered rates, 5 minutes",
                  "D136": "p95 per operation", "D71": "5 MB write test"}


def scenarios():
    out = []
    for fn in sorted(glob.glob(os.path.join(ROOT, "features", "*.feature"))):
        feature = parse_file(fn)
        for sc in feature.scenarios:
            cases = []
            if hasattr(sc, "examples") and sc.examples:
                for ex in sc.examples:
                    n = len(ex.table.rows) if ex.table else 0
                    cases.append((set(t for t in sc.effective_tags) | set(ex.tags), n))
            else:
                cases.append((set(sc.effective_tags), 1))
            for tags, n in cases:
                out.append({"feature": os.path.basename(fn), "name": sc.name, "tags": tags, "cases": n})
    return out


UI_SECTION = [
    "## UI run (step 60)", "",
    "`./run.sh ui` runs every @ui scenario in headless Chromium with Playwright for Python and writes behave "
    "JSON to `reports/ui.json`. Each scenario opens a new browser context: no stored session, cookies or "
    "storage. A failed UI step leaves its URL and accessibility tree in `reports/ui-failures/`.", "",
    "@manual scenarios: none. Every @ui scenario could be automated with the roles, labels and texts of the "
    "decisions (D18, D23, D35, D60, D72, D75, D90, D109, D114, D115, D118, D141, D163) and the decided routes "
    "(D31, D74, D163) and API base path (E2).", "",
    "Assumptions of the UI steps (no decision fixes these; they use no build detail):", "",
    "- Sign-in uses the stock Keycloak login form of the decided identity provider (E1): 'Username or email', "
    "'Password', 'Sign In'.",
    "- An action a scenario calls a button may be a button or a link: the decisions fix the labels, not the "
    "roles ('New Vocabulary +' is a link in run 13).",
    "- Menu items (D72) are found by role menuitem, or else by their exact visible text.",
    "- A field error (D37, D62, D76, D101) is marked with aria-invalid=\"true\"; a given error text is read "
    "through aria-describedby or aria-errormessage, or else as visible text.",
    "- 'No request was sent to the API' means no POST, PUT, PATCH or DELETE to the API base path /v1.",
    "- Route stubs (D24) match the decided API base path /v1 plus the scenario's path and answer "
    "application/problem+json. The 101-version stub copies the wire format from the real first page.",
    "- A row a scenario does not filter for is looked up by following 'Next page' (D115), because the store is "
    "never reset (E4).",
    "- 'I see the text' and 'I do not see the text' match the text as a whole phrase, case-sensitive, so "
    "'Deprecate' does not match 'Deprecated'.", "",
    "Result against run 13 (21 Nov 2026, two full runs in a row): 59 of 59 UI cases pass; axe reports 0 "
    "violations on the four main pages; every list action is reachable by Tab. No difference between the build "
    "and the specs or decisions was found in the UI.", "",
    "Test-writer fixes made in step 60:", "",
    "- `ui_details.feature` 'A reactivated old version never links to itself': the two last checks were page-wide. "
    "D36 makes the version list show 'This version is deprecated.' and 'Latest ACTIVE version: 1' in the row of "
    "version 2, so a correct build failed them. They now check the region 'Version 1' (D64 is about the version "
    "shown). Tags unchanged.",
    "- Fixture placeholders: `{name}` takes the typed name only when it is a valid name (D37), so the "
    "'lower case name' scenario uploads valid Turtle, as the brief of step 40 noted.", "",
]


def main():
    ids = json.load(open(os.path.join(INPUT, "ids.json")))
    decisions = json.load(open(os.path.join(INPUT, "decisions", "decisions.json")))
    scs = scenarios()

    problems = [s for s in scs if not any(t.startswith(("AC_", "D", "E", "Q")) for t in s["tags"])]
    if problems:
        print("scenarios without a criterion or decision tag:", [p["name"] for p in problems])

    by_tag = collections.defaultdict(list)
    for s in scs:
        for t in s["tags"]:
            entry = f'{s["feature"]}: {s["name"]}'
            if entry not in by_tag[t]:
                by_tag[t].append(entry)

    def kind_count(pred):
        return sum(s["cases"] for s in scs if pred(s["tags"]))

    lines = ["# Coverage", "",
             "Generated by `tools/coverage.py` from the tags of `features/*.feature`, `input/ids.json` and "
             "`input/decisions/decisions.json`. A scenario outline counts once per Examples block in the tables "
             "below; the case counts expand the examples.", ""]

    # Counts
    lines += ["## Counts", "", "| Feature | Scenarios (definitions) | Cases (examples expanded) |", "|---|---|---|"]
    per_feature = collections.OrderedDict()
    for s in scs:
        d = per_feature.setdefault(s["feature"], [set(), 0])
        d[0].add(s["name"])
        d[1] += s["cases"]
    for f, (names, cases) in per_feature.items():
        lines.append(f"| {f} | {len(names)} | {cases} |")
    total_defs = sum(len(v[0]) for v in per_feature.values())
    total_cases = sum(v[1] for v in per_feature.values())
    lines.append(f"| **total** | **{total_defs}** | **{total_cases}** |")
    lines += ["", "| Tag kind | Cases |", "|---|---|",
              f"| run by `./run.sh api` (not @ui, @wip, @store_down) | {kind_count(lambda t: not t & {'ui', 'wip', 'store_down'})} |",
              f"| @store_down | {kind_count(lambda t: 'store_down' in t)} |",
              f"| @ui, run by `./run.sh ui` (Playwright, step 60) | {kind_count(lambda t: 'ui' in t and 'manual' not in t)} |",
              f"| @manual (not automated, reason below) | {kind_count(lambda t: 'manual' in t)} |",
              f"| @wip | {kind_count(lambda t: 'wip' in t)} |",
              f"| @negative | {kind_count(lambda t: 'negative' in t)} |",
              f"| with a criterion tag | {kind_count(lambda t: any(x.startswith('AC_') for x in t))} |",
              f"| with only decision tags | {kind_count(lambda t: not any(x.startswith('AC_') for x in t))} |", ""]

    # Criteria
    lines += ["## Acceptance criteria", "",
              "Every live criterion of `ids.json` (SIMPL-28680 is not in the live set, D61; SIMPL-393 and "
              "SIMPL-395 have no criteria, D1).", "",
              "| Criterion | API scenarios | UI scenarios (@ui) | Note |", "|---|---|---|---|"]
    uncovered = []
    for story in ids["stories"]:
        for ac in story["acs"]:
            if ac.get("struck"):
                continue
            tag = "AC_" + ac["id"].replace("/", "_")
            entries = by_tag.get(tag, [])
            ui = [e for e in entries if e.startswith("ui_")]
            api = [e for e in entries if not e.startswith("ui_") and not e.startswith("not_runnable")]
            note = AC_NOTES.get(ac["id"], "")
            if not api and ui and not note:
                note = "UI criterion; API behaviour behind it is covered by the decision scenarios."
            if not entries:
                uncovered.append(ac["id"])
            lines.append(f"| {ac['id']} | {'<br>'.join(api) or '-'} | {'<br>'.join(ui) or '-'} | {note} |")
    lines += ["", f"Criteria without any scenario: {', '.join(uncovered) or 'none'}.", ""]

    # Decisions
    lines += ["## Decisions", "", "| Decision | Topic | Scenarios | Note |", "|---|---|---|---|"]
    none = []
    for d in decisions:
        did = d["id"]
        entries = by_tag.get(did, [])
        note = DECISION_NOTES.get(did) or ""
        if did in LOAD_DECISIONS:
            note = (note + " " if note else "") + f"Load test: {LOAD_DECISIONS[did]}."
        if d.get("status") == "withdrawn":
            note = note or "Withdrawn."
        shown = entries[:6] + ([f"... {len(entries) - 6} more"] if len(entries) > 6 else [])
        if not entries:
            none.append(did)
        lines.append(f"| {did} | {d.get('topic', '')} | {'<br>'.join(shown) or '-'} | {note} |")
    lines += ["", f"Decisions without a scenario: {', '.join(none)}. Each has its reason in the table.", ""]

    # Quality targets
    lines += ["## Quality targets (NFR-13)", "", "| Target | Checked by | Note |", "|---|---|---|",
              "| Q1 Response time and throughput | `./run.sh load`: one Locust task per Q1 operation (12 reads, 2 writes), "
              "20 reads/s and 1 write/s offered for 5 min after warm-up, p50/p95/p99 and error rate per operation in "
              "`reports/load.json`; separate 5 MB write test. Scenarios `A file of exactly 5 MB is accepted within 30 "
              "seconds` (create and new version) check D71 in the API run. | p95 targets 300 ms (reads), 3 s (writes up "
              "to 1 MB), 30 s (5 MB). |",
              "| Q2 Limits | Scenarios tagged @Q2: at the limit and above for D15 (5 MB file), D22 (page, pageSize), D37 "
              "(name, description, changelog), D38 (100 items, 100 URIs), D56 (20 aliases, description), D71 (5 MB "
              "write time), D76 (6 MB request), plus D125 and D150. | NFR-04. |",
              "| Q3 Availability and recovery | Not measured by the suite. | Backup and restore round trip is the build's "
              "own test; availability needs production monitoring. |",
              "| Q4 Design load | `./run.sh load`: seed of 1000 vocabularies x 20 versions (D99), 20 concurrent read users. "
              "| Two instances (D102) are the stack's concern. |",
              "| Q5 Maintainability | Not measured by the suite. | Coverage, Trivy and the bounded reference query are "
              "measured by the build and step 70. |",
              "| Q6 Accessibility | @ui scenarios `No accessibility violations on ...` (axe-core, WCAG 2.0 and 2.1 A "
              "and AA tags, one per main page: list, details, create form, new version form) and keyboard "
              "reachability. | Run by `./run.sh ui`; axe runs with the CSP bypass of D123. |", ""]

    # Rules
    rule_tags = sorted({t for s in scs for t in s["tags"] if t[:4] in ("OWAS", "OWT-", "OWA-", "API-", "NFR-", "BLG-", "MACH", "ARCH")})
    lines += ["## Rules checked", "", "| Rule | Scenarios |", "|---|---|"]
    for r in rule_tags:
        lines.append(f"| {r} | {len(by_tag[r])} |")
    lines += ["",
              "Rules a black-box test cannot observe or that the decisions place outside this build: OWA-API4 "
              "(call-rate limit, Tier 1 gateway, D46), OWT-A07 failed-token limiting (gateway, D164), API-39 "
              "(no operation negotiates between several success media types), API-46 on belgif codes (recorded "
              "deviation, D48: checked on the simpl codes only), API-25 (TLS is the platform's), MSV-11 (contract "
              "tests from consumers).", ""]
    lines += UI_SECTION
    with open(os.path.join(ROOT, "COVERAGE.md"), "w") as fh:
        fh.write("\n".join(lines))
    print(f"COVERAGE.md written: {total_defs} scenario definitions, {total_cases} cases, "
          f"{len(uncovered)} criteria and {len(none)} decisions without a scenario")


if __name__ == "__main__":
    main()
