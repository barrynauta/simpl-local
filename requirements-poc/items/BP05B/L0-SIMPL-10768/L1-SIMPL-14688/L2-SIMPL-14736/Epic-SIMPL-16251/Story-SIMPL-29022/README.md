> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14736](../../README.md) / [Epic-SIMPL-16251](../README.md) / **Story-SIMPL-29022**

# SIMPL-29022: [API] - API to store Contract Clause document

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14736](../../README.md) (L2) › [SIMPL-16251](../README.md) (Epic) › **SIMPL-29022** (User story)

## Text

**As a** Provider\
**I want to** store a contract clause document\
**So that** I can publish a resource description that informs potential Consumers about the contractual terms and obligations for the resource.

**Acceptance Criteria**

**AC1: A Provider stores a contract clause document - positive case**

1. **Given** I am authenticated as a Provider, with a role authorising contract clauses management\
   **When** I request to store a contract clause document\
   **And** I invoke POST /contractClauses/sourceDocument\
   **And** the body of the request contains\
   the documentName (string)\
   the sourceDocument\
   **Then** I receive a 201 Created response\
   **And** the contract clause document is stored and document hash is generated\
   **And** the body of the response contains\
   the documentId (UUID)\
   the documentHash (string)\
   the hashAlgorithm (string)\
   the documentName (string)

**AC2: A Provider stores a contract clause document - error case (authenticated)**

2. **Given** I am authenticated as a Provider, with a role authorising contract clauses management\
   **When** I request to store a contract clause document\
   **And** I invoke POST /contractClauses/sourceDocument\
   **And** the request cannot be properly fulfilled\
   **Then** an appropriate error response is returned (e.g. 400, 406, 415, 500)

**AC3: A Provider stores a contract clause document - unauthenticated case**

3. **Given** I am not authenticated\
   **When** I request to store a contract clause document\
   **And** I invoke POST /contractClauses/sourceDocument\
   **Then** the request is rejected with a 401 Unauthorized error

**AC4: A Provider stores a contract clause document - error case (authenticated with unauthorised role)**

4. **Given** I am authenticated as a Provider, without a role authorising contract clauses management\
   **When** I request to store a contract clause document\
   **And** I invoke POST /contractClauses/sourceDocument\
   **Then** the request is rejected with a 403 Forbidden

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-16251](../README.md) (Epic) A Provider adds contract clauses to a new Resource description via UI |
