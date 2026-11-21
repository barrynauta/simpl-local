"""behave hooks. Each scenario gets a fresh token, name map and memory (E4: no store reset)."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from vmtests import config  # noqa: E402
from vmtests.data import Names, new_token  # noqa: E402


def before_scenario(context, scenario):
    context.names = Names(new_token())
    context.memory = {}
    context.uploaded = {}
    context.snapshots = {}
    context.token = None
    context.user = None
    context.resp = None
    context.elapsed = None
    context.responses = []
    context.current_name = None
    context.last_name = None
    context.base = config.STORE_DOWN_API_URL if "store_down" in scenario.effective_tags else config.API_URL
    context.ui_context = None
    context.ui_page = None
    context.form_name = None


def before_all(context):
    # One browser per run; every scenario gets its own new browser context (no stored session).
    context.ui_store = {}


def after_step(context, step):
    # Keep the URL and accessibility tree of a failed UI step for the failure analysis.
    page = getattr(context, "ui_page", None)
    if step.status.name not in ("failed", "error") or page is None:
        return
    try:
        os.makedirs("reports/ui-failures", exist_ok=True)
        safe = "".join(c if c.isalnum() else "_" for c in context.scenario.name)[:80]
        with open(f"reports/ui-failures/{safe}.txt", "w", encoding="utf-8") as fh:
            fh.write(f"step: {step.keyword} {step.name}\nurl: {page.url}\n\n")
            fh.write(page.locator("body").aria_snapshot(timeout=5000))
    except Exception:  # noqa: BLE001
        pass


def after_scenario(context, scenario):
    # Close the scenario's browser context, so the next scenario starts without a session.
    ctx = getattr(context, "ui_context", None)
    if ctx is not None:
        try:
            ctx.close()
        except Exception:  # noqa: BLE001
            pass
    context.ui_context = None
    context.ui_page = None


def after_all(context):
    browser, pw = context.ui_store.get("_ui_browser"), context.ui_store.get("_ui_pw")
    try:
        if browser is not None:
            browser.close()
        if pw is not None:
            pw.stop()
    except Exception:  # noqa: BLE001
        pass
