"""Resource description (RD) builder for the D80 test data rules.

Every RD gets credentialSubject.@id = did:web:test.simpl.local:<Type>:<uuid4>
and the scenario run token in simpl:name and simpl:description.
"""
import copy
import json
import os
import uuid

from . import signing

FIXTURES = os.path.join(os.path.dirname(__file__), "..", "..", "fixtures")
RESOURCE_TYPES = ("DataOffering", "ApplicationOffering", "InfrastructureOffering")
SEARCH_ACTION = "http://simpl.eu/odrl/actions/search"


def access_policy(attributes):
    """ODRL access policy string (D30) with one search permission per attribute."""
    return json.dumps({
        "@context": "http://www.w3.org/ns/odrl.jsonld",
        "@type": "Set",
        "uid": str(uuid.uuid4()),
        "permission": [
            {"action": [SEARCH_ACTION], "assignee": {"uid": a}, "constraint": []}
            for a in attributes
        ],
    }, separators=(",", ":"))


def _replace(value, mapping):
    if isinstance(value, str):
        for k, v in mapping.items():
            value = value.replace(k, v)
        return value
    if isinstance(value, list):
        return [_replace(v, mapping) for v in value]
    if isinstance(value, dict):
        return {k: _replace(v, mapping) for k, v in value.items()}
    return value


def load_template(resource_type):
    if resource_type not in RESOURCE_TYPES:
        raise ValueError(f"unknown resource type {resource_type!r}")
    with open(os.path.join(FIXTURES, "rd", f"{resource_type}.json"), encoding="utf-8") as fh:
        return json.load(fh)


def load_broken(name):
    path = os.path.join(FIXTURES, "rd", "broken", f"{name}.json")
    with open(path, encoding="utf-8") as fh:
        return json.load(fh)


def split_path(path):
    return [p for p in path.split("/") if p != ""]


def set_path(doc, path, value):
    parts = split_path(path)
    node = doc
    for p in parts[:-1]:
        if isinstance(node, list):
            node = node[int(p)]
        else:
            node = node.setdefault(p, {})
    last = parts[-1]
    if isinstance(node, list):
        node[int(last)] = value
    else:
        node[last] = value


def remove_path(doc, path):
    parts = split_path(path)
    node = doc
    for p in parts[:-1]:
        node = node[int(p)] if isinstance(node, list) else node[p]
    if isinstance(node, list):
        del node[int(parts[-1])]
    else:
        node.pop(parts[-1], None)


def get_path(doc, path):
    node = doc
    for p in split_path(path):
        node = node[int(p)] if isinstance(node, list) else node[p]
    return node


class ResourceDescription:
    """One RD of a scenario: its unsigned VC plus how to sign and send it."""

    def __init__(self, alias, resource_type, provider, token):
        self.alias = alias
        self.resource_type = resource_type
        self.provider = provider            # participant id and user name of the provider
        self.token = token
        self.id = f"did:web:test.simpl.local:{resource_type}:{uuid.uuid4()}"
        self.signer = provider if provider in signing.KEYS else "unregistered"
        self.issuer_key = None              # default: the signer
        self.verification_key = "signer"    # "signer" or "issuer"
        self.header = None
        self.jws = None
        self.unsigned = False
        self.tamper = False
        self.raw_body = None
        self.sd_hash = None                 # set after publication
        self.published_by = None
        self.published_on = None
        self.deleted = False
        self.last_body = None
        mapping = {"__ID__": self.id, "__TOKEN__": token, "__ALIAS__": alias,
                   "__PROVIDER__": provider, "__ISSUER__": "__ISSUER__"}
        self.vc = _replace(load_template(resource_type), mapping)
        self.set_policy(["CONSUMER"])

    # ---- content changes -------------------------------------------------
    def set_policy(self, attributes):
        self.vc["credentialSubject"]["simpl:servicePolicy"]["simpl:access-policy"] = access_policy(attributes)

    def set(self, path, value):
        set_path(self.vc, path, value)

    def remove(self, path):
        remove_path(self.vc, path)

    def apply_broken(self, name):
        spec = load_broken(name)
        mapping = {"__TOKEN__": self.token, "__ID__": self.id, "__ALIAS__": self.alias}
        for change in spec.get("changes", []):
            if change.get("remove"):
                self.remove(change["path"])
            else:
                self.set(change["path"], _replace(change["value"], mapping))
        if spec.get("unsigned"):
            self.unsigned = True
        if spec.get("tamperAfterSigning"):
            self.tamper = True
        if "signWith" in spec:
            self.signer = spec["signWith"]
        if "issuerKey" in spec:
            self.issuer_key = spec["issuerKey"]
        if "verificationKey" in spec:
            self.verification_key = spec["verificationKey"]
            if spec["verificationKey"] == "issuer" and self.issuer_key is None:
                self.issuer_key = self.provider if self.provider in signing.KEYS else "unregistered"
        if "header" in spec:
            self.header = spec["header"]
        if "jws" in spec:
            self.jws = spec["jws"]
        if "rawBody" in spec:
            self.raw_body = spec["rawBody"]
        return spec

    def make_version_of(self, previous, number):
        """Copy previous content into a new RD id with simpl:version and simpl:previousVersion (D13)."""
        self.vc = copy.deepcopy(previous.vc)
        self.vc["credentialSubject"]["@id"] = self.id
        self.vc["credentialSubject"]["simpl:version"] = number
        self.vc["credentialSubject"]["simpl:previousVersion"] = {"@id": previous.id}
        gsp = self.vc["credentialSubject"]["simpl:generalServiceProperties"]
        gsp["simpl:description"] = f"{self.token} description of {self.alias} version {number}"

    # ---- signing ---------------------------------------------------------
    def body(self):
        """The final JSON document to send, signed per the directives."""
        if self.raw_body is not None:
            return None
        vc = copy.deepcopy(self.vc)
        issuer_key = signing.key(self.issuer_key or self.signer)
        if "issuer" in vc:
            vc["issuer"] = issuer_key.did
        if self.unsigned:
            vc.pop("proof", None)
            self.last_body = vc
            return vc
        signer = signing.key(self.signer)
        vm = issuer_key.did if self.verification_key == "issuer" else signer.did
        signed = signing.sign_vc(vc, signer, header=self.header, verification_did=vm)
        if self.jws is not None:
            signed["proof"]["jws"] = self.jws
        if self.tamper:
            gsp = signed["credentialSubject"]["simpl:generalServiceProperties"]
            gsp["simpl:description"] = gsp.get("simpl:description", "") + " changed after signing"
        self.last_body = signed
        return signed

    def computed_hash(self):
        if self.last_body is None:
            return None
        return signing.sd_hash(self.last_body)
