"""Tokens from Keycloak (section 5.2): password grant for users, client credentials for GAs."""
import time

import requests

from .config import USERS


class TokenProvider:
    def __init__(self, cfg):
        self.cfg = cfg
        self._cache = {}

    def _token_url(self, realm):
        return f"{self.cfg.keycloak_url}/realms/{realm}/protocol/openid-connect/token"

    def _cached(self, key, fetch):
        entry = self._cache.get(key)
        if entry and entry[1] > time.time():
            return entry[0]
        token, lifetime = fetch()
        self._cache[key] = (token, time.time() + max(30, lifetime - 60))
        return token

    def user_token(self, user):
        if self.cfg.auth_mode == "none":
            return None
        if user not in USERS:
            raise KeyError(f"unknown test user {user!r}")
        realm = USERS[user]["realm"]

        def fetch():
            r = requests.post(self._token_url(realm), data={
                "grant_type": "password", "client_id": self.cfg.test_client_id,
                "username": user, "password": self.cfg.user_password, "scope": "openid",
            }, timeout=self.cfg.http_timeout)
            if r.status_code != 200:
                raise AssertionError(f"token for {user} in {realm} failed: {r.status_code} {r.text[:300]}")
            body = r.json()
            return body["access_token"], int(body.get("expires_in", 300))
        return self._cached(("user", user), fetch)

    def client_token(self, client_id):
        if self.cfg.auth_mode == "none":
            return None
        realm = self.cfg.client_realms[client_id]
        secret = self.cfg.client_secrets[client_id]

        def fetch():
            r = requests.post(self._token_url(realm), data={
                "grant_type": "client_credentials", "client_id": client_id, "client_secret": secret,
            }, timeout=self.cfg.http_timeout)
            if r.status_code != 200:
                raise AssertionError(f"token for client {client_id} in {realm} failed: {r.status_code} {r.text[:300]}")
            body = r.json()
            return body["access_token"], int(body.get("expires_in", 300))
        return self._cached(("client", client_id), fetch)
