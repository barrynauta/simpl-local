> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-21117](../../../README.md) / [L2-SIMPL-14642](../../README.md) / [Epic-SIMPL-18026](../README.md) / **Story-SIMPL-22586**

# SIMPL-22586: [UI] Inform end user (provider) about successful revocation

**User story (implementable work)** · Development: **Planned**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-21117](../../../README.md) (L1) › [SIMPL-14642](../../README.md) (L2) › [SIMPL-18026](../README.md) (Epic) › **SIMPL-22586** (User story)

## Text

#### Storycard

**As a** Provider\
**I want to** notify Consumers about the revocation of a resource description\
**So that** they can react on this.

#### Acceptance criteria

**Scenario:** The Provider notifies Consumers - revokes a resource description - positive case

1. **Given** I am a Provider\
   **And** I'm authorized to manage resource descriptions\
   **When** I revoke an own resource description via UI\
   **And** the revocation of the resource description is successful\
   **Then** Simpl-Open send an email to Consumers with an active contract of the resource description about the revocation of the resource description.

**Scenario:** The Provider notifies Consumers - revokes a resource description - negative case: revocation is not successful

2. **Given** I am a Provider\
   **And** I'm authorized to manage resource descriptions\
   **When** I revoke an own resource description via UI\
   **And** the revocation of the resource description is not successful\
   **And** and an error message is returned via UI\
   **Then** Simpl-Open does not send an email to Consumers with an active contract of the resource description about the revocation of the resource description.

**Scenario:** The Provider notifies Consumers - revokes a resource description - negative case: internal server error

3. **Given** I am a Provider\
   **And** I'm authorized to manage resource descriptions\
   **When** I revoke a resource description via UI\
   **And** there is an internal error\
   **And** and an error message is returned via UI\
   **Then** Simpl-Open does not send an email to Consumers with an active contract of the resource description about the revocation of the resource description.

#### Technical details

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-18026](../README.md) (Epic) A Provider notifies Consumers about the revocation of a resource description |
