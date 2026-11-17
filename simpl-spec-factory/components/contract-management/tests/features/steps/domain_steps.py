"""Domain steps: test data, negotiations, contracts, stub, wallet, uploads."""
import json
import threading
import time

from behave import step

import param_types  # noqa: F401
from cmtest.core import (body_json, days_from_now, get_path, has_path, new_uuid,
                         poll, render, sha256_hex)
from cmtest.checks import _store, check_problem, compare

ALL_STATUSES = ["REQUESTED", "ACCEPTED", "REJECTED", "SIGNED", "ACTIVE", "FAILED",
                "SUSPENDED", "REACTIVATION_REQUESTED", "REVOCATION_REQUESTED",
                "REVOKED", "TERMINATED"]


def W(context):
    return context.world


def cfg(context):
    return context.config_cm


def _track(context, nid):
    if nid not in context.nids:
        context.nids.append(nid)


def _set_ids(context, nid=None, aid=None, did=None, offer=None):
    context.vars["nid"] = nid or new_uuid()
    context.vars["aid"] = aid or new_uuid()
    context.vars["did"] = did or "def-" + new_uuid()[:8]
    context.vars["offerId"] = offer or "offer-" + new_uuid()[:8]
    _track(context, context.vars["nid"])


def _asset_vars(context, asset):
    context.vars["assetId"] = asset["assetId"]
    context.vars["rdId"] = asset.get("resourceDescriptionId", "rd-" + asset["assetId"])
    context.vars["resourceName"] = asset.get("resourceName")
    context.asset = asset


# ------------------------------------------------------------------ test data

@step('a fresh asset')
def fresh_asset(context):
    _asset_vars(context, W(context).register_asset())


@step('a fresh asset with')
def fresh_asset_with(context):
    fields = {}
    for row in context.table:
        fields[row["field"]] = _typed(render(row["value"], context.vars))
    provider = fields.pop("providerId", None)
    if provider is not None:
        provider = cfg(context).participant(provider)
    _asset_vars(context, W(context).register_asset(provider_id=provider, **fields))


def _typed(text):
    if text in ("true", "false"):
        return text == "true"
    if text.startswith("[") or text.startswith("{"):
        return json.loads(text)
    return text


@step('a fresh unregistered asset id')
def fresh_unregistered(context):
    context.vars["assetId"] = "asset-" + new_uuid()
    context.vars["rdId"] = "rd-" + context.vars["assetId"]


@step('fresh negotiation ids')
def fresh_ids(context):
    _set_ids(context)


@step('a second set of negotiation ids')
def second_ids(context):
    context.vars["firstNid"] = context.vars["nid"]
    context.vars["firstAid"] = context.vars["aid"]
    context.vars["firstDid"] = context.vars["did"]
    _set_ids(context)


@step('the variable {name:Q} is {value:Q}')
def set_var(context, name, value):
    context.vars[name] = render(value, context.vars)


@step('the variable {name:Q} is a text of {n:d} characters')
def set_long_var(context, name, n):
    context.vars[name] = "x" * n


# ------------------------------------------------------------------ direct CM setups

def search_body(context):
    v = context.vars
    return {"contractAgreementId": v["aid"], "contractDefinitionId": v["did"],
            "assetId": v["assetId"], "providerId": v["providerId"],
            "consumerId": v["consumerId"]}


def create_body(context):
    v = context.vars
    return {"contractNegotiationId": v["nid"], "assetId": v["assetId"],
            "providerId": v["providerId"], "consumerId": v["consumerId"],
            "contractOfferId": v["offerId"]}


def _requested_at_provider(context, consent=False):
    if context.vars.get("assetId") and not consent:
        pass  # reuse the asset of an earlier step
    elif cfg(context).skip_stub:
        fresh_unregistered(context)
    else:
        _asset_vars(context, W(context).register_asset(consentRequired=consent))
    _set_ids(context)
    resp = W(context).cm("provider", "POST", "/agreements/%s/search" % context.vars["nid"],
                         json_body=search_body(context))
    assert resp.status_code == 200, "existence check failed: %s %s" % (
        resp.status_code, resp.text[:300])
    context.setup_response = resp


@step('a contract requested at the provider')
def requested_at_provider(context):
    _requested_at_provider(context)


@step('a contract requested at the provider for an asset with consent required')
def requested_at_provider_consent(context):
    _requested_at_provider(context, consent=True)


@step('the provider contract is terminated by the connector')
def provider_terminated(context):
    v = context.vars
    resp = W(context).cm("provider", "PATCH", "/agreements/%s/definitions/%s/status" % (
        v["aid"], v["did"]), json_body={"status": "TERMINATED"})
    assert resp.status_code == 200, "terminate failed: %s %s" % (resp.status_code, resp.text[:300])


