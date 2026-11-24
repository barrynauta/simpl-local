"""behave hooks: one unique run id per scenario (E4), base URL per mode, and for @ui a fresh
browser context per scenario (no stored sessions, step 60)."""
import os
import random
import string
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from vmtest import config  # noqa: E402
from vmtest.state import ScenarioState  # noqa: E402


def _run_id():
    alphabet = string.ascii_lowercase + string.digits
    return random.choice(string.ascii_lowercase) + "".join(random.choice(alphabet) for _ in range(9))


def before_scenario(context, scenario):
    base = config.STORE_DOWN_API_URL if "store_down" in scenario.effective_tags else config.API_URL
    context.vm = ScenarioState(run=_run_id(), base_url=base)
    context.ui = None
    if "ui" in scenario.effective_tags and "manual" not in scenario.effective_tags:
        from vmtest.ui import UiSession
        context.ui = UiSession(bypass_csp="a11y" in scenario.effective_tags)


def after_scenario(context, scenario):
    if getattr(context, "ui", None) is not None:
        context.ui.close()
        context.ui = None


def after_all(context):
    from vmtest import ui
    ui.stop()
