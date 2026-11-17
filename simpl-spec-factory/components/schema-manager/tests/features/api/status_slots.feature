Feature: One published RESOURCE schema per resource type (the slot rule)
  SIMPL-20686 and SIMPL-32801, with the status model of D16. A slot is the set
  of RESOURCE concepts with the same resourceType. Each scenario owns its
  resource types, so the slots are its own (D61).

  State table (RESOURCE):
    action           / concept before / other published in slot  -> result
    create           / none           / no                       -> 201, PUBLISHED
    create           / none           / yes                      -> 201, REVOKED
    new version      / PUBLISHED      / (impossible)             -> 201, stays PUBLISHED
    new version      / REVOKED        / no                       -> 201, becomes PUBLISHED
    new version      / REVOKED        / yes                      -> 201, stays REVOKED
    PATCH PUBLISHED  / REVOKED        / no                       -> 200, PUBLISHED
    PATCH PUBLISHED  / REVOKED        / yes                      -> 409
    PATCH PUBLISHED  / PUBLISHED      / -                        -> 409
    PATCH REVOKED    / PUBLISHED      / -                        -> 200, REVOKED
    PATCH REVOKED    / REVOKED        / -                        -> 409

  @SIMPL-20686 @SIMPL-32801 @D16
  Scenario: The first schema of a resource type is published
    Given I am "ga-admin"
    When I create the RESOURCE schema "$A" for resource type "$RT"
    Then the response status is 201
    And the JSON field "status" is "PUBLISHED"
    And schema "$A" has status "PUBLISHED"

  @SIMPL-20686 @SIMPL-32801 @D16
  Scenario: A new schema is revoked when its resource type already has a published one
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I create the RESOURCE schema "$B" for resource type "$RT"
    Then the response status is 201
    And the JSON field "status" is "REVOKED"
    And schema "$B" has status "REVOKED"
    And schema "$A" has status "PUBLISHED"

  @SIMPL-20686 @SIMPL-32801 @D16
  Scenario: A new schema is published when all schemas of its resource type are revoked
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And the RESOURCE schema "$B" exists for resource type "$RT"
    And schema "$A" has been revoked
    When I create the RESOURCE schema "$C" for resource type "$RT"
    Then the response status is 201
    And the JSON field "status" is "PUBLISHED"
    And listing "status=PUBLISHED&resourceType=$RT" returns exactly "$C"

  @SIMPL-20686 @SIMPL-32801 @D16
  Scenario: Slots of different resource types are independent
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I create the RESOURCE schema "$B" for resource type "$RT2"
    Then the JSON field "status" is "PUBLISHED"
    And schema "$A" has status "PUBLISHED"

  @SIMPL-13357 @SIMPL-32807 @SIMPL-20530 @D16
  Scenario: A new version of a published schema stays published and supersedes the previous version
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I add a new version to schema "$A"
    Then the JSON field "status" is "PUBLISHED"
    And schema "$A" has status "PUBLISHED"
    And version "1" of schema "$A" has status "REVOKED"
    And version "2" of schema "$A" has status "PUBLISHED"

  @SIMPL-20530 @D16
  Scenario: A new version of a revoked schema becomes published when the slot is free
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And schema "$A" has been revoked
    When I add a new version to schema "$A"
    Then the response status is 201
    And the JSON field "status" is "PUBLISHED"
    And schema "$A" has status "PUBLISHED"
    And version "2" of schema "$A" has status "PUBLISHED"
    And version "1" of schema "$A" has status "REVOKED"

  @SIMPL-20530 @D16
  Scenario: A new version of a revoked schema stays revoked when another schema of the slot is published
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And the RESOURCE schema "$B" exists for resource type "$RT"
    When I add a new version to schema "$B"
    Then the response status is 201
    And the JSON field "status" is "REVOKED"
    And schema "$B" has status "REVOKED"
    And version "2" of schema "$B" has status "REVOKED"
    And schema "$A" has status "PUBLISHED"

  @SIMPL-20529 @D16 @D18
  Scenario: Revoke one and publish the other within a slot
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And the RESOURCE schema "$B" exists for resource type "$RT"
    And schema "$A" has been revoked
    When I publish schema "$B"
    Then the response status is 200
    And listing "status=PUBLISHED&resourceType=$RT" returns exactly "$B"
    When I publish schema "$A"
    Then the response is a "conflict" problem with status 409

  @SIMPL-20686 @D16
  Scenario: Two concurrent creates in one empty slot leave exactly one published schema
    When two admins create the RESOURCE schemas "$A" and "$B" for resource type "$RT" at the same time
    Then both requests succeeded with status 201
    And exactly one of the schemas "$A,$B" has status "PUBLISHED"
