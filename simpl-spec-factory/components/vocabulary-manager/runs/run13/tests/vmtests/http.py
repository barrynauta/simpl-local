"""HTTP helpers: raw requests (no URL re-quoting) and an exact multipart builder."""
import json as _json
import uuid

import requests

from . import config


class Part:
    """One multipart part. filename set means a file part."""

    def __init__(self, name, value, filename=None, content_type=None):
        self.name = name
        self.value = value if isinstance(value, bytes) else str(value).encode("utf-8")
        self.filename = filename
        self.content_type = content_type


def multipart_body(parts, boundary=None):
    boundary = boundary or "vmtests" + uuid.uuid4().hex
    out = bytearray()
    for p in parts:
        out += f"--{boundary}\r\n".encode()
        disp = f'Content-Disposition: form-data; name="{p.name}"'
        if p.filename is not None:
            disp += f'; filename="{p.filename}"'
        out += disp.encode("utf-8") + b"\r\n"
        if p.content_type:
            out += f"Content-Type: {p.content_type}\r\n".encode()
        out += b"\r\n" + p.value + b"\r\n"
    out += f"--{boundary}--\r\n".encode()
    return bytes(out), f"multipart/form-data; boundary={boundary}"


def multipart_overhead(parts, boundary):
    """Size of the body without the value bytes of the last part."""
    body, _ = multipart_body(parts, boundary)
    return len(body) - len(parts[-1].value)


def send(method, path, base=None, token=None, headers=None, body=None, json=None, content_type=None,
         timeout=None, absolute=False):
    """Send a request without letting requests re-quote the URL.

    path is relative to the API base unless absolute is true.
    """
    url = path if absolute else (base or config.API_URL) + path
    hdrs = {}
    if token is not None:
        hdrs["Authorization"] = f"Bearer {token}"
    data = body
    if json is not None:
        data = _json.dumps(json).encode("utf-8")
        hdrs["Content-Type"] = "application/json"
    if content_type is not None:
        hdrs["Content-Type"] = content_type
    if headers:
        hdrs.update(headers)
    req = requests.Request(method, "http://placeholder.invalid/", headers=hdrs, data=data)
    prep = req.prepare()
    prep.url = url
    # requests adds Content-Length for bytes bodies; keep any explicit header as given.
    with requests.Session() as s:
        return s.send(prep, timeout=timeout or config.HTTP_TIMEOUT, allow_redirects=False)
