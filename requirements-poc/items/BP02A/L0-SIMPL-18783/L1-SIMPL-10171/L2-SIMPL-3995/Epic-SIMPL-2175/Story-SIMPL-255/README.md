> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10171](../../../README.md) / [L2-SIMPL-3995](../../README.md) / [Epic-SIMPL-2175](../README.md) / **Story-SIMPL-255**

# SIMPL-255: [UI] Onboarding administration dashboard - access denied

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10171](../../../README.md) (L1) › [SIMPL-3995](../../README.md) (L2) › [SIMPL-2175](../README.md) (Epic) › **SIMPL-255** (User story)

## Text

**As a** dataspace governance authority user without the T2IAA_M role\
**I want to** access the administration dashboard of the Public Dataspace Onboarding Site through keycloak configured into my agent (Tier 1)\
**So that** I cannot see into the administration dashboard

Linked Sovereign X US ID: ARU_US_001

**Acceptance Criteria:**

1. **A dataspace governance authority user accesses the keycloak login page for the public dataspace onboarding site by clicking on the login button.**
1. A dataspace governance authority user fills the login form composed of username and password{}
1. **A dataspace governance authority user without T2IAA_M role** **access to the administration panel of the Public Dataspace Onboarding Site** **is rejected.**

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-2175](../README.md) (Epic) Onboarding Process Governance Framework - resources/documents |
