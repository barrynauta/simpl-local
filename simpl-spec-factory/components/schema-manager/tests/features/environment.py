"""Behave environment for the Schema Manager black-box suite.

All configuration comes from environment variables. Names follow D60 of the
spec digest; defaults are the reference environment of the factory build.
An empty value switches a piece off, and the scenarios that need it are skipped.

SM_BASE_URL
    Base URL of the API, including any prefix. Default http://localhost:8098/v1.
    Upstream local stack: http://localhost:8085.
AUTH_MODE
    oidc (default): fetch tokens from Keycloak with the password grant (D59).
    none: the stack does not validate tokens. The suite sends a self-made
    HS256 token carrying the role claims (`roles` and `realm_access.roles`)
    and `preferred_username`. Scenarios tagged @needs-idp are skipped.
KEYCLOAK_URL
    Keycloak base URL. Default http://localhost:8185. Realm `simpl` (D59).
KEYCLOAK_REALM
    Realm name. Default simpl.
CLIENT_ID
    Public client with direct access grants. Default schema-tests.
TEST_USER_PASSWORD
    Password of ga-admin, ga-viewer and ga-norole. Default password.
VOCABULARY_MANAGER_URL
    Vocabulary Manager stub (D60 contract), seeded with PUT. Default
    http://localhost:8099. Empty: scenarios tagged @vocab are skipped.
VOCABULARY_NAMESPACE_BASE
    Namespace base of local vocabularies, as configured in the service (D34).
    Default https://vocabulary.simpl.test/content/.
MAIL_API_URL
    Mailpit HTTP API. Default http://localhost:8027. Empty: @mail is skipped.
PROVIDER_NOTIFICATION_EMAILS
    Optional. The recipients configured in the service (comma-separated). When
    set, the mail scenarios also check that every address received the mail.
WEBHOOK_RECEIVER_URL
    URL under which the service reaches the receiver the suite runs on the host.
    Default http://host.docker.internal:8299. Empty: @webhook is skipped.
WEBHOOK_LISTEN_PORT
    Port the receiver listens on, on all interfaces. Default 8299.
SM_VM_DOWN_RUN
    true only in the dedicated run where the Vocabulary Manager is stopped or
    unreachable from the service. Enables @vm-down (D36). Default false.
SM_RUN_ID
    Optional fixed run id (uppercase letters and digits) instead of a random
    one. Only for debugging: names must stay unique, so never reuse a run id.
HTTP_TIMEOUT
    Timeout per HTTP call in seconds. Default 30.
EVENT_WAIT_SECONDS / ABSENCE_WAIT_SECONDS
    How long to wait for an e-mail or delivery (default 10, D44) and how long
    to wait to confirm the absence of one (default 3, D44).

UI scenarios (@ui, browser steps with Playwright and headless Chromium):

SM_UI_URL
    Base URL of the UI (D60 name). Default http://localhost:4334 (factory
    build). Upstream local stack: http://localhost:4322.
UI_AUTH
    oidc (default): the UI sends the browser to the identity provider; the
    steps sign in on its login page as ga-admin, ga-viewer or ga-norole with
    TEST_USER_PASSWORD (generic locators: a user name field, a password field,
    a sign-in button). none: the UI has no sign-in; the user cannot be chosen,
    so scenarios that depend on the role are expected to fail (environment).
SM_UI_API_URL
    Base URL(s) of the API as the browser calls it (comma-separated), used to
    recognise API requests ("the UI sent the request ...") and for fault
    injection. Default: SM_BASE_URL plus the path of SM_BASE_URL on the UI
    origin (a UI that proxies the API, e.g. http://localhost:4334/v1). Upstream
    local stack: http://localhost:4322/v1 (its nginx proxy).
UI_HEADLESS
    false shows the browser window. Default true.
UI_SCREENSHOT_DIR
    Optional. When set, a full-page screenshot of every failed UI scenario is
    saved there.

The browser is launched once, at the first @ui scenario; every @ui scenario
gets its own browser context (fresh cookies, so a fresh sign-in) and page.
"""
import itertools

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import smlib  # noqa: E402

# Tag -> (reason, predicate on config that must be true to run)
REQUIREMENTS = {
    "needs-idp": ("needs a validating identity provider (AUTH_MODE=oidc)", lambda c: c.auth_mode == "oidc"),
    "vocab": ("needs the Vocabulary Manager stub (VOCABULARY_MANAGER_URL)", lambda c: bool(c.vocabulary_manager_url)),
    "mail": ("needs mail capture (MAIL_API_URL)", lambda c: bool(c.mail_api_url)),
    "webhook": ("needs the webhook receiver (WEBHOOK_RECEIVER_URL)", lambda c: bool(c.webhook_receiver_url)),
    "vm-down": ("only in the dedicated run with the Vocabulary Manager down (SM_VM_DOWN_RUN=true)",
                lambda c: c.vm_down_run),
}


