"""Shared response checks used by several step modules."""
from .core import body_json, coerce, problem_type

LEAK_MARKERS = ["\tat ", ".java:", "Traceback (most recent call last)",
                "org.springframework", "NullPointerException", "stubs:8080",
                "kafka:9092", "postgres:5432"]


def _store(context, resp):
    context.response = resp
    context.responses.append(resp)
    return resp


def compare(actual, expected_text):
    expected = coerce(expected_text)
    if isinstance(actual, bool) or actual is None:
        return actual == expected
    if isinstance(actual, (int, float)) and isinstance(expected, int):
        return actual == expected
    return str(actual) == expected_text


def check_problem(resp, code, short_type):
    assert resp.status_code == code, "expected %d, got %d: %s" % (
        code, resp.status_code, resp.text[:800])
    ct = resp.headers.get("Content-Type", "")
    assert ct.startswith("application/problem+json"), \
        "problem content type expected, got %r" % ct
    body = body_json(resp)
    for field in ("type", "title", "status", "detail"):
        assert field in body, "problem field %s missing: %s" % (field, body)
    assert body["status"] == code, "problem status %r != %d" % (body["status"], code)
    if short_type:
        assert body["type"] == problem_type(short_type), "problem type %r, expected %r" % (
            body["type"], problem_type(short_type))
    for marker in LEAK_MARKERS:
        assert marker not in resp.text, "error leaks internals (%r): %s" % (
            marker, resp.text[:500])
    return body


