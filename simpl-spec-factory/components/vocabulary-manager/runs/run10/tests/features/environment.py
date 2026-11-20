import itertools
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent / "support"))

import vmclient  # noqa: E402
from vmclient import CONFIG  # noqa: E402

_scenario_counter = itertools.count(1)


def before_all(context):
    context.config_vm = CONFIG
    context.content_base = CONFIG.content_base
    context.pw_holder = {}  # Playwright and the browser of the UI run (step 60), started on first use
    print(f"Vocabulary Manager suite: API_URL={CONFIG.api_url} run id={CONFIG.run_id}")


def before_scenario(context, scenario):
    if "manual" in scenario.effective_tags:
        scenario.skip("manual: cannot be automated without an undecided UI detail (see COVERAGE.md)")
        return
    if "wip" in scenario.effective_tags:
        scenario.skip("wip: cannot run against the API")
        return
    n = next(_scenario_counter)
    # Unique per run and per scenario; a letter follows the counter so that a
    # 'contains' name filter on the prefix never matches another scenario.
    context.prefix = f"Vt{CONFIG.run_id}S{n:04d}X"
    context.api_url = CONFIG.api_url
    context.token = None
    context.extra_headers = {}
    context.resp = None
    context.responses = []
    context.vars = {}
    context.files = {}        # alias -> {version: bytes}
    context.descriptions = {}  # alias -> {version: str}
    context.created = {}      # alias -> {version: body}
    context.saved = {}
    context.marker = itertools.count(1)
    context.page = None       # browser page of a @ui scenario, a fresh context without stored sessions
    context.ui_alias = "A"


def after_scenario(context, scenario):
    hooks = getattr(vmclient, "UI_HOOKS", None)
    if hooks and getattr(context, "page", None) is not None:
        hooks["close_page"](context, scenario)


def after_all(context):
    hooks = getattr(vmclient, "UI_HOOKS", None)
    if hooks:
        hooks["stop_browser"](context)
