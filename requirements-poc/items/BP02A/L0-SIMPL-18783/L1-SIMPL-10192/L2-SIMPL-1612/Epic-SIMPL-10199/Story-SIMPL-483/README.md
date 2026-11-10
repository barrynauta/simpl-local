> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP02A](../../../../../README.md) / [L0-SIMPL-18783](../../../../README.md) / [L1-SIMPL-10192](../../../README.md) / [L2-SIMPL-1612](../../README.md) / [Epic-SIMPL-10199](../README.md) / **Story-SIMPL-483**

# SIMPL-483: Retrieve Identity Attributes via Dataspace Governace Authority API

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP02A](../../../../../README.md) › [SIMPL-18783](../../../../README.md) (L0) › [SIMPL-10192](../../../README.md) (L1) › [SIMPL-1612](../../README.md) (L2) › [SIMPL-10199](../README.md) (Epic) › **SIMPL-483** (User story)

## Text

**As A**

Dataspace participant agent

**I want to**

call the dataspace governance authority API for retrieving the list of dataspace identity attributes ([Identity Attribute|an internal page]) to store a local copy of the Attributes in the agent's &lt;User and Roles&gt; component

**So that**

I can return them to any API caller

**Acceptance Criteria:**

1. **Given that I am a dataspace participant agent with valid API endpoint and credentials, I can successfully call the `/identity-attribute/` endpoint to retrieve the list of identity attributes.**
1. **Given that I have a valid API endpoint and credentials, I can authenticate and authorize the API call to retrieve the list of identity attributes.**
1. **Given that the API call is successful, I can retrieve the list of identity attributes from the Dataspace Governance Authority API.**
1. **Given that I have retrieved the list of identity attributes, I can store a local copy of the list in the agent User And Roles.**
1. **Given that the local copy of the list is stored, I can verify that the list of identity attributes is up-to-date and matches the one retrieved from the API.**
1. **Given that I am an API caller without participant credentials, when I send a request to the `/identity-attribute/` endpoint, I should receive an unauthorized access error response with 401 status code.**
1. **Given that I am an API caller without participant credentials, when I examine the error response, I should see the generic error message "unauthorized access".**
1. **Given that I am a dataspace participant agent, when I call the `{}/identity-attribute/{participantID{`}} endpoint with a valid participant ID, I can retrieve the list of identity attributes assigned to that participant.**
1. **Given that I have retrieved the list of identity attributes assigned to a participant, I can verify that the list includes only the attributes assigned to that participant.**
1. **Given that I am an API caller without participant credentials, when I send a request to the `{}/identity-attribute/{participantID{`}} endpoint with an invalid participant ID, I should receive an error response with 400 status code.**

*The Attachment contains the OpenAPI specification*

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP02A](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp02-configuration-governance-authority/bp02a-configure-idtrust-security-solution.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp02a-configure-idtrust-security-solution)) |
| **Parent** | [SIMPL-10199](../README.md) (Epic) Managing identity attributes |
