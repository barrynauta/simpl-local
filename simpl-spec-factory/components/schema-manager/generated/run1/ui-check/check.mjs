// Own UI smoke check (Playwright, headless Chromium) against the running compose stack.
// It logs in through Keycloak (schema-ui, PKCE) and walks the main UI stories.
import { chromium } from 'playwright';
import { writeFileSync, mkdtempSync } from 'node:fs';
import { join } from 'node:path';
import { tmpdir } from 'node:os';

const UI = process.env.SM_UI_URL ?? 'http://localhost:4334';
const sfx = `${Date.now()}`;
const dir = mkdtempSync(join(tmpdir(), 'smui-'));
const ttl = join(dir, 'schema.ttl');
writeFileSync(ttl, `@prefix sh: <http://www.w3.org/ns/shacl#> .
@prefix simpl: <https://simpl-programme.ec.europa.eu/ns/default#> .
<urn:shape:Ui> a sh:NodeShape ;
  sh:property [ sh:path simpl:uuid ] ;
  sh:property [ sh:path simpl:version ] .
`);
const bad = join(dir, 'bad.ttl');
writeFileSync(bad, '@prefix sh: <http://www.w3.org/ns/shacl#> .\n<urn:x> a sh:NodeShape ; sh:property [ \n');

let failures = 0;
const ok = (cond, msg) => { console.log(`${cond ? 'PASS' : 'FAIL'}  ${msg}`); if (!cond) failures++; };

async function login(browser, user) {
  const ctx = await browser.newContext({ acceptDownloads: true });
  const page = await ctx.newPage();
  await page.goto(`${UI}/schemas`);
  await page.fill('#username', user);
  await page.fill('#password', 'password');
  await page.click('#kc-login');
  await page.waitForURL(`${UI}/schemas**`);
  return { ctx, page };
}

const browser = await chromium.launch();
try {
  // Admin
  const { page } = await login(browser, 'ga-admin');
  await page.getByTestId('schema-list-table').waitFor();
  const headers = await page.locator('[data-testid=schema-list-table] th').allInnerTexts();
  ok(headers.join('|') === 'Schema Name|Title|Description|Schema Type|Resource Type|Latest Version|Status|Creation Date|Creator|Actions',
    `list headers: ${headers.join('|')}`);
  ok(await page.getByTestId('create-schema-button').isVisible(), 'admin sees create button');

  // Create with invalid file -> error notification, stay on form
  const name = `TUi${sfx}`;
  await page.getByTestId('create-schema-button').click();
  await page.getByTestId('field-name').fill(name);
  await page.getByTestId('field-title').fill('UI title');
  await page.getByTestId('field-description').fill('UI description');
  ok(await page.getByTestId('submit-button').isDisabled(), 'submit disabled while form incomplete');
  await page.getByTestId('field-schema-type').selectOption('SEARCH');
  ok(await page.getByTestId('field-resource-type').isDisabled(), 'resource type disabled for SEARCH');
  await page.getByTestId('field-schema-type').selectOption('RESOURCE');
  await page.getByTestId('field-resource-type').selectOption('Infrastructure');
  await page.getByTestId('field-file').setInputFiles(bad);
  await page.getByTestId('submit-button').click();
  await page.getByTestId('notification-error').waitFor();
  const err = await page.getByTestId('notification-error').innerText();
  ok(err.includes('Syntax Error: '), `syntax error shown: ${err.slice(0, 60)}`);
  ok(page.url().endsWith('/schemas/new'), 'stays on the form after error');

  await page.getByTestId('field-file').setInputFiles(ttl);
  await page.getByTestId('submit-button').click();
  await page.getByTestId('versions-table').waitFor();
  ok((await page.getByTestId('notification-success').innerText()).includes(`Schema "${name}" created successfully.`), 'create success text');
  ok(await page.getByTestId('version-row-1').isVisible(), 'details shows version 1');

  // New version
  await page.getByTestId(`action-new-version-${name}`).click();
  ok(await page.getByTestId('field-schema-type').isDisabled(), 'schema type read-only on new version');
  await page.getByTestId('field-changelog').fill('Second');
  await page.getByTestId('field-file').setInputFiles(ttl);
  await page.getByTestId('submit-button').click();
  await page.getByTestId('version-row-2').waitFor();
  ok(true, 'new version 2 visible on details page');

  // Download a version
  const dl = page.waitForEvent('download');
  await page.getByTestId('action-download-version-1').click();
  ok((await dl).suggestedFilename() === `${name}-1.ttl`, 'version download file name');
  ok((await page.getByTestId('notification-success').innerText()).includes('Schema version 1 downloaded successfully.'), 'download success text');

  // List: filter, download latest, revoke, publish
  await page.goto(`${UI}/schemas`);
  await page.getByTestId('filter-resource-type').selectOption('Infrastructure');
  await page.getByTestId(`schema-row-${name}`).waitFor();
  const dl2 = page.waitForEvent('download');
  await page.getByTestId(`action-download-${name}`).click();
  ok((await dl2).suggestedFilename() === `${name}-2.ttl`, 'latest download file name');
  const status = await page.getByTestId(`schema-row-${name}`).locator('td').nth(6).innerText();
  if (status === 'PUBLISHED') {
    await page.getByTestId(`action-revoke-${name}`).click();
    await page.getByTestId('cancel-button').click();
    ok(!(await page.getByTestId('confirm-dialog').isVisible()), 'cancel closes dialog');
    await page.getByTestId(`action-revoke-${name}`).click();
    await page.getByTestId('confirm-button').click();
    await page.getByTestId('notification-success').waitFor();
    ok((await page.getByTestId('notification-success').innerText()).includes(`Schema "${name}" revoked successfully.`), 'revoke success text');
    await page.getByTestId(`action-publish-${name}`).waitFor();
  }
  if (await page.locator('[data-testid^=notification-] .close').count()) await page.locator('[data-testid^=notification-] .close').click();
  await page.getByTestId(`action-publish-${name}`).click();
  await page.getByTestId('notification-success').or(page.getByTestId('notification-error')).first().waitFor();
  const note = await page.locator('[data-testid^=notification-] span').innerText();
  ok(note.includes(`Schema ${name} published successfully!`) || note.includes('Another schema is in PUBLISHED state'), `publish result: ${note}`);

  await page.getByTestId('filter-status').selectOption('REVOKED');
  await page.getByTestId('filter-schema-type').selectOption('SEARCH');
  ok(await page.getByTestId('filter-resource-type').isDisabled(), 'list resource type filter disabled for SEARCH');

  // Viewer
  const v = await login(browser, 'ga-viewer');
  await v.page.getByTestId('schema-list-table').waitFor();
  ok(!(await v.page.getByTestId('create-schema-button').count()), 'viewer has no create button');
  ok((await v.page.locator('[data-testid^=action-revoke-], [data-testid^=action-publish-], [data-testid^=action-new-version-]').count()) === 0, 'viewer has no admin actions');

  // No role
  const n = await login(browser, 'ga-norole');
  await n.page.getByTestId('error-state').waitFor();
  ok((await n.page.getByTestId('error-state').innerText()).includes('You do not have permission to view this page.'), 'norole sees permission message');
  ok((await n.page.getByTestId('schema-list-table').count()) === 0, 'norole sees no table');
} catch (e) {
  console.error(e);
  failures++;
} finally {
  await browser.close();
}
console.log(failures ? `UI CHECK FAILED (${failures})` : 'UI CHECK OK');
process.exit(failures ? 1 : 0);
