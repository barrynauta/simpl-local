> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14688](../../README.md) / [L2-SIMPL-14731](../README.md) / **Epic-SIMPL-8340**

# SIMPL-8340: A Provider defines the resource source address for resource sharing

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14688](../../README.md) (L1) › [SIMPL-14731](../README.md) (L2) › **SIMPL-8340** (Epic)

## Text

**As** a Provider,\
**I want to** provide via API or UI, as part of a resource description, the Resource Source based on the resource address template that is valid for the resource sharing method, which I use for sharing my resources,\
**so that** the Resource Source is published with the resource description and provides information about the Resource Source required to support the automated resource sharing.\
----

###### Acceptance Criteria:

**1. Submission of Resource Address with Resource Description**\
*

   - **Given** I am a provider user with permission to manage resource descriptions\
   **When** I include a resource address based on the resource sharing method in the resource description and submit it\
   **Then** the resource address is validated, and feedback is provided indicating whether it is valid

**2. Submission with Invalid Resource Address**\
*

   - **Given** I am a provider user with permission to manage resource descriptions\
   **When** I include an invalid resource address in the resource description and submit it\
   **Then** the submission is rejected, and a validation error message is displayed

**3. Publication of Resource with Resource Address**\
*

   - **Given** a resource description with a valid resource address has been submitted\
   **When** the submission is accepted\
   **Then** the resource description, including the resource address, becomes available in the catalogue

**4. Accessing the Resource Address of a Published Resource**\
*

   - **Given** I have access to retrieve published resource descriptions\
   **When** I retrieve a specific published resource\
   **Then** I can view the resource address details included in that resource description

**5. Template Use Based on Resource Sharing Method**\
*

   - **Given** I am creating a resource description\
   **When** I select a resource sharing method\
   **Then** I should be able to retrieve the corresponding resource address template that is valid for that method

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14731](../README.md) (L2) A Provider adds common metadata to a new Resource description |
| **Children** | [SIMPL-10301](Story-SIMPL-10301/README.md) (User story) [UI] Viewing Resource Sharing Methods and Their Templates (Provider)<br>[SIMPL-10562](Story-SIMPL-10562/README.md) (User story) [API] Extend the SD Tooling API for Retrieving Available Resource Sharing Methods and Templates (Provider)<br>[SIMPL-12291](Story-SIMPL-12291/README.md) (User story) [API] Resource Source Address Validation Based on Resource Sharing Method and Provider Template<br>[SIMPL-13565](Story-SIMPL-13565/README.md) (User story) [UI] Selecting, Filling, and Submitting the Resource Source Template (Provider)<br>[SIMPL-13635](Story-SIMPL-13635/README.md) (User story) [API] Resource Source Address Validation Upon Provider Resource Submission<br>[SIMPL-18360](Story-SIMPL-18360/README.md) (User story) Create Resource Address Template for eDelivery |
