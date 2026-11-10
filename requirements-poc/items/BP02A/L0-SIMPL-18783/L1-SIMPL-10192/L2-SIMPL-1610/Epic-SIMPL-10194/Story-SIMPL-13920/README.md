> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10192](../../../README.md) / [L2-SIMPL-1610](../../README.md) / [Epic-SIMPL-10194](../README.md) / **Story-SIMPL-13920**

# SIMPL-13920: [UI] Prevent Governance Authority from updating or creating built-in identity attributes

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10192](../../../README.md) (L1) › [SIMPL-1610](../../README.md) (L2) › [SIMPL-10194](../README.md) (Epic) › **SIMPL-13920** (User story)

## Text

**As a** user responsible for managing data space identity attributes (`{}IATTR_M{`} role), viewing the list of identity attributes in the data space,\
**I want** to be prevented from editing built-in identity attributes,\
**So that** I ensure that built-in identity attributes remain unchanged and the data space remains compliant with the SIMPL-Open standard.

**Acceptance Criteria**

1. **Given** that I am a user with IATTR_M role, responsible for managing identity attributes\
   **When** viewing the list of identity attributes\
   **Then** I want the edit and delete button to be hidden when an identity attribute is built-in *(see attached screenshot)*

2. **Given** that I am a user with IATTR_M role, responsible for managing identity attributes\
   **When** viewing the list of identity attributes\
   **Then** I want to be able to filter by built-in identity attributes

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10194](../README.md) (Epic) Setup default identity attributes in the Governance Authority Agent |
