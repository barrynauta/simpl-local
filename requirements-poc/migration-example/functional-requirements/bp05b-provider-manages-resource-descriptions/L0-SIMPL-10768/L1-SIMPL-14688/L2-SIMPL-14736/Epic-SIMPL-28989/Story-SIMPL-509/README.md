> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../../README.md) · [What the levels mean](../../../../../../../../levels.md) · [All items](../../../../../../../../all-items.md) · [By business process](../../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14736](../../README.md) / [Epic-SIMPL-28989](../README.md) / **Story-SIMPL-509**

# SIMPL-509: UI for assigning a defining billing schemas [Placeholder]

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14736](../../README.md) (L2) › [SIMPL-28989](../README.md) (Epic) › **SIMPL-509** (User story)

## Text

Disclaimer: The User Story is intended to link resources that are referred as a placeholder for the billing schema, which is yet to be defined.

**As a** Provider User\
**I want to** use an UI to set the reference to the billing schema to the self-description\
**So that** I can set the billing schema for my dataset/application/infrastructure.

**Technical Specification**

The UI of the SD Tooling will be extended by displaying another field, where a URL must be inserted. The extension of the attributes is done accordingly in the Schema File where the Structure of the Self-Descriptions is based on. The Hash Value that has been created based on the URL (SIMPL-510 (internal page)) will not be displayed to the end user, it will be contained in the Json-ld after saving / signing only. Another field shall contain the hash algorithm that has been used.

AC:

1. Check if the URL field is present on the SD Tooling UI

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-28989](../README.md) (Epic) A Provider manages contract clauses [MVP] |
