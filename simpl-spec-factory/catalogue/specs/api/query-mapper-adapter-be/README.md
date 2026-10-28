# OpenAPI Layout

This directory now uses a composed OpenAPI structure:

- `adapter_openapi.yaml` is the active, editable root spec composed from tier
  paths and shared components.
- `adapter_openapi-itb.yaml` is the generated, self-contained version for ITB
  and other importers that cannot resolve external path-item references.
- `adapter_openapi-legacy.yaml` preserves the original canonical snapshot.
- `tier2-gateway/t2-adapter-openapi.yaml` contains the existing tier2 route set.
- `tier1-gateway/t1-adapter-openapi.yaml` defines selected-catalogue quick search
  (`GET /governanceAuthority/selfDescriptions`) and the existing advanced-search
  draft (`POST /governanceAuthority/selfDescriptions/advancedSearch`). Only the
  quick-search path is composed into the root.
- `common/common-components.yaml` contains shared OpenAPI components.
- [`resource-relationships.md`](resource-relationships.md) diagrams every active
  search, participant, and federated resource and its gateway boundary.

## Contract

- Root version `1.2.0` adds selected-catalogue quick search to the existing
  Tier-2 paths. Tier-1 is version `1.1.0`; Tier-2 remains `1.1.0`. All retain
  the major-version `/v1` server URL, contact, and EUPL license metadata.
- The root selects paths from tier files using path-item `$ref`.
  The generated ITB version inlines path items and external components, retaining
  local schema `$ref`s so importers do not need to load sibling files.
- Tier files reference shared components from `../common/common-components.yaml`.
- Shared schemas/responses/parameters/security schemes are centralized in `common/`.
- `GET /governanceAuthority/selfDescriptions` lets a Consumer send quick-search
  criteria and selected catalogue IDs to its Trusted Governance Authority.
  `catalogueIds` is a required query array with at least one non-empty string,
  serialized with `style: form` and `explode: true`. IDs select catalogues, not
  caller-supplied endpoint URLs; no custom catalogue-count or URL-length cap
  is defined. For example, with `Accept: application/json` and OAuth2:

  ```text
  /v1/governanceAuthority/selfDescriptions?catalogueIds=climateResearchCatalogue&searchString=climate
  /v1/governanceAuthority/selfDescriptions?catalogueIds=climateResearchCatalogue&catalogueIds=transportDataCatalogue&searchString=climate
  ```

  There is no request body or request `Content-Type` requirement. Existing
  search-string validation, optional `minimumScore` (non-negative) and
  `minimumClassification`, and pagination remain: `page` defaults to 1,
  `pageSize` defaults to 20, and values above 100 are silently capped at 100.
  The contract provides non-empty and empty 200 response examples, RFC 9457
  SIMPL problem examples for invalid catalogue selection/search/pagination
  (400), unsupported response media types (406), and the shared 500 response.
  The local `GET /selfDescriptions` remains unchanged.
- The new GET is **specification-only**, not an implemented Java/runtime route.
  Delegation to selected Trustor Governance Authorities depends on the separate
  SIMPL-31880 API. No routing implementation, result-aggregation rules, or
  partial-failure behavior is introduced here. Advanced search, participant
  operations, existing federated retrieval, and shared components are unchanged.
- `GET /federated/resourceDescriptions/{resourceDescriptionId}` is a separate
  contract: the gateway authenticates the Trusted Governance Authority via
  mTLS, maps Consumer attributes and enforces discovery ABAC before forwarding
  required `User-Attributes` and `Correlation-Id` headers. The adapter does not
  repeat authorization; gateway-generated authentication/ABAC errors are not
  adapter responses. The JSON body carries the full resource description only,
  without a separate schema; `Correlation-Id` is echoed in the response header only.
- The Common Data Model linkage and traceability update is deferred to
  SIMPL-30887 and is not satisfied by this API-definition change.

## Validation

