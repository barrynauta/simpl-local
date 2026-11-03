#!/usr/bin/env python3
"""Walk the read page of the local upstream UI (shell plus local overlay): type an agreement id,
press the button, report the HTTP call and the fields the page shows.

  walk_upstream_ui.py <ui base url> <contractAgreementId>
"""
import sys
from playwright.sync_api import sync_playwright

base, aid = sys.argv[1].rstrip('/'), sys.argv[2]
with sync_playwright() as p:
    b = p.chromium.launch(); pg = b.new_page(); calls = []
    pg.on('response', lambda r: calls.append(f'{r.request.method} {r.url.replace(base, "")} {r.status}') if '/contract/' in r.url else None)
    pg.goto(base + '/'); pg.wait_for_load_state('networkidle')
    pg.get_by_placeholder('Contract agreement id (UUID)').fill(aid)
    pg.get_by_role('button').first.click(); pg.wait_for_timeout(2500)
    print('calls:', '; '.join(calls) or 'none')
    dts = pg.locator('dt').all_inner_texts(); dds = pg.locator('dd').all_inner_texts()
    for k, v in zip(dts, dds): print(f'  {k}: {v}')
    if not dts: print('  page text:', pg.locator('main, body').first.inner_text()[:300].replace('\n', ' | '))
    b.close()
