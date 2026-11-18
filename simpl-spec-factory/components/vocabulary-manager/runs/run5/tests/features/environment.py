"""Behave hooks: per-scenario state and skip rules; the browser for @ui (step 60)."""
import os
import re
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from vmtest import client  # noqa: E402

REPORTS = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "reports"))


def before_all(context):
    # A holder object at the root layer, so the browser outlives each scenario's layer.
    context.ui_holder = {"playwright": None, "browser": None}


def _start_browser(context):
    holder = context.ui_holder
    if holder["browser"] is None:
        from playwright.sync_api import sync_playwright
        holder["playwright"] = sync_playwright().start()
        holder["browser"] = holder["playwright"].chromium.launch(
            headless=os.environ.get("UI_HEADED") != "1")


def before_scenario(context, scenario):
    tags = set(scenario.effective_tags)
    if "wip" in tags:
        scenario.skip("@wip: cannot run against the API from outside")
        return
    if "ui" in tags and not os.environ.get("UI_STEPS"):
        scenario.skip("@ui: browser steps are added in step 60 (set UI_STEPS=1 when they exist)")
        return
    if "manual" in tags:
        scenario.skip("@manual: not automated, see COVERAGE.md")
        return
    if "store-down" in tags and not client.STORE_DOWN_API_URL:
        scenario.skip("@store-down: set STORE_DOWN_API_URL to a backend started with an unreachable store")
        return
    client.reset_state(context)
    context.ui_context = context.ui_page = context.ui_role = None
    if "ui" in tags:
        _start_browser(context)


def after_scenario(context, scenario):
    page = getattr(context, "ui_page", None)
    if page is not None and scenario.status == "failed":
        shots = os.path.join(REPORTS, "ui-screenshots")
        os.makedirs(shots, exist_ok=True)
        name = re.sub(r"[^A-Za-z0-9]+", "-", scenario.name)[:90]
        try:
            page.screenshot(path=os.path.join(shots, f"{name}-{scenario.line}.png"), full_page=True)
        except Exception:  # noqa: BLE001
            pass
    if getattr(context, "ui_context", None) is not None:
        context.ui_context.close()
        context.ui_context = context.ui_page = None


def after_all(context):
    holder = getattr(context, "ui_holder", None)
    if holder and holder["browser"] is not None:
        holder["browser"].close()
        holder["playwright"].stop()
