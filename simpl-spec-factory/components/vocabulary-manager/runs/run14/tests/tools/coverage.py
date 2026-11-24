"""Generate COVERAGE.md from the feature tags, ids.json and decisions.json.

Usage: python3 tools/coverage.py [input_dir]   (default ../input)
Reasons for criteria and decisions without a scenario are kept in REASONS below.
"""
import collections
import glob
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
INPUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "..", "input")

AC_REASONS = {}

DECISION_REASONS = {
    "D2": "Process decision (CDM snapshot); nothing observable over HTTP.",
    "D6": "Covered as publication (201, ACTIVE) through SIMPL-18186/18187 scenarios; the UI labels are in @ui scenarios.",
    "D13": "Browser-side check; in @ui scenarios only.",
    "D31": "Naming rule for code and texts; scenarios use the glossary terms throughout.",
    "D39": "Records that decisions win over the OpenAPI file; every listed point has its own scenario (D5, D7, D10, D17, D19, D21, D22, D27, D37, D38).",
    "D40": "Token acquisition: the suite requests tokens from Keycloak without scopes (vmtest/client.py); no own scenario.",
    "D45": "Pull only, no events: absence of events is not observable through the API.",
    "D61": "Inventory rule: SIMPL-28680 is out of scope; no scenario.",
    "D79": "List of contract changes; each item is tested under its own decision.",
    "D80": "Log content is not observable over HTTP (step 50 review).",
    "D81": "Withdrawn by D82.",
    "D83": "Store users and network layout; not observable through the API.",
    "D85": "Store query bounds; observable only indirectly (paging scenarios, load test).",
    "D87": "Build deliverables (docs/), checked in step 50.",
    "D88": "Key refetch timing needs control over the identity provider keys; unit tests in the build (D44).",
    "D89": "Restore procedure; operations task, checked in step 70.",
    "D92": "At most 4 uploads per instance: a fifth parallel upload is timing-dependent and would make the D43 test flaky (D119); not tested from outside.",
    "D93": "Build choice (REUSE.md), checked in step 50.",
    "D94": "Library choice, checked in step 50.",
    "D95": "Lint and format on the finish line, checked in step 50.",
    "D96": "Log format, not observable over HTTP.",
    "D97": "Build deliverables (LICENSE, glossary), checked in step 50.",
    "D98": "Stack hardening (ports, limits), checked in step 70.",
    "D99": "Load test shape: implemented by load/ (Q1).",
    "D102": "Two backend instances: stack layout, checked on the finish line (smoke.sh).",
    "D104": "Exception model inside the build; the problem types it yields are tested under D48.",
    "D105": "Log format scope; not observable over HTTP.",
    "D106": "Storage layout; not observable over HTTP.",
    "D107": "No host port for the store; checked in step 70.",
    "D108": "Lazy key fetch; health stays UP is tested under D44; fetch timing is unit-tested.",
    "D116": "Contract formats (openapi-decided.yaml), checked in step 50; behaviour tested under D20, D9, D49.",
    "D117": "Model deliverable, checked in step 50.",
    "D120": "Management port layout; /actuator paths through the API port are tested under D67.",
    "D121": "The container ignores a part with an empty name; requests cannot send a multipart part with an empty name reliably, and the outcome depends on the rest of the request.",
    "D122": "Front proxy: the API port of the build profile may be the backend or the proxy; 405, 400 and 413 answers are tested under D67, D158 and D15.",
    "D123": "Content-Security-Policy of the UI; step 60 (axe with CSP bypass).",
    "D129": "Unknown key id after a failed fetch needs control over Keycloak; unit tests (D44).",
    "D131": "Start command, used by the orchestrator.",
    "D132": "Library choice for claim checks; the outcomes (issuer, audience, expiry) are tested under E5, E7, E8.",
    "D133": "Routing implementation; the 404, 405, 406, 415 outcomes are tested under D63, D67, D139.",
    "D134": "Breaker failure counting and health without key fetch: not observable without fault injection; health is tested under D44.",
    "D135": "Code structure rule (deviation record).",
    "D136": "Per-operation p95 is what load/ reports (Q1).",
    "D152": "Container health check uses the management port; GET /health stays UP while the store is down is not reachable from outside the stack.",
    "D156": "Compaction script, operations; step 70.",
    "D157": "Retry backoff timing inside the service; the outcomes are tested under D43.",
    "D164": "Failed-token limiting is the gateway's; nothing to test in the component.",
    "D165": "createdBy is shown to viewers (tested under D27); logging and model marks are step 50.",
    "D167": "UI component library, step 50 and 60.",
    "D170": "Slot refusal before the body is read: timing-dependent (see D92).",
    "D173": "Logs of every container, step 50 and 70.",
    "D174": "Parallel requests during the first key fetch need control over Keycloak; unit tests.",
    "D175": "Trivy scan, finish line.",
    "D176": "Status-change receipts are internal to the store.",
    "D179": "SIMPL-18162/AC4 is covered by unit tests and the store-down 503 cases; the @wip scenario is the record.",
    "D183": "Load test rule: implemented in load/ (writes not retried, 503 counted as error).",
    "D185": "Platform items (TLS, encryption at rest, alerting); not in the component.",
    "D186": "THREATS.md deliverable, step 50.",
    "D187": "Rule status, no behaviour.",
    "D188": "Store size for Q1: load/ seeds the D99 set on the store as it is.",
    "D189": "Text of Q4, no behaviour.",
    "D190": "Management port and proxy layout, smoke.sh; /actuator via the API port is tested under D67.",
    "D191": "Availability not measured (stated in QUALITY.md).",
    "D193": "Store compaction access, internal network only.",
    "D194": "Hash comparison inside the build; the outcome is tested under D46.",
    "D200": "Browser and server Turtle checks; the server side is tested under D9, the browser side in @ui scenarios.",
    "D201": "Store 401 or 403 needs a misconfigured store; not reachable from the suite.",
    "D202": "Container CPU and memory defaults, step 70.",
    "D203": "Contract file content, step 50 and fuzzing.",
    "D204": "Free issue detail inside a 404: the suite checks only in and name, as decided.",
    "D206": "Build-internal routing table test.",
    "D207": "CDM points for the owners, no behaviour.",
    "D208": "Records the difference with the existing build; the D3 behaviour itself is tested under D3.",
    "E2": "Default ports and URLs; the suite reads every value from the environment (E4).",
    "E3": "Store choice; the reset sentence is withdrawn by E4.",
    "E4": "The suite follows E4: every scenario uses its own run id and filters on it; no store reset.",
    "E6": "Reuse choices, step 50.",
    "Q3": "Availability and recovery: backup and restore round trip is measured in step 70; availability not measured (D191).",
    "Q4": "Design load: used as the load shape of load/ (D99); two instances on the finish line (D102).",
    "Q5": "Coverage, Trivy and the bounded reference check are measured by the build and step 70, not by this suite.",
}

