"""High level helpers: CM, UIBE and stub calls, negotiations, VC variants.

Only the HTTP APIs of the digest are used: CM (OpenAPI + Appendix A), UIBE
(OpenAPI + Appendix B) and the stub (Appendix C). No implementation internals.
"""
import copy
import json
import time
from datetime import timedelta
from urllib.parse import quote

from .core import (body_json, new_uuid, now_utc, poll, rfc3339)


class World:
    def __init__(self, config, http, tokens):
        self.config = config
        self.http = http
        self.tokens = tokens

    # ------------------------------------------------------------ raw calls

    def cm(self, role, method, path, json_body=None, api_key="default",
           headers=None, params=None, data=None, files=None):
        party = self.config.party(role)
        hdrs = dict(headers or {})
        if api_key == "default":
            hdrs["X-Api-Key"] = party.cm_key
        elif api_key is not None:
            hdrs["X-Api-Key"] = api_key
        return self.http.request(method, party.cm_url + path, headers=hdrs,
                                 json_body=json_body, params=params, data=data,
                                 files=files)

    def uibe(self, role, method, path, json_body=None, user="default",
             api_key=None, headers=None, params=None, data=None):
        party = self.config.party(role)
        hdrs = dict(headers or {})
        if user == "default":
            user = self.config.default_user[party.role]
        if user is not None:
            hdrs.update(self.tokens.auth_header(user))
        if api_key == "default":
            hdrs["x-api-key"] = party.uibe_key
        elif api_key is not None:
            hdrs["x-api-key"] = api_key
        return self.http.request(method, party.uibe_url + path, headers=hdrs,
                                 json_body=json_body, params=params, data=data)

    def stub(self, method, path, json_body=None, params=None, headers=None):
        return self.http.request(method, self.config.stub_url + path,
                                 json_body=json_body, params=params,
                                 headers=headers)

    # ------------------------------------------------------------ stub control

    def register_asset(self, provider_id=None, **fields):
        asset_id = fields.pop("assetId", None) or "asset-" + new_uuid()
        body = {
            "assetId": asset_id,
            "resourceDescriptionId": "rd-" + asset_id,
            "resourceName": fields.pop("resourceName", "Resource " + asset_id[-8:]),
            "description": fields.pop("description", "Test resource for " + asset_id),
            "providerId": provider_id or self.config.provider.participant_id,
            "providerName": fields.pop("providerName", "Provider One"),
            "consentRequired": fields.pop("consentRequired", False),
            "policies": fields.pop("policies", [
                {"type": "ACCESS", "description": "Members of the data space"},
                {"type": "USAGE", "description": "Research use only"}]),
            "contractClauses": fields.pop("contractClauses", [
                {"documentName": "clauses.pdf",
                 "hash": "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08"}]),
        }
        body.update(fields)
        resp = self.stub("POST", "/_control/catalogue/assets", json_body=body)
        assert resp.status_code == 201, "asset registration failed: %s %s" % (
            resp.status_code, resp.text[:300])
        return body_json(resp)

    def start_negotiation(self, asset_id, **extra):
        body = {"providerId": self.config.provider.participant_id,
                "consumerId": self.config.consumer.participant_id,
                "assetId": asset_id,
                "contractNegotiationId": new_uuid(),
                "contractAgreementId": new_uuid()}
        body.update({k: v for k, v in extra.items() if v is not None})
        resp = self.stub("POST", "/_control/negotiations", json_body=body)
        assert resp.status_code == 201, "negotiation start failed: %s %s" % (
            resp.status_code, resp.text[:300])
        return body_json(resp)

    def negotiation(self, nid):
        resp = self.stub("GET", "/_control/negotiations/" + nid)
        assert resp.status_code == 200, "negotiation lookup failed: %s" % resp.status_code
        return body_json(resp)

    def wait_negotiation_state(self, nid, states, timeout=None):
        if isinstance(states, str):
            states = [states]
        terminal = ["TERMINATED", "FINALIZED"]

        def check(n):
            if n["state"] in states:
                return True
            if n["state"] in terminal and n["state"] not in states:
                raise _Stop(n)
            return False

        try:
            return poll(lambda: self.negotiation(nid), check, self.config,
                        "stub negotiation %s in %s" % (nid, states), timeout)
        except _Stop as stop:
            raise AssertionError("stub negotiation %s ended in %s (%s) instead of %s" % (
                nid, stop.value["state"], stop.value.get("terminationReason"), states))

    def advance(self, nid):
        resp = self.stub("POST", "/_control/negotiations/%s/advance" % nid)
        assert resp.status_code == 202, "advance failed: %s" % resp.status_code

    def set_fault(self, nid, service, mode):
        resp = self.stub("POST", "/_control/faults", json_body={
            "contractNegotiationId": nid, "service": service, "mode": mode})
        assert resp.status_code == 201, "fault registration failed: %s %s" % (
            resp.status_code, resp.text[:200])

    def clear_faults(self, nid):
        return self.stub("DELETE", "/_control/faults",
                         params={"contractNegotiationId": nid})

    def set_overrides(self, nid, **overrides):
        resp = self.stub("PUT", "/_control/negotiations/%s/overrides" % nid,
                         json_body=overrides)
        assert resp.status_code == 200, "override failed: %s %s" % (
            resp.status_code, resp.text[:200])

    def calls(self, nid):
        resp = self.stub("GET", "/_control/calls", params={"contractNegotiationId": nid})
        assert resp.status_code == 200, "calls lookup failed: %s" % resp.status_code
        return body_json(resp)

    def edc_calls(self, nid, participant_id, endpoint, value=None):
        """Inbound connector callbacks (C.1) recorded by the stub."""
        out = []
        for call in self.calls(nid):
            path = call.get("path", "")
            parts = [p for p in path.split("?")[0].split("/") if p]
            # /edc/{p}/{endpoint}/{nid}/{value}  or /edc/{p}/vcExchange/{nid}/request
            if len(parts) >= 4 and parts[0] == "edc" and parts[1] == participant_id \
                    and parts[2] == endpoint and parts[3] == nid:
                if value is None or (len(parts) > 4 and parts[4] == value):
                    out.append(call)
        return out

    def wallet(self, participant_id, nid):
        resp = self.stub("GET", "/wallet/v1/%s/credentials" % participant_id,
                         params={"contractNegotiationId": nid})
        assert resp.status_code == 200, "wallet read failed: %s" % resp.status_code
        return body_json(resp)

    def issue_vc(self, participant_id, subject, valid_from=None, valid_until=None):
        body = {"participantId": participant_id, "credentialSubject": subject}
        if valid_from:
            body["validFrom"] = valid_from
        if valid_until:
            body["validUntil"] = valid_until
        resp = self.stub("POST", "/vc-issuer/v1/credentials/issue", json_body=body)
        assert resp.status_code == 201, "stub issue failed: %s %s" % (
            resp.status_code, resp.text[:300])
        return body_json(resp)

    def directory(self, participant_id):
        resp = self.stub("GET", "/directory/v1/participants/" + participant_id)
        assert resp.status_code == 200, "directory read failed: %s" % resp.status_code
        return body_json(resp)

    # ------------------------------------------------------------ contracts

    def contract(self, role, nid):
        resp = self.cm(role, "GET", "/contracts/" + quote(nid))
        assert resp.status_code == 200, "GET /contracts/%s at %s CM: %s %s" % (
            nid, role, resp.status_code, resp.text[:300])
        return body_json(resp)

    def contract_or_none(self, role, nid):
        resp = self.cm(role, "GET", "/contracts/" + quote(nid))
        if resp.status_code == 404:
            return None
        assert resp.status_code == 200, "GET /contracts/%s at %s CM: %s %s" % (
            nid, role, resp.status_code, resp.text[:300])
        return body_json(resp)

    def wait_contract(self, role, nid, check, description, timeout=None):
        return poll(lambda: self.contract(role, nid), check, self.config,
                    "%s contract %s: %s" % (role, nid, description), timeout)

    def wait_status(self, role, nid, status, timeout=None):
        return self.wait_contract(role, nid, lambda c: c.get("status") == status,
                                  "status %s" % status, timeout)

    # ------------------------------------------------------------ flows

    def offer_decision(self, nid, operation, user="consumer-admin"):
        """The Consumer user's decision (D17, D23) through UIBE, or CM if no UIBE."""
        if self.config.skip_uibe:
            return self.cm("consumer", "PATCH", "/contractOffers/%s/%s" % (nid, operation))
        return self.uibe("consumer", "PATCH", "/contractOffers/%s/%s" % (nid, operation),
                         user=user)

    def lifecycle(self, role, nid, action, reason=None, user="default"):
        """A lifecycle action (D37 L table) through UIBE (B.3), or CM (A.4)."""
        body = {"action": action}
        if reason is not None:
            body["reason"] = reason
        if self.config.skip_uibe:
            if user == "default":
                user = self.config.default_user[role]
            body["actor"] = user
            return self.cm(role, "POST", "/contracts/%s/lifecycleActions" % nid,
                           json_body=body)
        return self.uibe(role, "POST", "/contracts/%s/lifecycleActions" % nid,
                         json_body=body, user=user)

    def drive_to_active(self, asset_id):
        neg = self.start_negotiation(asset_id)
        nid = neg["contractNegotiationId"]
        self.wait_negotiation_state(nid, "OFFERED")
        resp = self.offer_decision(nid, "accept")
        assert resp.status_code == 200, "accept failed: %s %s" % (
            resp.status_code, resp.text[:300])
        self.wait_negotiation_state(nid, "FINALIZED")
        self.wait_status("provider", nid, "ACTIVE")
        self.wait_status("consumer", nid, "ACTIVE")
        return neg

    # ------------------------------------------------------------ VC variants

    def vc_variant(self, kind, base_vc, signer_role):
        """Build a broken or valid VC per D56 without any crypto code.

        signer_role is the role of the party that signed base_vc
        ("consumer" or "provider"). base_vc is a genuine VC of that party.
        """
        signer = self.config.party(signer_role).participant_id
        other = self.config.other(signer_role).participant_id
        subject = copy.deepcopy(base_vc["credentialSubject"])
        vc = copy.deepcopy(base_vc)
        now = now_utc()
        if kind == "valid":
            return vc
        if kind == "invalid signature":
            pv = vc["proof"]["proofValue"]
            last = pv[-1]
            vc["proof"]["proofValue"] = pv[:-1] + ("2" if last != "2" else "3")
            return vc
        if kind == "tampered subject":
            vc["credentialSubject"]["resourceDescriptionId"] = \
                subject["resourceDescriptionId"] + "x"
            return vc
        if kind == "untrusted issuer":
            return self.issue_vc(self.config.rogue_participant_id, subject)
        if kind == "wrong trusted issuer":
            return self.issue_vc(other, subject)
        if kind == "expired":
            return self.issue_vc(signer, subject,
                                 rfc3339(now - timedelta(days=2)),
                                 rfc3339(now - timedelta(days=1)))
        if kind == "not yet valid":
            return self.issue_vc(signer, subject,
                                 rfc3339(now + timedelta(days=1)),
                                 rfc3339(now + timedelta(days=2)))
        if kind == "missing assetId":
            del vc["credentialSubject"]["assetId"]
            return vc
        if kind == "other assetId":
            subject["assetId"] = subject["assetId"] + "-other"
            return self.issue_vc(signer, subject)
        if kind == "other agreement id":
            subject["contractAgreementId"] = new_uuid()
            return self.issue_vc(signer, subject)
        if kind == "other usageContractHash":
            subject["usageContractHash"] = "0" * 64
            return self.issue_vc(signer, subject)
        if kind == "wrong signerRole":
            subject["signerRole"] = "PROVIDER" if signer_role == "consumer" else "CONSUMER"
            return self.issue_vc(signer, subject)
        raise ValueError("unknown VC variant %r" % kind)


class _Stop(Exception):
    def __init__(self, value):
        super().__init__()
        self.value = value


def json_dumps(value):
    return json.dumps(value, indent=1, default=str)


def sleep(seconds):
    time.sleep(seconds)
