#!/usr/bin/env bash
# Entry point of the Vocabulary Manager black-box suite.
#   ./run.sh api         default API scenarios          -> reports/api.json
#   ./run.sh store-down  scenarios for a backend without its store (STORE_DOWN_API_URL)
#                                                       -> reports/store-down.json
#   ./run.sh ui          @ui scenarios in a headless Chromium (Playwright) against UI_URL
#                                                       -> reports/ui.json
#   ./run.sh load        load test of Q1 (Locust)       -> reports/load.json
#   ./run.sh dry-run     parse every feature and check for undefined steps (no system needed)
# Exits 0 when the run itself worked, whether scenarios pass or fail.
set -u
HERE="$(cd "$(dirname "$0")" && pwd)"
cd "$HERE"
MODE="${1:-api}"
mkdir -p reports

venv() {
  local dir="$1" req="$2"
  if [ ! -x "$dir/bin/python" ]; then
    python3 -m venv "$dir" || return 1
    "$dir/bin/pip" install -q --disable-pip-version-check -r "$req" || return 1
  fi
}

run_behave() {
  local out="$1"; shift
  local env="${BEHAVE_VENV:-.venv}" req="${BEHAVE_REQ:-requirements.txt}"
  venv "$env" "$req" || { echo "could not set up the virtualenv" >&2; exit 2; }
  rm -f "$out"
  "$env"/bin/behave --no-capture --format json.pretty --outfile "$out" --format progress "$@" features
  local rc=$?
  if [ -s "$out" ] && .venv/bin/python -c "import json,sys; json.load(open(sys.argv[1]))" "$out" 2>/dev/null; then
    echo "behave exit code $rc; report $out"
    exit 0
  fi
  echo "behave did not produce a valid report (exit code $rc)" >&2
  exit 2
}

case "$MODE" in
  api)
    run_behave reports/api.json --tags='~@ui' --tags='~@wip' --tags='~@store_down'
    ;;
  store-down)
    export API_URL="${STORE_DOWN_API_URL:-http://localhost:8199/v1}"
    run_behave reports/store-down.json --tags='@store_down' --tags='~@wip'
    ;;
  ui)
    # fresh, non-persistent browser contexts only: nothing of an earlier run is reused
    export BEHAVE_VENV=.venv-ui BEHAVE_REQ=requirements-ui.txt
    venv .venv-ui requirements-ui.txt || { echo "could not set up the UI virtualenv" >&2; exit 2; }
    .venv-ui/bin/python -m playwright install chromium >/dev/null || { echo "could not install Chromium" >&2; exit 2; }
    rm -rf reports/a11y reports/ui-screens
    run_behave reports/ui.json --tags='@ui' --tags='~@manual' --tags='~@wip'
    ;;
  dry-run)
    venv .venv requirements.txt || exit 2
    .venv/bin/behave --dry-run --format progress --tags='~@ui' features
    rc=$?
    .venv/bin/python tools/check_suite.py
    exit $(( rc == 0 ? $? : rc ))
    ;;
  load)
    venv .venv-load requirements-load.txt || { echo "could not set up the load virtualenv" >&2; exit 2; }
    .venv-load/bin/python load/seed.py || { echo "seeding failed" >&2; exit 2; }
    users=$(( ${LOAD_READ_USERS:-20} + 1 ))
    .venv-load/bin/locust -f load/locustfile.py --headless \
      --users "$users" --spawn-rate "$users" \
      --run-time "${LOAD_RUN_TIME:-5m}" --only-summary \
      --host "${API_URL:-http://localhost:8196/v1}" || true
    .venv-load/bin/python load/big_writes.py || true
    .venv-load/bin/python load/report.py && exit 0
    echo "load report missing" >&2
    exit 2
    ;;
  *)
    echo "usage: $0 api|store-down|ui|load|dry-run" >&2
    exit 64
    ;;
esac
