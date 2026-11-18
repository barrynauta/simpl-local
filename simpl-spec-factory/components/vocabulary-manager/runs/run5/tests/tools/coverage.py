"""Generate COVERAGE.md from the feature tags, input/ids.json and input/decisions/decisions.json.

Run from the work folder:  .venv/bin/python tools/coverage.py
"""
import json
import os
import re
from collections import defaultdict

from behave.parser import parse_file

WORK = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
INPUT = os.path.join(os.path.dirname(WORK), "input")
FEATURES = os.path.join(WORK, "features")

# Why a decision has no scenario of its own.
DECISION_REASONS = {
    "D2": "Which CDM snapshot the writers use is not observable through the API.",
    "D39": "Records that the decisions win over the OpenAPI file; adds no behaviour. The points it lists are tested under their own decisions (D5, D7, D10, D17, D19, D21, D22, D27, D37, D38).",
    "D61": "Takes SIMPL-28680 out of the live set; nothing to test (its criteria are not in ids.json).",
    "E2": "Default ports and URLs; replaced by the build profile and environment variables (E4).",
    "E3": "The store technology (Fuseki) is not observable from outside; its reset rule is withdrawn by E4.",
    "E4": "Applied by the harness itself: every URL and user comes from environment variables, every scenario creates its own data with a unique run id and checks counts through a name filter on that id.",
}

# Step 60: how the @ui scenarios are automated, and what is not.
UI_SECTION = [
    "## UI automation (step 60)", "",
    "- Browser steps: Playwright for Python (Chromium, headless) in `features/steps/ui_steps.py`; the browser is "
    "started in `features/environment.py` and every `@ui` scenario gets a fresh browser context. Elements are "
    "found by role, label and text; no test ids (the decisions name none).",
    "- Login: the Keycloak form of the build is filled with the E1 user of the role; the session is kept per role "
    "in `.ui-state/` so later scenarios reuse it.",
    "- API stubs (D24): `the API answers ... for the UI` fulfils the matching browser request under `UI_API_PREFIX` "
    "(default `/v1`, the path the UI calls through its nginx proxy) with a problem+json body.",
    "- Rows that are not on the first list page (the shared store holds many vocabularies, E4) are found by typing "
    "the name in the 'Name contains' filter. 'Opens the create or new version form by its URL' uses the routes the "
    "app shows (`/vocabularies/new`, `/vocabularies/<Name>/update`).",
    "- 'The actions menu offers [...]' checks that every listed action is there and that no write action outside "
    "the list is offered; the extra read action 'History' is allowed (D35 names it).",
    "- 'An error message says' accepts an element with role alert or status (the scenarios do not fix the role).",
    "- Accessibility: `ui_accessibility.feature`, one `@a11y` scenario per main page (list, details, creation form, "
    "new version form), axe-core through axe-playwright-python with the tags wcag2a, wcag2aa, wcag21a, wcag21aa. "
    "Results per page in `reports/axe/<page>.json`.",
    "- Set-up fix: 'A sortable column sorts ascending on the first click' waits one more second before the status "
    "change, so the PATCH does not share a second with the creation of Beta (times are kept to the second, D58).",
    "- `@manual`: none. Every `@ui` scenario runs in the browser.",
    "",
]

# Why a live criterion has no runnable API scenario (UI or @wip only).
AC_NOTES = {
    "SIMPL-18162/AC4": "500 cannot be caused from outside without a fault in the build; kept as a @wip scenario. D44 covers the store-down case with 503.",
}


def scenarios():
    out = []
    for name in sorted(os.listdir(FEATURES)):
        if not name.endswith(".feature"):
            continue
        feature = parse_file(os.path.join(FEATURES, name))
        for sc in feature.scenarios:
            tags = set(feature.tags) | set(sc.tags)
            examples = 0
            if hasattr(sc, "examples") and sc.examples:
                examples = sum(len(ex.table.rows) for ex in sc.examples if ex.table)
            out.append({"feature": name, "name": sc.name, "tags": tags,
                        "line": sc.line, "examples": examples})
    return out


