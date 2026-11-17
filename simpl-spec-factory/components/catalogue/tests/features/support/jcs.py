"""RFC 8785 JSON Canonicalization Scheme (JCS), enough for the test data.

Keys are sorted by their UTF-16 code units, strings use the minimal JSON
escaping, numbers use the ECMAScript shortest form.
"""
import json
import math
from decimal import Decimal


def _number(value):
    if isinstance(value, bool):
        raise TypeError("bool is not a number")
    if isinstance(value, int):
        return str(value)
    if math.isnan(value) or math.isinf(value):
        raise ValueError("JCS does not allow NaN or Infinity")
    if value == 0:
        return "0"
    if value.is_integer() and abs(value) < 1e21:
        return str(int(value))
    text = repr(value)
    mantissa, _, exponent = text.partition("e")
    if not exponent:
        return text
    exp = int(exponent)
    if -7 < exp < 21:
        # ECMAScript writes these in plain decimal notation.
        return format(Decimal(text), "f")
    sign = "+" if exp > 0 else "-"
    return f"{mantissa}e{sign}{abs(exp)}"


def _string(value):
    return json.dumps(value, ensure_ascii=False)


def canonicalize(value):
    """Return the JCS serialisation of a JSON value as a str."""
    if value is None:
        return "null"
    if value is True:
        return "true"
    if value is False:
        return "false"
    if isinstance(value, (int, float)):
        return _number(value)
    if isinstance(value, str):
        return _string(value)
    if isinstance(value, (list, tuple)):
        return "[" + ",".join(canonicalize(v) for v in value) + "]"
    if isinstance(value, dict):
        keys = sorted(value.keys(), key=lambda k: k.encode("utf-16-be"))
        return "{" + ",".join(_string(k) + ":" + canonicalize(value[k]) for k in keys) + "}"
    raise TypeError(f"not a JSON value: {type(value)!r}")


def canonical_bytes(value):
    return canonicalize(value).encode("utf-8")
