# OpenAPI Layout

This directory uses a composed OpenAPI structure. The root specs do **not**
define endpoints themselves — they only wire the API together through `$ref`
references. The actual operation definitions live in the tier files, and the
shared schemas/responses live in `common/`.

- `fc_openapi.yaml` and `fc_openapi-itb.yaml` are the active root specs. Each
  contains only top-level metadata (`info`, `servers`, `security`, `tags`) and a
  `paths` map where **every path is a `$ref`** pointing to a path-item in a tier
  file. No endpoint is described inline in the root spec.
- `tier1-gateway/` and `tier2-gateway/` contain the real path definitions
  (operations, parameters, request/response bodies) that the root specs
  reference.
- `common/` contains shared components (`schemas`, `responses`, `parameters`,
  `securitySchemes`, …) referenced by the tier and root specs.
- `fc_openapi-legacy.yaml` and `fc_openapi-itb-legacy.yaml` are preserved legacy
  snapshots.

In short: **root spec = references only → tier files = operations → common =
shared components.** Tools resolve the `$ref`s to assemble the full API surface.

## Contract

- Root specs must represent the full API surface used by code generation.
- Tier files are used for gateway-specific exposure and composition.
- Common files hold reusable `components` (`schemas`, `responses`, `parameters`, `securitySchemes`, etc.).
- No endpoint should be invented outside the canonical legacy definitions.

## Validation

Run from repository root:

```bash
./mvnw -f pom.xml -DskipTests generate-sources
```

Expected result: `BUILD SUCCESS`.

## Notes

- Path-item `$ref` is used in root specs to reference tier files.
- Operation-level `$ref` can cause generator validation issues in this project/toolchain.
