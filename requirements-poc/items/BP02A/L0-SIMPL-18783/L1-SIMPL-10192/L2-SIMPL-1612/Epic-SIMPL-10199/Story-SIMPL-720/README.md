> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10192](../../../README.md) / [L2-SIMPL-1612](../../README.md) / [Epic-SIMPL-10199](../README.md) / **Story-SIMPL-720**

# SIMPL-720: API to store the dataspace identity attributes

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10192](../../../README.md) (L1) › [SIMPL-1612](../../README.md) (L2) › [SIMPL-10199](../README.md) (Epic) › **SIMPL-720** (User story)

## Text

**As a** user with the *Tier 1 user and role manager* role\
**I want to** via an API, save in my participant agent a list of Identity Attribute Entity (Identity Attribute (internal page)) and store in my participant agent\
**So that** I can assign a subset of this list to agent roles

**Acceptance Criteria:**

1. **Given** that I am a dataspace participant with the T1UAR_M role, **when** I call the API to store Identity Attribute Entity, **then** the list of attributes is saved in my participant agent

2. **Given** that I am a dataspace participant with the T1UAR_M role and that the API request contains a list of valid Identity Attribute Entities, then the API returns a 200 OK status code

3. **Given** that I am a dataspace participant with the T1UAR_M role and that the API request contains an invalid list of Identity Attribute Entities, then the API returns a 400 Bad Request status code and a clear error message

4. **Given** that I am a dataspace participant with the T1UAR_M role and that the API request is successful, then the list of attributes is stored in the participant agent and can be retrieved later

5. **Given** that I am a dataspace participant with the T1UAR_M role and that I call the API multiple times with different lists of attributes, then the API should handle the requests without errors and store all the attributes in the participant agent

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10199](../README.md) (Epic) Managing identity attributes |
