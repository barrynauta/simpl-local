"""behave hooks: per-scenario unique data (decision E4), no store reset."""
import os
import re
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from vmtest import client, config  # noqa: E402
from vmtest.resolve import Names  # noqa: E402

# RUN_ID ends up in vocabulary names and in the name filter (D19, D37):
# keep only lower-case letters and digits.
RUN_ID = re.sub(r"[^a-z0-9]", "", (config.RUN_ID or "").lower())[:8] or client.random_token(4)


def before_all(context):
    context.config.setup_logging()
    context.run_id = RUN_ID


def before_scenario(context, scenario):
    # Unique per scenario: run id plus a random part. Lower case letters and
    # digits only, so it fits the name pattern and the name filter (D19, D37).
    context.names = Names("q" + RUN_ID + client.random_token(6))
    context.principal = "ga-admin"
    context.base = None
    context.extra_headers = {}
    context.resp = None
    context.saved = {}
    context.uploaded = {}
    context.parallel = []
    context.elapsed = None


def after_scenario(context, scenario):
    ui = sys.modules.get("vmtest.ui")
    if ui is not None:
        ui.close_scenario(context)


def after_all(context):
    ui = sys.modules.get("vmtest.ui")
    if ui is not None:
        ui.close_all()