@step('an offer created at the consumer')
def offer_created(context):
    if not hasattr(context, "asset") or context.vars.get("assetId") is None:
        _asset_vars(context, W(context).register_asset())
    _set_ids(context)
    v = context.vars
    resp = W(context).cm("consumer", "POST", "/credentials/agreements/%s/definitions/%s/create" % (
        v["aid"], v["did"]), json_body=create_body(context))
    assert resp.status_code == 201, "create failed: %s %s" % (resp.status_code, resp.text[:300])
    context.setup_response = resp


@step('the connector asks the consumer CM to create the offer')
def connector_create(context):
    v = context.vars
    _store(context, W(context).cm("consumer", "POST",
                                  "/credentials/agreements/%s/definitions/%s/create" % (
                                      v["aid"], v["did"]), json_body=create_body(context)))


@step('the connector asks the {party:Party} CM to create the offer with {field:Q} set to {value:Q}')
def connector_create_field(context, party, field, value):
    v = context.vars
    body = create_body(context)
    body[field] = render(value, context.vars)
    _store(context, W(context).cm(party, "POST",
                                  "/credentials/agreements/%s/definitions/%s/create" % (
                                      v["aid"], v["did"]), json_body=body))


@step('the connector asks the consumer CM to create the offer without {field:Q}')
def connector_create_missing(context, field):
    v = context.vars
    body = create_body(context)
    body.pop(field)
    _store(context, W(context).cm("consumer", "POST",
                                  "/credentials/agreements/%s/definitions/%s/create" % (
                                      v["aid"], v["did"]), json_body=body))


@step('the connector asks the provider CM to check existence')
def connector_search(context):
    _store(context, W(context).cm("provider", "POST",
                                  "/agreements/%s/search" % context.vars["nid"],
                                  json_body=search_body(context)))


@step('the connector asks the {party:Party} CM to check existence without {field:Q}')
def connector_search_missing(context, party, field):
    body = search_body(context)
    body.pop(field)
    _store(context, W(context).cm(party, "POST", "/agreements/%s/search" % context.vars["nid"],
                                  json_body=body))


@step('the connector asks the {party:Party} CM to check existence with {field:Q} set to {value:Q}')
def connector_search_field(context, party, field, value):
    body = search_body(context)
    body[field] = render(value, context.vars)
    _store(context, W(context).cm(party, "POST", "/agreements/%s/search" % context.vars["nid"],
                                  json_body=body))


@step('the connector asks the {party:Party} CM to validate and sign')
def connector_issue(context, party):
    v = context.vars
    _store(context, W(context).cm(party, "POST",
                                  "/credentials/agreements/%s/definitions/%s/issue" % (
                                      v["aid"], v["did"]), json_body=create_body(context)))


@step('the connector asks the {party:Party} CM to validate and sign without a body')
def connector_issue_nobody(context, party):
    v = context.vars
    _store(context, W(context).cm(party, "POST",
                                  "/credentials/agreements/%s/definitions/%s/issue" % (
                                      v["aid"], v["did"])))


@step('the connector sets the {party:Party} agreement status to {status:Q}')
def connector_status(context, party, status):
    v = context.vars
    _store(context, W(context).cm(party, "PATCH", "/agreements/%s/definitions/%s/status" % (
        v["aid"], v["did"]), json_body={"status": status}))


@step('the connector reports the offer status {status:Q} to the consumer CM')
def connector_update(context, status):
    _store(context, W(context).cm("consumer", "PUT",
                                  "/contractOffers/%s/update" % context.vars["nid"],
                                  json_body={"status": status,
                                             "additionalInfo": "Provider validation or signing failed"}))


# ------------------------------------------------------------------ stub negotiations

def _start(context, stop_after=None, faults=None, **overrides):
    if context.vars.get("assetId") is None or not hasattr(context, "asset"):
        _asset_vars(context, W(context).register_asset())
    extra = {"stopAfter": stop_after}
    if faults:
        extra["faults"] = [{"service": f.split(":")[0], "mode": f.split(":")[1]} for f in faults]
    extra.update(overrides)
    neg = W(context).start_negotiation(context.vars["assetId"], **extra)
    _set_ids(context, neg["contractNegotiationId"], neg["contractAgreementId"],
             neg["contractDefinitionId"], neg["contractOfferId"])
    context.negotiation = neg
    return neg


@step('a negotiation started at the stub')
@step('a negotiation is started at the stub')
def start_plain(context):
    _start(context)


@step('a negotiation started at the stub that stops after {state:Q}')
def start_stop(context, state):
    _start(context, stop_after=state)


@step('a negotiation started at the stub with the fault {fault:Q}')
@step('a negotiation is started at the stub with the fault {fault:Q}')
def start_fault(context, fault):
    _start(context, faults=[fault])


