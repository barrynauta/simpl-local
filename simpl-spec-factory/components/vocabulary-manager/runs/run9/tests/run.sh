#!/usr/bin/env bash
# One entry point for the orchestrator.
#   ./run.sh api         default API scenarios          -> reports/api.json (behave JSON)
#   ./run.sh store-down  scenarios for the store-down backend (STORE_DOWN_API_URL) -> reports/store-down.json
#   ./run.sh ui          browser scenarios (@ui, Playwright, fresh browser per scenario) -> reports/ui.json
#   ./run.sh load        load test of Q1 (Locust)       -> reports/load.json
#   ./run.sh dry-run     parse check, no system needed  -> reports/dry-run.json
# Exits 0 when the run itself worked, whether scenarios pass or fail.
set -u
cd "$(dirname "$0")"
MODE="${1:-api}"
PY="${PYTHON:-python3}"
mkdir -p reports

setup() {
  if [ ! -x .venv/bin/behave ]; then
    "$PY" -m venv .venv || exit 2
    .venv/bin/pip install -q -r requirements.txt || exit 2
  fi
}

setup_ui() {
  setup
  if ! .venv/bin/python -c "import playwright, axe_playwright_python" 2>/dev/null; then
    .venv/bin/pip install -q -r requirements-ui.txt || exit 2
  fi
  .venv/bin/python -m playwright install chromium >/dev/null || exit 2
}

setup_load() {
  setup
  if [ ! -x .venv/bin/locust ]; then
    .venv/bin/pip install -q -r requirements-load.txt || exit 2
  fi
}

valid_json() {
  [ -s "$1" ] && .venv/bin/python -c "import json,sys; json.load(open(sys.argv[1]))" "$1" 2>/dev/null
}

case "$MODE" in
  api)
    setup
    rm -f reports/api.json
    .venv/bin/behave --tags="not @ui and not @wip and not @store_down" \
      -f json -o reports/api.json -f progress features
    valid_json reports/api.json || { echo "run failed: no report"; exit 1; }
    ;;
  store-down)
    setup
    rm -f reports/store-down.json
    .venv/bin/behave --tags="@store_down and not @wip" \
      -f json -o reports/store-down.json -f progress features
    valid_json reports/store-down.json || { echo "run failed: no report"; exit 1; }
    ;;
  ui)
    setup_ui
    rm -f reports/ui.json
    rm -rf reports/a11y
    # no stored browser sessions: every scenario opens a new browser context without storage state
    .venv/bin/behave --tags="@ui and not @wip and not @manual" \
      -f json -o reports/ui.json -f progress features
    valid_json reports/ui.json || { echo "run failed: no report"; exit 1; }
    ;;
  load)
    setup_load
    rm -f reports/load.json
    .venv/bin/python load/run_load.py
    valid_json reports/load.json || { echo "run failed: no report"; exit 1; }
    ;;
  dry-run)
    setup
    .venv/bin/behave --dry-run -f json -o reports/dry-run.json -f progress features
    valid_json reports/dry-run.json || exit 1
    ;;
  *)
    echo "usage: $0 api|store-down|ui|load|dry-run" >&2
    exit 2
    ;;
esac
exit 0
