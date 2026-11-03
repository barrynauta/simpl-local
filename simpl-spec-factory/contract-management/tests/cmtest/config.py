"""Configuration of the suite, read from environment variables.

All defaults are those of the reference environment of the factory build
(digest D67, D76, D77). See features/environment.py for the full list.
"""
import os


def _env(name, default=None):
    value = os.environ.get(name)
    if value is None or value == "":
        return default
    return value


def _flag(name):
    return str(_env(name, "0")).strip().lower() in ("1", "true", "yes", "on")


class Party:
    def __init__(self, role, participant_id, cm_url, cm_key, uibe_url, uibe_key,
                 realm, ui_url):
        self.role = role                      # "provider" or "consumer"
        self.participant_id = participant_id
        self.cm_url = cm_url.rstrip("/")
        self.cm_key = cm_key
        self.uibe_url = uibe_url.rstrip("/")
        self.uibe_key = uibe_key
        self.realm = realm
        self.ui_url = ui_url.rstrip("/")

    @property
    def upper_role(self):
        return self.role.upper()


class Config:
    def __init__(self):
        self.provider = Party(
            "provider",
            _env("PROVIDER_PARTICIPANT_ID", "provider-01"),
            _env("CM_PROVIDER_URL", "http://localhost:19101/contract/v1"),
            _env("CM_PROVIDER_API_KEY", "test-cm-provider-key"),
            _env("UIBE_PROVIDER_URL", "http://localhost:19102/contract-ui/v1"),
            _env("UIBE_PROVIDER_API_KEY", "test-uibe-provider-key"),
            _env("PROVIDER_REALM", "provider"),
            _env("UI_PROVIDER_URL", "http://localhost:19103"),
        )
        self.consumer = Party(
            "consumer",
            _env("CONSUMER_PARTICIPANT_ID", "consumer-01"),
            _env("CM_CONSUMER_URL", "http://localhost:19201/contract/v1"),
            _env("CM_CONSUMER_API_KEY", "test-cm-consumer-key"),
            _env("UIBE_CONSUMER_URL", "http://localhost:19202/contract-ui/v1"),
            _env("UIBE_CONSUMER_API_KEY", "test-uibe-consumer-key"),
            _env("CONSUMER_REALM", "consumer"),
            _env("UI_CONSUMER_URL", "http://localhost:19203"),
        )
        self.rogue_participant_id = _env("ROGUE_PARTICIPANT_ID", "rogue-01")
        self.stub_url = _env("STUB_URL", "http://localhost:19090").rstrip("/")
        self.stub_edc_key = _env("STUB_EDC_API_KEY", "test-edc-key")
        self.kafka_bootstrap = _env("KAFKA_BOOTSTRAP", "localhost:19092")
        self.keycloak_url = _env("KEYCLOAK_URL", "http://localhost:19080").rstrip("/")
        self.keycloak_client_id = _env("KEYCLOAK_CLIENT_ID", "contract-ui")
        self.auth_mode = _env("AUTH_MODE", "oidc").lower()
        self.consent_manager_url = _env("CONSENT_MANAGER_URL",
                                        "http://localhost:19090/consent")
        self.clause_max_bytes = int(_env("CLAUSE_MAX_BYTES", str(5 * 1024 * 1024)))
        self.offer_expiry_timeout_days = int(_env("OFFER_EXPIRY_TIMEOUT_DAYS", "7"))
        self.poll_interval = float(_env("POLL_INTERVAL", "0.25"))
        self.poll_timeout = float(_env("POLL_TIMEOUT", "20"))
        self.quiet_period = float(_env("QUIET_PERIOD", "2"))
        self.http_timeout = float(_env("HTTP_TIMEOUT", "15"))
        self.skip_stub = _flag("SKIP_STUB")
        self.skip_kafka = _flag("SKIP_KAFKA")
        self.skip_consumer = _flag("SKIP_CONSUMER")
        self.skip_uibe = _flag("SKIP_UIBE")
        # UI suite (features/ui, tagged @ui)
        self.ui_auth = _env("UI_AUTH", "oidc").lower()          # oidc | none
        self.ui_headless = not _flag("UI_HEADED")
        self.ui_timeout_ms = int(float(_env("UI_TIMEOUT", "20")) * 1000)
        self.ui_screenshot_dir = _env("UI_SCREENSHOT_DIR", "reports/ui-screenshots")
        # Users of D67: username -> (party role, password)
        self.users = {
            "provider-admin": ("provider", _env("PROVIDER_ADMIN_PASSWORD", "ProviderAdmin1!")),
            "provider-operator": ("provider", _env("PROVIDER_OPERATOR_PASSWORD", "ProviderOperator1!")),
            "provider-norole": ("provider", _env("PROVIDER_NOROLE_PASSWORD", "ProviderNoRole1!")),
            "consumer-admin": ("consumer", _env("CONSUMER_ADMIN_PASSWORD", "ConsumerAdmin1!")),
            "consumer-norole": ("consumer", _env("CONSUMER_NOROLE_PASSWORD", "ConsumerNoRole1!")),
        }
        self.default_user = {"provider": "provider-admin", "consumer": "consumer-admin"}

    def party(self, role):
        role = role.lower()
        if role == "provider":
            return self.provider
        if role == "consumer":
            return self.consumer
        raise ValueError("unknown party %r" % role)

    def other(self, role):
        return self.consumer if role.lower() == "provider" else self.provider

    def participant(self, name):
        """Map a participant name used in feature files to its id."""
        name = name.lower()
        if name == "provider":
            return self.provider.participant_id
        if name == "consumer":
            return self.consumer.participant_id
        if name == "rogue":
            return self.rogue_participant_id
        return name