@step('a negotiation started at the stub with the provider credential override "not found"')
def start_provider_notfound(context):
    _start(context, providerVcOverride={"notFound": True})


@step('the stub negotiation reaches state {state:Q}')
def reach_state(context, state):
    context.negotiation_state = W(context).wait_negotiation_state(context.vars["nid"], state)


@step('the stub negotiation ends in state {state:Q} with reason {reason:Q}')
def ends_state(context, state, reason):
    n = W(context).wait_negotiation_state(context.vars["nid"], state)
    assert n.get("terminationReason") == reason, "termination reason %r, expected %r" % (
        n.get("terminationReason"), reason)


@step('the stub negotiation passed the states {states:Q}')
def passed_states(context, states):
    n = W(context).negotiation(context.vars["nid"])
    seen = [s["state"] for s in n.get("steps", [])]
    expected = [s.strip() for s in states.split(",")]
    assert seen == expected, "states %s, expected %s" % (seen, expected)


@step('the stub negotiation is advanced')
def advance(context):
    W(context).advance(context.vars["nid"])


@step('an offer waiting for the consumer decision')
def offer_waiting(context):
    _start(context)
    W(context).wait_negotiation_state(context.vars["nid"], "OFFERED")


@step('an offer waiting for the consumer decision that stops after {state:Q}')
def offer_waiting_stop(context, state):
    _start(context, stop_after=state)
    W(context).wait_negotiation_state(context.vars["nid"], "OFFERED")


@step('an offer waiting for the consumer decision with the fault {fault:Q}')
def offer_waiting_fault(context, fault):
    _start(context, faults=[fault])
    W(context).wait_negotiation_state(context.vars["nid"], "OFFERED")


def _accept_ok(context):
    resp = W(context).offer_decision(context.vars["nid"], "accept")
    assert resp.status_code == 200, "accept failed: %s %s" % (resp.status_code, resp.text[:300])


@step('a negotiation where the consumer has signed')
def consumer_signed(context):
    _start(context, stop_after="CONSUMER_SIGNED")
    W(context).wait_negotiation_state(context.vars["nid"], "OFFERED")
    _accept_ok(context)
    W(context).wait_negotiation_state(context.vars["nid"], "CONSUMER_SIGNED")
    W(context).wait_status("consumer", context.vars["nid"], "SIGNED")


@step('a negotiation where the provider has signed')
def provider_signed(context):
    _start(context, stop_after="PROVIDER_SIGNED")
    W(context).wait_negotiation_state(context.vars["nid"], "OFFERED")
    _accept_ok(context)
    W(context).wait_negotiation_state(context.vars["nid"], "PROVIDER_SIGNED")
    W(context).wait_status("provider", context.vars["nid"], "SIGNED")


@step('a negotiation where the consumer has verified the provider')
def consumer_verified(context):
    _start(context, stop_after="CONSUMER_VERIFIED")
    W(context).wait_negotiation_state(context.vars["nid"], "OFFERED")
    _accept_ok(context)
    W(context).wait_negotiation_state(context.vars["nid"], "CONSUMER_VERIFIED")
    W(context).wait_status("consumer", context.vars["nid"], "ACTIVE")
    W(context).wait_status("provider", context.vars["nid"], "SIGNED")


@step('an active contract between both parties')
def active_contract(context):
    if context.vars.get("assetId") is None or not hasattr(context, "asset"):
        _asset_vars(context, W(context).register_asset())
    neg = W(context).drive_to_active(context.vars["assetId"])
    _set_ids(context, neg["contractNegotiationId"], neg["contractAgreementId"],
             neg["contractDefinitionId"], neg["contractOfferId"])
    context.negotiation = neg


def _act_ok(context, role, action, reason="Test reason"):
    resp = W(context).lifecycle(role, context.vars["nid"], action, reason)
    assert resp.status_code == 200, "%s %s failed: %s %s" % (role, action, resp.status_code,
                                                            resp.text[:300])


def _both(context, status):
    W(context).wait_status("provider", context.vars["nid"], status)
    W(context).wait_status("consumer", context.vars["nid"], status)


@step('an active contract between both parties that is now {status:Q}')
def contract_in_status(context, status):
    active_contract(context)
    if status == "ACTIVE":
        return
    if status in ("SUSPENDED", "REACTIVATION_REQUESTED"):
        _act_ok(context, "provider", "SUSPEND")
        _both(context, "SUSPENDED")
        if status == "REACTIVATION_REQUESTED":
            _act_ok(context, "consumer", "REQUEST_REACTIVATION")
            _both(context, "REACTIVATION_REQUESTED")
        return
    if status == "REVOCATION_REQUESTED":
        _act_ok(context, "provider", "REQUEST_REVOCATION")
        _both(context, "REVOCATION_REQUESTED")
        return
    if status == "REVOKED":
        _act_ok(context, "provider", "REQUEST_REVOCATION")
        _both(context, "REVOCATION_REQUESTED")
        _act_ok(context, "consumer", "ACCEPT_REVOCATION", None)
        _both(context, "REVOKED")
        return
    if status == "TERMINATED":
        _act_ok(context, "provider", "TERMINATE")
        _both(context, "TERMINATED")
        return
    raise ValueError("cannot prepare status %s" % status)


