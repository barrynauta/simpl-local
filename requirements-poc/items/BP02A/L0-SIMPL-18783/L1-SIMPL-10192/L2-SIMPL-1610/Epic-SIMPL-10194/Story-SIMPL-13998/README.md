> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10192](../../../README.md) / [L2-SIMPL-1610](../../README.md) / [Epic-SIMPL-10194](../README.md) / **Story-SIMPL-13998**

# SIMPL-13998: [API] Prevent Governance Authority from updating or creating built-in identity attributes

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10192](../../../README.md) (L1) › [SIMPL-1610](../../README.md) (L2) › [SIMPL-10194](../README.md) (Epic) › **SIMPL-13998** (User story)

## Text

**As a** user responsible for managing data space identity attributes (`{}IATTR_M{`} role)\
**I want** to be prevented from editing built-in identity attributes,\
**So that** I ensure that built-in identity attributes remain unchanged and the data space remains compliant with the SIMPL-Open standard.

**Acceptance Criteria**

1. **Given** that I am a user with the `IATTR_M` role, wanting to delete an identity attribute\
   **And** the identity attribute corresponds to a built-in attribute\
   **When** I DELETE `{}/identityAttributes/{identityAttributeId{`}}\
   **Then** I want to receive a 400 Bad Request error stating "A built-in identity attribute cannot be deleted"

2. **Given** that I am a user with the `IATTR_M` role, wanting to update an identity attribute\
   **And** the identity attribute corresponds to a built-in attribute\
   **When** I PUT `{}/identityAttributes/{identityAttributeId{`}}\
   **Then** I want to receive a 400 Bad Request error stating "A built-in identity attribute cannot be updated"

3. **Given** that I am a user with the `IATTR_M` role, wanting to create an identity attribute\
   **When** I POST to `/identityAttributes`\
   **Then** the the built-in flag is not part of the allowed input parameters and is ignored

4. **Given** that I am a user with the `IATTR_M` role, wanting to import a list of identity attributes\
   **When** I POST to `/identityAttributes/import`\
   **Then** the the built-in flag is not part of the allowed input parameters and is ignored

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10194](../README.md) (Epic) Setup default identity attributes in the Governance Authority Agent |
