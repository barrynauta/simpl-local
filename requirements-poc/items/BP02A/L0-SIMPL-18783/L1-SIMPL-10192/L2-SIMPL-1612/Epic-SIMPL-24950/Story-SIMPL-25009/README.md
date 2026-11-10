> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10192](../../../README.md) / [L2-SIMPL-1612](../../README.md) / [Epic-SIMPL-24950](../README.md) / **Story-SIMPL-25009**

# SIMPL-25009: [API] Governance authority manages federable identity attributes

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10192](../../../README.md) (L1) › [SIMPL-1612](../../README.md) (L2) › [SIMPL-24950](../README.md) (Epic) › **SIMPL-25009** (User story)

## Text

*This user story improves the APIs related to identity attributes. Refer to SIMPL-720 for an overview of the initial v1 endpoints (already deprecated), and to SIMPL-25198, SIMPL-14569 for the v2 impacted endpoints.*\
----\
**As A**

user with *Tier 2 identity attributes manager* (T2IAA_M) role in the Dataspace Governance Authority

**I want**

to view and mark each of my custom identity attributes as federable or not federable

**So that**

I can control which attributes are visible to external Governance Authorities and therefore available for federation mapping.

**Acceptance Criteria**

1. **GIVEN** that I am logged in with the T2IAA_M role\
   **WHEN** I call GET /identityAttributes\
   **AND** the response status is 200 OK\
   **THEN** each identity attribute in the returned list includes a non-null field named "federable" of type boolean
1. **GIVEN** that I am logged in with the T2IAA_M role\
   **WHEN** I call POST /identityAttributes to create a new custom identity attribute\
   **AND** I include the "federable" field in the request\
   **AND** the response status is 201 Created\
   **THEN** the created identity attribute includes the "federable" field with the value provided in the request
1. **GIVEN** that I am logged in with the T2IAA_M role\
   **WHEN** I call PUT /identityAttributes/{identityAttributeId} to update a custom identity attribute\
   **AND** I include the "federable" field in the request\
   **AND** the response status is 204 No Content\
   **THEN** the updated identity attribute includes the "federable" field with the value provided in the request
1. **GIVEN** that I am logged in with the T2IAA_M role\
   **WHEN** I call POST tier1/v2/identityAttributes to create a new custom identity attribute\
   **AND** the "federable" field is null\
   **AND** the response status is 400 Bad Request
1. **GIVEN** that I am logged in with the T2IAA_M role\
   **WHEN** I call PUT tier1/v2/identityAttributes/{identityAttributeId} to create a new custom identity attribute\
   **AND** the "federable" field is null\
   **AND** the response status is 400 Bad Request
1. **GIVEN** that I am logged in with the T2IAA_M role\
   **WHEN** I call POST tier1/v2/identityAttributes/import to create a list of new custom identity attributes\
   **AND** the "federable" field is null in at least one item in the list\
   **AND** the response status is 400 Bad Request
1. **GIVEN** that I am a participant in the dataspace\
   **WHEN** I call GET tier2/v2/identityAttributes to get the list of identity attribute of the dataspace\
   **THEN** each identity attribute in the returned list includes a non-null field named "federable" of type boolean
1. **GIVEN** that I am a participant in the dataspace\
   **WHEN** I call GET tier2/v2/credentials/{credentialId}/identityAttributes to get the list of identity attribute of another participant by credential\
   **THEN** each identity attribute in the returned list includes a non-null field named "federable" of type boolean
1. **GIVEN** that I am a participant in the dataspace\
   **WHEN** I call GET tier2/v2/credentials/{participantId}/identityAttributes to get the list of identity attribute of another participant by participantId\
   **THEN** each identity attribute in the returned list includes a non-null field named "federable" of type boolean

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-24950](../README.md) (Epic) Governance Authority marks identity attributes as federable |
