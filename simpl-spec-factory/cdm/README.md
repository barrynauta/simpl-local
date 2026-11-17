# CDM snapshot

The CDM register of Simpl-Open (`cdm-register/cdm-index.md` in the architecture repository, commit `5d28e04a`) and every CDM it links to, plus the CDMs found in the repositories that the register does not list. Frozen on **17 Nov 2026** by [`../pipeline/collect_cdms.py`](../pipeline/collect_cdms.py), with corrected locations in [`register.yaml`](./register.yaml). [`SOURCES.json`](./SOURCES.json) has, per CDM, the branch, the pinned commit and a sha256. [`scan.json`](./scan.json) is the repository scan ([`../pipeline/scan_cdms.py`](../pipeline/scan_cdms.py)). [`inventory.md`](./inventory.md) is the extraction (step 12). Do not refresh in place: a new snapshot goes to a new folder.

What the snapshot shows (17 Nov 2026):

- **26 CDMs: 23 in the register, 3 found only in the repositories** (Infrastructure BE; a second Schema Manager model, `schema-default-properties-dm.md`, a flowchart rather than a class diagram; Policy Manager BE, `openapi/cdm.yaml`).
- **All register entries are "In review", none approved for delivery** (API-01 and DATA-01 ask for a CDM with every API).
- **7 register links do not resolve, but the CDMs exist.** Five point to the branch `feature/S36-cdm-models`, which no longer exists; the files are on `feature/add-cdm` (EDC Connector Adapter, Schema Sync Adapter, SD Tooling BE, SD Validation BE, Search Client BE). Contract Consumption BE is also on `feature/add-cdm`. Identity Provider BE is linked as `identity-provider-cmd.md`; the file is `identity-provider-cdm.md` on `main`.
- **Most CDMs live only on feature branches.** Of the 26, the CDMs of the IAA and C&B teams, Consent, Infrastructure and Schema Manager are on `main` or `develop`; those of the C&C and Data01 teams are on `feature/add-cdm` only, so they are not in a release.
- **The scan found CDM files in 25 of 301 public projects** (the obsolete `development/` subgroup skipped; only `documents/architecture/cdm/` and CDM files next to the OpenAPI, on the default branch, `develop` and branches named after a CDM).
- **The register's "CDM Concept Ownership and Reference Index"** is linked but not in the repository.
- **The Contract Management CDMs** (C&B team) link their API definitions to the old `development/contract-billing` repositories and define no value sets. The Contract UI BE CDM has one class.
