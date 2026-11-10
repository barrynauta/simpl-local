> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP13](../../../../../README.md) / [L0-SIMPL-10783](../../../../README.md) / [L1-SIMPL-1277](../../../README.md) / [L2-SIMPL-1616](../../README.md) / [Epic-SIMPL-2049](../README.md) / **Story-SIMPL-17954**

# SIMPL-17954: [UI][REFACTOR] Verify connectivity to other dataspace participants

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP13](../../../../../README.md) › [SIMPL-10783](../../../../README.md) (L0) › [SIMPL-1277](../../../README.md) (L1) › [SIMPL-1616](../../README.md) (L2) › [SIMPL-2049](../README.md) (Epic) › **SIMPL-17954** (User story)

## Text

*This story refactors the API endpoint called by the page functionality using the v2 endpoint*

**As a**\
user of an onboarded participant without any specific role, navigating on the "/ping" service page

**I want to**\
via UI, have the possibility to fill an input text specifying the destination FQDN tier2 gateway of the agent I want to contact, then submit the connection test request

**So that**\
I can test the connection between my participant and the other participant, verifying that there isn't a connectivity issue

**Acceptance Criteria:**

1. **GIVEN** that I am an onboarded participant user without a specific role\
   **WHEN** I navigate to the "/ping" service page\
   **THEN** I should see an input text field allowing me to enter the destination FQDN tier2 gateway of the agent I want to contact
1. **GIVEN** that I am an onboarded participant user without a specific role\
   **WHEN** I navigate to the "/ping" service page\
   **THEN** I should see a submit button to initiate the connection test request
1. **GIVEN** that I have entered a valid FQDN tier2 gateway and submitted the connection test request\
   **WHEN** the page invokes the GET tier1/v2/ping API with the destination parameter\
   **THEN** I should see a success indicator if the connection between my participant and the other participant is established successfully
1. **GIVEN** that I have entered a valid FQDN tier2 gateway and submitted the connection test request\
   **WHEN** the page invokes the GET tier1/v2/ping API and the request fails\
   **THEN** I should see an error indicator with an error message stating that the connection could not be established

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP13](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp13-it-administration/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp13-it-administration)) |
| **Parent** | [SIMPL-2049](../README.md) (Epic) Secure interaction between agents |
