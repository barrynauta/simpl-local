#!/usr/bin/env python3
"""Run one factory step on a local model through Ollama, so non-public input stays on this machine.

Step 1 (spec digest) only for now: a single prompt with all specs in the context, one
answer back. The input is the same set run 1 used: the step prompt, the spec index, the
OpenAPI, the business process, and the local-only originals in specs/_private/ (stories
with review comments, the Confluence pages).

The output goes to local-run/ (gitignored) until a person has read it: it is derived
from non-public material and may quote it. The script prints counts and timings only,
never spec or output text.

Configuration (environment variables):
  OLLAMA_URL    Ollama base URL      (default http://localhost:11434)
  OLLAMA_MODEL  model name           (default qwen3:30b-a3b-instruct-2507-q4_K_M)

Usage:
  run_local.py --step 1
  run_local.py --step 1 --public-only   # specs/stories and specs/functional instead of _private
"""
import argparse
import json
import os
import pathlib
import re
import sys
import time
import urllib.request

OLLAMA_URL = os.environ.get("OLLAMA_URL", "http://localhost:11434").rstrip("/")
OLLAMA_MODEL = os.environ.get("OLLAMA_MODEL", "qwen3:30b-a3b-instruct-2507-q4_K_M")
FACTORY = pathlib.Path(__file__).resolve().parent.parent
CHARS_PER_TOKEN = 3.2  # rough, for sizing the context window only


def step1_inputs(public_only):
    specs = FACTORY / "specs"
    files = [specs / "README.md", *sorted((specs / "api").glob("*")), *sorted((specs / "functional").glob("*.md"))]
    if public_only:
        files += sorted((specs / "stories").glob("*.md"), key=story_order)
    else:
        files += sorted((specs / "_private" / "stories-full").glob("*.md"), key=story_order)
        files += sorted((specs / "_private" / "functional-confluence").glob("*.md"))
    return files


def story_order(path):
    m = re.search(r"(\d+)", path.stem)
    return int(m.group(1)) if m else 0


def step1_prompt(files):
    brief = (FACTORY / "pipeline" / "01-spec-digest.md").read_text()
    brief = brief.split("---", 1)[1] if "---" in brief else brief  # drop the "Reconstructed" note
    brief = brief.replace("{FACTORY}/", "")
    parts = [brief.strip(), "\nWrite the complete spec-digest.md as your answer, in Markdown, nothing before or after it.",
             "\n# Input files\n"]
    for f in files:
        parts.append(f"\n===== FILE: {f.relative_to(FACTORY)} =====\n{f.read_text()}")
    return "\n".join(parts)


def ollama(prompt, num_ctx, num_predict):
    body = {"model": OLLAMA_MODEL, "stream": False, "prompt": prompt,
            "options": {"temperature": 0.15, "num_ctx": num_ctx, "num_predict": num_predict}}
    req = urllib.request.Request(f"{OLLAMA_URL}/api/generate", data=json.dumps(body).encode(),
                                 headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=3 * 3600) as r:
        return json.load(r)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--step", type=int, choices=[1], required=True)
    ap.add_argument("--public-only", action="store_true")
    ap.add_argument("--out", default=str(FACTORY / "local-run"))
    ap.add_argument("--num-predict", type=int, default=12000)
    args = ap.parse_args()

    files = step1_inputs(args.public_only)
    prompt = step1_prompt(files)
    est = int(len(prompt) / CHARS_PER_TOKEN)
    num_ctx = min(131072, ((est + args.num_predict) // 8192 + 2) * 8192)
    print(f"model {OLLAMA_MODEL}: {len(files)} files, {len(prompt)} chars, about {est} tokens, num_ctx {num_ctx}")

    t0 = time.time()
    res = ollama(prompt, num_ctx, args.num_predict)
    took = time.time() - t0
    text = res.get("response", "")

    out = pathlib.Path(args.out) / f"step{args.step}-{OLLAMA_MODEL.replace(':', '_')}"
    out.mkdir(parents=True, exist_ok=True)
    (out / "spec-digest.md").write_text(text)
    stats = {
        "model": OLLAMA_MODEL, "files": len(files), "public_only": args.public_only,
        "prompt_tokens": res.get("prompt_eval_count"), "output_tokens": res.get("eval_count"),
        "prompt_seconds": round(res.get("prompt_eval_duration", 0) / 1e9, 1),
        "output_seconds": round(res.get("eval_duration", 0) / 1e9, 1),
        "wall_seconds": round(took, 1), "num_ctx": num_ctx, "done_reason": res.get("done_reason"),
        "decision_rows": len(set(re.findall(r"\bD(\d{1,2})\b", text))),
        "headings": len(re.findall(r"^#{1,4} ", text, re.M)),
    }
    (out / "stats.json").write_text(json.dumps(stats, indent=2) + "\n")
    for k, v in stats.items():
        print(f"  {k}: {v}")
    if stats["prompt_tokens"] and stats["prompt_tokens"] >= num_ctx - args.num_predict:
        print("WARNING: input may have been truncated", file=sys.stderr)


if __name__ == "__main__":
    main()
