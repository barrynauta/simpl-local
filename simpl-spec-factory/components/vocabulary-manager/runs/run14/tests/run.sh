#!/usr/bin/env bash
# One entry point for the orchestrator.
#   ./run.sh api         default API scenarios          -> reports/api.json
#   ./run.sh store-down  scenarios against STORE_DOWN_API_URL -> reports/store-down.json
#   ./run.sh load        Q1 load test (Locust)          -> reports/load.json
#   ./run.sh ui          browser scenarios (Playwright) -> reports/ui.json, axe details in reports/axe/
#   ./run.sh dry-run     parse check, no system needed  -> reports/dry-run.txt
# Exit 0 when the run itself worked, whether scenarios pass or fail; 2 when it could not run.
set -u
cd "$(dirname "$0")"
MODE="${1:-api}"
shift || true
VENV=".venv"
mkdir -p reports

setup() {
  local req="$1"
  if [ ! -x "$VENV/bin/python" ]; then
    python3 -m venv "$VENV" || exit 2
  fi
  local stamp="$VENV/.installed-$(basename "$req")"
  if [ ! -f "$stamp" ] || [ "$req" -nt "$stamp" ]; then
    "$VENV/bin/pip" install -q --upgrade pip >/dev/null 2>&1
    "$VENV/bin/pip" install -q -r "$req" || exit 2
    touch "$stamp"
  fi
}

valid_json() {
  "$VENV/bin/python" -c "import json,sys; json.load(open(sys.argv[1]))" "$1" 2>/dev/null
}

run_behave() {
  local out="$1"; shift
  rm -f "$out"
  "$VENV/bin/behave" --no-capture -f json -o "$out" -f progress "$@"
  local rc=$?
  if valid_json "$out"; then
    echo "behave exit code $rc; report $out"
    exit 0
  fi
  echo "behave did not produce a valid report (exit code $rc)" >&2
  exit 2
}

case "$MODE" in
  api)
    setup requirements.txt
    run_behave reports/api.json --tags="not @ui and not @wip and not @store_down" "$@"
    ;;
  store-down)
    setup requirements.txt
    run_behave reports/store-down.json --tags="@store_down and not @wip and not @ui" "$@"
    ;;
  load)
    setup requirements-load.txt
    "$VENV/bin/python" load/run_load.py "$@" || exit 2
    valid_json reports/load.json || exit 2
    exit 0
    ;;
  dry-run)
    setup requirements.txt
    "$VENV/bin/python" tools/check_dry_run.py "${1:-not @ui}" | tee reports/dry-run.txt
    exit "${PIPESTATUS[0]}"
    ;;
  ui)
    # Browser scenarios (step 60). Every scenario opens a new browser context: no stored sessions.
    setup requirements-ui.txt
    "$VENV/bin/python" -m playwright install chromium >/dev/null 2>&1 || exit 2
    rm -rf reports/axe
    run_behave reports/ui.json --tags="@ui and not @manual and not @wip" "$@"
    ;;
  *)
    echo "usage: $0 api|store-down|ui|load|dry-run" >&2
    exit 2
    ;;
esac
