import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "lib"))

from vmclient import Config  # noqa: E402
from world import World  # noqa: E402


def before_all(context):
    context.cfg = Config()


def before_scenario(context, scenario):
    # Every scenario gets its own run id and its own data (no store reset).
    context.w = World(context.cfg)
    context.ui = None


def after_scenario(context, scenario):
    # @ui scenarios: close the scenario's browser context (cookies, stubs, downloads).
    ui = getattr(context, "ui", None)
    if ui is not None:
        ui.close()
        context.ui = None


def after_all(context):
    if "uidriver" in sys.modules:
        sys.modules["uidriver"].Browser.stop()
