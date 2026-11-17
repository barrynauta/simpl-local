"""Signature profile of digest D22 and the fixed keys of D23.

proof.type JsonWebSignature2020, proofPurpose assertionMethod,
verificationMethod <issuer DID>#0, detached JWS with the header
{"alg":"EdDSA","b64":false,"crit":["b64"]}. Signing input is
ASCII(base64url(header)) + "." + UTF-8(JCS(VC without proof)).
"""
import base64
import copy
import hashlib
import json
import os
from datetime import datetime, timezone

from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey

from . import jcs

FIXTURES = os.path.join(os.path.dirname(__file__), "..", "..", "fixtures")
DEFAULT_HEADER = {"alg": "EdDSA", "b64": False, "crit": ["b64"]}


def b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def _load_keys():
    with open(os.path.join(FIXTURES, "keys", "keys.json"), encoding="utf-8") as fh:
        data = json.load(fh)
    data.pop("_comment", None)
    return data


KEYS = _load_keys()


class TestKey:
    def __init__(self, name):
        if name not in KEYS:
            raise KeyError(f"unknown test key {name!r}; known: {sorted(KEYS)}")
        self.name = name
        entry = KEYS[name]
        self.private = Ed25519PrivateKey.from_private_bytes(bytes.fromhex(entry["seed"]))
        raw = self.private.public_key().public_bytes(
            serialization.Encoding.Raw, serialization.PublicFormat.Raw)
        self.x = b64url(raw)
        if self.x != entry["x"]:
            raise AssertionError(f"fixture key {name} does not match its seed")

    @property
    def jwk(self):
        return {"crv": "Ed25519", "kty": "OKP", "x": self.x}

    @property
    def did(self):
        compact = json.dumps(self.jwk, separators=(",", ":"))
        return "did:jwk:" + b64url(compact.encode("utf-8"))

    def sign(self, data: bytes) -> bytes:
        return self.private.sign(data)


def key(name) -> TestKey:
    return TestKey(name)


def now_iso():
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def sign_vc(vc, signer: TestKey, header=None, verification_did=None):
    """Return a deep copy of vc with a D22 proof made by signer."""
    header = header or DEFAULT_HEADER
    unsigned = copy.deepcopy(vc)
    unsigned.pop("proof", None)
    header_b64 = b64url(json.dumps(header, separators=(",", ":")).encode("utf-8"))
    signing_input = header_b64.encode("ascii") + b"." + jcs.canonical_bytes(unsigned)
    signature = signer.sign(signing_input)
    signed = copy.deepcopy(unsigned)
    signed["proof"] = {
        "type": "JsonWebSignature2020",
        "created": now_iso(),
        "proofPurpose": "assertionMethod",
        "verificationMethod": (verification_did or signer.did) + "#0",
        "jws": header_b64 + ".." + b64url(signature),
    }
    return signed


def sd_hash(vc) -> str:
    """D11: lowercase hex SHA-256 of the JCS serialisation of the full VC (with proof)."""
    return hashlib.sha256(jcs.canonical_bytes(vc)).hexdigest()
