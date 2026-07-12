#!/bin/sh
# Container-side generator for the authentication-provider keypair stub.
#
# Runs as the one-shot `auth-stub-gen` compose service before peer-stubs starts,
# so `docker compose up` is self-contained (no host openssl/python step). It is
# the containerised twin of the old host script gen-auth-stubs.sh.
#
# The tier2 Feign clients bootstrap an mTLS-capable SimplClient before each call
# (fetch active keypair + credential chain + ephemeral proof). Over the stack's
# plain-HTTP wiring the material is never used in a real TLS handshake; it only
# has to PARSE. A throwaway self-signed RSA keypair is generated into a WireMock
# mapping in the (bind-mounted) mappings dir, and skipped if already present.
set -eu

MAPPING=/mappings/auth-provider-keypair.generated.json
if [ -f "$MAPPING" ]; then
  echo "Auth keypair stub already present, skipping (delete $MAPPING to regenerate)."
  exit 0
fi

apk add --no-cache openssl jq >/dev/null

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

openssl req -x509 -newkey rsa:2048 -nodes -days 3650 \
  -keyout "$TMP/key.pem" -out "$TMP/cert.pem" \
  -subj "/CN=sd-local-participant" >/dev/null 2>&1

key="$(cat "$TMP/key.pem")"
cert="$(cat "$TMP/cert.pem")"

jq -n --arg key "$key" --arg cert "$cert" '{
  mappings: [
    { name: "authentication-provider: active keypair (generated; tier2 mTLS client bootstrap)",
      request:  { method: "GET", urlPath: "/tier1/v2/keypairs/active" },
      response: { status: 200, headers: { "Content-Type": "application/json" }, jsonBody: { privateKey: $key } } },
    { name: "authentication-provider: active credential chain (generated; tier2 mTLS client bootstrap)",
      request:  { method: "GET", urlPath: "/tier1/v2/credentials/active" },
      response: { status: 200, headers: { "Content-Type": "application/json" }, jsonBody: { content: $cert } } },
    { name: "authentication-provider: ephemeral proof (static stub value)",
      request:  { method: "GET", urlPath: "/tier1/v2/ephemeralProof" },
      response: { status: 200, headers: { "Content-Type": "application/json" }, jsonBody: { proof: "stub-ephemeral-proof" } } }
  ]
}' > "$MAPPING"

echo "Wrote $MAPPING"
