Feature: Semantic validation against self-managed vocabularies
  SIMPL-22407 (semantic validation) and SIMPL-14797 (semantic error message).
  Decisions D34 to D37. The Vocabulary Manager stub of D60 is seeded per
  scenario with a unique vocabulary ($VOC); $VOCX is a name that is never
  seeded. $NS is VOCABULARY_NAMESPACE_BASE.

  @SIMPL-22407 @SIMPL-14797 @D34 @D35 @D37 @vocab
  Scenario: References to existing local vocabulary terms pass
    Given the vocabulary "$VOC" defines the terms "Asset,owner,Party,kind,Open,Closed"
    And I am "ga-admin"
    When I create the RESOURCE schema "$A" for resource type "$RT" from "semantic-local-valid.ttl"
    Then the response status is 201
    And the JSON field "validationWarnings" is an empty list

  @SIMPL-22407 @SIMPL-14797 @D35 @D37 @vocab
  Scenario: A new version with valid local references passes
    Given the vocabulary "$VOC" defines the terms "Asset,owner,Party,kind,Open,Closed"
    And the RESOURCE schema "$A" exists for resource type "$RT"
    When I add a new version to schema "$A" from "semantic-local-valid.ttl"
    Then the response status is 201
    And the JSON field "version" is "2"
    And the JSON field "validationWarnings" is an empty list

  @SIMPL-22407 @SIMPL-14797 @D33 @D35 @vocab
  Scenario: A reference to a local vocabulary that does not exist is an error
    When I create the RESOURCE schema "$A" for resource type "$RT" from "semantic-unknown-vocabulary.ttl"
    Then the response is a "badRequest" problem with status 400
    And the problem detail starts with "Semantic Validation Error: "
    And the problem lists the issue "localReferenceUnavailable" mentioning "$NS$VOCX#owner"
    And every problem issue has a code, a description and a location
    And listing "resourceType=$RT" is empty

  @SIMPL-22407 @SIMPL-14797 @D35 @vocab
  Scenario: References to terms the vocabulary does not define are errors
    Given the vocabulary "$VOC" defines the terms "owner"
    When I create the RESOURCE schema "$A" for resource type "$RT" from "semantic-unknown-term.ttl"
    Then the response is a "badRequest" problem with status 400
    And the problem detail starts with "Semantic Validation Error: "
    And the problem lists the issue "unknownVocabularyTerm" mentioning "$NS$VOC#missingProperty"
    And the problem lists the issue "unknownVocabularyTerm" mentioning "$NS$VOC#MissingClass"
    And the problem lists 2 issues with code "unknownVocabularyTerm"

  @SIMPL-22407 @SIMPL-14797 @D35
  Scenario: sh:node and sh:qualifiedValueShape must point to shapes of the same document
    When I create the RESOURCE schema "$A" for resource type "$RT" from "semantic-unresolved-shape.ttl"
    Then the response is a "badRequest" problem with status 400
    And the problem detail starts with "Semantic Validation Error: "
    And the problem lists the issue "unresolvedShapeReference" mentioning "urn:simpl:test:NowhereShape"
    And the problem lists the issue "unresolvedShapeReference" mentioning "urn:simpl:test:AlsoNowhereShape"
    And the problem lists 2 issues with code "unresolvedShapeReference"

  @SIMPL-22407 @D35
  Scenario: A shape reference defined in the same document passes
    Given I am "ga-admin"
    When I create the RESOURCE schema "$A" for resource type "$RT" from "semantic-resolved-shape.ttl"
    Then the response status is 201

  @SIMPL-22407 @D35 @vocab
  Scenario: All semantic errors are returned at once
    Given the vocabulary "$VOC" defines the terms "owner"
    When I create the RESOURCE schema "$A" for resource type "$RT" from "semantic-many-errors.ttl"
    Then the response is a "badRequest" problem with status 400
    And the problem detail starts with "Semantic Validation Error: "
    And the problem lists the issue "localReferenceUnavailable" mentioning "$NS$VOCX#owner"
    And the problem lists the issue "unknownVocabularyTerm" mentioning "$NS$VOC#missingProperty"
    And the problem lists the issue "unresolvedShapeReference" mentioning "urn:simpl:test:NowhereShape"

  @SIMPL-22407 @SIMPL-14797 @D35 @vocab
  Scenario: A new version with semantic errors is not created and the schema is unchanged
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I add a new version to schema "$A" from "semantic-unknown-vocabulary.ttl"
    Then the response is a "badRequest" problem with status 400
    And the problem detail starts with "Semantic Validation Error: "
    And schema "$A" has the versions "1"
    And version "1" of schema "$A" has status "PUBLISHED"

  @SIMPL-22407 @D34 @D35 @openapi-only
  Scenario: An external namespace gives a warning, not an error
    Given I am "ga-admin"
    When I create the RESOURCE schema "$A" for resource type "$RT" from "semantic-external-prefix.ttl"
    Then the response status is 201
    And the JSON field "validationWarnings" is a list of 1 items
    And the JSON field "validationWarnings[0].code" is "externalReferenceSkipped"
    And the JSON field "validationWarnings[0].location" is "prefix 'ex' -> 'https://example.org/vocab/'"

  @SIMPL-22407 @D35 @openapi-only
  Scenario: A new version with an external namespace returns the warning
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I add a new version to schema "$A" from "semantic-external-prefix.ttl"
    Then the response status is 201
    And the JSON field "validationWarnings[0].code" is "externalReferenceSkipped"

  @SIMPL-33537 @D37 @vocab @search-serial
  Scenario: Semantic validation also runs for SEARCH schemas
    Given no SEARCH schema is published
    When I create the SEARCH schema "$S" from "semantic-search-unknown-vocabulary.ttl"
    Then the response is a "badRequest" problem with status 400
    And the problem detail starts with "Semantic Validation Error: "
    And the problem lists the issue "localReferenceUnavailable" mentioning "$NS$VOCX#title"
    And listing "schemaType=SEARCH&status=PUBLISHED" is empty

  @SIMPL-22407 @D36 @vm-down
  Scenario: With the Vocabulary Manager unreachable, a schema with local references fails with 500
    When I create the RESOURCE schema "$A" for resource type "$RT" from "semantic-unknown-vocabulary.ttl"
    Then the response is a "internalServerError" problem with status 500
    And the problem detail is "Semantic validation unavailable: vocabulary manager unreachable"
    And listing "resourceType=$RT" is empty

  @SIMPL-22407 @D36 @vm-down
  Scenario: With the Vocabulary Manager unreachable, a schema without local references is still accepted
    Given I am "ga-admin"
    When I create the RESOURCE schema "$A" for resource type "$RT"
    Then the response status is 201
