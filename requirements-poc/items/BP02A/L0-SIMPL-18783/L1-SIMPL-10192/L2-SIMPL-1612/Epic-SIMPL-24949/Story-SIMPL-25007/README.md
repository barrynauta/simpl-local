> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10192](../../../README.md) / [L2-SIMPL-1612](../../README.md) / [Epic-SIMPL-24949](../README.md) / **Story-SIMPL-25007**

# SIMPL-25007: [API] Governance authority manages public identity attributes

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10192](../../../README.md) (L1) › [SIMPL-1612](../../README.md) (L2) › [SIMPL-24949](../README.md) (Epic) › **SIMPL-25007** (User story)

## Text

**As a** user with *Tier 2 identity attributes manager* (T2IAA_M) role in the Dataspace Governance Authority\
**I want** to view and mark each of my custom identity attributes as public or non-public\
**So that** I can control which attributes are visible to external Governance Authorities and therefore available for federation mapping.

**Acceptance Criteria**

1. **Given** that I am logged in with the T2IAA_M role\
   **When** I call GET tier1/v2/identityAttributes\
   **And** the response status is 200 OK\
   **Then** each identity attribute in the returned list includes a non-null field named "public" of type boolean

2. **Given** that I am logged in with the T2IAA_M role\
   **When** I call POST tier1/v2/identityAttributes to create a new custom identity attribute\
   **And** I include the "public" field in the request\
   **And** the response status is 201 Created\
   **Then** the created identity attribute includes the "public" field with the value provided in the request

3. **Given** that I am logged in with the T2IAA_M role\
   **When** I call PUT tier1/v2/identityAttributes/{identityAttributeId} to update a custom identity attribute\
   **And** I include the "public" field in the request\
   **And** the response status is 204 No Content\
   **Then** the updated identity attribute includes the "public" field with the value provided in the request

4. **Given** that I am logged in with the T2IAA_M role\
   **When** I call POST tier1/v2/identityAttributes to create a new custom identity attribute\
   **And** the "public" field is null\
   **And** the response status is 400 Bad Request

5. **Given** that I am logged in with the T2IAA_M role\
   **When** I call PUT tier1/v2/identityAttributes/{identityAttributeId} to update a custom identity attribute\
   **And** the "public" field is null\
   **And** the response status is 400 Bad Request

6. **Given** that I am logged in with the T2IAA_M role\
   **When** I call POST tier1/v2/identityAttributes/import to create a list of new custom identity attributes\
   **And** the "public" field is null in at least one item in the list\
   **And** the response status is 400 Bad Request

7. **Given** that I am a participant in the dataspace\
   **When** I call GET tier2/v2/identityAttributes to get the list of identity attribute of the dataspace\
   **Then** each identity attribute in the returned list includes a non-null field named "public" of type boolean

8. **Given** that I am a participant in the dataspace\
   **When** I call GET tier2/v2/credentials/{credentialId}/identityAttributes to get the list of identity attribute of another participant by credential\
   **Then** each identity attribute in the returned list includes a non-null field named "public" of type boolean

9. **Given** that I am a participant in the dataspace\
   **When** I call GET tier2/v2/credentials/{credentialId}/identityAttributes to get the list of identity attribute of another participant by participantId\
   **Then** each identity attribute in the returned list includes a non-null field named "public" of type boolean

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-24949](../README.md) (Epic) Governance Authority marks identity attributes as public |
