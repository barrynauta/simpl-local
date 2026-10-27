#!/usr/bin/env python3
"""Collect user stories from Jira into specs/stories/, with a local LLM for the conversion.

Everything stays on this machine: Jira is read with a personal access token, and the
conversion from Jira wiki markup to Markdown runs on a local model through Ollama. No
story text is sent to any other service.

Steps per story:
  1. Fetch key, summary, status, type, dates and description (no comments, no people).
  2. The local model converts the description from Jira wiki markup to plain Markdown.
  3. A check compares the model's words with the source words. If the model added,
     dropped or changed text, it retries once, then falls back to a plain conversion
     without the model. The report says which stories fell back.
  4. One layout for all stories: one line per clause, story and scenario keywords
     (As a, I want, So that, Given, When, Then, And, But) in bold and capitalised.

The script prints counts and check results only, never story text.

Configuration (environment variables):
  JIRA_URL      Jira base URL                 (default https://jira.simplprogramme.eu)
  JIRA_PAT      personal access token         (required)
  OLLAMA_URL    Ollama base URL               (default http://localhost:11434)
  OLLAMA_MODEL  model name                    (default mistral-small3.2)

Usage:
  collect_stories.py --out specs/stories SIMPL-18161 SIMPL-18162 ...
  collect_stories.py --out specs/stories --keys-from specs/stories   # same keys as the current files
  collect_stories.py --out specs/stories --no-llm ...                # plain conversion only
"""
import argparse
import datetime
import difflib
import json
import os
import re
import sys
import urllib.parse
import urllib.request

JIRA_URL = os.environ.get("JIRA_URL", "https://jira.simplprogramme.eu").rstrip("/")
OLLAMA_URL = os.environ.get("OLLAMA_URL", "http://localhost:11434").rstrip("/")
OLLAMA_MODEL = os.environ.get("OLLAMA_MODEL", "mistral-small3.2")
MIN_RATIO = 0.98  # share of words that must match the source, in order

PROMPT = """Convert the Jira wiki markup below to plain Markdown.

Rules:
- Keep every word exactly as it is. Do not add, remove, reword, translate, correct or summarise anything.
- Remove the markup only: *bold*, _italic_, {_}, {{monospace}}, {color}, h1. to h6., [text|url] links.
- Write links as [text](url). Write bullet lists with "- ".
- Keep table rows as they are, one row per line: |cell|cell|. Do not split cells over lines.
- Keep the line structure: each line of the source stays its own line. Keep empty lines between blocks.
- Output only the converted text, nothing before or after it.

Jira wiki markup:
"""

STORY_KW = r"As an?|As the|I want|I need|So that"
STEP_KW = r"Given|When|Then|And|But"


# ---------- Jira ----------

def jira_issue(key, token):
    fields = "summary,status,issuetype,created,updated,description"
    url = f"{JIRA_URL}/rest/api/2/issue/{urllib.parse.quote(key)}?fields={fields}"
    req = urllib.request.Request(url, headers={"Authorization": "Bearer " + token, "Accept": "application/json"})
    with urllib.request.urlopen(req, timeout=60) as r:
        f = json.load(r)["fields"]
    return {"key": key, "summary": f["summary"], "status": f["status"]["name"], "type": f["issuetype"]["name"],
            "created": f["created"][:10], "updated": f["updated"][:10], "description": f.get("description") or ""}


# ---------- conversion ----------

def plain(markup):
    """Conversion without a model: strip the common Jira wiki markup."""
    t = markup.replace("\r\n", "\n")
    t = re.sub(r"\{color(:[^}]*)?\}", "", t)
    t = re.sub(r"\{(code|noformat)(:[^}]*)?\}", "```", t)
    t = t.replace("{_}", "").replace("{*}", "")
    t = re.sub(r"\{\{(.*?)\}\}", r"`\1`", t)
    t = re.sub(r"\[([^|\]]+)\|([^\]]+)\]", r"[\1](\2)", t)
    t = re.sub(r"^h[1-6]\.\s*", "", t, flags=re.M)
    t = re.sub(r"^\s*[*#]+\s+", "- ", t, flags=re.M)
    t = re.sub(r"(?<![\w*])\*(\S(?:.*?\S)?)\*(?![\w*])", r"\1", t)
    t = re.sub(r"(?<![\w_])_(\S(?:.*?\S)?)_(?![\w_])", r"\1", t)
    return "\n".join(l.rstrip() for l in t.split("\n")).strip()


def ollama(markup):
    body = {"model": OLLAMA_MODEL, "stream": False, "options": {"temperature": 0},
            "messages": [{"role": "user", "content": PROMPT + markup}]}
    req = urllib.request.Request(OLLAMA_URL + "/api/chat", data=json.dumps(body).encode(),
                                 headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=600) as r:
        text = json.load(r)["message"]["content"]
    text = re.sub(r"<think>.*?</think>", "", text, flags=re.S)  # thinking models
    return re.sub(r"^```(markdown)?\n|\n```$", "", text.strip()).strip()