@step('an active contract between both parties with a revocation requested by the {party:Party}')
def revocation_by(context, party):
    active_contract(context)
    _act_ok(context, party, "REQUEST_REVOCATION")
    _both(context, "REVOCATION_REQUESTED")


# ------------------------------------------------------------------ user actions

@step('the consumer accepts the offer')
def consumer_accepts(context):
    _store(context, W(context).offer_decision(context.vars["nid"], "accept"))


@step('the consumer rejects the offer')
def consumer_rejects(context):
    _store(context, W(context).offer_decision(context.vars["nid"], "reject"))


@step('the consumer user {user:Q} sends the offer decision {op:Q}')
def consumer_user_decides(context, user, op):
    _store(context, W(context).uibe("consumer", "PATCH",
                                    "/contractOffers/%s/%s" % (context.vars["nid"], op),
                                    user=user))


@step('the consumer UIBE sends the offer decision {op:Q} to the {party:Party} CM')
def uibe_to_cm_decision(context, op, party):
    _store(context, W(context).cm(party, "PATCH",
                                  "/contractOffers/%s/%s" % (context.vars["nid"], op)))


@step('the {party:Party} user performs {action:Q} with reason {reason:Q}')
def user_action(context, party, action, reason):
    _store(context, W(context).lifecycle(party, context.vars["nid"], action,
                                         render(reason, context.vars)))


@step('the {party:Party} user performs {action:Q} without a reason')
def user_action_noreason(context, party, action):
    _store(context, W(context).lifecycle(party, context.vars["nid"], action, None))


@step('the {party:Party} user {user:Q} performs {action:Q} with reason {reason:Q}')
def named_user_action(context, party, user, action, reason):
    _store(context, W(context).lifecycle(party, context.vars["nid"], action,
                                         render(reason, context.vars), user=user))


@step('the {party:Party} user sends {action:Q} twice at the same time with reason {reason:Q}')
def concurrent_action(context, party, action, reason):
    results = []

    def run():
        results.append(W(context).lifecycle(party, context.vars["nid"], action, reason))

    threads = [threading.Thread(target=run) for _ in range(2)]
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    context.concurrent = results


@step('one response is 200 and the other is a problem with status 409 and type "invalidTransition"')
def concurrent_result(context):
    codes = sorted(r.status_code for r in context.concurrent)
    assert codes == [200, 409], "status codes %s, expected [200, 409]" % codes
    check_problem([r for r in context.concurrent if r.status_code == 409][0], 409,
                  "invalidTransition")


# ------------------------------------------------------------------ contract checks

def _matches(obj, table, context):
    for row in table:
        field = row["field"]
        if not has_path(obj, field):
            return False
        if not compare(get_path(obj, field), render(row["value"], context.vars)):
            return False
    return True


@step('the {party:Party} contract reaches status {status:Q}')
def reaches_status(context, party, status):
    context.contract = W(context).wait_status(party, context.vars["nid"], status)


@step('both contracts reach status {status:Q}')
def both_reach(context, status):
    _both(context, status)


@step('the {party:Party} contract has')
def contract_has(context, party):
    table = list(context.table)
    context.contract = W(context).wait_contract(
        party, context.vars["nid"], lambda c: _matches(c, table, context),
        "fields %s" % [(r["field"], r["value"]) for r in table])


@step('the {party:Party} contract field {field:Q} is not null')
def contract_field_not_null(context, party, field):
    c = W(context).wait_contract(party, context.vars["nid"],
                                 lambda c: has_path(c, field) and get_path(c, field) is not None,
                                 "%s not null" % field)
    context.contract = c


@step('the {party:Party} contract field {field:Q} is null')
def contract_field_null(context, party, field):
    c = W(context).contract(party, context.vars["nid"])
    assert has_path(c, field) and get_path(c, field) is None, "%s is %r" % (
        field, c.get(field))


@step('the {party:Party} contract stays in status {status:Q}')
def stays(context, party, status):
    time.sleep(cfg(context).quiet_period)
    c = W(context).contract(party, context.vars["nid"])
    assert c["status"] == status, "status %s, expected it to stay %s" % (c["status"], status)


def _history_matches(c, table, context):
    return [e for e in c.get("history", []) if _matches(e, table, context)]


