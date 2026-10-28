@search-serial @D61
Feature: The federated search (SEARCH) schema
  SIMPL-31098, SIMPL-33537 (lead story for schemaType), the SEARCH paths of
  SIMPL-32801, SIMPL-32802, SIMPL-32807, SIMPL-32786, SIMPL-32788, SIMPL-32798,
  and the SEARCH part of SIMPL-32436. One global SEARCH slot (D16, D17), so
  every scenario here runs serially and frees the slot first (D61).

  State table (SEARCH):
    create while no SEARCH is published        -> 201, PUBLISHED
    create while another SEARCH is published   -> 409
    new version of the published SEARCH        -> 201, stays PUBLISHED
    new version of a revoked SEARCH, slot free -> 201, becomes PUBLISHED
    new version of a revoked SEARCH, slot used -> 201, stays REVOKED
    PATCH PUBLISHED, slot free                 -> 200, PUBLISHED
    PATCH PUBLISHED, slot used                 -> 409
    PATCH REVOKED of the published SEARCH      -> 200, REVOKED

  @SIMPL-31098 @SIMPL-33537 @SIMPL-32802 @SIMPL-32801 @D2 @D16 @D24 @D28
  Scenario: The first SEARCH schema is created and published
    Given no SEARCH schema is published
    And I am "ga-admin"
    When I create the SEARCH schema "$S"
    Then the response status is 201
    And the response header "Location" is an absolute URI ending with "/schemas/$S?schemaType=SEARCH"
    And the JSON field "schemaName" is "$S"
    And the JSON field "schemaVersion" is "1"
    And the JSON field "status" is "PUBLISHED"
    And the JSON field "schemaType" is "SEARCH"
    And the JSON field "resourceType" is null
    And the JSON field "validationWarnings" is an empty list
    And schema "$S" is listed with
      | field        | value     |
      | schemaType   | SEARCH    |
      | resourceType | <null>    |
      | status       | PUBLISHED |
      | version      | 1         |
    And listing "schemaType=SEARCH&status=PUBLISHED" returns exactly "$S"

  @SIMPL-31098 @SIMPL-33537 @SIMPL-32801 @SIMPL-32802 @D17
  Scenario: A second SEARCH schema is refused while one is published
    Given no SEARCH schema is published
    And the SEARCH schema "$S" exists
    When I create the SEARCH schema "$T"
    Then the response is a "conflict" problem with status 409
    And the problem detail is "A published federated search schema already exists."
    And schema "$T" is not listed as a SEARCH schema
    And listing "schemaType=SEARCH&status=PUBLISHED" returns exactly "$S"

  @SIMPL-32802 @D12 @D17
  Scenario: A name clash inside SEARCH is reported before the slot conflict
    Given no SEARCH schema is published
    And the SEARCH schema "$S" exists
    When I create the SEARCH schema "$S"
    Then the response is a "conflict" problem with status 409
    And the problem detail is "A schema with that name already exists."

  @SIMPL-32802 @D12
  Scenario: The name of a revoked SEARCH schema cannot be reused within SEARCH
    Given no SEARCH schema is published
    And the SEARCH schema "$S" exists
    And schema "$S" has been revoked
    When I create the SEARCH schema "$S"
    Then the response is a "conflict" problem with status 409
    And the problem detail is "A schema with that name already exists."

  @SIMPL-32801 @SIMPL-33537 @D16 @D17
  Scenario: A revoked SEARCH schema does not block a new one
    Given no SEARCH schema is published
    And the SEARCH schema "$S" exists
    And schema "$S" has been revoked
    When I create the SEARCH schema "$T"
    Then the response status is 201
    And the JSON field "status" is "PUBLISHED"
    And schema "$S" has status "REVOKED"
    And listing "schemaType=SEARCH&status=PUBLISHED" returns exactly "$T"

  @SIMPL-33537 @SIMPL-32436 @D18 @D21 @D22
  Scenario: Revoking the SEARCH schema leaves no published SEARCH schema and keeps it visible
    Given no SEARCH schema is published
    And the SEARCH schema "$S" exists
    And I am "ga-admin"
    When I revoke schema "$S"
    Then the response status is 200
    And the JSON field "status" is "REVOKED"
    And the JSON field "schemaType" is "SEARCH"
    And the JSON field "resourceType" is null
    And listing "schemaType=SEARCH&status=PUBLISHED" is empty
    And schema "$S" has status "REVOKED"
    When I request the latest content of schema "$S"
    Then the response status is 200
    And the response body is the content uploaded for version "1" of schema "$S"
    When I request the versions of schema "$S"
    Then the response status is 200
    And the versions in "metadata" are "1"

  @SIMPL-31098 @SIMPL-20529 @D18
  Scenario: A revoked SEARCH schema is published again with 200 when the slot is free
    Given no SEARCH schema is published
    And the SEARCH schema "$S" exists
    And schema "$S" has been revoked
    When I publish schema "$S"
    Then the response status is 200
    And the JSON field "status" is "PUBLISHED"
    And listing "schemaType=SEARCH&status=PUBLISHED" returns exactly "$S"

  @SIMPL-20529 @SIMPL-33537 @D18
  Scenario: Publishing a SEARCH schema while another is published gives 409
    Given no SEARCH schema is published
    And the SEARCH schema "$S" exists
    And schema "$S" has been revoked
    And the SEARCH schema "$T" exists
    When I publish schema "$S"
    Then the response is a "conflict" problem with status 409
    And the problem detail is "A published federated search schema already exists."
    And schema "$S" has status "REVOKED"

  @SIMPL-32807 @SIMPL-33537 @D2 @D14 @D16
  Scenario: A new version of the published SEARCH schema stays published
    Given no SEARCH schema is published
    And the SEARCH schema "$S" exists
    And I am "ga-admin"
    When I add a new version to schema "$S"
    Then the response status is 201
    And the response header "Location" is an absolute URI ending with "/schemas/$S/2?schemaType=SEARCH"
    And the JSON field "version" is "2"
    And the JSON field "schemaType" is "SEARCH"
    And the JSON field "status" is "PUBLISHED"
    And version "1" of schema "$S" has status "REVOKED"
    And version "2" of schema "$S" has status "PUBLISHED"

  @SIMPL-33537 @D16
  Scenario: A new version of a revoked SEARCH schema is published when the slot is free
    Given no SEARCH schema is published
    And the SEARCH schema "$S" exists
    And schema "$S" has been revoked
    When I add a new version to schema "$S"
    Then the response status is 201
    And the JSON field "status" is "PUBLISHED"
    And schema "$S" has status "PUBLISHED"

  @SIMPL-33537 @D16
  Scenario: A new version of a revoked SEARCH schema stays revoked while another is published
    Given no SEARCH schema is published
    And the SEARCH schema "$S" exists
    And schema "$S" has been revoked
    And the SEARCH schema "$T" exists
    When I add a new version to schema "$S"
    Then the response status is 201
    And the JSON field "status" is "REVOKED"
    And listing "schemaType=SEARCH&status=PUBLISHED" returns exactly "$T"

  @SIMPL-32807 @D20
  Scenario: A resourceType on a new SEARCH version is rejected
    Given no SEARCH schema is published
    And the SEARCH schema "$S" exists
    When I add a new version to schema "$S" with
      | field        | value |
      | resourceType | $RT   |
    Then the response is a "badRequest" problem with status 400

  @SIMPL-32807 @D11
  Scenario: A SEARCH name without schemaType is not found as RESOURCE
    Given no SEARCH schema is published
    And the SEARCH schema "$S" exists
    When I send a GET request to "/schemas/$S"
    Then the response is a "notFound" problem with status 404
    When I send a GET request to "/schemas/$S/versions"
    Then the response is a "notFound" problem with status 404
    When I add a new version to schema "$S" with
      | field      | value  |
      | schemaType | <omit> |
    Then the response is a "notFound" problem with status 404

  @SIMPL-33537 @SIMPL-32788 @SIMPL-32798 @SIMPL-32786 @D9 @D10 @D2
  Scenario: Content, version and version history of a SEARCH schema
    Given no SEARCH schema is published
    And the SEARCH schema "$S" exists
    And schema "$S" has a new version
    And I am "ga-viewer"
    When I request the latest content of schema "$S"
    Then the response status is 200
    And the response content type is "text/turtle"
    And the response body is the content uploaded for version "2" of schema "$S"
    And the response header "Content-Location" contains "/schemas/$S/2"
    When I request version "1" of schema "$S"
    Then the response status is 200
    And the response body is the content uploaded for version "1" of schema "$S"
    When I request the versions of schema "$S"
    Then the response status is 200
    And the versions in "metadata" are "1,2"
    And every item of "metadata" has "schemaType" equal to "SEARCH"
    And the JSON field "metadata[0].resourceType" is null
    When I request version "2" of schema "$S" as JSON
    Then the JSON field "schemaType" is "SEARCH"
    And the JSON field "status" is "PUBLISHED"

  @SIMPL-33537 @SIMPL-32258 @D27
  Scenario: The SEARCH list holds only SEARCH schemas and the default list holds none
    Given no SEARCH schema is published
    And the SEARCH schema "$S" exists
    And I am "ga-viewer"
    When I list schemas with "schemaType=SEARCH&status=PUBLISHED"
    Then the response status is 200
    And the names in "metadata" are "$S"
    And every item of "metadata" has "schemaType" equal to "SEARCH"
    And schema "$S" is not listed as a RESOURCE schema

  @SIMPL-33537 @SIMPL-32798 @D11
  Scenario: The same name can exist once as RESOURCE and once as SEARCH
    Given no SEARCH schema is published
    And the RESOURCE schema "$A" exists for resource type "$RT"
    And I am "ga-admin"
    When I create the SEARCH schema "$A"
    Then the response status is 201
    When I send a GET request to "/schemas/$A?schemaType=SEARCH"
    Then the response status is 200
    And the response body is the content uploaded for version "1" of the SEARCH schema "$A"
    When I send a GET request to "/schemas/$A"
    Then the response status is 200
    And the response body is the content uploaded for version "1" of the RESOURCE schema "$A"

  @SIMPL-33537 @D31
  Scenario: A SEARCH schema needs no default properties but may carry them
    Given no SEARCH schema is published
    And I am "ga-admin"
    When I create the SEARCH schema "$S" from "valid-search-with-defaults.ttl"
    Then the response status is 201
    When I revoke schema "$S"
    Then the response status is 200
    When I create the SEARCH schema "$T" from "missing-defaults.ttl"
    Then the response status is 201

  @SIMPL-33537 @D30 @D37
  Scenario: An invalid SEARCH schema is not created
    Given no SEARCH schema is published
    And I am "ga-admin"
    When I create the SEARCH schema "$S" from "syntax-error.ttl"
    Then the response is a "badRequest" problem with status 400
    And the problem detail starts with "Syntax Error: "
    And listing "schemaType=SEARCH&status=PUBLISHED" is empty

  @SIMPL-33537 @D16
  Scenario: Two concurrent SEARCH creates leave exactly one published SEARCH schema
    Given no SEARCH schema is published
    When two admins create the SEARCH schemas "$S" and "$T" at the same time
    Then one request succeeded with status 201 and the other got 409
    And exactly one of the schemas "$S,$T" has status "PUBLISHED"
