"""A valid request for every operation of the OpenAPI, by operationId.

The requests act on the vocabulary 'Acl' (one version) and the external vocabulary 'AclExt'
that the access control feature creates in its Background. Write requests create 'AclNew' and
'AclExtNew'. Used to check roles and tokens on every operation (decisions D16, D17).
"""
READ_OPERATIONS = [
    "getVocabularies", "searchVocabularies", "getVocabulary", "getVocabularyVersions",
    "getVocabularyVersion", "downloadVocabulary", "downloadVocabularyVersion",
    "getExternalVocabularies", "searchExternalVocabularies", "downloadExternalVocabularyContent",
    "getExternalVocabularyContentByName", "getExternalVocabulary",
]
WRITE_OPERATIONS = [
    "createVocabulary", "updateVocabularyStatus", "createVocabularyVersion",
    "updateVocabularyVersionStatus", "createExternalVocabulary", "deleteExternalVocabulary",
]


def build(world, op):
    acl = world.real("Acl")
    ext = world.real("AclExt")
    ns = world.ext_namespace("AclExt")
    if op == "getVocabularies":
        return "GET", "/vocabularies", {}
    if op == "createVocabulary":
        return "POST", "/vocabularies", {"files": world.multipart(world.create_parts("AclNew"))}
    if op == "searchVocabularies":
        return "POST", "/vocabularies/searches", {"json": {"items": [{"name": acl}]}}
    if op == "getVocabulary":
        return "GET", f"/vocabularies/{acl}", {}
    if op == "updateVocabularyStatus":
        return "PATCH", f"/vocabularies/{acl}", {"json": {"status": "DEPRECATED"}}
    if op == "getVocabularyVersions":
        return "GET", f"/vocabularies/{acl}/versions", {}
    if op == "createVocabularyVersion":
        return "POST", f"/vocabularies/{acl}/versions", {"files": world.multipart(world.version_parts("Acl"))}
    if op == "getVocabularyVersion":
        return "GET", f"/vocabularies/{acl}/versions/1", {}
    if op == "updateVocabularyVersionStatus":
        return "PATCH", f"/vocabularies/{acl}/versions/1", {"json": {"status": "DEPRECATED"}}
    if op == "downloadVocabulary":
        return "GET", f"/content/{acl}", {}
    if op == "downloadVocabularyVersion":
        return "GET", f"/content/{acl}/versions/1", {}
    if op == "getExternalVocabularies":
        return "GET", "/external-vocabularies", {}
    if op == "createExternalVocabulary":
        return "POST", "/external-vocabularies", {"files": world.multipart(world.external_parts("AclExtNew"))}
    if op == "searchExternalVocabularies":
        return "POST", "/external-vocabulary/searches", {"json": {"namespaceUri": ns}}
    if op == "downloadExternalVocabularyContent":
        return "POST", "/external-vocabularies/content", {"json": {"namespaceUri": ns}}
    if op == "getExternalVocabularyContentByName":
        return "GET", f"/external-vocabularies/{ext}/content", {}
    if op == "getExternalVocabulary":
        return "GET", f"/external-vocabularies/{ext}", {}
    if op == "deleteExternalVocabulary":
        return "DELETE", f"/external-vocabularies/{ext}", {}
    if op == "getHealth":
        return "GET", "/health", {}
    raise AssertionError(f"unknown operation '{op}'")


def check_nothing_changed(world, op):
    """After a refused request, prove through reads that the store did not change."""
    acl = world.real("Acl")
    if op == "createVocabulary":
        name = world.real("AclNew")
        found = [i for i in world.list_all([("name", name)]) if i.get("name", "").lower() == name.lower()]
        assert not found, f"vocabulary {name} was stored: {found}"
    elif op == "createVocabularyVersion":
        resp = world.admin_get(f"/vocabularies/{acl}/versions")
        assert resp.status_code == 200, resp.text
        assert resp.json().get("total") == 1, f"versions changed: {resp.text[:300]}"
    elif op == "updateVocabularyStatus":
        resp = world.admin_get(f"/vocabularies/{acl}")
        assert resp.json().get("status") == "ACTIVE", resp.text
    elif op == "updateVocabularyVersionStatus":
        resp = world.admin_get(f"/vocabularies/{acl}/versions/1")
        assert resp.json().get("status") == "ACTIVE", resp.text
    elif op == "createExternalVocabulary":
        resp = world.admin_get(f"/external-vocabularies/{world.real('AclExtNew')}")
        assert resp.status_code == 404, f"external vocabulary was stored: {resp.status_code}"
    elif op == "deleteExternalVocabulary":
        resp = world.admin_get(f"/external-vocabularies/{world.real('AclExt')}")
        assert resp.status_code == 200, f"external vocabulary was removed: {resp.status_code}"