@step('the {party:Party} contract history has an entry with')
def history_has(context, party):
    table = list(context.table)
    W(context).wait_contract(party, context.vars["nid"],
                             lambda c: bool(_history_matches(c, table, context)),
                             "history entry %s" % [(r["field"], r["value"]) for r in table])


@step('the {party:Party} contract history has no entry with')
def history_has_not(context, party):
    table = list(context.table)
    time.sleep(cfg(context).quiet_period)
    c = W(context).contract(party, context.vars["nid"])
    found = _history_matches(c, table, context)
    assert not found, "unexpected history entries: %s" % json.dumps(found)[:600]


@step('the {party:Party} contract history has {n:d} entries with')
def history_count(context, party, n):
    table = list(context.table)
    time.sleep(cfg(context).quiet_period)
    c = W(context).contract(party, context.vars["nid"])
    found = _history_matches(c, table, context)
    assert len(found) == n, "%d matching history entries, expected %d: %s" % (
        len(found), n, json.dumps(c.get("history"))[:800])


@step('every {party:Party} contract history entry has the fields of D61')
def history_fields(context, party):
    c = W(context).contract(party, context.vars["nid"])
    assert c.get("history"), "history is empty"
    for e in c["history"]:
        for f in ("at", "fromStatus", "toStatus", "entryType", "trigger", "actor",
                  "reasonCode", "reason", "correlationId"):
            assert f in e, "history entry lacks %s: %s" % (f, e)


@step('the {party:Party} contract history is ordered oldest first')
def history_order(context, party):
    from cmtest.core import parse_datetime
    c = W(context).contract(party, context.vars["nid"])
    times = [parse_datetime(e["at"]) for e in c["history"]]
    assert times == sorted(times), "history not oldest first: %s" % times


@step('the {party:Party} has no contract for the negotiation')
def no_contract(context, party):
    assert W(context).contract_or_none(party, context.vars["nid"]) is None, \
        "a %s contract exists for %s" % (party, context.vars["nid"])


@step('the {party:Party} has exactly one contract for the negotiation')
def one_contract(context, party):
    resp = W(context).cm(party, "GET", "/contracts",
                         params={"contractNegotiationId": context.vars["nid"]})
    assert resp.status_code == 200, "GET /contracts: %s" % resp.status_code
    items = body_json(resp)["items"]
    assert len(items) == 1, "%d contracts for the nid" % len(items)


@step('the {party:Party} contract allowed actions are {actions:Q}')
def allowed_actions(context, party, actions):
    expected = sorted(a.strip() for a in actions.split(",") if a.strip())

    def check(c):
        return sorted(c.get("allowedActions") or []) == expected
    try:
        W(context).wait_contract(party, context.vars["nid"], check,
                                 "allowedActions %s" % expected, timeout=5)
    except AssertionError:
        c = W(context).contract(party, context.vars["nid"])
        raise AssertionError("allowedActions %s, expected %s" % (
            c.get("allowedActions"), expected))


@step('the {party:Party} contract allowed actions contain {action:Q}')
def allowed_contains(context, party, action):
    W(context).wait_contract(party, context.vars["nid"],
                             lambda c: action in (c.get("allowedActions") or []),
                             "allowedActions containing %s" % action, timeout=5)


@step('the {party:Party} contract field {field:Q} is a date-time')
def contract_field_dt(context, party, field):
    from cmtest.core import parse_datetime
    c = W(context).wait_contract(party, context.vars["nid"],
                                 lambda c: c.get(field) is not None, "%s set" % field)
    parse_datetime(c[field])


@step('the {party:Party} contract field "usageContractHash" is the SHA-256 of its "humanReadable"')
def contract_hash(context, party):
    c = W(context).contract(party, context.vars["nid"])
    assert c["usageContractHash"] == sha256_hex(c["humanReadable"]), "hash mismatch"


# ------------------------------------------------------------------ stub faults, overrides

@step('the stub fault {fault:Q} is set for the negotiation')
def set_fault(context, fault):
    service, mode = fault.split(":")
    W(context).set_fault(context.vars["nid"], service, mode)


@step('the stub faults are cleared for the negotiation')
def clear_faults(context):
    resp = W(context).clear_faults(context.vars["nid"])
    assert resp.status_code == 204, "clear faults: %s" % resp.status_code


@step('the stub override {name:Q} is "not found"')
def override_notfound(context, name):
    W(context).set_overrides(context.vars["nid"], **{name: {"notFound": True}})


def _own_vc(context, role):
    resp = W(context).cm(role, "GET", "/verifications/%s/vc" % context.vars["nid"])
    assert resp.status_code == 200, "GET own VC at %s: %s %s" % (role, resp.status_code,
                                                                 resp.text[:200])
    return body_json(resp)


