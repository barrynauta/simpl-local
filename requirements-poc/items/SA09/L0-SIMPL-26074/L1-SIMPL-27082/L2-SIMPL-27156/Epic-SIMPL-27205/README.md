> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [SA09](../../../../README.md) / [L0-SIMPL-26074](../../../README.md) / [L1-SIMPL-27082](../../README.md) / [L2-SIMPL-27156](../README.md) / **Epic-SIMPL-27205**

# SIMPL-27205: Data Provider consults and updates consent assertions

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [SA09](../../../../README.md) › [SIMPL-26074](../../../README.md) (L0) › [SIMPL-27082](../../README.md) (L1) › [SIMPL-27156](../README.md) (L2) › **SIMPL-27205** (Epic)

## Text

**As a** Data Provider\
**I want to** search, list, and view the details of existing consent assertions, and update their attributes and metadata\
**So that** I can maintain an accurate and up-to-date representation of my consent assertions within the platform.

**Acceptance Criteria:**

1. **Given** I am a Data Provider user with the appropriate roles\
   **When** I request the list of consent assertions, optionally filtering by type, status, or metadata\
   **Then** the platform returns a paginated list of consent assertions matching the specified criteria

2. **Given** I am a Data Provider user with the appropriate roles\
   **When** I request the details of a specific consent assertion\
   **Then** the platform returns the full details of the assertion, including its attributes and metadata

3. **Given** I am a Data Provider user with the appropriate roles\
   **When** I update the attributes or metadata of an existing consent assertion\
   **Then** the assertion is updated with the provided values\
   **And** the updated representation is immediately available within the platform

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA09](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions)) |
| **Parent** | [SIMPL-27156](../README.md) (L2) Data Provider consults and updates existing consent assertions |
| **Children** | [SIMPL-28593](Story-SIMPL-28593/README.md) (User story) [API] Data provider updates consent assertion<br>[SIMPL-28594](Story-SIMPL-28594/README.md) (User story) [API] Data Provider lists and filters consent assertions<br>[SIMPL-28595](Story-SIMPL-28595/README.md) (User story) [API] Data provider retrieve the details of a consent assertion<br>[SIMPL-28600](Story-SIMPL-28600/README.md) (User story) [UI] Data Provider updates consent assertion<br>[SIMPL-28601](Story-SIMPL-28601/README.md) (User story) [UI] Data Provider lists and filters consent assertions<br>[SIMPL-28602](Story-SIMPL-28602/README.md) (User story) [UI] Data provider shows the details of a consent assertion<br>[SIMPL-29113](Story-SIMPL-29113/README.md) (User story) [DEPRECATED] [UI] Data Provider views the details of a consent assertion<br>[SIMPL-29955](Story-SIMPL-29955/README.md) (User story) [API][IMPROVEMENT] Data provider configures the maximum amount of metadata key-value pairs for a consent assertion<br>[SIMPL-30219](Story-SIMPL-30219/README.md) (User story) [UI][IMPROVEMENT] Add creation date and update date to consent assertion list |
