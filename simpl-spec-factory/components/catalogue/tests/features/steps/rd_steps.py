"""Steps for resource descriptions: build, sign (D22, D23), publish, version, revoke, delete."""
import json
from urllib.parse import quote, unquote

from behave import given, then, when

from support import body as B
from support import client
from support.config import HOME_CATALOGUE, USERS
from support.rd import ResourceDescription


def _rd(context, alias):
    assert alias in context.rds, f"unknown RD alias {alias!r}"
    return context.rds[alias]


def _home(user):
    return HOME_CATALOGUE[USERS[user]["realm"]]


def _new_rd(context, rtype, alias, provider):
    assert alias not in context.rds, f"alias {alias!r} used twice"
    rd = ResourceDescription(alias, rtype, provider, context.vars["token"])
    context.rds[alias] = rd
    return rd


# ---------------------------------------------------------------- building

@given('a {rtype:Q} resource description {alias:Q} by {provider:Q}')
def step_build(context, rtype, alias, provider):
    _new_rd(context, rtype, alias, provider)


@given('{alias:Q} is discoverable by {attrs:QList}')
def step_policy(context, alias, attrs):
    _rd(context, alias).set_policy(attrs)


@given('{alias:Q} has no access policy')
def step_no_policy(context, alias):
    _rd(context, alias).remove("credentialSubject/simpl:servicePolicy/simpl:access-policy")


@given('{alias:Q} has an access policy that is not JSON')
def step_bad_policy(context, alias):
    _rd(context, alias).set("credentialSubject/simpl:servicePolicy/simpl:access-policy", "swvwe")


@given('{alias:Q} has the field {path:Q} set to {value:Q}')
def step_set_field(context, alias, path, value):
    text = B.render(value, context)
    try:
        parsed = json.loads(text)
    except ValueError:
        parsed = text
    _rd(context, alias).set(path, parsed)


@given('{alias:Q} has the field {path:Q} removed')
def step_remove_field(context, alias, path):
    _rd(context, alias).remove(path)


@given('{alias:Q} is broken as {broken:Q}')
def step_broken(context, alias, broken):
    _rd(context, alias).apply_broken(broken)


@given('{alias:Q} is signed with the key of {key:Q}')
def step_sign_with(context, alias, key):
    rd = _rd(context, alias)
    rd.signer = key
    rd.issuer_key = key


@given('{alias:Q} conforms to the shape {shape:Q}')
def step_conforms(context, alias, shape):
    rd = _rd(context, alias)
    name = context.shapes[shape].name if shape in context.shapes else shape
    rd.set("credentialSubject/dct:conformsTo", {
        "@id": f"urn:simpl:schema:{name}:1", "dct:schemaName": name, "dct:hasVersion": "1"})


@given('{alias:Q} conforms to the shape {shape:Q} version {version:Q}')
def step_conforms_version(context, alias, shape, version):
    step_conforms(context, alias, shape)
    _rd(context, alias).set("credentialSubject/dct:conformsTo/dct:hasVersion", version)


@given('{alias:Q} is version {n:d} of {prev:Q}')
def step_version_of(context, alias, n, prev):
    previous = _rd(context, prev)
    rd = _new_rd(context, previous.resource_type, alias, previous.provider)
    rd.make_version_of(previous, n)


# ---------------------------------------------------------------- publishing

def publish(context, who, alias, service=None, path="/self-descriptions"):
    rd = _rd(context, alias)
    if service is None:
        service = _home(who) if who in USERS else _home(rd.provider)
    payload = rd.body()
    raw = rd.raw_body if payload is None else json.dumps(payload)
    resp = client.send(context, who, "POST", service, path, raw_body=raw)
    if resp.status in (200, 201, 202):
        rd.published_on = service
        rd.published_by = who
        doc = resp.json()
        if isinstance(doc, dict) and isinstance(doc.get("sdHash"), str):
            rd.sd_hash = doc["sdHash"]
        else:
            rd.sd_hash = rd.computed_hash()
    return resp


@when('{who:Q} publishes {alias:Q}')
def step_publish(context, who, alias):
    publish(context, who, alias)