def words(text):
    text = re.sub(r"\]\([^)]*\)", "]", text)  # link targets do not count
    return re.findall(r"[a-z0-9]+", text.lower())


def same_words(source_markup, converted):
    """Ratio of matching words between the source and the converted text, in order."""
    return difflib.SequenceMatcher(None, words(plain(source_markup)), words(converted), autojunk=False).ratio()


# ---------- layout ----------

def layout(text):
    """One line per clause, keywords in bold and capitalised, hard line breaks between lines."""
    # a keyword may come wrapped in emphasis or after a bullet; that markup is dropped
    story = re.compile(rf"^(?:[-*]\s+)?[*_]*({STORY_KW})\b[*_]*\s*(.*)$", re.I)
    step = re.compile(rf"^(?:[-*]\s+)?[*_]*({STEP_KW})\b[*_]*\s+(.*)$", re.I)
    out, in_scenario = [], False
    table_row = re.compile(rf"^\|+\s*\**({STEP_KW})\**\s*\|+\s*(.*?)\s*\|*$", re.I)
    for raw in text.split("\n"):
        line = raw.strip()
        # emphasis around the keyword ("*As* a", "*Given*") is dropped; only the first 12 characters are touched
        if re.match(r"^(?:[-*]\s+)?[*_]\w", line):
            bare = re.sub(r"[*_]", "", line[:12]) + line[12:]
            if story.match(bare) or step.match(bare):
                line = bare
        t = table_row.match(line)
        if t:  # acceptance criteria written as a two-column table: |Given|text|
            line = f"{t.group(1)} {t.group(2)}"
        m = story.match(line)
        if m:
            kw = m.group(1)
            line = f"**{kw[0].upper() + kw[1:]}** {m.group(2)}".rstrip()
        else:
            m = step.match(line)
            # a lowercase keyword only counts when it continues a scenario
            if m and (m.group(1)[0].isupper() or in_scenario):
                line = f"**{m.group(1).capitalize()}** {m.group(2)}".rstrip()
                in_scenario = True
            elif line:
                in_scenario = False  # any other text line ends the scenario; empty lines do not
        out.append(line)
    for i in range(len(out) - 1):
        if out[i] and out[i + 1] and not out[i].endswith("\\"):
            out[i] += "\\"
    return "\n".join(out).strip()


def story_file(issue, body, today):
    return (f"# {issue['key']}: {issue['summary']}\n"
            f"- Status: {issue['status']}\n- Type: {issue['type']}\n"
            f"- Created: {issue['created']}, updated: {issue['updated']}\n"
            f"- Source: {JIRA_URL}/browse/{issue['key']} (collected {today})\n\n"
            f"## Description and acceptance criteria\n\n{body}\n")


# ---------- main ----------

def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("keys", nargs="*", help="issue keys, e.g. SIMPL-18161")
    ap.add_argument("--keys-from", help="take the keys from the *.md file names in this folder")
    ap.add_argument("--out", required=True, help="output folder for the story files")
    ap.add_argument("--no-llm", action="store_true", help="plain conversion only, no model")
    a = ap.parse_args()

    token = os.environ.get("JIRA_PAT") or sys.exit("JIRA_PAT is not set")
    keys = list(a.keys)
    if a.keys_from:
        keys += sorted(f[:-3] for f in os.listdir(a.keys_from) if re.fullmatch(r"[A-Z]+-\d+\.md", f))
    if not keys:
        sys.exit("no issue keys given")
    os.makedirs(a.out, exist_ok=True)
    today = datetime.date.today().isoformat()

    print(f"model: {'none' if a.no_llm else OLLAMA_MODEL}, stories: {len(keys)}")
    fallbacks = []
    for key in keys:
        issue = jira_issue(key, token)
        source, how, ratio = issue["description"], "plain", 1.0
        if not a.no_llm and source.strip():
            for attempt in (1, 2):
                converted = ollama(source)
                ratio = same_words(source, converted)
                if ratio >= MIN_RATIO:
                    how = "model" if attempt == 1 else "model, 2nd try"
                    break
            else:
                fallbacks.append(key)
        if how == "plain":
            converted = plain(source)
            ratio = same_words(source, converted)
        with open(os.path.join(a.out, key + ".md"), "w") as f:
            f.write(story_file(issue, layout(converted), today))
        print(f"  {key:12} {how:15} word match {ratio:.3f}")
    print(f"done: {len(keys)} stories, {len(fallbacks)} fell back to the plain conversion"
          + (f" ({', '.join(fallbacks)})" if fallbacks else ""))


if __name__ == "__main__":
    main()
