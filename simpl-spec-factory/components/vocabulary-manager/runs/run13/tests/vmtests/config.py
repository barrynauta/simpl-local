"""Configuration of the suite. Everything comes from environment variables.

Defaults follow the build profile (input/build.yaml) and decisions E1, E2, E7.
"""
import os


def _env(name, default):
    value = os.environ.get(name)
    return value if value not in (None, "") else default


API_URL = _env("API_URL", "http://localhost:8196/v1").rstrip("/")
STORE_DOWN_API_URL = _env("STORE_DOWN_API_URL", "http://localhost:8199/v1").rstrip("/")
TOKEN_URL = _env("TOKEN_URL", "http://localhost:8193/realms/simpl/protocol/openid-connect/token")
TOKEN_CLIENT = _env("TOKEN_CLIENT", "vocabulary-tests")
TOKEN_CLIENT_NOAUD = _env("TOKEN_CLIENT_NOAUD", "vocabulary-tests-noaud")
TOKEN_URL_OTHER = _env(
    "TOKEN_URL_OTHER", "http://localhost:8193/realms/simpl-other/protocol/openid-connect/token"
)
TOKEN_CLIENT_OTHER = _env("TOKEN_CLIENT_OTHER", "vocabulary-tests")
UI_URL = _env("UI_URL", "http://localhost:4433").rstrip("/")

# Decision E2: contentBase, used for contentHref and internal references (D12).
CONTENT_BASE = _env("VOCABULARY_CONTENT_BASE_URL", "https://authority.example/vocabulary-manager").rstrip("/")

# Decision E1: the three test users.
USER_PASSWORD = _env("USER_PASSWORD", "Password1!")
USERS = {
    "ga-admin": {
        "username": _env("ADMIN_USER", "ga-admin"),
        "email": _env("ADMIN_EMAIL", "ga-admin@example.com"),
    },
    "ga-viewer": {
        "username": _env("VIEWER_USER", "ga-viewer"),
        "email": _env("VIEWER_EMAIL", "ga-viewer@example.com"),
    },
    "ga-norole": {
        "username": _env("NOROLE_USER", "ga-norole"),
        "email": _env("NOROLE_EMAIL", "ga-norole@example.com"),
    },
}

# Request timeout in seconds. Writes at the 5 MB limit may take up to 30 s (D71).
HTTP_TIMEOUT = float(_env("HTTP_TIMEOUT", "75"))
