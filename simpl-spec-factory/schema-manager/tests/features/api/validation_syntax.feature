Feature: Syntax, structure and default property validation
  SIMPL-14703 and SIMPL-26105 (validation), SIMPL-14795 (syntax error message)
  and SIMPL-14796 (default property message). Decisions D30 to D33: Turtle
  with at least one sh:NodeShape; for RESOURCE, one shape must carry simpl:uuid
  and simpl:version as sh:path; exact detail prefixes; the first failing stage
  stops processing.

  @SIMPL-14703 @SIMPL-26105 @SIMPL-14795 @SIMPL-14796 @D30 @D32
  Scenario: A valid schema passes without any error
    Given I am "ga-admin"
    When I create the RESOURCE schema "$A" for resource type "$RT"
    Then the response status is 201
    And the JSON field "validationWarnings" is an empty list

  @SIMPL-14795 @SIMPL-13353 @SIMPL-32802 @D30 @D33
  Scenario Outline: Content that is not valid Turtle gives a syntax error and creates nothing
    When I create the RESOURCE schema "$A" for resource type "$RT" from "<fixture>"
    Then the response is a "badRequest" problem with status 400
    And the problem detail starts with "Syntax Error: "
    And listing "resourceType=$RT" is empty

    Examples:
      | fixture         |
      | syntax-error.ttl |
      | not-turtle.json |
      | not-turtle.png  |
      | empty.ttl       |

  @SIMPL-14703 @SIMPL-14795 @D30 @D33
  Scenario: Turtle without any sh:NodeShape gives a syntax error
    When I create the RESOURCE schema "$A" for resource type "$RT" from "no-nodeshape.ttl"
    Then the response is a "badRequest" problem with status 400
    And the problem detail starts with "Syntax Error: "

  @SIMPL-14703 @SIMPL-14795 @D30 @D33
  Scenario: A new version with a syntax error is not created
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I add a new version to schema "$A" from "syntax-error.ttl"
    Then the response is a "badRequest" problem with status 400
    And the problem detail starts with "Syntax Error: "
    And schema "$A" has the versions "1"

  @SIMPL-26105 @SIMPL-14796 @D31 @D32 @D33
  Scenario: A RESOURCE schema without simpl:uuid is rejected with one issue
    When I create the RESOURCE schema "$A" for resource type "$RT" from "missing-uuid.ttl"
    Then the response is a "badRequest" problem with status 400
    And the problem detail starts with "Default Properties Error: "
    And the problem lists the issue "missingDefaultProperty" at "$DPuuid"
    And the problem lists 1 issues with code "missingDefaultProperty"
    And listing "resourceType=$RT" is empty

  @SIMPL-26105 @SIMPL-14796 @D31 @D32 @D33
  Scenario: A RESOURCE schema without simpl:version is rejected with one issue
    When I create the RESOURCE schema "$A" for resource type "$RT" from "missing-version.ttl"
    Then the response is a "badRequest" problem with status 400
    And the problem detail starts with "Default Properties Error: "
    And the problem lists the issue "missingDefaultProperty" at "$DPversion"
    And the problem lists 1 issues with code "missingDefaultProperty"

  @SIMPL-14703 @SIMPL-26105 @SIMPL-14796 @D32 @D33
  Scenario: A RESOURCE schema without any default property gets one issue per property
    When I create the RESOURCE schema "$A" for resource type "$RT" from "missing-defaults.ttl"
    Then the response is a "badRequest" problem with status 400
    And the problem detail starts with "Default Properties Error: "
    And the problem lists the issue "missingDefaultProperty" at "$DPuuid"
    And the problem lists the issue "missingDefaultProperty" at "$DPversion"
    And the problem lists 2 issues with code "missingDefaultProperty"

  @SIMPL-14703 @D32
  Scenario: Default properties split over two shapes do not pass
    When I create the RESOURCE schema "$A" for resource type "$RT" from "defaults-split-over-shapes.ttl"
    Then the response is a "badRequest" problem with status 400
    And the problem detail starts with "Default Properties Error: "

  @SIMPL-14703 @D32
  Scenario: A default property IRI that is not an sh:path does not count
    When I create the RESOURCE schema "$A" for resource type "$RT" from "defaults-not-as-path.ttl"
    Then the response is a "badRequest" problem with status 400
    And the problem detail starts with "Default Properties Error: "
    And the problem lists the issue "missingDefaultProperty" at "$DPuuid"

  @SIMPL-14703 @SIMPL-26105 @D32
  Scenario: A new version without default properties is not created
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I add a new version to schema "$A" from "missing-defaults.ttl"
    Then the response is a "badRequest" problem with status 400
    And the problem detail starts with "Default Properties Error: "
    And schema "$A" has the versions "1"

  @SIMPL-14796 @D33
  Scenario: A syntax error is reported before missing default properties
    When I create the RESOURCE schema "$A" for resource type "$RT" from "syntax-error.ttl"
    Then the problem detail starts with "Syntax Error: "
    And the problem has no issue with code "missingDefaultProperty"

  @SIMPL-14796 @SIMPL-14797 @D33
  Scenario: Missing default properties are reported before semantic errors
    When I create the RESOURCE schema "$A" for resource type "$RT" from "missing-defaults-and-unresolved-shape.ttl"
    Then the response is a "badRequest" problem with status 400
    And the problem detail starts with "Default Properties Error: "
    And the problem has no issue with code "unresolvedShapeReference"
