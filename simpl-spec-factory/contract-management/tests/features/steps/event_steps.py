"""Kafka event steps (Appendix D, D46 to D51)."""
import json

from behave import step

import param_types  # noqa: F401
from cmtest.checks import compare
from cmtest.core import get_path, has_path, render
from cmtest.events import TopicWatcher, message_nid


def _watcher(context, topic):
    if topic not in context.watchers:
        raise AssertionError("not listening to %s; add 'Given I listen to the Kafka topic'" % topic)
    return context.watchers[topic]


@step('I listen to the Kafka topic {topic:Q}')
def listen(context, topic):
    if topic not in context.watchers:
        context.watchers[topic] = TopicWatcher(context.config_cm, topic)


def _for_nid(msg, nid):
    if message_nid(msg) == nid:
        return True
    return nid in json.dumps(msg.get("value"), default=str)


def _field(msg, field):
    if field == "key":
        return True, msg.get("key")
    if field.startswith("header."):
        name = field[len("header."):]
        return name in msg.get("headers", {}), msg.get("headers", {}).get(name)
    value = msg.get("value")
    if not has_path(value, field):
        return False, None
    return True, get_path(value, field)


def _match(msg, rows, context):
    for row in rows:
        present, actual = _field(msg, row["field"])
        expected = render(row["value"], context.vars)
        if expected == "<present>":
            if not present or actual is None:
                return False
            continue
        if expected.startswith("<contains>"):
            if not present or expected[len("<contains>"):].strip() not in str(actual):
                return False
            continue
        if not present or not compare(actual, expected):
            return False
    return True


@step('a message for the negotiation is published on {topic:Q} with')
def published(context, topic):
    nid = context.vars["nid"]
    rows = list(context.table)
    found = _watcher(context, topic).wait_for(
        lambda m: _for_nid(m, nid) and _match(m, rows, context),
        "nid %s and %s" % (nid, [(r["field"], r["value"]) for r in rows]))
    context.last_message = found[0]


@step('no message for the negotiation is published on {topic:Q} with')
def not_published(context, topic):
    nid = context.vars["nid"]
    rows = list(context.table)
    msgs = _watcher(context, topic).collect(context.config_cm.quiet_period)
    bad = [m for m in msgs if _for_nid(m, nid) and _match(m, rows, context)]
    assert not bad, "unexpected messages: %s" % json.dumps([m["value"] for m in bad])[:800]


@step('no message for the negotiation is published on {topic:Q}')
def nothing_published(context, topic):
    nid = context.vars["nid"]
    msgs = _watcher(context, topic).collect(context.config_cm.quiet_period)
    bad = [m for m in msgs if _for_nid(m, nid)]
    assert not bad, "unexpected messages: %s" % json.dumps([m["value"] for m in bad])[:800]


@step('exactly {n:d} messages for the negotiation are published on {topic:Q} with')
def published_count(context, n, topic):
    nid = context.vars["nid"]
    rows = list(context.table)
    _watcher(context, topic).wait_for(lambda m: _for_nid(m, nid) and _match(m, rows, context),
                                     "nid %s" % nid)
    msgs = _watcher(context, topic).collect(context.config_cm.quiet_period)
    found = [m for m in msgs if _for_nid(m, nid) and _match(m, rows, context)]
    ids = {(m["value"] or {}).get("eventId") or (m["value"] or {}).get("messageId")
           or json.dumps(m["value"], sort_keys=True) for m in found}
    assert len(ids) == n, "%d distinct matching messages, expected %d" % (len(ids), n)


@step('the last message has exactly the fields {fields:Q}')
def exact_fields(context, fields):
    expected = sorted(f.strip() for f in fields.split(","))
    actual = sorted((context.last_message.get("value") or {}).keys())
    assert actual == expected, "message fields %s, expected %s" % (actual, expected)


@step('the last message key is the negotiation id')
def key_is_nid(context):
    assert context.last_message.get("key") == context.vars["nid"], \
        "key %r" % context.last_message.get("key")


@step('the last message has the header {name:Q}')
def has_header(context, name):
    assert context.last_message.get("headers", {}).get(name), \
        "header %s missing: %s" % (name, context.last_message.get("headers"))


@step('the last message field {field:Q} is an RFC 3339 UTC date-time')
def message_dt(context, field):
    import re
    value = get_path(context.last_message["value"], field)
    assert isinstance(value, str) and re.fullmatch(
        r"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?Z", value), "%s = %r" % (field, value)


def _lifecycle_message(context, message_type, sender, recipient_id):
    from cmtest.core import new_uuid, now_utc, rfc3339
    cfg = context.config_cm
    return {
        "messageId": new_uuid(),
        "messageType": message_type,
        "contractNegotiationId": context.vars["nid"],
        "contractAgreementId": context.vars["aid"],
        "senderParticipantId": cfg.party(sender).participant_id,
        "senderRole": sender.upper(),
        "recipientParticipantId": recipient_id,
        "reason": "Injected by the test suite",
        "actor": None,
        "occurredAt": rfc3339(now_utc()),
    }


def _inject(context, msg, times=1):
    from cmtest.events import publish
    for _ in range(times):
        publish(context.config_cm, "simpl.contract.lifecycle.v1", context.vars["nid"], msg,
                {"correlationId": "inject-" + context.vars["nid"]})


@step('a lifecycle message {mtype:Q} from the {sender:Party} to the {recipient:Party} is published for the negotiation')
def inject_once(context, mtype, sender, recipient):
    rid = context.config_cm.party(recipient).participant_id
    _inject(context, _lifecycle_message(context, mtype, sender, rid))


@step('a lifecycle message {mtype:Q} from the {sender:Party} to the {recipient:Party} is published twice for the negotiation')
def inject_twice(context, mtype, sender, recipient):
    rid = context.config_cm.party(recipient).participant_id
    _inject(context, _lifecycle_message(context, mtype, sender, rid), times=2)


@step('a lifecycle message {mtype:Q} from the {sender:Party} to {recipient:Q} is published for the negotiation')
def inject_other(context, mtype, sender, recipient):
    _inject(context, _lifecycle_message(context, mtype, sender, recipient))


@step('the last message field {field:Q} is present')
def message_field_present(context, field):
    value = context.last_message.get("value") or {}
    assert has_path(value, field) and get_path(value, field) is not None, \
        "%s missing in %s" % (field, value)
