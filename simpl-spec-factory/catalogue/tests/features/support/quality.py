"""Quality Assessment Records (D55) and test shapes (D58)."""
import copy
import json
import os
import secrets
import uuid

FIXTURES = os.path.join(os.path.dirname(__file__), "..", "..", "fixtures")


def _number(text):
    value = float(text)
    return int(value) if value.is_integer() else value


def build_qar(rd_id, score, max_score, dimension_score=None):
    with open(os.path.join(FIXTURES, "qar", "qar-template.json"), encoding="utf-8") as fh:
        doc = json.load(fh)
    uid = str(uuid.uuid4())
    dim = dimension_score if dimension_score is not None else score

    def fill(value):
        if isinstance(value, str):
            if value == "__SCORE__":
                return _number(score)
            if value == "__DIM_SCORE__":
                return _number(dim)
            if value == "__MAX__":
                return _number(max_score)
            return value.replace("__RD_ID__", rd_id).replace("__UUID__", uid)
        if isinstance(value, list):
            return [fill(v) for v in value]
        if isinstance(value, dict):
            return {k: fill(v) for k, v in value.items()}
        return value
    return fill(doc)


def broken_qar(rd_id, variant):
    """Variants of D55 structure errors."""
    doc = build_qar(rd_id, 50, 100)
    graph = doc["@graph"]
    if variant == "an empty graph":
        doc["@graph"] = []
    elif variant == "a node without @type":
        graph[1].pop("@type")
    elif variant == "a node without @id":
        graph[2].pop("@id")
    elif variant == "no QualityAssessmentRecord node":
        doc["@graph"] = [n for n in graph if n["@type"] != "QualityAssessmentRecord"]
    elif variant == "two QualityAssessmentRecord nodes":
        extra = copy.deepcopy([n for n in graph if n["@type"] == "QualityAssessmentRecord"][0])
        extra["@id"] = f"urn:uuid:{uuid.uuid4()}-qar2"
        graph.append(extra)
    elif variant == "computedOn another resource description":
        for n in graph:
            if n["@type"] == "QualityAssessmentRecord":
                n["computedOn"] = "did:web:test.simpl.local:DataOffering:" + str(uuid.uuid4())
    elif variant == "no SelfDescriptionScore node":
        doc["@graph"] = [n for n in graph if n["@type"] != "SelfDescriptionScore"]
    elif variant == "a non-numeric score":
        for n in graph:
            if n["@type"] == "SelfDescriptionScore":
                n["score"] = "high"
    elif variant == "a maxScore of zero":
        for n in graph:
            if n["@type"] == "SelfDescriptionScore":
                n["maxScore"] = 0
    else:
        raise ValueError(f"unknown QAR variant {variant!r}")
    return doc


class TestShape:
    """A SHACL shape with a unique PascalCase name (D58, D80)."""

    def __init__(self, alias, template="TestShape", target="DataOffering"):
        self.alias = alias
        self.name = "T" + secrets.token_hex(5) + "Shape"
        self.id = self.name
        self.template = template
        self.target = target
        self.uploaded_by = None
        self.uploaded_on = None

    def turtle(self):
        path = os.path.join(FIXTURES, "shapes", f"{self.template}.ttl.tmpl")
        with open(path, encoding="utf-8") as fh:
            return fh.read().replace("__SHAPE__", self.name).replace("__TARGET__", self.target)

    def computed_hash(self):
        return None

    sd_hash = None
