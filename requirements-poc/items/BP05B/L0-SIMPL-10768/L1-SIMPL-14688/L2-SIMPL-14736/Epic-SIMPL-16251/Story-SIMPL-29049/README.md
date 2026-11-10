> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14736](../../README.md) / [Epic-SIMPL-16251](../README.md) / **Story-SIMPL-29049**

# SIMPL-29049: [API] - API to retrieve Contract Clause document (xfsc-adv-search)

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14736](../../README.md) (L2) › [SIMPL-16251](../README.md) (Epic) › **SIMPL-29049** (User story)

## Text

**As a** Provider or Consumer\
**I want to** retrieve a contract clause document\
**so that** I can investigate the details of the contract clause when inspecting a resource description or usage contract

**Acceptance Criteria**

**AC1: A Provider or Consumer retrieves a contract clause document - positive case**\
**Given** I am authenticated as a Provider or Consumer, with a role authorising the retrieval of contract clauses\
**And** a contract clause document has been created in the past\
**When** I request to retrieve a contract clause document for a specific documentId\
**And** I invoke the GET /contractClauses/sourceDocument/{documentId} API\
**Then** I receive a 200 Ok response\
**And** the body of the response contains the requested document

**AC2: A Provider or Consumer retrieves a contract clause document - error case (documentId not found)**\
**Given** I am authenticated as a Provider or Consumer, with a role authorising the retrieval of contract clauses\
**When** I request to retrieve a contract clause document for a specific documentId\
**And** the documentId does not exist\
**And** I invoke the GET /contractClauses/sourceDocument/{documentId} API\
**Then** the request is rejected with a 404 Not Found

**AC3:** **A Provider or Consumer** **retrieves a contract clause document - error case (authenticated)**\
**Given** I am authenticated as a Provider or Consumer, with a role authorising the retrieval of contract clauses\
**When** I request to retrieve a contract clause document for a specific documentId\
**And** I invoke the GET /contractClauses/sourceDocument/{documentId} API\
**And** the request cannot be properly fulfilled\
**Then** an appropriate error response is returned (e.g. 400, 406, 415, 500)

**AC4:** **A Provider or Consumer** **retrieves a contract clause document - unauthenticated case**\
**Given** I am not authenticated\
**When** I request to retrieve a contract clause document for a specific documentId\
**And** I invoke the GET /contractClauses/sourceDocument/{documentId} API\
**Then** the request is rejected with a 401 Unauthorized error

**AC5:** **A Provider or Consumer** **retrieves a contract clause document - error case (authenticated with unauthorised role)**\
**Given** I am authenticated as a Provider or Consumer, with a role authorising the retrieval of contract clauses\
**When** I request to retrieve a contract clause document for a specific documentId\
**And** I invoke the GET /contractClauses/sourceDocument/{documentId} API\
**Then** the request is rejected with a 403 Forbidden

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-16251](../README.md) (Epic) A Provider adds contract clauses to a new Resource description via UI |
