#!/usr/bin/env bash
# Entry point for the orchestrator.
#   ./run.sh api         default API scenarios          -> reports/api.json
#   ./run.sh store-down  backend without its store      -> reports/store-down.json
#   ./run.sh load        Locust load test (Q1, Q4)      -> reports/load.json
#   ./run.sh ui          @ui scenarios (steps from step 60) -> reports/ui.json
# Exit 0 when the run itself worked, whether scenarios pass or fail.
set -u
cd "$(dirname "$0")"
mode="${1:-api}"

export API_URL="${API_URL:-http://localhost:8196/v1}"
export TOKEN_URL="${TOKEN_URL:-http://localhost:8193/realms/simpl/protocol/openid-connect/token}"
export TOKEN_CLIENT="${TOKEN_CLIENT:-vocabulary-tests}"
export UI_URL="${UI_URL:-http://localhost:4433}"
export STORE_DOWN_API_URL="${STORE_DOWN_API_URL:-http://localhost:8199/v1}"

VENV=.venv
if [ ! -x "$VENV/bin/behave" ] || [ ! -x "$VENV/bin/locust" ] || [ ! -x "$VENV/bin/playwright" ]; then
  python3 -m venv "$VENV" || exit 2
  "$VENV/bin/pip" install -q --upgrade pip >/dev/null 2>&1
  "$VENV/bin/pip" install -q -r requirements.txt || exit 2
fi
mkdir -p reports

run_behave() {
  local out="$1"; shift
  rm -f "$out"
  "$VENV/bin/behave" features -f json -o "$out" -f progress "$@"
  local rc=$?
  # behave exits 1 when scenarios fail; that is a valid run. Check the report exists.
  if [ -s "$out" ] && python3 -c "import json,sys; json.load(open(sys.argv[1]))" "$out" 2>/dev/null; then
    return 0
  fi
  echo "behave did not produce a valid report (exit $rc)" >&2
  return 2
}

case "$mode" in
  api)
    run_behave reports/api.json --tags="not @ui and not @wip and not @store_down"
    ;;
  store-down)
    run_behave reports/store-down.json --tags="@store_down and not @wip"
    ;;
  ui)
    # Browser for the UI steps (no-op when already installed). Every scenario opens a
    # new browser context, so no stored session or cookie is reused (step 60).
    "$VENV/bin/playwright" install chromium >/dev/null 2>&1 || exit 2
    rm -rf reports/a11y reports/ui-failures
    run_behave reports/ui.json --tags="@ui and not @wip and not @manual"
    ;;
  load)
    rm -f reports/load.json
    "$VENV/bin/locust" -f load/locustfile.py --headless \
      --host "$API_URL" \
      -u "${LOAD_USERS:-20}" -r "${LOAD_SPAWN_RATE:-5}" \
      --only-summary
    if [ -s reports/load.json ]; then exit 0; fi
    echo "load test did not write reports/load.json" >&2
    exit 2
    ;;
  dry-run)
    "$VENV/bin/behave" features --dry-run -f null
    ;;
  *)
    echo "usage: $0 api|store-down|load|ui|dry-run" >&2
    exit 2
    ;;
esac
