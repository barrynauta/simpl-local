#!/usr/bin/env bash
# Single entry point of the Vocabulary Manager test suite.
#   ./run.sh api         default API scenarios          -> reports/api.json (behave JSON)
#   ./run.sh store-down  scenarios against the backend without its store (STORE_DOWN_API_URL)
#                                                       -> reports/store-down.json
#   ./run.sh ui          UI scenarios in a headless browser (Playwright) -> reports/ui.json
#   ./run.sh load        load test of Q1/Q4/D99         -> reports/load.json
#   ./run.sh dry-run     parse check, no system needed  -> reports/dry-run.json
# Exit code 0 when the run itself worked, whether scenarios pass or fail.
set -u
cd "$(dirname "$0")"
MODE="${1:-api}"
shift || true
mkdir -p reports

venv() {  # venv <dir> <requirements>
  if [ ! -x "$1/bin/python" ]; then
    python3 -m venv "$1" || return 1
    "$1/bin/pip" install -q --upgrade pip >/dev/null 2>&1
    "$1/bin/pip" install -q -r "$2" || return 1
  fi
}

behave_run() {  # behave_run <report> <tag expression> [extra args]
  local report="$1" tags="$2"; shift 2
  venv .venv requirements.txt || { echo "could not set up .venv" >&2; exit 2; }
  rm -f "reports/$report"
  .venv/bin/behave --tags="$tags" -f json.pretty -o "reports/$report" -f progress "$@"
  local rc=$?
  if [ -s "reports/$report" ] && .venv/bin/python -c "import json,sys; json.load(open(sys.argv[1]))" "reports/$report"; then
    echo "behave exit code $rc; report written to reports/$report"
    exit 0
  fi
  echo "behave did not produce a valid report (exit $rc)" >&2
  exit 2
}

case "$MODE" in
  api)
    behave_run api.json "not @ui and not @wip and not @store_down" "$@"
    ;;
  ui)
    # Own virtualenv with Playwright and axe. Every scenario opens a new browser context:
    # no stored browser session, cookies or storage are reused (no storage_state anywhere).
    venv .venv-ui requirements.txt || { echo "could not set up .venv-ui" >&2; exit 2; }
    .venv-ui/bin/pip install -q -r requirements-ui.txt || { echo "could not install the UI requirements" >&2; exit 2; }
    .venv-ui/bin/python -m playwright install chromium >/dev/null || { echo "could not install Chromium" >&2; exit 2; }
    rm -f reports/ui.json
    .venv-ui/bin/behave --tags="@ui and not @wip and not @manual" -f json.pretty -o reports/ui.json -f progress "$@"
    rc=$?
    if [ -s reports/ui.json ] && .venv-ui/bin/python -c "import json,sys; json.load(open(sys.argv[1]))" reports/ui.json; then
      echo "behave exit code $rc; report written to reports/ui.json"
      exit 0
    fi
    echo "behave did not produce a valid report (exit $rc)" >&2
    exit 2
    ;;
  store-down)
    behave_run store-down.json "@store_down and not @wip" "$@"
    ;;
  dry-run)
    venv .venv requirements.txt || exit 2
    out=$(.venv/bin/behave --dry-run -f json.pretty -o reports/dry-run.json -f null "$@" 2>&1)
    echo "$out" | tail -4
    if echo "$out" | grep -Eq "[1-9][0-9]* undefined|[1-9][0-9]* error"; then
      echo "dry run found undefined steps or errors" >&2; exit 1
    fi
    exit 0
    ;;
  load)
    venv .venv-load requirements-load.txt || { echo "could not set up .venv-load" >&2; exit 2; }
    .venv-load/bin/python load/run_load.py "$@"
    rc=$?
    [ -s reports/load.json ] && exit 0
    echo "load test did not write reports/load.json (exit $rc)" >&2
    exit 2
    ;;
  *)
    echo "usage: ./run.sh api|ui|store-down|load|dry-run [extra behave or load args]" >&2
    exit 2
    ;;
esac
