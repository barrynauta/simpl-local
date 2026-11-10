> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14728](../../README.md) / [L2-SIMPL-14738](../README.md) / **Epic-SIMPL-12250**

# SIMPL-12250: A Provider updates the resource source address for resource sharing

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14728](../../README.md) (L1) › [SIMPL-14738](../README.md) (L2) › **SIMPL-12250** (Epic)

## Text

**As a** Provider,\
**I want to** update via API or UI, as part of a resource description, the Resource Source based on the resource address template that is valid for the resource sharing method, which I use for sharing my resources,\
**So that** the Resource Source is published with the resource description and provides information about the Resource Source required to support the automated resource sharing.\
----

###### Acceptance Criteria:

**1. Update of Resource Address with Existing Resource**\
*

1. **Given** I am a provider user with permission to manage resource descriptions\
   **When** I update the resource address in an existing resource description using a valid template for the selected resource sharing method\
   **Then** the updated resource address is validated with respect to the template, and feedback is provided indicating whether it is valid

**2. Acceptance of Valid Resource Address Update**\
*

2. **Given** I am a provider user with permission to manage resource descriptions\
   **And** I provide an updated resource address valid with respect to the selected resource address template\
   **When** the updated resource description is submitted\
   **Then** the new resource address is accepted and stored with the resource description

**3. Rejection of Invalid Resource Address Update**\
*

3. **Given** I attempt to update the resource address using a value that does not conform to the selected resource address template\
   **When** the update is submitted\
   **Then** the update is rejected, and a clear and actionable error message is returned

**4. Visibility of Updated Resource Address**\
*

4. **Given** a resource description with an updated resource address has been successfully submitted\
   **When** a user accesses the published resource\
   **Then** the updated resource address is visible in the resource details

**5. Error Handling for Invalid Updates**\
*

5. **Given** I attempt to update the resource address with an invalid template or one that doesn’t match the selected resource sharing method\
   **When** I submit the update\
   **Then** the update is rejected, and a clear error message is returned

**6. System Extensibility for Future Update Scenarios**\
*

6. **Given** new resource sharing methods and templates may be introduced\
   **When** I update a resource address using a newly supported template\
   **Then** the update is accepted and processed without requiring changes to the update logic

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14738](../README.md) (L2) A Provider updates the common metadata of a new version of a Resource description |
| **Children** | [SIMPL-13565](../../../L1-SIMPL-14688/L2-SIMPL-14731/Epic-SIMPL-8340/Story-SIMPL-13565/README.md) (User story) [UI] Selecting, Filling, and Submitting the Resource Source Template (Provider) |