After editing the root, tier files, or shared components, regenerate
the ITB version from repository root using `prance==25.4.8.0` and `PyYAML` in a local
Python environment (no API upload):

```python
from pathlib import Path
import yaml
from prance.util.resolver import RefResolver

source = Path("openapi/adapter_openapi.yaml").resolve()
resolver = RefResolver(yaml.safe_load(source.read_text()), source.as_uri())
resolver.resolve_references()
document = resolver.specs
schemas = document["components"]["schemas"]

def reuse_schemas(value):
    if isinstance(value, list):
        return [reuse_schemas(item) for item in value]
    if not isinstance(value, dict):
        return value
    result = {}
    for key, child in value.items():
        if key == "schema":
            for name, schema in schemas.items():
                if child == schema:
                    result[key] = {"$ref": f"#/components/schemas/{name}"}
                    break
            else:
                result[key] = reuse_schemas(child)
        else:
            result[key] = reuse_schemas(child)
    return result

source.with_name("adapter_openapi-itb.yaml").write_text(
    "# Generated from adapter_openapi.yaml; see README.md for regeneration.\n"
    + yaml.safe_dump(reuse_schemas(document), sort_keys=False, allow_unicode=True),
    encoding="utf-8",
)
```

Do not edit the generated ITB version directly. Commit it alongside its source
changes. Check that the root and ITB version resolve to the same API contract,
with only local schema `$ref`s remaining in the ITB version. Schema reuse is
based on exact equality with resolved shared schemas, preserving the contract.
The shared `Problem.type` pattern accepts absolute URIs (including `about:blank`
and problem URNs), and both shared 500 responses provide named media-type
`examples`. Configure ITB to import
`openapi/adapter_openapi-itb.yaml`, not the composed root.

Validate contracts locally, without sending private specifications to external
services. Use an environment with `PyYAML`, `openapi-spec-validator`,
`openapi-schema-validator`, and `regex` installed. From repository root:

```bash
python - <<'PY'
from pathlib import Path
import regex
import yaml
from jsonschema import Draft4Validator
from openapi_schema_validator import OAS30Validator
from openapi_spec_validator import OpenAPIV30SpecValidator

def unicode_regex(value):
    regex.compile(value)
    return True

# The existing Unicode property escapes are not supported by Python's re.
for checker in (Draft4Validator.FORMAT_CHECKER, OAS30Validator.FORMAT_CHECKER):
    checker.checkers["regex"] = (unicode_regex, regex.error)

for name in ("adapter_openapi.yaml", "adapter_openapi-itb.yaml",
             "tier1-gateway/t1-adapter-openapi.yaml",
             "tier2-gateway/t2-adapter-openapi.yaml"):
    path = Path("openapi", name).resolve()
    document = yaml.safe_load(path.read_text())
    OpenAPIV30SpecValidator(document, base_uri=path.as_uri()).validate()
    print(f"{name}: valid OpenAPI 3.0, with local references resolved")
PY
```

Also resolve every local `$ref` recursively and validate the resolved documents;
check unique operation IDs in the composed root and verify that its only added
path is the Tier-1 GET. Validate the new operation's parameter and response
examples against resolved schemas with `OAS30Validator`, using `regex` for
`pattern` checks without modifying contract patterns. Check missing/empty
catalogue selections, empty identifiers, search-string boundaries, negative
quality scores, and pagination defaults/capping. Compare the Tier-2 document,
Tier-1 advanced-search path, and common components with the baseline; unchanged
legacy examples are outside this change's example-validation scope.

The YAML, local references, and examples support automated validation, contract
sharing, and mock/client-stub generation. Generation with locally available
OpenAPI/Microcks tooling is optional evidence; do not claim generation if not
performed. ITB checks require separately authorized access; manually review the
ticket's guideline rules when ITB is unavailable and do not claim ITB compliance.
This specification-only change requires no Java build or production-code
coverage. The files are versioned in this repository; GitLab publication remains
pending an authorized push.
