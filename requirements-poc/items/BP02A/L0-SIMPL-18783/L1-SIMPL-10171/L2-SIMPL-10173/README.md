> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../README.md) · [What the levels mean](../../../../../levels.md) · [All items](../../../../../all-items.md) · [By business process](../../../../../processes.md)

Folder: [items](../../../../README.md) / [BP02A](../../../README.md) / [L0-SIMPL-18783](../../README.md) / [L1-SIMPL-10171](../README.md) / **L2-SIMPL-10173**

# SIMPL-10173: Configure a ruleset for the automatic validation of onboarding request documents

**L2 requirement (detailed requirement)**

**Trace:** [BP02A](../../../README.md) › [SIMPL-18783](../../README.md) (L0) › [SIMPL-10171](../README.md) (L1) › **SIMPL-10173** (L2)

## Text

Simpl shall support the configuration of validation rules to enable the automated validation of documents uploaded during participant onboarding.

Simpl shall, at the least, enable the following rules:

- rules to check whether a document has been uploaded.
- rules to validate the content of a document (delegated to an external validation service based on a well-established API contract)
- Any combination of the above.

By defining these types of rules, the governance authority can:

- define the conditions for automatic approval or rejection of onboarding requests.
- limit the number of onboarding requests to be validated manually.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10171](../README.md) (L1) Configure Onboarding Procedure Templates |
| **Children** | [SIMPL-8337](Epic-SIMPL-8337/README.md) (Epic) Governance authority creates content check rules for automated document validation<br>[SIMPL-10507](Epic-SIMPL-10507/README.md) (Epic) Governance authority creates presence check rules to check if a document is uploaded<br>[SIMPL-10508](Epic-SIMPL-10508/README.md) (Epic) Governance Authority creates composite rules with custom logical conditions for document validation<br>[SIMPL-12326](Epic-SIMPL-12326/README.md) (Epic) Governance authority deletes a validation rule<br>[SIMPL-19153](Epic-SIMPL-19153/README.md) (Epic) Governance authority creates eIDAS rules for automated onboarding request validation<br>[SIMPL-24264](Epic-SIMPL-24264/README.md) (Epic) Governance authority deletes validation rules used in existing onboarding requests |
