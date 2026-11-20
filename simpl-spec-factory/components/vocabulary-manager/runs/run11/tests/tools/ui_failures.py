"""Print the failing step and the last error line of each failed scenario in a behave JSON report."""
import json
import sys

report = json.load(open(sys.argv[1] if len(sys.argv) > 1 else "reports/ui.json"))
for feature in report:
    for sc in feature.get("elements", []):
        if sc.get("type") != "scenario" or sc.get("status") != "failed":
            continue
        for st in sc["steps"]:
            res = st.get("result", {})
            if res.get("status") == "failed":
                msg = res.get("error_message")
                msg = "\n".join(msg) if isinstance(msg, list) else str(msg)
                lines = [ln for ln in msg.splitlines() if ln.strip()]
                tail = next((ln for ln in reversed(lines) if "Error" in ln or "assert" in ln.lower()), lines[-1] if lines else "")
                print(f"- {sc['location']} {sc['name'][:70]}\n    step: {st['keyword']} {st['name']}\n    {tail.strip()[:400]}")
