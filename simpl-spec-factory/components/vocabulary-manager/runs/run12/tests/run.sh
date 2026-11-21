#!/usr/bin/env bash
# One entry point for the orchestrator.
#   ./run.sh api         default API scenarios          -> reports/api.json (behave JSON)
#   ./run.sh store-down  scenarios needing a store-less backend at STORE_DOWN_API_URL
#                                                        -> reports/store-down.json
#   ./run.sh load        Q1 load test (Locust)           -> reports/load.json
# Exit 0 when the run itself worked, whether scenarios pass or fail.
set -u
HERE="$(cd "$(dirname "$0")" && pwd)"
cd "$HERE"
MODE="${1:-api}"
mkdir -p reports

venv() {
  local dir="$1" req="$2"
  if [ ! -x "$dir/bin/python" ]; then
    python3 -m venv "$dir" || return 1
    "$dir/bin/pip" install -q --upgrade pip >/dev/null 2>&1
    "$dir/bin/pip" install -q -r "$req" || return 1
  fi
}

valid_json() {
  [ -s "$1" ] && python3 -c "import json,sys; json.load(open(sys.argv[1]))" "$1" 2>/dev/null
}

run_behave() {
  local report="$1"; shift
  local env_dir="${BEHAVE_VENV:-.venv}" req="${BEHAVE_REQ:-requirements.txt}"
  venv "$env_dir" "$req" || { echo "virtualenv setup failed"; exit 2; }
  rm -f "$report"
  "$env_dir"/bin/behave "$@" -f json.pretty -o "$report" -f progress --no-capture-stderr
  local status=$?
  if valid_json "$report"; then
    echo "behave exit $status; report $report"
    exit 0
  fi
  echo "behave did not produce a report (exit $status)"
  exit 3
}

case "$MODE" in
  api)
    run_behave reports/api.json --tags="~@ui" --tags="~@wip" --tags="~@store_down"
    ;;
  store-down)
    run_behave reports/store-down.json --tags="@store_down" --tags="~@wip"
    ;;
  ui)
    venv .venv-ui requirements-ui.txt || { echo "virtualenv setup failed"; exit 2; }
    .venv-ui/bin/playwright install chromium >/dev/null 2>&1 || { echo "browser install failed"; exit 2; }
    # start without stored browser sessions: drop any saved state from earlier runs
    rm -rf reports/axe reports/ui-screens .auth playwright/.auth
    BEHAVE_VENV=.venv-ui BEHAVE_REQ=requirements-ui.txt \
      run_behave reports/ui.json --tags="@ui" --tags="~@manual" --tags="~@wip"
    ;;
  load)
    venv .venv-load load/requirements.txt || { echo "virtualenv setup failed"; exit 2; }
    rm -f reports/load.json
    .venv-load/bin/python load/run_load.py --report reports/load.json
    status=$?
    if valid_json reports/load.json; then
      echo "load exit $status; report reports/load.json"
      exit 0
    fi
    echo "load test did not produce a report (exit $status)"
    exit 3
    ;;
  *)
    echo "usage: $0 api|store-down|load|ui" >&2
    exit 64
    ;;
esac
