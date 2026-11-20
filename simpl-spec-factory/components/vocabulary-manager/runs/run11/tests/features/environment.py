import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "support"))

import vm  # noqa: E402


def before_scenario(context, scenario):
    context.sid = vm.new_sid()
    context.identity = "admin"
    context.base = vm.STORE_DOWN_API_URL if "store_down" in scenario.effective_tags else vm.API_URL
    context.resp = None
    context.request = None
    context.remembered = {}
    context.contents = {}      # (name, version) -> bytes uploaded
    context.revision = 0
    context.parallel = []
    context.last_upload = None
    if "ui" in scenario.effective_tags and "manual" not in scenario.effective_tags:
        import ui_hooks
        ui_hooks.start_scenario(context)


def after_scenario(context, scenario):
    if getattr(context, "bctx", None) is not None:
        import ui_hooks
        ui_hooks.end_scenario(context, scenario)


def after_all(context):
    if "ui_hooks" in sys.modules:
        sys.modules["ui_hooks"].stop_all()