def kind(sc):
    if "wip" in sc["tags"]:
        return "wip"
    if "ui" in sc["tags"]:
        return "ui"
    if "store-down" in sc["tags"]:
        return "api, store down"
    return "api"


def ref(sc):
    ex = f" ({sc['examples']} examples)" if sc["examples"] else ""
    return f"{sc['feature']}:{sc['line']} {sc['name']}{ex} [{kind(sc)}]"


def main():
    ids = json.load(open(os.path.join(INPUT, "ids.json")))
    decisions = json.load(open(os.path.join(INPUT, "decisions", "decisions.json")))
    scs = scenarios()

    by_ac, by_dec, by_rule = defaultdict(list), defaultdict(list), defaultdict(list)
    rule_re = re.compile(r"^(OWA|OWT|OWASP|API|BLG|ARCH|SEC|MACH|MSV|CODE|DATA|OPS|DDD|UX)-")
    for sc in scs:
        for t in sc["tags"]:
            if t.startswith("AC_"):
                key = t[3:].replace("_AC", "/AC")
                by_ac[key].append(sc)
            elif re.match(r"^[DE]\d+$", t):
                by_dec[t].append(sc)
            elif rule_re.match(t):
                by_rule[t].append(sc)

    live = [(s["key"], s["title"], a) for s in ids["stories"] for a in s["acs"] if not a.get("struck")]
    live_ids = {a["id"] for _, _, a in live}
    unknown = sorted(set(by_ac) - live_ids)

    n_api = sum(1 for s in scs if kind(s) == "api")
    n_ui = sum(1 for s in scs if kind(s) == "ui")
    n_wip = sum(1 for s in scs if kind(s) == "wip")
    n_down = sum(1 for s in scs if kind(s) == "api, store down")
    n_neg = sum(1 for s in scs if "negative" in s["tags"])
    n_ex = sum(max(1, s["examples"]) for s in scs)

    lines = ["# Coverage of the Vocabulary Manager suite (run 5, step 40)", "",
             "Generated by `tools/coverage.py` from the feature tags, `input/ids.json` and "
             "`input/decisions/decisions.json`. Do not edit by hand; run the tool again.", "",
             "## Totals", "",
             f"- Scenarios (an outline counts once): {len(scs)}; executed cases (outline examples counted): {n_ex}.",
             f"- API: {n_api}; API with an unreachable store (`@store-down`): {n_down}; UI (`@ui`, browser steps since step 60): {n_ui}, of which `@a11y`: {sum(1 for s in scs if "a11y" in s["tags"])}; `@manual`: {sum(1 for s in scs if "manual" in s["tags"])}; `@wip`: {n_wip}.",
             f"- Negative (`@negative`): {n_neg}.",
             f"- Live acceptance criteria: {len(live_ids)}; with at least one scenario: {len(live_ids & set(by_ac))}.",
             f"- Decisions: {len(decisions)}; with at least one scenario: {len({d['id'] for d in decisions} & set(by_dec))}.",
             ""]
    if unknown:
        lines += [f"Tags that name no live criterion: {', '.join(unknown)}", ""]

    lines += ["## Acceptance criteria", "",
              "| Criterion | Story | Heading | API | UI | Scenarios |", "|---|---|---|---|---|---|"]
    for key, title, ac in live:
        scl = by_ac.get(ac["id"], [])
        api = sum(1 for s in scl if kind(s).startswith("api"))
        ui = sum(1 for s in scl if kind(s) == "ui")
        refs = "<br>".join(ref(s) for s in scl) or "none"
        heading = ac["heading"].replace("|", "/") or "-"
        lines.append(f"| {ac['id']} | {title} | {heading} | {api} | {ui} | {refs} |")
    lines.append("")

    lines += ["### Criteria without a runnable API scenario, and why", ""]
    for key, title, ac in live:
        scl = by_ac.get(ac["id"], [])
        if not scl:
            lines.append(f"- {ac['id']}: no scenario. {AC_NOTES.get(ac['id'], 'REASON MISSING')}")
        elif not any(kind(s).startswith("api") for s in scl):
            reason = AC_NOTES.get(ac["id"]) or (
                "UI criterion: covered by `@ui` scenarios run in the browser (step 60)."
                if all(kind(s) == "ui" for s in scl) else "see the scenarios")
            lines.append(f"- {ac['id']} ({title}): {reason}")
    lines += ["",
              "Stories without criteria: SIMPL-393 (met by the generic upload, D1; one scenario tagged `@D1` "
              "uploads a CPSV-like file and finds it in the list) and SIMPL-395 (replaced by the SIMPL-181xx "
              "stories, D1). SIMPL-28680 is out of scope (D1, D61) and not in `ids.json`.", ""]

    lines += ["## Decisions", "", "| Decision | Topic | Status | Scenarios | Examples of scenarios |",
              "|---|---|---|---|---|"]
    for d in decisions:
        scl = by_dec.get(d["id"], [])
        sample = "<br>".join(ref(s) for s in scl[:4]) + (f"<br>... and {len(scl) - 4} more" if len(scl) > 4 else "")
        extra = f" (refined by {d['refined_by']})" if d.get("refined_by") else ""
        lines.append(f"| {d['id']} | {d['topic']}{extra} | {d['status']} | {len(scl)} | {sample or 'none'} |")
    lines += ["", "### Decisions without a scenario, and why", ""]
    for d in decisions:
        if not by_dec.get(d["id"]):
            lines.append(f"- {d['id']} ({d['topic']}): {DECISION_REASONS.get(d['id'], 'REASON MISSING')}")
    lines += ["", "### Decisions covered only by `@ui` or `@store-down` scenarios", ""]
    for d in decisions:
        scl = by_dec.get(d["id"], [])
        if scl and not any(kind(s) == "api" for s in scl):
            kinds = sorted({kind(s) for s in scl})
            lines.append(f"- {d['id']} ({d['topic']}): {', '.join(kinds)}")
    lines.append("")

    lines += ["## Rules checked from outside", "",
              "Rules from `input/rules.json` that a black-box test can observe. Rules about code, "
              "pipelines, Kafka messages, TLS between agents, scaling or documentation are left to the "
              "review steps.", "", "| Rule | Scenarios |", "|---|---|"]
    for rule in sorted(by_rule):
        lines.append(f"| {rule} | {len(by_rule[rule])} |")
    lines += ["",
              "Not checked from outside, with reason:",
              "- OWA-API4 (call-rate limit): D46 places it in the Tier 1 gateway, outside this build; it is not in rules.json for this run either.",
              "- API-37 (406 on an Accept the API cannot serve): the OpenAPI lists 406, but no criterion or decision fixes it, and every scenario must carry a criterion or decision tag.",
              "- API-39 (Vary: Accept): no operation negotiates between several response media types in this API.",
              "- API-25, SEC-08 to SEC-10, OWT-A04 (TLS): the build profile serves plain HTTP on localhost; TLS is a gateway concern here.",
              "- OWA-API7 (server calls a client URL): no operation fetches a client URL; external vocabularies are uploaded, never fetched (E3).",
              "- OWA-API10, OWT-A09, CODE-26: need the logs or the code, not the API.",
              ""]
    lines += UI_SECTION
    with open(os.path.join(WORK, "COVERAGE.md"), "w", encoding="utf-8") as fh:
        fh.write("\n".join(lines).replace("\u2013", "-").replace("\u2014", "-"))
    missing = [d["id"] for d in decisions if not by_dec.get(d["id"]) and d["id"] not in DECISION_REASONS]
    missing_ac = [a for a in live_ids if a not in by_ac and a not in AC_NOTES]
    print(f"{len(scs)} scenarios; ACs covered {len(live_ids & set(by_ac))}/{len(live_ids)}; "
          f"decisions without scenario or reason: {missing}; ACs without scenario or reason: {missing_ac}; unknown AC tags: {unknown}")


if __name__ == "__main__":
    main()
