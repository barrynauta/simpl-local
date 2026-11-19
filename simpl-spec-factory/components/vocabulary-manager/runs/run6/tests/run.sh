#!/usr/bin/env bash
# Entry point of the Vocabulary Manager test suite.
#   ./run.sh api         default API scenarios          -> reports/api.json
#   ./run.sh store-down  scenarios for a store-less backend at STORE_DOWN_API_URL -> reports/store-down.json
#   ./run.sh ui          @ui browser scenarios against UI_URL (Playwright, fresh browser context each) -> reports/ui.json
#   ./run.sh load        load test (none in this build, see COVERAGE.md) -> reports/load.json
#   ./run.sh dry-run     parse every feature and report undefined steps (no system needed)
# Exits 0 when the run itself worked, whether scenarios pass or fail.
set -u
cd "$(dirname "$0")"
MODE="${1:-api}"
shift || true
mkdir -p reports

VENV=.venv
if [ ! -x "$VENV/bin/behave" ] || ! "$VENV/bin/python" -c "import playwright, axe_playwright_python" 2>/dev/null; then
  python3 -m venv "$VENV" || { echo "could not create the virtualenv" >&2; exit 2; }
  "$VENV/bin/pip" install --quiet --disable-pip-version-check -r requirements.txt \
    || { echo "could not install requirements" >&2; exit 2; }
fi
export PYTHONPATH="$PWD${PYTHONPATH:+:$PYTHONPATH}"

run_behave() {
  local report="$1"; shift
  rm -f "$report"
  "$VENV/bin/behave" --no-capture -f json -o "$report" -f progress "$@" features
  local rc=$?
  if [ ! -s "$report" ] || ! "$VENV/bin/python" -c "import json,sys; json.load(open(sys.argv[1]))" "$report" 2>/dev/null; then
    echo "behave did not write a valid report (exit $rc)" >&2
    exit 2
  fi
  exit 0
}

case "$MODE" in
  api)
    run_behave reports/api.json --tags="not ui and not wip and not store_down" "$@"
    ;;
  ui)
    # Chromium for Playwright; installed once, no browser profile is reused between runs.
    "$VENV/bin/python" -m playwright install chromium >/dev/null 2>&1 \
      || { echo "could not install the Playwright browser" >&2; exit 2; }
    rm -rf reports/a11y
    run_behave reports/ui.json --tags="ui and not wip and not manual" "$@"
    ;;
  store-down)
    export SUITE_MODE=store-down
    run_behave reports/store-down.json --tags="store_down and not wip" "$@"
    ;;
  load)
    # No Q decision gives a response time or throughput target, so there is no load test (COVERAGE.md).
    cat > reports/load.json <<'JSON'
{"skipped": true, "reason": "No Q decision gives a response time or throughput target for the Vocabulary Manager; no load test is written (NFR-03, NFR-13). See COVERAGE.md.", "operations": {}}
JSON
    exit 0
    ;;
  dry-run)
    out=$("$VENV/bin/behave" --dry-run -f null features 2>&1)
    echo "$out" | tail -4
    # Only the scenario and step lines count: in a dry run behave 1.3 may report a feature as
    # 'failed' when it mixes outlines and scenarios, although nothing ran.
    if echo "$out" | grep -E "^[0-9]+ (scenarios|steps) passed" | grep -Eq "[1-9][0-9]* (undefined|failed|error)"; then
      echo "dry run found undefined or failing steps" >&2
      exit 1
    fi
    exit 0
    ;;
  *)
    echo "usage: $0 api|ui|store-down|load|dry-run [behave options]" >&2
    exit 2
    ;;
esac
