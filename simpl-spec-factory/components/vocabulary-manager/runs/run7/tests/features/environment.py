import os
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from vmlib import client  # noqa: E402

STORE_DOWN_API_URL = os.environ.get("STORE_DOWN_API_URL", "http://localhost:8199/v1").rstrip("/")


def before_all(context):
    context.config.setup_logging()
    context.ui_holder = {}  # browser shared by the @ui scenarios


def before_scenario(context, scenario):
    # Every scenario creates its own data with its own run id (decision E4).
    context.rid = client.new_run_id()
    context.contents = {}  # (vocabulary name, version) -> bytes sent
    context.ext_contents = {}  # external name -> bytes sent
    context.saved = {}
    context.counter = 1
    context.resp = None
    context.request_query = None
    context.parallel = []
    context.elapsed = None
    tags = set(scenario.effective_tags)
    context.base_url = STORE_DOWN_API_URL if "store_down" in tags else client.API_URL
    # UI: each scenario gets a new browser context (no stored sessions), created on first use.
    context.ui_page = None
    context.ui_context = None


def after_scenario(context, scenario):
    bctx = getattr(context, "ui_context", None)
    if bctx is not None:
        if scenario.status == "failed":
            try:
                shots = os.path.join(os.path.dirname(__file__), "..", "reports", "ui-failures")
                os.makedirs(shots, exist_ok=True)
                safe = "".join(c if c.isalnum() else "-" for c in scenario.name)[:80]
                context.ui_page.screenshot(path=os.path.join(shots, f"{safe}.png"), full_page=True)
            except Exception:
                pass
        try:
            bctx.close()
        except Exception:
            pass
    context.ui_page = None
    context.ui_context = None


def after_all(context):
    holder = getattr(context, "ui_holder", {})
    if holder.get("browser") is not None:
        holder["browser"].close()
        holder["pw"].stop()
