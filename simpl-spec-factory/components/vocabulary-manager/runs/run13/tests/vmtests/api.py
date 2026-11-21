"""Domain helpers on top of the HTTP layer: create vocabularies, versions and external vocabularies."""
from . import auth, config
from .data import default_turtle
from .http import Part, multipart_body, send


def admin_token():
    return auth.fetch_token("ga-admin")


def turtle_part(content, filename="vocabulary.ttl"):
    return Part("vocabularyFile", content, filename=filename, content_type="text/turtle")


def create_parts(name, description, changelog, content):
    parts = []
    if name is not None:
        parts.append(Part("name", name))
    if description is not None:
        parts.append(Part("description", description))
    if changelog is not None:
        parts.append(Part("changelog", changelog))
    if content is not None:
        parts.append(turtle_part(content, f"{name or 'vocabulary'}.ttl"))
    return parts


def post_multipart(path, parts, token, headers=None, base=None):
    body, ctype = multipart_body(parts)
    return send("POST", path, base=base, token=token, body=body, content_type=ctype, headers=headers)


def create_vocabulary(name, description=None, changelog="Initial version.", content=None, token=None):
    description = description if description is not None else f"Description of {name}."
    content = content if content is not None else default_turtle(name)
    return post_multipart("/vocabularies", create_parts(name, description, changelog, content),
                          token or admin_token())


def create_version(name, version_hint, description=None, changelog=None, content=None, token=None):
    description = description if description is not None else f"Description of {name} version {version_hint}."
    changelog = changelog if changelog is not None else f"Changes for version {version_hint}."
    content = content if content is not None else default_turtle(name, version_hint)
    parts = create_parts(None, description, changelog, content)
    return post_multipart(f"/vocabularies/{name}/versions", parts, token or admin_token())


def patch_status(path, status, token=None):
    return send("PATCH", path, token=token or admin_token(), json={"status": status})


def register_external(name, namespace, aliases=(), description=None, source=None, content=None, token=None):
    parts = [Part("name", name), Part("namespaceUri", namespace)]
    for a in aliases:
        parts.append(Part("aliasUris", a))
    if description is not None:
        parts.append(Part("description", description))
    if source is not None:
        parts.append(Part("sourceUri", source))
    if content is None:
        content = (
            f"@prefix t: <{namespace.rstrip('#/')}#> .\n"
            "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
            "t:term a rdfs:Class ;\n  rdfs:label \"External term\"@en .\n"
        ).encode("utf-8")
    parts.append(turtle_part(content, f"{name}.ttl"))
    return post_multipart("/external-vocabularies", parts, token or admin_token())


def expect(resp, status, what):
    if resp.status_code != status:
        raise AssertionError(f"{what}: expected {status}, got {resp.status_code}: {resp.text[:800]}")
    return resp


# Operation catalogue for the access-control outlines (D16, D17, D84). Each entry builds a
# request that is valid for the admin, so the expected admin status is the success status.
READ_OPS = {
    "getVocabularies": 200,
    "getVocabulary": 200,
    "getVocabularyVersions": 200,
    "getVocabularyVersion": 200,
    "downloadVocabulary": 200,
    "downloadVocabularyVersion": 200,
    "searchVocabularies": 200,
    "getExternalVocabularies": 200,
    "searchExternalVocabularies": 200,
    "downloadExternalVocabularyContent": 200,
    "getExternalVocabularyContentByName": 200,
    "getExternalVocabulary": 200,
}
WRITE_OPS = {
    "createVocabulary": 201,
    "updateVocabularyStatus": 200,
    "createVocabularyVersion": 201,
    "updateVocabularyVersionStatus": 200,
    "createExternalVocabulary": 201,
    "deleteExternalVocabulary": 204,
}


def build_operation(op, vocab, external, external_ns, new_name, new_external, tok):
    """Return (method, path, kwargs for send) for an operation id."""
    if op == "getHealth":
        return "GET", "/health", {}
    if op == "getVocabularies":
        return "GET", f"/vocabularies?name={tok}", {}
    if op == "getVocabulary":
        return "GET", f"/vocabularies/{vocab}", {}
    if op == "getVocabularyVersions":
        return "GET", f"/vocabularies/{vocab}/versions", {}
    if op == "getVocabularyVersion":
        return "GET", f"/vocabularies/{vocab}/versions/1", {}
    if op == "downloadVocabulary":
        return "GET", f"/content/{vocab}", {}
    if op == "downloadVocabularyVersion":
        return "GET", f"/content/{vocab}/versions/1", {}
    if op == "searchVocabularies":
        return "POST", "/vocabularies/searches", {"json": {"items": [{"name": vocab}]}}
    if op == "getExternalVocabularies":
        return "GET", "/external-vocabularies", {}
    if op == "searchExternalVocabularies":
        return "POST", "/external-vocabulary/searches", {"json": {"namespaceUri": external_ns}}
    if op == "downloadExternalVocabularyContent":
        return "POST", "/external-vocabularies/content", {"json": {"namespaceUri": external_ns}}
    if op == "getExternalVocabularyContentByName":
        return "GET", f"/external-vocabularies/{external}/content", {}
    if op == "getExternalVocabulary":
        return "GET", f"/external-vocabularies/{external}", {}
    if op == "createVocabulary":
        body, ctype = multipart_body(create_parts(new_name, "Access test.", "Initial version.",
                                                  default_turtle(new_name)))
        return "POST", "/vocabularies", {"body": body, "content_type": ctype}
    if op == "updateVocabularyStatus":
        return "PATCH", f"/vocabularies/{vocab}", {"json": {"status": "DEPRECATED"}}
    if op == "createVocabularyVersion":
        body, ctype = multipart_body(create_parts(None, "Access test version 2.", "Second version.",
                                                  default_turtle(vocab, 2)))
        return "POST", f"/vocabularies/{vocab}/versions", {"body": body, "content_type": ctype}
    if op == "updateVocabularyVersionStatus":
        return "PATCH", f"/vocabularies/{vocab}/versions/1", {"json": {"status": "DEPRECATED"}}
    if op == "createExternalVocabulary":
        ns = f"https://ext-{new_external.lower()}.example.org/ns#"
        parts = [Part("name", new_external), Part("namespaceUri", ns),
                 turtle_part(f"@prefix t: <{ns}> .\nt:a t:b t:c .\n".encode())]
        body, ctype = multipart_body(parts)
        return "POST", "/external-vocabularies", {"body": body, "content_type": ctype}
    if op == "deleteExternalVocabulary":
        return "DELETE", f"/external-vocabularies/{external}", {}
    raise AssertionError(f"unknown operation {op}")


def call_operation(op, token, **names):
    method, path, kwargs = build_operation(op, **names)
    return send(method, path, token=token, **kwargs)


def content_base():
    return config.CONTENT_BASE
