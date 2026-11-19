#!/usr/bin/env bash
# One entry point for the orchestrator.
#   ./run.sh api         default API scenarios          -> reports/api.json
#   ./run.sh store-down  scenarios against the backend without store
#                        (STORE_DOWN_API_URL)           -> reports/store-down.json
#   ./run.sh load        Locust load test (Q1, Q4)      -> reports/load.json
#   ./run.sh dry-run     parse check, no system needed  -> reports/dry-run.json
#   ./run.sh ui          browser scenarios (Playwright, UI_URL), axe
#                        reports in reports/a11y/       -> reports/ui.json
# Exits 0 when the run itself worked, whether scenarios pass or fail.
set -u
HERE="$(cd "$(dirname "$0")" && pwd)"
cd "$HERE"
MODE="${1:-api}"
shift || true
mkdir -p reports

venv() {
  local dir="$1" req="$2"
  if [ ! -x "$dir/bin/python" ]; then
    python3 -m venv "$dir" || return 1
    "$dir/bin/pip" install --quiet --upgrade pip || return 1
  fi
  if [ ! -f "$dir/.installed" ] || ! cmp -s "$req" "$dir/.installed"; then
    "$dir/bin/pip" install --quiet -r "$req" || return 1
    cp "$req" "$dir/.installed"
  fi
}

run_behave() {
  local report="$1"; shift
  local env="${VENV:-.venv}" req="${VENV_REQ:-requirements.txt}"
  venv "$env" "$req" || { echo "virtualenv setup failed" >&2; exit 2; }
  rm -f "$report"
  "$env/bin/behave" "$@" -f json.pretty -o "$report" -f progress
  local rc=$?
  if [ ! -s "$report" ]; then
    echo "behave produced no report (exit $rc)" >&2
    exit 2
  fi
  # rc 1 means scenarios failed: the run itself worked.
  if [ "$rc" -gt 1 ]; then exit "$rc"; fi
  exit 0
}

case "$MODE" in
  api)
    run_behave reports/api.json --tags="not @ui and not @store_down and not @wip" "$@"
    ;;
  store-down)
    run_behave reports/store-down.json --tags="@store_down and not @wip" "$@"
    ;;
  ui)
    # Fresh browser per run and per scenario: a new context with no stored
    # session, no storage state and no user data directory (vmtest/ui.py).
    VENV=.venv-ui VENV_REQ=requirements-ui.txt
    venv "$VENV" "$VENV_REQ" || { echo "virtualenv setup failed" >&2; exit 2; }
    "$VENV/bin/playwright" install chromium >/dev/null || { echo "playwright browser install failed" >&2; exit 2; }
    rm -rf reports/a11y
    VENV="$VENV" VENV_REQ="$VENV_REQ" run_behave reports/ui.json --tags="@ui and not @manual and not @wip" "$@"
    ;;
  dry-run)
    run_behave reports/dry-run.json --dry-run --tags="not @wip" "$@"
    ;;
  load)
    venv .venv-load requirements-load.txt || { echo "virtualenv setup failed" >&2; exit 2; }
    rm -f reports/load.json
    USERS="${LOAD_USERS:-20}"
    SPAWN="${LOAD_SPAWN_RATE:-5}"
    DURATION="${LOAD_DURATION:-5m}"
    .venv-load/bin/locust -f load/locustfile.py --headless \
      -u "$USERS" -r "$SPAWN" -t "$DURATION" --only-summary \
      --host "${API_URL:-http://localhost:8196/v1}" "$@"
    if [ ! -s reports/load.json ]; then
      echo "load test produced no report" >&2
      exit 2
    fi
    exit 0
    ;;
  *)
    echo "usage: $0 api|store-down|ui|load|dry-run" >&2
    exit 64
    ;;
esac
