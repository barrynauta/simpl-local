Feature: Overlapping changes to one vocabulary
  Changes to one vocabulary are serialised; overlapping creations never give 409 (D43, D82).
  The test sends at most 4 parallel writes (D119).

  Background:
    Given I am authenticated as "ga-admin"

  @D43 @D82 @D119 @D4 @D7
  Scenario: Four overlapping new versions all succeed with distinct numbers
    Given a vocabulary "Alpha" exists
    When 4 new versions of "Alpha" are created in parallel
    Then all 4 responses have the status 201
    And the created version numbers are 2 to 5, each exactly once
    And the Location headers of the responses are all different
    And the vocabulary "Alpha" has 5 versions
    And version 5 of "Alpha" has the version status "ACTIVE"
    And version 1 of "Alpha" has the version status "DEPRECATED"
    And version 2 of "Alpha" has the version status "DEPRECATED"
    And version 3 of "Alpha" has the version status "DEPRECATED"
    And version 4 of "Alpha" has the version status "DEPRECATED"

  @D43 @D82 @D37 @negative
  Scenario: Two overlapping creations with the same name give one 201 and one 409
    When the vocabulary "Alpha" is created twice in parallel
    Then the responses have the statuses 201 and 409
    And exactly 1 vocabulary of this scenario is stored

  @D43 @D4 @negative
  Scenario: Two identical overlapping status changes give 200 and 409
    Given a vocabulary "Alpha" exists
    When the vocabulary status of "Alpha" is set to "DEPRECATED" twice in parallel
    Then the responses have the statuses 200 and 409
    And the vocabulary "Alpha" has the vocabulary status "DEPRECATED"
