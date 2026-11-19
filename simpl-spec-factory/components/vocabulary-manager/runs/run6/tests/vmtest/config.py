"""Configuration of the suite. Every value comes from an environment variable (decision E4).

Defaults follow the build profile of the run (build.yaml) and decision E1 for the users.
"""
import os


def _env(name, default):
    value = os.environ.get(name)
    return value if value not in (None, "") else default


API_URL = _env("API_URL", "http://localhost:8196/v1").rstrip("/")
TOKEN_URL = _env("TOKEN_URL", "http://localhost:8193/realms/simpl/protocol/openid-connect/token")
TOKEN_CLIENT = _env("TOKEN_CLIENT", "vocabulary-tests")
TOKEN_CLIENT_SECRET = _env("TOKEN_CLIENT_SECRET", "")
UI_URL = _env("UI_URL", "http://localhost:4433").rstrip("/")
STORE_DOWN_API_URL = _env("STORE_DOWN_API_URL", "http://localhost:8199/v1").rstrip("/")

# Users of decision E1. The Gherkin names the users by their E1 user name; these variables
# let a different realm map them to other accounts.
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
PASSWORD = _env("TEST_PASSWORD", "Password1!")

# contentBase of decision E2. The suite learns the real value from the first contentHref it
# receives; this default is used only before that.
CONTENT_BASE = _env("VOCABULARY_CONTENT_BASE_URL", "https://authority.example/vocabulary-manager").rstrip("/")

HTTP_TIMEOUT = float(_env("HTTP_TIMEOUT", "60"))
UPLOAD_LIMIT = 5 * 1024 * 1024  # decision D15: 5 242 880 bytes
