# simpl-infrastructure-provisioner

Local-evaluation stack for the **Crossplane Infrastructure Provisioner**
(`development/infrastructure/infrastructure-provisioner`), the control plane behind
BP08 infrastructure consumption. Unlike the other simpl-local stacks this one is
**Kubernetes based** (Helm on OrbStack's built-in k8s), because the component has no
Docker Compose form at all: its only install path is Helm charts on a cluster.

## Purpose of the experiment

Upstream claims the infrastructure components have installation guides; validation
(2026-07-19) showed the provisioner's guide is a **cluster** guide requiring live
cloud credentials (IONOS/OVH), and its own text admits: *"the charts have to be
installed as-is with all their listed dependencies... Decoupling of dependencies is
still a work in progress!"*

This stack measures **how far the provisioner control plane gets on a 16 GB laptop
without real cloud accounts**: dependencies (Crossplane, ArgoCD, Argo Events,
Argo Workflows, Gitea, FluxCD), tofu-controller, then the resources chart with
dummy IONOS/OVH tokens. Success criterion is not a provisioned VM (impossible and
undesirable locally); it is a precise map of which parts run, which parts fail, and
why: the modularity answer in concrete form.

## Prerequisites

- OrbStack with Kubernetes enabled (`orbctl config set k8s.enable true`, restart OrbStack)
- `kubectl`, `helm`, `rsync`
- Source checkout at `../../gitlab/development/infrastructure/infrastructure-provisioner`
  (or clone it into `repos/`; see start.sh section 3)

## Usage

```
./start.sh                  # dependencies + tofu-controller + resources (dummy creds, no Kafka)
./start.sh --with-kafka     # additionally install a local SASL-PLAIN Kafka and wire the event sources to it
./start.sh --skip-resources # only the dependency layer
./stop.sh                   # uninstall stack releases, keep namespace and PVCs
./stop.sh --purge           # also delete the namespaces

./drive-provision.sh [uuid] # produce a crafted provisioning request (needs --with-kafka)
```

Deviation from simpl-local convention: `start.sh` copies source from the local
`~/src/gitlab` checkout instead of git-cloning (rsync, `.git` excluded). A fresh
clone works identically; the command is printed in start.sh.

## Deliberate limitations

- **Dummy cloud credentials.** IONOS/OVH tokens are placeholders. Anything past
  "provider tries to authenticate" is out of scope; that is the boundary being measured.
- **No Kafka by default.** The Argo Events Kafka sources point at a non-resolving
  endpoint; their failure mode is a finding, not a bug in the stack. Use
  `--with-kafka` to test the event path with a real local broker.
- **Existing cluster state.** This machine's OrbStack cluster carries a pre-existing
  ArgoCD (namespace `argocd`) plus April-era experiment workloads
  (`catalogue-test`, `dataorchestrator`, `common01`). The dependencies chart is
  therefore installed with `argo-cd.crds.install=false`. A replica snapshot of those
  workloads (for restore) is in the findings section.

## Upstream findings (2026-07-19, pre-run)

| # | Finding | Where |
|---|---|---|
| F1 | InstallGuide references `charts/resources`, which does not exist; the resources chart is the root `charts/` directory | `documents/InstallGuide.md` vs `charts/` |
| F2 | Root chart version is the literal `${PROJECT_RELEASE_VERSION}` CI placeholder; invalid semver, `helm install` fails unpatched. Same pattern as the SD-tooling poms | `charts/Chart.yaml` |
| F3 | `values.env.yaml` files are not valid values files but CI sed templates (`key: #PLACEHOLDER`); following the guide verbatim installs empty/null values | `charts/values.env.yaml`, `charts/dependencies/values.env.yaml` |
| F4 | Guide's own note: dependency decoupling "still a work in progress"; you cannot bring your own Gitea/ArgoCD instance | `documents/InstallGuide.md` note 2 |
| F5 | No local installation path exists or is claimed in-repo; the guide requires a cluster plus IONOS/OVH credentials | `documents/InstallGuide.md` |

Run findings are appended below as the experiment progresses.

## Run findings (2026-07-19)

| # | Finding | Detail |
|---|---|---|
| F6 | Guide install order fails on a pristine cluster | The resources chart contains `ProviderConfig` CRs (IONOS, OVH) whose CRDs are installed asynchronously by Crossplane after it pulls the configuration package. First `helm install` fails with "ensure CRDs are installed first". start.sh adds the missing wait step. |
| F7 | `giteaUrl` format undocumented and scheme-intolerant | The chart's configmap prepends `http://` itself; supplying an actual URL (with scheme) makes the setup job curl host "http". Must be `host:port`. |
| F8 | Registry images pull anonymously | The configuration package, gitops and observer images on `code.europa.eu:4567` need no PAT, contrary to the BE install guide's PAT instructions for its images. |
| F9 | Transient `ErrImagePull` bursts on first install | Kubelet "pull QPS exceeded" during the ~19-pod burst; self-heals within a minute. Cosmetic, but alarming without context. |
| F10 | Event mesh runs without Kafka | STAN eventbus (3 pods), all four event sources and sensors, and the observer settle to Running even with a non-resolving Kafka endpoint; Kafka sources retry internally. Early CrashLoops were startup ordering against the not-yet-ready eventbus, not the missing broker. |
| F11 | ArgoCD manager apps in error until first provision (resolved: lazy-by-design) | `crossplane-manager` and `terraform-manager` point at repo paths that only the FIRST provisioning workflow creates (`setup_directories` in `workflows/gitops/operation/provision.py`, paths from `constants.DIRECTORIES`). Fresh install steady state = ComparisonError "app path does not exist" until first use; cosmetic but poisons monitoring/acceptance checks. The setup job's `applications/` and `claims/` seeds are vestigial (nothing references them). Not a code bug; worth an upstream note, plus: the shipped example claim (`package/examples/compute/ionos.yaml`) does not pass the message validator (`metadata` has `namespace`, lacks `labels.provider`). |
| F12 | Bitnami Kafka image purged from Docker Hub | `bitnami/kafka:4.0.0-debian-12-r10` returns manifest unknown (2025 Bitnami relocation); local stack overrides `image.repository=bitnamilegacy/kafka`. Local-stack concern only, not upstream. |

### Environment notes

- OrbStack k8s had been previously enabled on this machine and carried dormant April-era
  workloads (namespaces `argocd`, `catalogue-test`, `dataorchestrator`, `common01`).
  These were scaled to 0 (cnpg clusters hibernated) before the run; replica snapshot for
  restore: `k8s-replica-snapshot-2026-07-19.txt` (session scratchpad; all replicas were 1).
  Restore: scale everything back to 1 and set `cnpg.io/hibernation=off`.
- `argo-cd.crds.install=false` because the cluster already had ArgoCD CRDs.

## Run log

- 16:27 run 1: dependencies + tofu-controller deployed clean; resources install failed on F6 (no CRD wait).
- 16:29 run 2: CRD wait added and passed; resources applied but post-install hook failed on F7 (gitea URL format), then the release was left in failed state by an operator-induced race (job deleted mid-hook-watch).
- 16:35 run 3: gitea URL fixed (`host:port`), all three releases deployed, `gitea-setup-job` Completed, **26/26 pods Running and Ready**, start.sh exits 0. Steady state: ArgoCD manager apps in ComparisonError (F11); Kafka event sources retry silently against the dummy endpoint (F10).

## Verdict on local runnability and modularity (2026-07-19)

The provisioner **control plane runs fully on a 16 GB laptop** with zero real cloud
credentials: Crossplane + both cloud providers (pulled anonymously from the
code.europa.eu registry), the ArgoCD/Argo Events/Argo Workflows/Gitea/Flux/tofu
dependency mesh, the STAN eventbus, all event sources and sensors, and the observer.
Cloud credentials are only touched when a claim actually reaches a provider, so the
"needs IONOS/OVH accounts" barrier in the InstallGuide applies to *provisioning*, not
to *installation*: a local install path clearly could exist, it just is not documented.

Modularity, however, is as upstream admits: the chart brings its own ArgoCD, Gitea,
and Flux and cannot adopt existing instances (F4); on this cluster only the CRD-level
conflict with a pre-existing ArgoCD was avoidable (`crds.install=false`). Five
guide/chart defects (F1-F3, F6, F7) had to be patched or worked around to get here,
which is the real answer to "does each component have a local installation guide".

### Phase 2: full provisioning loop driven (2026-07-19)

`./start.sh --with-kafka` then `./drive-provision.sh` verified the complete chain
end to end on this laptop:

1. Crafted request produced to Kafka `to-provision` (headers: `scriptTriggerId`;
   body: single-line JSON claim satisfying the gitops validator; see drive-provision.sh).
2. Argo Events Kafka source picked it up (headers propagate correctly), sensor
   submitted workflow `provision-<id>`, which **Succeeded in ~30 s**: the gitops
   image (pulled anonymously from code.europa.eu) committed and pushed
   `crossplane-argocd-applications/<uuid>/claim-<uuid>.yaml` to management-repo and
   `crossplane-claims/<uuid>/...` to data-repo.
3. `crossplane-manager` synced (F11 error state cleared on first provision, as coded),
   created `application-<uuid>`, which synced the claim from data-repo.
4. Claim → composite (`ionos-enterprise-server` composition) → managed `Datacenter`
   resource, which hit the real IONOS API and failed exactly at the intended boundary:

   `create failed: failed to create datacenter. error: 401 Unauthorized`

**Conclusion:** every part of the provisioner works locally except the paid cloud
call itself. The InstallGuide's implied "requires cloud accounts" applies only to
that final API call. Decommission path not driven (would need the provision to have
succeeded upstream of it); event sources for decommission are connected and idle.
