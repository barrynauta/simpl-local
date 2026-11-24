"""Configuration of the suite. Everything comes from environment variables
(build profile, decision E4); the defaults are the values of build.yaml."""
import os


def env(name, default):
    value = os.environ.get(name)
    return value if value not in (None, "") else default


API_URL = env("API_URL", "http://localhost:8196/v1").rstrip("/")
STORE_DOWN_API_URL = env("STORE_DOWN_API_URL", "http://localhost:8199/v1").rstrip("/")
TOKEN_URL = env("TOKEN_URL", "http://localhost:8193/realms/simpl/protocol/openid-connect/token")
TOKEN_URL_OTHER = env("TOKEN_URL_OTHER", "http://localhost:8193/realms/simpl-other/protocol/openid-connect/token")
TOKEN_CLIENT = env("TOKEN_CLIENT", "vocabulary-tests")
TOKEN_CLIENT_NOAUD = env("TOKEN_CLIENT_NOAUD", "vocabulary-tests-noaud")
TOKEN_CLIENT_SHORT = env("TOKEN_CLIENT_SHORT", "vocabulary-tests-short")

# Test users of decision E1.
USERS = {
    "ga-admin": env("ADMIN_USER", "ga-admin"),
    "ga-viewer": env("VIEWER_USER", "ga-viewer"),
    "ga-norole": env("NOROLE_USER", "ga-norole"),
}
USER_EMAILS = {
    "ga-admin": env("ADMIN_EMAIL", "ga-admin@example.com"),
    "ga-viewer": env("VIEWER_EMAIL", "ga-viewer@example.com"),
    "ga-norole": env("NOROLE_EMAIL", "ga-norole@example.com"),
}
PASSWORD = env("USER_PASSWORD", "Password1!")

# contentBase of E2. When not set, the suite derives it from the contentHref
# of a vocabulary it created (contentHref = <contentBase>/content/<Name>/...).
CONTENT_BASE = os.environ.get("CONTENT_BASE", "").rstrip("/")

HTTP_TIMEOUT = float(env("HTTP_TIMEOUT", "60"))
