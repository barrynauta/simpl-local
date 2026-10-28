"""Configuration from environment variables. See features/environment.py for the full list."""
import os


def _env(name, default):
    value = os.environ.get(name)
    return value if value not in (None, "") else default


def _bool(name, default):
    return _env(name, "true" if default else "false").lower() in ("1", "true", "yes", "on")


class Config:
    def __init__(self):
        self.auth_mode = _env("AUTH_MODE", "oidc").lower()
        if self.auth_mode not in ("oidc", "none"):
            raise ValueError("AUTH_MODE must be oidc or none")
        cbe = _env("CBE_A_URL", "http://localhost:18103").rstrip("/")
        self.services = {
            "cat-a": _env("CAT_A_URL", "http://localhost:18101").rstrip("/"),
            "qma-a": _env("QMA_A_URL", "http://localhost:18102/v1").rstrip("/"),
            "cbe-a-v1": _env("CBE_A_V1_URL", cbe + "/v1").rstrip("/"),
            "cbe-a-v2": _env("CBE_A_V2_URL", cbe + "/v2").rstrip("/"),
            "ui-a": _env("UI_A_URL", "http://localhost:18104").rstrip("/"),
            "cat-b": _env("CAT_B_URL", "http://localhost:18201").rstrip("/"),
            "qma-b": _env("QMA_B_URL", "http://localhost:18202/v1").rstrip("/"),
            "qma-c": _env("QMA_C_URL", "http://localhost:18302/v1").rstrip("/"),
            "schema-stub": _env("SCHEMA_STUB_URL", "http://localhost:18401").rstrip("/"),
            "quality-stub": _env("QUALITY_STUB_URL", "http://localhost:18402").rstrip("/"),
        }
        # Health is served at the service root, also for the /v1 based services (D3).
        self.health_roots = {
            "cat-a": self.services["cat-a"],
            "qma-a": _strip_version(self.services["qma-a"]),
            "cbe-a": cbe,
            "ui-a": self.services["ui-a"],
            "cat-b": self.services["cat-b"],
            "qma-b": _strip_version(self.services["qma-b"]),
            "qma-c": _strip_version(self.services["qma-c"]),
            "schema-stub": self.services["schema-stub"],
            "quality-stub": self.services["quality-stub"],
        }
        self.keycloak_url = _env("KEYCLOAK_URL", "http://localhost:18080").rstrip("/")
        self.test_client_id = _env("TEST_CLIENT_ID", "test-client")
        self.user_password = _env("USER_PASSWORD", "Passw0rd!")
        self.client_secrets = {
            "qma-a-federation": _env("QMA_A_FEDERATION_SECRET", "qma-a-federation-secret"),
            "qma-b-service": _env("QMA_B_SERVICE_SECRET", "qma-b-service-secret"),
            "qma-c-federation": _env("QMA_C_FEDERATION_SECRET", "qma-c-federation-secret"),
        }
        self.client_realms = {
            "qma-a-federation": "dataspace-a",
            "qma-b-service": "dataspace-b",
            "qma-c-federation": "dataspace-c",
        }
        self.http_timeout = float(_env("HTTP_TIMEOUT", "30"))
        self.none_mode_attribute_header = _bool("NONE_MODE_ATTRIBUTE_HEADER", True)
        self.skip_tags = [t.strip().lstrip("@") for t in _env("SKIP_TAGS", "").split(",") if t.strip()]
        self.keep_data = _bool("KEEP_TEST_DATA", False)
        self.verbose = _bool("TEST_VERBOSE", False)


def _strip_version(url):
    return url[:-3] if url.endswith("/v1") else url


# Section 5.3 of the digest: users, realms and identity attributes.
USERS = {
    "consumer-a": {"realm": "dataspace-a", "attributes": ["CONSUMER", "RESEARCHER"]},
    "provider-a": {"realm": "dataspace-a", "attributes": ["PROVIDER"]},
    "provider-a2": {"realm": "dataspace-a", "attributes": ["PROVIDER"]},
    "schema-viewer-a": {"realm": "dataspace-a", "attributes": []},
    "schema-admin-a": {"realm": "dataspace-a", "attributes": []},
    "nobody-a": {"realm": "dataspace-a", "attributes": ["CONSUMER"]},
    "consumer-b": {"realm": "dataspace-b", "attributes": ["CONSUMER"]},
    "provider-b": {"realm": "dataspace-b", "attributes": ["PROVIDER"]},
    "schema-admin-b": {"realm": "dataspace-b", "attributes": []},
}

# Where a provider publishes by default (its own data space).
HOME_CATALOGUE = {"dataspace-a": "cat-a", "dataspace-b": "cat-b"}
