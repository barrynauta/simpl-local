> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10171](../../../README.md) / [L2-SIMPL-10173](../../README.md) / [Epic-SIMPL-19153](../README.md) / **Story-SIMPL-20637**

# SIMPL-20637: [API] Governance Authority retrieves the list of eIDAS attributes

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10171](../../../README.md) (L1) › [SIMPL-10173](../../README.md) (L2) › [SIMPL-19153](../README.md) (Epic) › **SIMPL-20637** (User story)

## Text

*Returns a list of all the eIDAS attributes. See eIDAS Attributes Overview*\
*section of [an internal page] .*\
----\
**As A**

dataspace governance authority user with the _Tier 2 authorization manager (T2IAA_M)_ role

**I want to**

via API, get the list of all the available eIDAS identity attributes that are part of the [eIDAS attribute profile](https://ec.europa.eu/digital-building-blocks/sites/download/attachments/467109280/eIDAS%20SAML%20Attribute%20Profile%20v1.4.1_final.pdf?version=1&modificationDate=1729176514404&api=v2) (see also the [eIDAS dashboard](https://eidas.ec.europa.eu/efda/browse/notification/eid-chapter-contacts/IT))

**So** **that**

I can use them to create onboarding validation rules of type eIDAS

**Acceptance Criteria**

1. **GIVEN** that I am logged with the T2IAA_M role\
   **AND** eIDAS login is enabled in the deployment configurations of the onboarding backend component ^(1)^\
   **WHEN** I call GET /eidasAttributes\
   **THEN** I obtain a list of all the eidas attributes available\
   **AND** each item in the list contains\
   the eIDAS URI of the attribute\
   the eIDAS URI friendly name of the attribute\
   the human readable name of the attribute\
   the personType of the attribute (NATURAL_PERSON or LEGAL_PERSON)\
   the category of the attribute (MANDATORY_MDS, OPTIONAL_MDS, COMMON, SECTOR_SPECIFIC)
1. **GIVEN** that I am logged without the T2IAA_M role\
   **WHEN** I call GET /eidasAttributes\
   **THEN** I receive a 403 Forbidden error
1. **GIVEN** that I am not authenticated\
   **WHEN** I call GET /eidasAttributes\
   **THEN** I receive a 401 Unauthorized error{}
1. -**GIVEN** that I am logged with the T2IAA_M role-\
   -**AND** eIDAS login is disabled in the deployment configurations of the onboarding backend component ^(1)^-\
   -**WHEN** I call GET /eidasAttributes-\
   -**THEN** I receive a 403 forbidden response with the message "eIDAS Login feature disabled"-
1. **GIVEN** that I am an IT system administrator, responsible for installing the onboarding backend component ^(1)^\
   **WHEN** I read the deployment documentation of the component\
   **THEN** I see the property to use to enable or disable the eIDAS login feature\
   **AND** I see that by default the feature is disabled

*(1) By default, this feature is disabled on the component, and should be enabled at deployment time by the IT Administrators installing the component into the agent (see last AC).*

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-19153](../README.md) (Epic) Governance authority creates eIDAS rules for automated onboarding request validation |
