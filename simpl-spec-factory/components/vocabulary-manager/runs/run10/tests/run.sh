#!/usr/bin/env bash
# One entry point for the orchestrator.
#   ./run.sh api         default API scenarios          -> reports/api.json
#   ./run.sh ui          UI scenarios in a browser (Playwright, fresh context each) -> reports/ui.json
#   ./run.sh store-down  scenarios against the backend without its store -> reports/store-down.json
#   ./run.sh load        load test of Q1 (Locust)        -> reports/load.json
#   ./run.sh dry-run     parse check, no system needed  -> reports/dry-run.json
# Exits 0 when the run itself worked, whether scenarios pass or fail.
set -uo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
cd "$HERE"
MODE="${1:-api}"
shift || true
mkdir -p reports
VENV="$HERE/.venv"

ensure_venv() {
  local req="$1" stamp="$VENV/.installed-$(basename "$1")"
  if [ ! -x "$VENV/bin/python" ]; then
    python3 -m venv "$VENV" || { echo "cannot create virtualenv" >&2; exit 2; }
  fi
  if [ ! -f "$stamp" ] || [ "$req" -nt "$stamp" ]; then
    "$VENV/bin/pip" install -q --upgrade pip >/dev/null 2>&1
    "$VENV/bin/pip" install -q -r "$req" || { echo "pip install failed" >&2; exit 2; }
    touch "$stamp"
  fi
}

run_behave() {
  local report="$1"; shift
  ensure_venv "$HERE/requirements.txt"
  if [ "${NEED_UI:-0}" = "1" ]; then
    ensure_venv "$HERE/requirements-ui.txt"
    "$VENV/bin/python" -m playwright install chromium >/dev/null 2>&1 \
      || { echo "playwright browser install failed" >&2; exit 2; }
  fi
  "$VENV/bin/behave" --no-capture -f json.pretty -o "$report" -f progress "$@"
  local rc=$?
  # behave: 0 all passed, 1 failures; anything else means the run itself broke.
  if [ "$rc" -le 1 ] && [ -s "$report" ]; then exit 0; fi
  echo "behave run failed (exit $rc)" >&2
  exit 2
}

case "$MODE" in
  api)
    run_behave reports/api.json --tags="not ui and not wip and not store_down" "$@"
    ;;
  ui)
    # Every scenario opens a new browser context: no cookies, no storage, no stored sign-in.
    rm -rf reports/ui-artifacts reports/a11y.json
    NEED_UI=1 run_behave reports/ui.json --tags="ui and not wip and not manual" "$@"
    ;;
  store-down)
    run_behave reports/store-down.json --tags="store_down and not wip" "$@"
    ;;
  dry-run)
    run_behave reports/dry-run.json --dry-run "$@"
    ;;
  load)
    ensure_venv "$HERE/requirements-load.txt"
    "$VENV/bin/python" load/run_load.py "$@"
    rc=$?
    if [ "$rc" -eq 0 ] && [ -s reports/load.json ]; then exit 0; fi
    echo "load run failed (exit $rc)" >&2
    exit 2
    ;;
  *)
    echo "usage: $0 api|ui|store-down|load|dry-run [behave or load options]" >&2
    exit 2
    ;;
esac