QUALITY = {
    "Q1": "load/ (Locust): per-operation p50/p95/p99 and error rate for the 8 read operations at 20 reads/s and createVocabularyVersion at 1 write/s for 5 minutes after warm-up, plus 3 writes at the 5 MB limit (D71, D99, D136, D183). The API scenarios '@Q1' only check that the 5 MB file is accepted.",
    "Q2": "API scenarios tagged @Q2: each limit at the limit and one above (name 3 and 64/65, description and changelog 1000/1001, pageSize 100/101, search 100/101 items and URIs, aliases 20/21, file 5 MB / +1 byte, request 6 MB, 64 parts, JSON body 1 MB, validation report 1000 issues). The 4 parallel uploads limit of D92 is not tested (timing-dependent).",
    "Q3": "Not in this suite: backup and restore round trip is measured in step 70; availability is not measured (D191).",
    "Q4": "Load shape of load/ (seed 1000 vocabularies x 20 versions, D99); the two-instance stack is on the finish line (D102).",
    "Q5": "Not in this suite: coverage and Trivy are measured by the build and step 70.",
    "Q6": "@ui scenarios in features/ui/accessibility_ui.feature (axe on four pages, keyboard reach); step 60 adds the browser steps.",
}


def scenarios():
    result = []
    for path in sorted(glob.glob(os.path.join(ROOT, "features", "**", "*.feature"), recursive=True)):
        rel = os.path.relpath(path, ROOT)
        lines = open(path, encoding="utf-8").read().splitlines()
        feature_tags, pending, current = [], [], None
        in_examples, header_seen = False, False
        for number, line in enumerate(lines, 1):
            text = line.strip()
            if text.startswith("@"):
                pending += text.split()
            elif text.startswith("Feature:"):
                feature_tags, pending = pending, []
            elif text.startswith(("Scenario:", "Scenario Outline:")):
                current = {"file": rel, "line": number, "name": text.split(":", 1)[1].strip(),
                           "tags": set(t[1:] for t in feature_tags + pending), "examples": 0,
                           "outline": text.startswith("Scenario Outline:")}
                result.append(current)
                pending, in_examples = [], False
            elif text.startswith("Examples:"):
                in_examples, header_seen = True, False
            elif in_examples and text.startswith("|"):
                if header_seen:
                    current["examples"] += 1
                header_seen = True
    return result


def kind(item):
    if "ui" in item["tags"]:
        return "UI"
    if "wip" in item["tags"]:
        return "wip"
    if "store_down" in item["tags"]:
        return "store-down"
    return "API"


def ref(item):
    return f"{item['file']}:{item['line']}"


