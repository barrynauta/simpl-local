import os
import sys

sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(__file__), "..", "lib")))

import vmtest  # noqa: E402


def before_scenario(context, scenario):
    context.run = vmtest.new_run_id()
    context.names = {}
    context.ext_names = {}
    context.uploads = {}
    context.descriptions = {}
    context.ext_uploads = {}
    context.remembered = {}
    context.user = "ga-admin"
    context.token_override = None
    context.extra_headers = {}
    context.response = None
    # scenarios tagged @store_down talk to the backend whose store is unreachable (D44)
    if "store_down" in scenario.effective_tags:
        context.api_base = vmtest.STORE_DOWN_API_URL
    else:
        context.api_base = vmtest.API_URL



def after_scenario(context, scenario):
    # ui_steps.py registers its browser hooks on vmtest (step 60)
    hooks = getattr(vmtest, "ui_hooks", None)
    if hooks:
        hooks["close"](context)


def after_all(context):
    hooks = getattr(vmtest, "ui_hooks", None)
    if hooks:
        hooks["stop"]()
