> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA09](../../../../../README.md) / [L0-SIMPL-26074](../../../../README.md) / [L1-SIMPL-27082](../../../README.md) / [L2-SIMPL-27154](../../README.md) / [Epic-SIMPL-27203](../README.md) / **Story-SIMPL-28808**

# SIMPL-28808: [API][IMPROVEMENT] Simpl-Open user to retrieve participant details

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [SA09](../../../../../README.md) › [SIMPL-26074](../../../../README.md) (L0) › [SIMPL-27082](../../../README.md) (L1) › [SIMPL-27154](../../README.md) (L2) › [SIMPL-27203](../README.md) (Epic) › **SIMPL-28808** (User story)

## Text

_This user story improves SIMPL-17522 by allowing a user with the SIMPL_USER role to retrieve the participant details of the agent of their organisation. In the context of consent management, this API will be used to populate the participant party of the assertion during creation._\
----\
**As a**

Simpl-Open user with the SIMPL_USER role

**I want to**

via an API, request information about the participant linked to my organisations's agent

**So that**\
So that I can obtain the participant's organisation details and identifiers, and get a clearer overview of my organisation's presence in the dataspace

**Acceptance Criteria**

1. **GIVEN** I am logged in with the SIMPL_USER role\
   **AND** my organisation's agent is onboarded\
   **WHEN** I send a GET /participant request\
   **THEN** the API returns HTTP 200 OK [SEE *SIMPL-17522 for the details of the repsonse]*
1. **GIVEN** I am logged in without the SIMPL_USER role\
   **WHEN** I send a GET /participant request\
   **THEN** the API returns a 403 response
1. **GIVEN** I am not logged in\
   **WHEN** I send a GET /participant request\
   **THEN** the API returns a 401 response

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA09](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions)) |
| **Parent** | [SIMPL-27203](../README.md) (Epic) Data Provider creates a resource consent assertion |
