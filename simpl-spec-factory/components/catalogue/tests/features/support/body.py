"""Templating of step text and navigation in JSON responses."""
import re
from urllib.parse import quote

_TEMPLATE = re.compile(r"\{([A-Za-z][A-Za-z0-9_\-]*)(?:\.([A-Za-z]+))?\}")
_MISSING = object()


def render(text, context):
    """Replace {name} and {alias.attr} with scenario values.

    {token}       the run token of the scenario (D80)
    {alias.id}    credentialSubject.@id of an RD alias
    {alias.enc}   the same, URL-encoded for a path or query
    {alias.hash}  sdHash of the published RD
    {alias.name}  the name of a test shape alias
    {name}        any value saved with "I remember ..." or by a step
    """
    if text is None:
        return None

    def sub(match):
        name, attr = match.group(1), match.group(2)
        if attr is None:
            if name in context.vars:
                return str(context.vars[name])
            raise KeyError(f"unknown template value {{{name}}}")
        obj = context.rds.get(name) or context.shapes.get(name)
        if obj is None:
            raise KeyError(f"unknown alias {name!r} in {{{name}.{attr}}}")
        if attr == "id":
            return obj.id
        if attr == "enc":
            return quote(obj.id, safe="")
        if attr == "hash":
            return obj.sd_hash or obj.computed_hash() or ""
        if attr == "name":
            return obj.name
        raise KeyError(f"unknown attribute {attr!r} of {name!r}")
    return _TEMPLATE.sub(sub, text)


def _select(node, part):
    if isinstance(node, list):
        m = re.fullmatch(r"\[([^=\]]+)=([^\]]*)\]", part)
        if m:
            key, value = m.group(1), m.group(2)
            for item in node:
                if isinstance(item, dict) and str(_get(item, key)) == value:
                    return item
            return _MISSING
        try:
            idx = int(part)
        except ValueError:
            return _MISSING
        return node[idx] if -len(node) <= idx < len(node) else _MISSING
    if isinstance(node, dict):
        return node.get(part, _MISSING)
    return _MISSING


def _get(node, key):
    out = _select(node, key)
    return None if out is _MISSING else out


def lookup(doc, path):
    """Navigate with "/" separated parts. A part may be a list index or [key=value]."""
    node = doc
    if path in ("", "/"):
        return node
    for part in [p for p in path.split("/") if p != ""]:
        node = _select(node, part)
        if node is _MISSING:
            return _MISSING
    return node


def missing(value):
    return value is _MISSING


def same_value(actual, expected_text):
    """Compare a JSON value with the text of a step. A JSON-LD value object {"@value": v}
    or node reference {"@id": v} compares as v."""
    if isinstance(actual, dict) and len(actual) <= 2 and ("@value" in actual or set(actual) == {"@id"}):
        actual = actual.get("@value", actual.get("@id"))
    if actual is None:
        return expected_text == "null"
    if isinstance(actual, bool):
        return expected_text.lower() == str(actual).lower()
    if isinstance(actual, (int, float)):
        try:
            return float(expected_text) == float(actual)
        except ValueError:
            return False
    return str(actual) == expected_text


def result_items(doc):
    """Search, list and version payloads: the top-level items."""
    if isinstance(doc, dict) and isinstance(doc.get("items"), list):
        return doc["items"]
    if isinstance(doc, list):
        return doc
    return []


def item_ids(item):
    """Ids carried by one result item of any of the backends."""
    ids = []
    if not isinstance(item, dict):
        return ids
    for wrapper in ("n", "i"):
        inner = item.get(wrapper)
        if isinstance(inner, dict):
            cg = inner.get("claimsGraphUri")
            if isinstance(cg, list):
                ids.extend(str(x) for x in cg)
            elif isinstance(cg, str):
                ids.append(cg)
    meta = item.get("meta")
    if isinstance(meta, dict) and meta.get("id"):
        ids.append(str(meta["id"]))
    for key in ("id", "resourceDescriptionId", "@id"):
        if isinstance(item.get(key), str):
            ids.append(item[key])
    cs = item.get("credentialSubject")
    if isinstance(cs, dict) and isinstance(cs.get("@id"), str):
        ids.append(cs["@id"])
    return ids


def find_item(items, rd_id):
    for item in items:
        if rd_id in item_ids(item):
            return item
    return None


def item_payload(item):
    """The inner object of a result item: n (cat, qma), i (cbe), meta (list) or the item."""
    for wrapper in ("n", "i"):
        if isinstance(item.get(wrapper), dict):
            return item[wrapper]
    return item