@when('{who:Q} publishes {alias:Q} through {path:Q}')
def step_publish_path(context, who, alias, path):
    publish(context, who, alias, path=path)


@when('{who:Q} publishes {alias:Q} to {service:Q}')
def step_publish_to(context, who, alias, service):
    publish(context, who, alias, service=service)


def _published(context, who, alias):
    resp = publish(context, who, alias)
    assert resp.status == 201, f"setup: publishing {alias} as {who} failed: {resp.summary()}"


@given('{who:Q} has published {alias:Q}')
def step_has_published(context, who, alias):
    _published(context, who, alias)


@given('{who:Q} has published a {rtype:Q} resource description {alias:Q}')
def step_has_published_new(context, who, rtype, alias):
    _new_rd(context, rtype, alias, who)
    _published(context, who, alias)


@given('{who:Q} has published a {rtype:Q} resource description {alias:Q} discoverable by {attrs:QList}')
def step_has_published_new_policy(context, who, rtype, alias, attrs):
    _new_rd(context, rtype, alias, who).set_policy(attrs)
    _published(context, who, alias)


@given('{who:Q} has published {alias:Q} as version {n:d} of {prev:Q}')
def step_has_published_version(context, who, alias, n, prev):
    step_version_of(context, alias, n, prev)
    _published(context, who, alias)


@when('{who:Q} sends {alias:Q} to {path:Q} on {service:Q}')
def step_send_rd(context, who, alias, path, service):
    rd = _rd(context, alias)
    payload = rd.body()
    raw = rd.raw_body if payload is None else json.dumps(payload)
    client.send(context, who, "POST", service, B.render(path, context), raw_body=raw)


@when('{who:Q} sends {alias:Q} wrapped in a presentation to {path:Q} on {service:Q}')
def step_send_vp(context, who, alias, path, service):
    rd = _rd(context, alias)
    vp = {"@context": ["https://www.w3.org/2018/credentials/v1"], "type": ["VerifiablePresentation"],
          "verifiableCredential": [rd.body()]}
    client.send(context, who, "POST", service, B.render(path, context), json_body=vp)


# ---------------------------------------------------------------- revoke, delete, read

@when('{who:Q} revokes {alias:Q}')
def step_revoke(context, who, alias):
    rd = _rd(context, alias)
    service = rd.published_on or _home(rd.provider)
    client.send(context, who, "POST", service, f"/self-descriptions/{quote(rd.id, safe='')}/revoke")


@given('{who:Q} has revoked {alias:Q}')
def step_has_revoked(context, who, alias):
    step_revoke(context, who, alias)
    assert context.response.status == 200, f"setup: revoke {alias} failed: {context.response.summary()}"


@when('{who:Q} deletes {alias:Q}')
def step_delete(context, who, alias):
    rd = _rd(context, alias)
    service = rd.published_on or _home(rd.provider)
    resp = client.send(context, who, "DELETE", service, f"/self-descriptions/{rd.sd_hash or rd.computed_hash()}")
    if resp.status in (200, 204):
        rd.deleted = True


@given('{who:Q} has deleted {alias:Q}')
def step_has_deleted(context, who, alias):
    step_delete(context, who, alias)
    assert context.response.status in (200, 204), f"setup: delete {alias} failed: {context.response.summary()}"


def _lookup_status(context, alias):
    rd = _rd(context, alias)
    service = rd.published_on or _home(rd.provider)
    reader = rd.published_by if rd.published_by in USERS else rd.provider
    resp = client.send(context, reader, "GET", service,
                       f"/self-descriptions?ids={quote(rd.id, safe='')}&statuses=active,revoked",
                       consume_pending=False)
    assert resp.status == 200, f"listing {alias} failed: {resp.summary()}"
    item = B.find_item(B.result_items(resp.json()), rd.id)
    return item, resp


@then('{alias:Q} is stored with the status {status:Q}')
def step_stored_status(context, alias, status):
    saved = context.response
    item, resp = _lookup_status(context, alias)
    context.response = saved
    assert item is not None, f"{alias} not listed: {resp.summary()}"
    actual = (item.get("meta") or {}).get("status")
    assert actual == status, f"{alias}: expected status {status}, got {actual!r}"


