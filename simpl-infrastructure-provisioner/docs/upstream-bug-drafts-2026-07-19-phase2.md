# Upstream bug drafts: infrastructure-provisioner, phase 2 (full-loop drive)

Drafted 2026-07-19 after driving the complete provisioning loop locally (see
`../README.md`, "Phase 2"). Environment as in the first batch: pristine local
Kubernetes (OrbStack/k3s, Helm 3), unmodified checkout of
`simpl/simpl-open/development/infrastructure/infrastructure-provisioner`,
configuration package `1.2.1`, dummy IONOS/OVH credentials, local SASL Kafka.
The loop itself works: Kafka request -> event source -> sensor -> workflow
(~30 s, Succeeded) -> Gitea push -> ArgoCD sync -> claim -> composite -> managed
resource -> real IONOS API `401 Unauthorized` at the dummy-credential boundary.
Both bugs below were found by that run, not by code reading alone.

---

## Bug 5 (F11): Setup job seeds vestigial directories; fresh installs show a permanent ArgoCD error state until first use

**Component:** `infrastructure-provisioner` `charts/templates/configmap.yaml` (gitea-setup-job script) vs `workflows/gitops/constants.py`
**Severity proposal:** Low (functional self-heal on first use, but fresh installs are indistinguishable from broken ones)

**Steps to reproduce**

1. Install dependencies, tofu-controller, and resources charts on a pristine cluster.
2. Wait for the `gitea-setup-job` to complete and ArgoCD to reconcile.
3. Inspect the `crossplane-manager` and `terraform-manager` Applications before any
   provisioning request has been processed.

**Expected:** a freshly installed, healthy system reports a healthy state.

**Actual:** both manager Applications sit in
`ComparisonError: app path does not exist` indefinitely, because they reference
`crossplane-argocd-applications/` and `terraform-fluxcd-runners/` in
`management-repo`, which are created only by the first provisioning workflow
(`setup_directories` in `workflows/gitops/operation/provision.py`, paths from
`constants.DIRECTORIES`).

**Root cause:** drift between the setup job and the workflow constants. The
`gitea-setup-job` seeds `applications/` plus `data-application-template.yaml` in
management-repo and `claims/` in data-repo; nothing in the codebase references any
of the three (the ArgoCD Application template used at runtime is baked into the
gitops image under `templates/`, not read from the repo). Meanwhile the four
directories the system actually uses (`crossplane-argocd-applications`,
`terraform-fluxcd-runners` in management-repo; `crossplane-claims`,
`terraform-configurations` in data-repo) are not seeded.

**Impact:** monitoring, smoke tests, or acceptance checks on a fresh install
report a degraded system with no way to distinguish "healthy, not yet used" from
"broken". Verified live: the error state cleared the moment the first provisioning
workflow pushed the paths.

**Proposed fix:** seed the four real directories (with `.empty` markers, exactly as
`setup_directories` does) in the setup job, and remove the vestigial
`applications/`, `claims/`, and `data-application-template.yaml` seeds. Four-line
change in the configmap script.

---

## Bug 6 (message contract): Kafka provisioning trigger contract is undocumented and the only shipped examples fail its validator

**Component:** `infrastructure-provisioner` documentation + `workflows/gitops/parse/crossplane.py` + `package/examples/`
**Severity proposal:** Low to Medium (integration surface must be reverse-engineered from parser source; misleading examples)

**Detail**

The provisioning entry point of the whole component is a Kafka message, yet its
contract appears nowhere in `documents/` or the values files:

- topic: `to-provision` (values: `kafkaProvisioningRequestsTopicName`), partition 0 only
- Kafka **headers**: `scriptTriggerId` (becomes the UUID in repo paths, claim name,
  and workflow labels; required, no fallback) and `provisioningMechanism`
  (optional, defaults to `crossplane` via the sensor parameter fallback)
- body: a single YAML document, validated by
  `workflows/gitops/parse/crossplane.py::validate_structure` with **exact key-set
  equality**: top level exactly `{apiVersion, kind, spec, metadata}`, `spec`
  exactly `{parameters}`, `metadata` exactly `{name, labels}` with `labels`
  containing `provider` in `{ionos, ovh}`

Two consequences (the contract itself was verified live with a passing crafted
message; the example rejection below follows from the validator code):

1. **The shipped examples do not pass the validator.** `package/examples/compute/ionos.yaml`
   carries `metadata.namespace` and lacks `metadata.labels.provider`; per
   `validate_structure`, metadata keys must be exactly `[name, labels]`, so as a
   message body it is rejected. The examples are valid claim CRs for direct
   `kubectl apply`, but nothing tells the reader they are not valid message
   bodies, and no valid message example exists anywhere in the repo.
2. **The exact-equality rule makes the interface brittle.** Any producer that adds
   an innocuous key (an annotation, a namespace, an extra label section) is
   rejected. This is the integration surface for `infrastructure-be`; contract
   changes on either side will fail silently at runtime, in the workflow, far from
   the producer.

**Proposed fix:** add a "Message contract" section to the InstallGuide or a
dedicated document (topic, headers, body schema, validation semantics, one
copy-paste example that passes; a working example payload can be provided from the
local-evaluation stack, see `drive-provision.sh` in simpl-local). Optionally relax
`validate_structure` to subset semantics for `metadata`, or at least emit the
allowed structure in the rejection message.
