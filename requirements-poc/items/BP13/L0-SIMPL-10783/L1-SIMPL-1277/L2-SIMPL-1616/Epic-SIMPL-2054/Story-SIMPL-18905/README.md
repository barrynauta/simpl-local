> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP13](../../../../../README.md) / [L0-SIMPL-10783](../../../../README.md) / [L1-SIMPL-1277](../../../README.md) / [L2-SIMPL-1616](../../README.md) / [Epic-SIMPL-2054](../README.md) / **Story-SIMPL-18905**

# SIMPL-18905: [API] Dataspace participant agent validates a Tier 1 credential

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP13](../../../../../README.md) › [SIMPL-10783](../../../../README.md) (L0) › [SIMPL-1277](../../../README.md) (L1) › [SIMPL-1616](../../README.md) (L2) › [SIMPL-2054](../README.md) (Epic) › **SIMPL-18905** (User story)

## Text

*+**Epic for this story is SIMPL-2054, not inserted due to lack of permissions. PSO to kindly update upon review.**+*

*This user story outlines the adaptation and migration of the v1 API implementation to v2, based on guidelines derived from the API definition tasks. Linked to this, there are the API definition tasks that explain what this API will substitute.*

*This is an internal API (i.e. not exposed via Tier1 gateway) that allows other components to ensure that the Tier 1 Credentials (JWTs) issued by other participants are trusted.*

**As a**\
dataspace agent component

**I want to**\
via an API, submit an End-User Tier 1 credential to be verified

**So that**\
I can ensure that the user has been authenticated by an onboarded participant and that the information contained in the credential can be trusted

**Acceptance Criteria:**

1. **GIVEN** the caller is an internal service within the participant agent (not via a Tier 1 gateway)\
   **AND** it provides a JSON body containing the End-User JWT\
   **WHEN** it sends a POST /tierOneCredentials/validate\
   **THEN** the API returns HTTP 204 No Content (the credential is valid)
1. **GIVEN** the caller is an internal service within the participant agent (not via a Tier 1 gateway)\
   **AND** it provides an invalid JSON body\
   **OR** it provides a JSON body containing a malformed content field as the credential to verify\
   **WHEN** it sends a POST /tierOneCredentials/validate\
   **THEN** the API returns HTTP 400 Bad Request

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-2054](../README.md) (Epic) Verify agent credentials |