@step('the stub override {name:Q} is the {kind:Q} variant of the {party:Party} credential')
def override_variant(context, name, kind, party):
    vc = W(context).vc_variant(kind, _own_vc(context, party), party)
    W(context).set_overrides(context.vars["nid"], **{name: vc})


@step('the connector sends the {kind:Q} variant of the provider credential to the consumer CM')
def send_provider_variant(context, kind):
    vc = W(context).vc_variant(kind, _own_vc(context, "provider"), "provider")
    context.sent_vc = vc
    _store(context, W(context).cm("consumer", "PUT", "/verifications/%s" % context.vars["nid"],
                                  json_body=vc))


@step('the connector sends the same provider credential to the consumer CM again')
def send_again(context):
    _store(context, W(context).cm("consumer", "PUT", "/verifications/%s" % context.vars["nid"],
                                  json_body=context.sent_vc))


@step('the connector sends the {kind:Q} variant of the provider credential to the {party:Party} CM')
def send_variant_to(context, kind, party):
    vc = W(context).vc_variant(kind, _own_vc(context, "provider"), "provider")
    _store(context, W(context).cm(party, "PUT", "/verifications/%s" % context.vars["nid"],
                                  json_body=vc))


@step('the connector sends a credential to the {party:Party} CM for the negotiation')
def send_any_vc(context, party):
    subject = {"id": "urn:simpl:contract:" + context.vars["nid"],
               "contractNegotiationId": context.vars["nid"],
               "contractAgreementId": context.vars["aid"],
               "contractDefinitionId": context.vars["did"],
               "assetId": context.vars["assetId"], "resourceDescriptionId": context.vars["rdId"],
               "providerId": context.vars["providerId"], "consumerId": context.vars["consumerId"],
               "usageContractHash": "0" * 64, "signerRole": "PROVIDER",
               "signedAt": days_from_now(0)}
    if cfg(context).skip_stub:
        vc = {"@context": ["https://www.w3.org/ns/credentials/v2"],
              "type": ["VerifiableCredential", "UsageContractCredential"],
              "issuer": "did:key:z6MktULudTtAsAhRegYPiZ6631RV3viv12qd4GQF8z1xB22S",
              "credentialSubject": subject}
    else:
        vc = W(context).issue_vc(cfg(context).provider.participant_id, subject)
    _store(context, W(context).cm(party, "PUT", "/verifications/%s" % context.vars["nid"],
                                  json_body=vc))


# ------------------------------------------------------------------ callbacks

def _pid(context, party):
    return cfg(context).party(party).participant_id


@step('the stub received the callback {endpoint:Q} with {value:Q} from the {party:Party}')
def got_callback(context, endpoint, value, party):
    poll(lambda: W(context).edc_calls(context.vars["nid"], _pid(context, party), endpoint, value),
         lambda calls: len(calls) > 0, cfg(context),
         "callback %s/%s from %s" % (endpoint, value, party))


@step('the stub received no callback {endpoint:Q} from the {party:Party}')
def no_callback(context, endpoint, party):
    time.sleep(cfg(context).quiet_period)
    calls = W(context).edc_calls(context.vars["nid"], _pid(context, party), endpoint)
    assert not calls, "unexpected %s callbacks: %s" % (endpoint, calls)


@step('the stub received no callback {endpoint:Q} with {value:Q} from the {party:Party}')
def no_callback_value(context, endpoint, value, party):
    time.sleep(cfg(context).quiet_period)
    calls = W(context).edc_calls(context.vars["nid"], _pid(context, party), endpoint, value)
    assert not calls, "unexpected %s/%s callbacks: %s" % (endpoint, value, calls)


@step('the stub received no connector callback for the negotiation')
def no_edc_at_all(context):
    time.sleep(cfg(context).quiet_period)
    calls = [c for c in W(context).calls(context.vars["nid"])
             if c.get("path", "").startswith("/edc/")]
    assert not calls, "unexpected connector callbacks: %s" % calls


@step('I remember the number of {endpoint:Q} callbacks from the {party:Party}')
def remember_callbacks(context, endpoint, party):
    context.saved[(endpoint, party)] = len(
        W(context).edc_calls(context.vars["nid"], _pid(context, party), endpoint))


@step('the number of {endpoint:Q} callbacks from the {party:Party} is unchanged')
def callbacks_unchanged(context, endpoint, party):
    time.sleep(cfg(context).quiet_period)
    now = len(W(context).edc_calls(context.vars["nid"], _pid(context, party), endpoint))
    assert now == context.saved[(endpoint, party)], "%s callbacks went from %d to %d" % (
        endpoint, context.saved[(endpoint, party)], now)


