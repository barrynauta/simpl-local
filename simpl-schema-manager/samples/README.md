# Sample schemas

Three canonical SHACL shape files, one per resource type, copied verbatim from
the upstream `simpl-schema-manager` test fixtures
(`src/test/resources/shacl/validation/{data,application,infrastructure}/...-offeringShape.ttl`).
They are the same shapes the upstream JUnit `ShaclValidationServiceTest`
asserts as valid, so the local stack will accept them.

| File | Resource type to enter in the UI | Top-level shape it must declare |
|---|---|---|
| `sample-data-offering.ttl` | `Data` | `gax-validation:DataOfferingShape` |
| `sample-application-offering.ttl` | `Application` | `gax-validation:ApplicationOfferingShape` |
| `sample-infrastructure-offering.ttl` | `Infrastructure` | `gax-validation:InfrastructureOfferingShape` |

## What to type in the upload form

Form fields (from `SchemaController.createSchema`):

| Field | Constraint | Example |
|---|---|---|
| Schema file | `.ttl` only; content-type `text/plain` or `text/turtle` | `sample-data-offering.ttl` |
| Name | PascalCase, 3–64 chars (regex `^\p{Lu}\p{Alnum}*$`) | `SimplDataOffering` |
| Title | free text | `Simpl Data Offering Schema` |
| Description | free text | `Canonical data offering schema (local stack sample).` |
| Resource Type | exactly `Application`, `Data`, or `Infrastructure` (case-insensitive) | `Data` |

After upload you should see the schema appear at
[http://localhost:4322](http://localhost:4322), and `GET
http://localhost:4322/v1/schemas` will return a non-empty list.

## How these files get here

The samples are **not committed** — they are copied from `repos/` by
`start.sh` (or by hand) after the upstream repo is cloned. This keeps
upstream test fixtures out of this repo's git history while still making
them one click away in the UI's file picker.

If `samples/` is empty after `./start.sh`, run:

```bash
cp repos/simpl-schema-manager/src/test/resources/shacl/validation/data/data-offeringShape.ttl                 samples/sample-data-offering.ttl
cp repos/simpl-schema-manager/src/test/resources/shacl/validation/application/application-offeringShape.ttl   samples/sample-application-offering.ttl
cp repos/simpl-schema-manager/src/test/resources/shacl/validation/infrastructure/infrastructure-offeringShape.ttl samples/sample-infrastructure-offering.ttl
```

## Upstream license

These files are EUPL-1.2 and originate from
`gaia-x-edc/simpl-schema-manager` on `code.europa.eu`. Treat any copies
in `samples/` as upstream artefacts, not local-stack code.