@then('{alias:Q} is not stored')
def step_not_stored(context, alias):
    rd = _rd(context, alias)
    saved = context.response
    service = _home(rd.provider)
    reader = rd.provider if rd.provider in USERS else "consumer-a"
    resp = client.send(context, reader, "GET", service, f"/self-descriptions/{quote(rd.id, safe='')}",
                       consume_pending=False)
    context.response = saved
    assert resp.status == 404, f"{alias} should not be stored, GET answered {resp.summary()}"


# ---------------------------------------------------------------- publication response

@then('the field {path:Q} is the sdHash of {alias:Q}')
def step_field_hash(context, path, alias):
    rd = _rd(context, alias)
    actual = B.lookup(context.response.json(), path)
    assert actual == rd.computed_hash(), f"{path}: expected {rd.computed_hash()} (D11), got {actual!r}"


@then('the field {path:Q} is the issuer of {alias:Q}')
def step_field_issuer(context, path, alias):
    rd = _rd(context, alias)
    actual = B.lookup(context.response.json(), path)
    assert actual == rd.last_body.get("issuer"), f"{path}: expected {rd.last_body.get('issuer')}, got {actual!r}"


@then('the field {path:Q} is the id of {alias:Q}')
def step_field_id(context, path, alias):
    rd = _rd(context, alias)
    actual = B.lookup(context.response.json(), B.render(path, context))
    assert actual == rd.id, f"{path}: expected {rd.id}, got {actual!r}"


@then('the response header {name:Q} points to {alias:Q}')
def step_location(context, name, alias):
    rd = _rd(context, alias)
    actual = context.response.headers.get(name)
    assert actual, f"no {name} header"
    assert unquote(actual).endswith("/self-descriptions/" + rd.id), \
        f"{name}: {actual!r} does not point to /self-descriptions/<urlencoded {rd.id}>"


@then('the response is the stored resource description {alias:Q}')
def step_is_stored_vc(context, alias):
    rd = _rd(context, alias)
    doc = context.response.json()
    if isinstance(doc, str):
        doc = json.loads(doc)
    assert isinstance(doc, dict), f"not a JSON object: {context.response.summary()}"
    cs = doc.get("credentialSubject") or {}
    assert cs.get("@id") == rd.id, f"credentialSubject.@id is {cs.get('@id')!r}, expected {rd.id}"
    assert "proof" in doc, "stored VC without proof"


@then('the field {path:Q} is the resource description {alias:Q}')
def step_field_is_vc(context, path, alias):
    rd = _rd(context, alias)
    value = B.lookup(context.response.json(), path)
    assert isinstance(value, dict), f"{path} is not an object: {value!r}"
    assert (value.get("credentialSubject") or {}).get("@id") == rd.id, f"{path} is not {alias}"


@given('{alias:Q} points to the previous version {prev:Q}')
def step_points_to(context, alias, prev):
    _rd(context, alias).set("credentialSubject/simpl:previousVersion", {"@id": _rd(context, prev).id})


@given('{alias:Q} points to an unknown previous version')
def step_points_to_unknown(context, alias):
    import uuid
    _rd(context, alias).set("credentialSubject/simpl:previousVersion",
                            {"@id": f"did:web:test.simpl.local:DataOffering:{uuid.uuid4()}"})


@given('{alias:Q} has the price {price:Num}')
def step_price(context, alias, price):
    _rd(context, alias).set("credentialSubject/simpl:offeringPrice/simpl:price",
                            {"@type": "xsd:decimal", "@value": price})


@given('{alias:Q} has an access policy with the action {action:Q} for {attr:Q}')
def step_policy_action(context, alias, action, attr):
    policy = json.dumps({"@context": "http://www.w3.org/ns/odrl.jsonld", "@type": "Set",
                         "permission": [{"action": [action], "assignee": {"uid": attr}, "constraint": []}]},
                        separators=(",", ":"))
    _rd(context, alias).set("credentialSubject/simpl:servicePolicy/simpl:access-policy", policy)
