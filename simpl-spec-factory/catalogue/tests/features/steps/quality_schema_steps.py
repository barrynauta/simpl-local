"""Steps for the quality stub (6.5), Quality Assessment Records (D55), shapes and schema events (D58, D60)."""
import json
import uuid
from urllib.parse import quote

from behave import given, when

from support import client
from support.quality import TestShape, broken_qar, build_qar


def _rd(context, alias):
    assert alias in context.rds, f"unknown RD alias {alias!r}"
    return context.rds[alias]


# ---------------------------------------------------------------- quality stub control

def _stub_put(context, rd_id, body):
    resp = client.send(context, "anonymous", "PUT", "quality-stub",
                       f"/stub/scores/{quote(rd_id, safe='')}", json_body=body, consume_pending=False)
    assert resp.status in (200, 201, 204), f"setup: quality stub control failed: {resp.summary()}"
    context.stub_scores.append(rd_id)


@given('the quality stub scores {alias:Q} with {scores:Q}')
def step_stub_scores(context, alias, scores):
    """scores: "overall=65, Completeness=95"; missing dimensions default to 100 in the stub."""
    body = {"dimensions": {}}
    for part in scores.split(","):
        name, _, value = part.strip().partition("=")
        number = float(value)
        number = int(number) if number.is_integer() else number
        if name == "overall":
            body["overall"] = number
        else:
            body["dimensions"][name] = number
    _stub_put(context, _rd(context, alias).id, body)


@given('the quality stub answers {alias:Q} in mode {mode:Q}')
def step_stub_mode(context, alias, mode):
    _stub_put(context, _rd(context, alias).id, {"mode": mode})


# ---------------------------------------------------------------- QAR upsert

def _post_qar(context, who, rd, doc):
    service = rd.published_on or "cat-a"
    client.send(context, who, "POST", service,
                f"/self-descriptions/{quote(rd.id, safe='')}/qualityAssessmentRecord",
                raw_body=json.dumps(doc), content_type="application/ld+json")


@when('{who:Q} posts a quality assessment record for {alias:Q} with the score {score:Num} of {max_score:Num}')
def step_post_qar(context, who, alias, score, max_score):
    rd = _rd(context, alias)
    _post_qar(context, who, rd, build_qar(rd.id, score, max_score))


@given('{who:Q} has replaced the quality assessment record of {alias:Q} with the score {score:Num} of {max_score:Num}')
def step_has_posted_qar(context, who, alias, score, max_score):
    step_post_qar(context, who, alias, score, max_score)
    assert context.response.status == 200, f"setup: QAR upsert failed: {context.response.summary()}"


@when('{who:Q} posts a quality assessment record for {alias:Q} with {variant:Q}')
def step_post_broken_qar(context, who, alias, variant):
    rd = _rd(context, alias)
    _post_qar(context, who, rd, broken_qar(rd.id, variant))


@when('{who:Q} posts a quality assessment record for an unknown resource description to {service:Q}')
def step_post_qar_unknown(context, who, service):
    rd_id = f"did:web:test.simpl.local:DataOffering:{uuid.uuid4()}"
    client.send(context, who, "POST", service,
                f"/self-descriptions/{quote(rd_id, safe='')}/qualityAssessmentRecord",
                raw_body=json.dumps(build_qar(rd_id, 50, 100)), content_type="application/ld+json")


# ---------------------------------------------------------------- shapes

@given('a test shape {alias:Q}')
def step_shape(context, alias):
    context.shapes[alias] = TestShape(alias)


@given('a test shape {alias:Q} without an offering target class')
def step_shape_bad(context, alias):
    context.shapes[alias] = TestShape(alias, template="NoOfferingTargetShape")


@when('{who:Q} uploads the shape {alias:Q} to {service:Q}')
def step_upload_shape(context, who, alias, service):
    shape = context.shapes[alias]
    resp = client.send(context, who, "POST", service, "/schemas", raw_body=shape.turtle(),
                       content_type="text/turtle")
    if resp.status in (200, 201):
        shape.uploaded_by = who
        shape.uploaded_on = service
        location = resp.headers.get("Location") or ""
        # Clean-up deletes by the id the service reports, which is the shape name per D58.
        shape.delete_id = location.rstrip("/").rsplit("/", 1)[-1] or shape.name


@given('{who:Q} has uploaded the shape {alias:Q} to {service:Q}')
def step_has_uploaded_shape(context, who, alias, service):
    step_upload_shape(context, who, alias, service)
    assert context.response.status == 201, f"setup: shape upload failed: {context.response.summary()}"


def _schema_event(context, service, event_type, name, version=None):
    data = {"schema": {"uri": f"https://schemas.test.simpl.local/{name}", "title": name,
                       "status": "PUBLISHED" if event_type == "SchemaPublished" else "REVOKED"}}
    if name is not None:
        data["schema"]["name"] = name
    if version is not None:
        data["version"] = {"uri": f"https://schemas.test.simpl.local/{name}/{version}",
                           "version": version, "created": "2026-10-28T10:00:00Z"}
    event = {"eventId": str(uuid.uuid4()), "eventType": event_type,
             "timestamp": "2026-10-28T10:00:00Z", "data": data}
    client.send(context, "anonymous", "POST", service, "/webhooks/schema-events", json_body=event)


@when('the Schema Manager sends {event_type:Q} for the shape {alias:Q} to {service:Q}')
def step_event(context, event_type, alias, service):
    _schema_event(context, service, event_type, context.shapes[alias].name)


@when('the Schema Manager sends {event_type:Q} with version {version:Q} for the shape {alias:Q} to {service:Q}')
def step_event_version(context, event_type, version, alias, service):
    _schema_event(context, service, event_type, context.shapes[alias].name, version)


@given('the Schema Manager has sent {event_type:Q} for the shape {alias:Q} to {service:Q}')
def step_has_event(context, event_type, alias, service):
    step_event(context, event_type, alias, service)
    assert context.response.status == 200, f"setup: schema event failed: {context.response.summary()}"


@given('the Schema Manager has sent {event_type:Q} with version {version:Q} for the shape {alias:Q} to {service:Q}')
def step_has_event_version(context, event_type, version, alias, service):
    step_event_version(context, event_type, version, alias, service)
    assert context.response.status == 200, f"setup: schema event failed: {context.response.summary()}"


@when('the Schema Manager sends {event_type:Q} without a schema name to {service:Q}')
def step_event_no_name(context, event_type, service):
    _schema_event(context, service, event_type, None)
