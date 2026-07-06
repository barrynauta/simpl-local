#!/usr/bin/env bash
# Generates the authentication-provider keypair stubs WireMock serves.
#
# The tier2 Feign clients (catalogue-adapter, federated-catalogue) build an
# mTLS-capable SimplClient before every call: they fetch the participant's
# private key (GET /tier1/v2/keypairs/active), certificate chain
# (GET /tier1/v2/credentials/active) and ephemeral proof from the
# authentication-provider. The material only has to PARSE (BouncyCastle PEM →
# PKCS12 keystore); over the stack's plain-HTTP wiring it is never used in a
# TLS handshake. A throwaway self-signed RSA keypair is generated here and
# embedded into a WireMock mapping — gitignored, regenerated when absent.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MAPPING="$SCRIPT_DIR/peer-stubs/mappings/auth-provider-keypair.generated.json"

if [ -f "$MAPPING" ]; then
  echo "Auth keypair stub already present, skipping (delete $MAPPING to regenerate)."
  exit 0
fi

TMPDIR_CERTS="$(mktemp -d)"
trap 'rm -rf "$TMPDIR_CERTS"' EXIT

openssl req -x509 -newkey rsa:2048 -nodes -days 3650 \
  -keyout "$TMPDIR_CERTS/key.pem" -out "$TMPDIR_CERTS/cert.pem" \
  -subj "/CN=sd-local-participant" >/dev/null 2>&1

python3 - "$TMPDIR_CERTS" "$MAPPING" <<'PYEOF'
import json, sys, pathlib
certs_dir, mapping_path = sys.argv[1], sys.argv[2]
key = pathlib.Path(certs_dir, "key.pem").read_text()
cert = pathlib.Path(certs_dir, "cert.pem").read_text()
mappings = {"mappings": [
    {
        "name": "authentication-provider: active keypair (generated; tier2 mTLS client bootstrap)",
        "request": {"method": "GET", "urlPath": "/tier1/v2/keypairs/active"},
        "response": {"status": 200,
                     "headers": {"Content-Type": "application/json"},
                     "jsonBody": {"privateKey": key}},
    },
    {
        "name": "authentication-provider: active credential chain (generated; tier2 mTLS client bootstrap)",
        "request": {"method": "GET", "urlPath": "/tier1/v2/credentials/active"},
        "response": {"status": 200,
                     "headers": {"Content-Type": "application/json"},
                     "jsonBody": {"content": cert}},
    },
    {
        "name": "authentication-provider: ephemeral proof (static stub value)",
        "request": {"method": "GET", "urlPath": "/tier1/v2/ephemeralProof"},
        "response": {"status": 200,
                     "headers": {"Content-Type": "application/json"},
                     "jsonBody": {"proof": "stub-ephemeral-proof"}},
    },
]}
pathlib.Path(mapping_path).write_text(json.dumps(mappings, indent=2))
print(f"Wrote {mapping_path}")
PYEOF
