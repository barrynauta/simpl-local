> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14687](../../../README.md) / [L2-SIMPL-14747](../../README.md) / [Epic-SIMPL-982](../README.md) / **Story-SIMPL-488**

# SIMPL-488: Syntax Validation Engine

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14687](../../../README.md) (L1) › [SIMPL-14747](../../README.md) (L2) › [SIMPL-982](../README.md) (Epic) › **SIMPL-488** (User story)

## Text

**As a** Governance Authority agent\
**I want** integrate a validation engine that is able to validate the syntax of self-descriptions.\
**So that** I can verify if a self-description adheres to the rules and structure defined by the JSON and JSON-LD specifications

**Technical Description:**

Customise Compliance service of GX to implement validation service for JSON-LD (GA Node) . SimplRegistry and SimplComplianceService with NotaryService creating a Compliance Credential in the name of the GovernanceAuthority

[https://gitlab.com/gaia-x/lab/compliance/gx-compliance/-/blob/development/README-api.md](https://gitlab.com/gaia-x/lab/compliance/gx-compliance/-/blob/development/README-api.md)

Repo:

[https://gitlab.com/gaia-x/lab/compliance/gx-compliance/-/tree/development](https://gitlab.com/gaia-x/lab/compliance/gx-compliance/-/tree/development)

AC

1. The validation engine successfully validates the resource file against the constraints
1. The validation result is parsed correctly
1. The validation result indicates whether the resource file adheres to the constraints or not
1. If any syntax errors are found, review the error messages provided by the validation engine to understand the issue

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-982](../README.md) (Epic) The Governance Authority checks the syntax of a new Resource description or a new version of a Resource Description |