@step('the stub recorded a call {method:Q} to a path starting with {prefix:Q}')
def stub_call_prefix(context, method, prefix):
    expected = render(prefix, context.vars)
    poll(lambda: [c for c in W(context).calls(context.vars["nid"])
                  if c.get("method") == method and c.get("path", "").startswith(expected)],
         lambda calls: len(calls) > 0, cfg(context),
         "stub call %s %s*" % (method, expected))


# ------------------------------------------------------------------ wallet

def _wallet(context, party):
    return W(context).wallet(_pid(context, party), context.vars["nid"])


@step('the {party:Party} wallet holds {n:d} credentials for the negotiation')
def wallet_count(context, party, n):
    poll(lambda: _wallet(context, party), lambda w: len(w) == n, cfg(context),
         "%s wallet with %d credentials" % (party, n))


@step('the {party:Party} wallet holds a credential signed as {role:Q}')
def wallet_has_role(context, party, role):
    poll(lambda: _wallet(context, party),
         lambda w: any(vc.get("credentialSubject", {}).get("signerRole") == role for vc in w),
         cfg(context), "%s wallet holding a %s credential" % (party, role))


@step('the {party:Party} wallet holds no credential signed as {role:Q}')
def wallet_lacks_role(context, party, role):
    time.sleep(cfg(context).quiet_period)
    w = _wallet(context, party)
    assert not any(vc.get("credentialSubject", {}).get("signerRole") == role for vc in w), \
        "%s wallet holds a %s credential" % (party, role)


@step('the response is the {role:Q} credential held in the {party:Party} wallet')
def response_is_wallet_vc(context, role, party):
    vc = body_json(context.response)
    assert vc.get("credentialSubject", {}).get("signerRole") == role, \
        "signerRole %r" % vc.get("credentialSubject", {}).get("signerRole")
    w = _wallet(context, party)
    assert any(x.get("id") == vc.get("id") for x in w), "VC %s not in the wallet" % vc.get("id")


@step('the response is a credential of D53 and D55 for the negotiation')
def response_vc_shape(context):
    vc = body_json(context.response)
    assert vc.get("@context") == ["https://www.w3.org/ns/credentials/v2"], vc.get("@context")
    assert vc.get("type") == ["VerifiableCredential", "UsageContractCredential"], vc.get("type")
    assert str(vc.get("id", "")).startswith("urn:uuid:"), vc.get("id")
    assert vc.get("validFrom") and vc.get("validUntil")
    proof = vc.get("proof", {})
    assert proof.get("type") == "DataIntegrityProof" and proof.get("cryptosuite") == "eddsa-jcs-2022", proof
    assert str(proof.get("proofValue", "")).startswith("z"), proof
    subject = vc.get("credentialSubject", {})
    for f in ("id", "contractNegotiationId", "contractAgreementId", "contractDefinitionId",
              "assetId", "resourceDescriptionId", "providerId", "consumerId",
              "usageContractHash", "signerRole", "signedAt"):
        assert f in subject, "credentialSubject lacks %s" % f
    assert subject["id"] == "urn:simpl:contract:" + context.vars["nid"], subject["id"]
    assert subject["contractNegotiationId"] == context.vars["nid"]
    assert subject["contractAgreementId"] == context.vars["aid"]


@step('the credential validity is {days:d} days')
def vc_validity(context, days):
    from cmtest.core import parse_datetime
    vc = body_json(context.response)
    delta = parse_datetime(vc["validUntil"]) - parse_datetime(vc["validFrom"])
    assert abs(delta.total_seconds() - days * 86400) < 120, "validity %s" % delta


# ------------------------------------------------------------------ uploads

@step('I upload the file {name:Q} with media type {mt:Q} and content {content:Q} to the {party:Party} CM')
def upload(context, name, mt, content, party):
    data = render(content, context.vars).encode("utf-8")
    context.uploaded = data
    _store(context, W(context).cm(party, "POST", "/contractClauseDocuments",
                                  files={"file": (name, data, mt)}))


@step('I upload the fixture {fixture:Q} with media type {mt:Q} to the {party:Party} CM')
def upload_fixture(context, fixture, mt, party):
    import os
    path = os.path.join(os.path.dirname(__file__), "..", "..", "fixtures", "documents", fixture)
    with open(path, "rb") as fh:
        data = fh.read()
    context.uploaded = data
    _store(context, W(context).cm(party, "POST", "/contractClauseDocuments",
                                  files={"file": (fixture, data, mt)}))


@step('I upload a file {name:Q} of the maximum size plus {extra:d} bytes with media type {mt:Q} to the provider CM')
def upload_big(context, name, extra, mt):
    data = b"a" * (cfg(context).clause_max_bytes + extra)
    context.uploaded = data
    _store(context, W(context).cm("provider", "POST", "/contractClauseDocuments",
                                  files={"file": (name, data, mt)}))