def before_all(context):
    context.cfg = smlib.Config()
    context.tokens = smlib.TokenProvider(context.cfg)
    context.run = smlib.run_id()
    # A mutable counter: a plain `context.seq += 1` in before_scenario would be set on the
    # scenario layer of the behave context and lost afterwards, so every scenario got seq 1.
    context.seq_counter = itertools.count(1)
    context.receiver = None
    dry = getattr(context.config, "dry_run", False)
    if context.cfg.webhook_receiver_url and not dry:
        context.receiver = smlib.WebhookReceiver(context.cfg.webhook_listen_port)
        context.receiver.start()
    context.mailpit = smlib.Mailpit(context.cfg.mail_api_url) if context.cfg.mail_api_url else None
    # Mutable holder on the root layer, so the lazily launched browser survives scenarios.
    context.browser_holder = {}
    print(f"Schema Manager suite, run id {context.run}, target {context.cfg.sm_base_url}, "
          f"auth {context.cfg.auth_mode}")


def after_all(context):
    if context.receiver:
        context.receiver.stop()
    holder = getattr(context, "browser_holder", {})
    if holder.get("browser"):
        holder["browser"].close()
        holder["playwright"].stop()


class UiSession:
    """Browser context and page of one @ui scenario, with the request log."""

    def __init__(self, browser_context):
        self.context = browser_context
        self.page = browser_context.new_page()
        self.requests = []        # (method, url) of every request the page sent
        self.checkpoint = 0       # index into requests: "sent no request" counts from here
        self.downloads = []
        self.download_error = None
        self.page.on("request", lambda r: self.requests.append((r.method, r.url)))


def start_ui(context):
    holder = context.browser_holder
    if not holder.get("browser"):
        from playwright.sync_api import sync_playwright
        holder["playwright"] = sync_playwright().start()
        holder["browser"] = holder["playwright"].chromium.launch(headless=context.cfg.ui_headless)
    browser_context = holder["browser"].new_context(accept_downloads=True, viewport={"width": 1600, "height": 1000})
    browser_context.set_default_timeout(15000)
    return UiSession(browser_context)


def before_scenario(context, scenario):
    context.names = smlib.Names(context.run, next(context.seq_counter), context.cfg)
    context.user = "ga-admin"
    context.auth = "user"          # user | none | garbage | tampered | unsigned | foreign | expired
    context.response = None
    context.concepts = {}          # alias -> {"type": ..., "rt": ...}
    context.uploads = {}           # alias -> {version: bytes}
    context.hooks = {}             # alias -> {"id": ..., "path": ...}
    context.parallel = []
    context.created_hook_ids = []  # every subscription id created, for cleanup
    context.ui = None
    for tag in scenario.effective_tags:
        req = REQUIREMENTS.get(tag)
        if req and not req[1](context.cfg):
            scenario.skip(req[0])
            return
    if "ui" in scenario.effective_tags and not getattr(context.config, "dry_run", False):
        context.ui = start_ui(context)


def after_scenario(context, scenario):
    session = getattr(context, "ui", None)
    if session is not None:
        try:
            if scenario.status == "failed" and context.cfg.ui_screenshot_dir:
                os.makedirs(context.cfg.ui_screenshot_dir, exist_ok=True)
                safe = "".join(ch if ch.isalnum() else "_" for ch in scenario.name)[:80]
                session.page.screenshot(path=os.path.join(context.cfg.ui_screenshot_dir, f"{safe}.png"),
                                        full_page=True)
        except Exception as exc:
            print(f"screenshot failed: {exc}")
        try:
            session.context.close()
        except Exception as exc:
            print(f"closing the browser context failed: {exc}")
        context.ui = None
    # Webhook subscriptions are the only data that can be cleaned up (D61).
    import requests
    for hook_id in set(getattr(context, "created_hook_ids", [])):
        if not hook_id:
            continue
        try:
            requests.delete(f"{context.cfg.sm_base_url}/webhooks/{hook_id}",
                            headers={"Authorization": f"Bearer {context.tokens.token('ga-admin')}"},
                            timeout=context.cfg.http_timeout)
        except Exception as exc:  # cleanup must never fail a scenario
            print(f"cleanup of webhook {hook_id} failed: {exc}")
