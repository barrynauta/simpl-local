import os
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

import parse  # noqa: E402
from behave import register_type  # noqa: E402

from vmtest import config  # noqa: E402
from vmtest.world import World  # noqa: E402


@parse.with_pattern(r'[^"]*')
def _quoted(text):
    return text


# A quoted string without quotes. Registered here, before any step module is loaded.
register_type(Q=_quoted)


def before_scenario(context, scenario):
    api_url = config.STORE_DOWN_API_URL if "store_down" in scenario.effective_tags else config.API_URL
    if os.environ.get("SUITE_MODE") == "store-down":
        api_url = config.STORE_DOWN_API_URL
    context.world = World(api_url=api_url)
    context.ui = None
    before_scenario_ui(context, scenario)


def before_all(context):
    context._pw = None
    context._browser = None


def _is_ui(scenario):
    return "ui" in scenario.effective_tags


def before_scenario_ui(context, scenario):
    if _is_ui(scenario):
        from vmtest.browser import open_ui
        open_ui(context)


def after_scenario(context, scenario):
    if getattr(context, "ui", None):
        from vmtest.browser import close_ui
        close_ui(context)


def after_all(context):
    from vmtest.browser import stop_browser
    stop_browser(context)