@step('I send a multipart upload without a file part to the provider CM')
def upload_nofile(context):
    _store(context, W(context).cm("provider", "POST", "/contractClauseDocuments",
                                  files={"other": ("x.txt", b"x", "text/plain")}))


@step('I upload the file {name:Q} with media type {mt:Q} to the provider CM without an API key')
def upload_nokey(context, name, mt):
    _store(context, W(context).cm("provider", "POST", "/contractClauseDocuments",
                                  files={"file": (name, b"clause", mt)}, api_key=None))


@step('the response field "hash" is the SHA-256 of the uploaded content')
def upload_hash(context):
    assert body_json(context.response)["hash"] == sha256_hex(context.uploaded)


@step('the response field "size" is the size of the uploaded content')
def upload_size(context):
    assert body_json(context.response)["size"] == len(context.uploaded)


@step('the response does not contain the uploaded content')
def upload_no_content(context):
    text = context.uploaded.decode("utf-8", "replace")
    assert text not in context.response.text, "the response embeds the document content"


# ------------------------------------------------------------------ audit

def _counts(context):
    resp = W(context).cm("provider", "GET", "/audit/contractStatusCounts")
    assert resp.status_code == 200, "audit counts: %s" % resp.status_code
    return body_json(resp)


@step('I remember the provider contract status counts')
def remember_counts(context):
    context.counts_before = _counts(context)


@step('the provider count of {status:Q} has grown by at least {n:d}')
def counts_grown(context, status, n):
    after = _counts(context)
    before = context.counts_before
    assert after["counts"][status] >= before["counts"][status] + n, "%s %d -> %d" % (
        status, before["counts"][status], after["counts"][status])
    assert after["total"] >= before["total"] + n, "total %d -> %d" % (
        before["total"], after["total"])


@step('the response counts every contract status')
def counts_every(context):
    counts = body_json(context.response)["counts"]
    assert sorted(counts.keys()) == sorted(ALL_STATUSES), "count keys %s" % sorted(counts)
    assert all(isinstance(v, int) and v >= 0 for v in counts.values()), counts


@step('the response field "total" equals the sum of the counts')
def counts_total(context):
    data = body_json(context.response)
    assert data["total"] == sum(data["counts"].values()), "total %d != sum %d" % (
        data["total"], sum(data["counts"].values()))


# ------------------------------------------------------------------ expiry and UIBE offers

@step('the consumer UIBE runs the offer expiry as of the timeout plus {minutes:d} minute for the negotiation')
def expiry_run_old(context, minutes):
    asof = days_from_now(cfg(context).offer_expiry_timeout_days, minutes)
    _store(context, W(context).uibe("consumer", "POST", "/admin/offerExpiry/run",
                                    json_body={"asOf": asof,
                                               "contractNegotiationIds": [context.vars["nid"]]},
                                    user=None, api_key="default"))


@step('the consumer UIBE runs the offer expiry as of now for the negotiation')
def expiry_run_now(context):
    _store(context, W(context).uibe("consumer", "POST", "/admin/offerExpiry/run",
                                    json_body={"asOf": days_from_now(0),
                                               "contractNegotiationIds": [context.vars["nid"]]},
                                    user=None, api_key="default"))


@step('the offer expiry is run without an API key')
def expiry_nokey(context):
    _store(context, W(context).uibe("consumer", "POST", "/admin/offerExpiry/run",
                                    json_body={"contractNegotiationIds": [new_uuid()]},
                                    user=None))


@step('the {party:Party} UIBE offer has status {status:Q}')
def uibe_offer_status(context, party, status):
    def fetch():
        resp = W(context).uibe(party, "GET", "/contractOffers/" + context.vars["nid"])
        assert resp.status_code == 200, "GET offer: %s %s" % (resp.status_code, resp.text[:200])
        return body_json(resp)
    poll(fetch, lambda o: o.get("status") == status, cfg(context),
         "%s UIBE offer status %s" % (party, status))


@step('the {party:Party} UIBE offer still has status {status:Q}')
def uibe_offer_still(context, party, status):
    time.sleep(cfg(context).quiet_period)
    resp = W(context).uibe(party, "GET", "/contractOffers/" + context.vars["nid"])
    assert resp.status_code == 200, "GET offer: %s" % resp.status_code
    assert body_json(resp)["status"] == status, "offer status %s" % body_json(resp)["status"]


@step('the consumer CM forwards a new offer presentation {html:Q}')
def cm_forward(context, html):
    html = render(html, context.vars)
    context.vars["newHtml"] = html
    context.vars["newHash"] = sha256_hex(html)
    _store(context, W(context).cm("consumer", "POST", "/contractOffers/" + context.vars["nid"],
                                  json_body={"humanReadable": html, "hash": sha256_hex(html)}))