def main():
    ids = json.load(open(os.path.join(INPUT, "ids.json")))
    decisions = json.load(open(os.path.join(INPUT, "decisions", "decisions.json")))
    items = scenarios()
    by_tag = collections.defaultdict(list)
    for item in items:
        for tag in item["tags"]:
            by_tag[tag].append(item)

    out = ["# Coverage of the Vocabulary Manager suite", "",
           "Generated by `tools/coverage.py` from the feature tags, `input/ids.json` and "
           "`input/decisions/decisions.json`. A scenario outline counts once; its example rows are in brackets.", ""]
    counts = collections.Counter(kind(i) for i in items)
    cases = collections.Counter()
    for i in items:
        cases[kind(i)] += i["examples"] or 1
    out += ["## Totals", "", "| Kind | Scenarios | Cases (outline rows expanded) |", "|---|---|---|"]
    for k in ("API", "store-down", "wip", "UI"):
        out.append(f"| {k} | {counts[k]} | {cases[k]} |")
    out.append(f"| all | {len(items)} | {sum(cases.values())} |")
    out.append("")

    out += ["## Acceptance criteria", "",
            "SIMPL-28680 is out of the live set (D1, D61). SIMPL-393 and SIMPL-395 have no criteria; SIMPL-393 is met by "
            "the generic upload (D1) in `features/vocabulary_create.feature` (CPSV-like file).", "",
            "| Criterion | API scenarios | UI scenarios (step 60) | Other |", "|---|---|---|---|"]
    uncovered = []
    for story in ids["stories"]:
        for ac in story["acs"]:
            if ac.get("struck"):
                continue
            key, acn = ac["id"].split("/")
            tag = f"AC_{key}_{acn}"
            found = by_tag.get(tag, [])
            api = [ref(i) for i in found if kind(i) in ("API", "store-down")]
            ui = [ref(i) for i in found if kind(i) == "UI"]
            other = [f"{ref(i)} (wip)" for i in found if kind(i) == "wip"]
            if not found:
                uncovered.append(ac["id"])
            out.append(f"| {ac['id']} {story['title'][:40]} | {len(api)}: {', '.join(api[:4])}{' ...' if len(api) > 4 else ''} "
                       f"| {len(ui)}: {', '.join(ui[:3])}{' ...' if len(ui) > 3 else ''} | {'; '.join(other)} |")
    out += ["", "### Criteria without a scenario", ""]
    if uncovered:
        for ac_id in uncovered:
            out.append(f"- {ac_id}: {AC_REASONS.get(ac_id, 'no reason recorded')}")
    else:
        out.append("None. Every live criterion has at least one scenario. Criteria of UI stories are covered by @ui "
                   "scenarios (Gherkin only until step 60 adds browser steps); where a UI criterion has an API side "
                   "(validation, versioning, deprecation, publication) the API scenarios carry its tag too. "
                   "SIMPL-18162/AC4 (500) has only a @wip scenario and the store-down 503 cases (D179).")
    out.append("")

    out += ["## Decisions", "", "| Decision | Topic | Scenarios (API / store-down / UI / wip) | Reason when none |",
            "|---|---|---|---|"]
    no_scenario = []
    for d in decisions:
        found = by_tag.get(d["id"], [])
        c = collections.Counter(kind(i) for i in found)
        reason = DECISION_REASONS.get(d["id"], "")
        if not found:
            no_scenario.append(d["id"])
            if not reason:
                reason = "NO REASON RECORDED"
        out.append(f"| {d['id']} | {d.get('topic', '')} | {c['API']} / {c['store-down']} / {c['UI']} / {c['wip']} | "
                   f"{reason if not found else ''} |")
    out += ["", f"Decisions without any scenario: {len(no_scenario)} of {len(decisions)}: {', '.join(no_scenario)}.", ""]

    out += ["## Quality targets (NFR-13)", "", "| Target | Checked by |", "|---|---|"]
    for q, text in QUALITY.items():
        n = len(by_tag.get(q, []))
        out.append(f"| {q} | {text} ({n} tagged scenarios) |")
    out.append("")

    rule_tags = sorted(t for t in by_tag if re.match(r"^(OWA|OWASP|OWT|API|BLG|NFR|MACH|ARCH|SEC)-", t))
    out += ["## Rules checked", "", "| Rule | Scenarios |", "|---|---|"]
    for tag in rule_tags:
        out.append(f"| {tag} | {len(by_tag[tag])} |")
    out += ["", "Rules a black-box test could observe but this suite does not check, and why:", "",
            "- API-39 (Vary: Accept): no operation negotiates between several representations; each has one success media type (D63, D151).",
            "- API-25 (HTTPS only), OWT-A04: TLS is the platform's (D185); the local stack runs plain HTTP.",
            "- OWA-API4 (call-rate limit): applied by the Tier 1 gateway, not by the component (D46).",
            "- OWT-A07 failed-token limiting: the gateway's (D164); token validation itself is tested.",
            "- MSV-07 and ARCH-06 (circuit breaker, D103): opening the breaker needs a slow or failing store; the store-down backend shows the outcome (503) only.",
            "- NFR-07 graceful shutdown: needs control over the container; step 70.",
            ""]
    with open(os.path.join(ROOT, "COVERAGE.md"), "w", encoding="utf-8") as handle:
        handle.write("\n".join(out))
    print(f"scenarios {len(items)}; uncovered criteria {len(uncovered)}; decisions without scenario {len(no_scenario)}")
    missing = [d for d in no_scenario if d not in DECISION_REASONS]
    if missing:
        print("decisions without scenario and without reason:", ", ".join(missing))


if __name__ == "__main__":
    main()
