"""Suite configuration. Everything comes from environment variables (decision E4).

Defaults follow the build profile (build.yaml) of run 8, so the suite runs
without any variable set against that stack.
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
TOKEN_URL_OTHER = _env("TOKEN_URL_OTHER", "http://localhost:8193/realms/simpl-other/protocol/openid-connect/token")
TOKEN_CLIENT_OTHER = _env("TOKEN_CLIENT_OTHER", TOKEN_CLIENT)

# Test users of decision E1. The password is shared.
USERS = {
    "ga-admin": _env("ADMIN_USER", "ga-admin"),
    "ga-viewer": _env("VIEWER_USER", "ga-viewer"),
    "ga-norole": _env("NOROLE_USER", "ga-norole"),
}
USER_EMAILS = {
    "ga-admin": _env("ADMIN_EMAIL", "ga-admin@example.com"),
    "ga-viewer": _env("VIEWER_EMAIL", "ga-viewer@example.com"),
    "ga-norole": _env("NOROLE_EMAIL", "ga-norole@example.com"),
}
USER_PASSWORD = _env("USER_PASSWORD", "Password1!")

# contentBase of decision E2. When empty the suite learns it from the
# contentHref of a vocabulary it creates (see vmtest.client.content_base).
CONTENT_BASE_URL = _env("CONTENT_BASE_URL", "")

HTTP_TIMEOUT = float(_env("HTTP_TIMEOUT", "60"))
RUN_ID = _env("RUN_ID", "")

# UI of the build (step 60). Browser runs headless unless UI_HEADLESS=0.
UI_URL = _env("UI_URL", "http://localhost:4433").rstrip("/")
UI_HEADLESS = _env("UI_HEADLESS", "1") != "0"
UI_TIMEOUT_MS = int(_env("UI_TIMEOUT_MS", "10000"))
