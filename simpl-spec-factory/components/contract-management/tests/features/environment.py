"""Behave hooks for the Contract Management black-box suite.

All configuration comes from environment variables. Defaults are the
reference environment of the factory build (digest D67, D76, D77, compose
project ``cm-factory``).

Endpoints
    CM_PROVIDER_URL        Provider CM base URL     (http://localhost:19101/contract/v1)
    CM_CONSUMER_URL        Consumer CM base URL     (http://localhost:19201/contract/v1)
    UIBE_PROVIDER_URL      Provider UIBE base URL   (http://localhost:19102/contract-ui/v1)
    UIBE_CONSUMER_URL      Consumer UIBE base URL   (http://localhost:19202/contract-ui/v1)
    UI_PROVIDER_URL        Provider UI              (http://localhost:19103), UI suite only
    UI_CONSUMER_URL        Consumer UI              (http://localhost:19203), UI suite only
    STUB_URL               Stub service             (http://localhost:19090)
    KAFKA_BOOTSTRAP        Kafka from the host      (localhost:19092)
    KEYCLOAK_URL           Keycloak                 (http://localhost:19080)
    CONSENT_MANAGER_URL    Consent link base, D43   (http://localhost:19090/consent)

Keys and identities
    CM_PROVIDER_API_KEY    (test-cm-provider-key)      CM_CONSUMER_API_KEY   (test-cm-consumer-key)
    UIBE_PROVIDER_API_KEY  (test-uibe-provider-key)    UIBE_CONSUMER_API_KEY (test-uibe-consumer-key)
    STUB_EDC_API_KEY       (test-edc-key)
    PROVIDER_PARTICIPANT_ID (provider-01)   CONSUMER_PARTICIPANT_ID (consumer-01)
    ROGUE_PARTICIPANT_ID    (rogue-01)
    PROVIDER_REALM (provider)   CONSUMER_REALM (consumer)   KEYCLOAK_CLIENT_ID (contract-ui)
    PROVIDER_ADMIN_PASSWORD, PROVIDER_OPERATOR_PASSWORD, PROVIDER_NOROLE_PASSWORD,
    CONSUMER_ADMIN_PASSWORD, CONSUMER_NOROLE_PASSWORD   (the D67 passwords)

Modes and switches
    AUTH_MODE      oidc (default): UIBE user calls carry a Keycloak token.
                   none: no Keycloak; user calls carry no token and scenarios
                   tagged @oidc are skipped.
    SKIP_STUB=1      skip scenarios tagged @stub (they need the Appendix C stub)
    SKIP_KAFKA=1     skip scenarios tagged @events
    SKIP_CONSUMER=1  skip scenarios tagged @two-party or @consumer (second party)
    SKIP_UIBE=1      skip scenarios tagged @uibe; offer decisions and lifecycle
                     actions are then sent to CM directly
    CLAUSE_MAX_BYTES (5242880)   upload limit of D63
    OFFER_EXPIRY_TIMEOUT_DAYS (7) timeout of D44, used for asOf (D82)

UI suite (features/ui, scenarios tagged @ui; browser started only for them)
    UI_PROVIDER_URL, UI_CONSUMER_URL  see Endpoints (D76 defaults 19103, 19203)
    UI_AUTH        oidc (default): the UI sends the browser to the identity
                   provider's login page; the steps sign in there with the D67
                   users (generic locators: a "user name" or "email" field, a
                   "password" field, a "sign in" or "log in" button).
                   none: the UI has no sign-in; the login steps only open the UI.
                   Scenarios tagged @oidc are skipped with AUTH_MODE=none.
    UI_TIMEOUT     (20 s) how long a browser expectation waits
    UI_HEADED=1    show the browser instead of running headless Chromium
    UI_SCREENSHOT_DIR (reports/ui-screenshots) full-page screenshot of every
                   failed UI scenario; empty to switch off

Timing (D79)
    POLL_INTERVAL (0.25 s)  POLL_TIMEOUT (20 s)  QUIET_PERIOD (2 s, how long a
    "nothing happens" check waits)  HTTP_TIMEOUT (15 s)
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from cmtest.config import Config  # noqa: E402
from cmtest.core import Http, Tokens  # noqa: E402
from cmtest.world import World  # noqa: E402
from cmtest import browser  # noqa: E402


SKIP_RULES = [
    ("skip_stub", "stub", "SKIP_STUB is set"),
    ("skip_kafka", "events", "SKIP_KAFKA is set"),
    ("skip_consumer", "two-party", "SKIP_CONSUMER is set"),
    ("skip_consumer", "consumer", "SKIP_CONSUMER is set"),
    ("skip_uibe", "uibe", "SKIP_UIBE is set"),
]


def before_all(context):
    context.config_cm = Config()
    context.http = Http(context.config_cm)
    context.tokens = Tokens(context.config_cm, context.http)
    context.world = World(context.config_cm, context.http, context.tokens)


def before_scenario(context, scenario):
    cfg = context.config_cm
    tags = set(scenario.effective_tags)
    for attr, tag, why in SKIP_RULES:
        if getattr(cfg, attr) and tag in tags:
            scenario.skip("%s (@%s)" % (why, tag))
            return
    if cfg.auth_mode == "none" and "oidc" in tags:
        scenario.skip("AUTH_MODE=none (@oidc)")
        return
    context.vars = {
        "providerId": cfg.provider.participant_id,
        "consumerId": cfg.consumer.participant_id,
        "rogueId": cfg.rogue_participant_id,
        "consumerUiUrl": cfg.consumer.ui_url,
        "providerUiUrl": cfg.provider.ui_url,
        "consentManagerUrl": cfg.consent_manager_url,
    }
    context.response = None
    context.responses = []
    context.watchers = {}
    context.nids = []
    context.saved = {}
    if "ui" in tags:
        browser.start(context)


def after_scenario(context, scenario):
    if getattr(context, "ui", None):
        browser.stop(context, scenario)
    for watcher in getattr(context, "watchers", {}).values():
        watcher.close()
    if not context.config_cm.skip_stub:
        for nid in getattr(context, "nids", []):
            try:
                context.world.clear_faults(nid)
            except Exception:  # noqa: BLE001
                pass


def after_all(context):
    browser.shutdown()
