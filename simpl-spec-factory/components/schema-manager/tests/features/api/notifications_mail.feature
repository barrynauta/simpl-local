@mail
Feature: E-mail notification to Providers
  SIMPL-14709 (new schema), SIMPL-14712 (new version), SIMPL-14791
  (revocation), restricted to RESOURCE schemas by SIMPL-33694. Subjects and
  body lines of D40, recipients of D39. Mails are found by the unique schema
  name in the subject.

  @SIMPL-14709 @SIMPL-33694 @D38 @D39 @D40
  Scenario: Creating a published RESOURCE schema notifies the Providers
    Given I am "ga-admin"
    When I create the RESOURCE schema "$A" for resource type "$RT"
    Then the response status is 201
    And a provider e-mail with the subject "Schema published: $A (version 1)" arrives
    And the e-mail body has the line "Name: $A"
    And the e-mail body has the line "Title: Title of $A"
    And the e-mail body has the line "Description: Description of $A"
    And the e-mail body has the line "Resource type: $RT"
    And the e-mail body has the line "Version: 1"
    And the e-mail body has the line "Change description: Initial version."
    And the e-mail body has a timestamp line
    And the e-mail body has a line starting with "Instructions: "
    And the e-mail went to every configured provider address
    And exactly 1 provider e-mail mentions schema "$A"

  @SIMPL-14712 @SIMPL-33694 @D40
  Scenario: A new published version of a RESOURCE schema notifies the Providers
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And I am "ga-admin"
    When I add a new version to schema "$A"
    Then the response status is 201
    And a provider e-mail with the subject "Schema new version published: $A (version 2)" arrives
    And the e-mail body has the line "Name: $A"
    And the e-mail body has the line "Version: 2"
    And the e-mail body has the line "Change description: Change number 2 of $A"
    And the e-mail body has a timestamp line
    And the e-mail body has a line starting with "Instructions: "
    And the e-mail went to every configured provider address

  @SIMPL-14712 @D40 @D42
  Scenario: A superseded version does not cause a revocation e-mail
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I add a new version to schema "$A"
    Then the response status is 201
    And no provider e-mail with the subject "Schema revoked: $A" arrives

  @SIMPL-14791 @SIMPL-33694 @D40
  Scenario: Revoking a RESOURCE schema notifies the Providers
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And I am "ga-admin"
    When I revoke schema "$A"
    Then the response status is 200
    And a provider e-mail with the subject "Schema revoked: $A" arrives
    And the e-mail body has the line "Name: $A"
    And the e-mail body has a timestamp line
    And the e-mail body has a line starting with "Instructions: "
    And the e-mail went to every configured provider address

  @SIMPL-14709 @SIMPL-20529 @D40
  Scenario: Publishing a revoked RESOURCE schema notifies the Providers
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And schema "$A" has been revoked
    When I publish schema "$A"
    Then the response status is 200
    And a provider e-mail with the subject "Schema published: $A (version 1)" arrives
    And exactly 3 provider e-mail mentions schema "$A"

  @SIMPL-14709 @D16 @D40
  Scenario: A schema that is revoked on create causes no e-mail
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I create the RESOURCE schema "$B" for resource type "$RT"
    Then the JSON field "status" is "REVOKED"
    And no provider e-mail mentions schema "$B"

  @SIMPL-14712 @D16 @D40
  Scenario: A new version that stays revoked causes no e-mail
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And the RESOURCE schema "$B" exists for resource type "$RT"
    When I add a new version to schema "$B"
    Then the JSON field "status" is "REVOKED"
    And no provider e-mail mentions schema "$B"

  @SIMPL-14709 @SIMPL-33694 @D40
  Scenario: A failed create causes no e-mail
    When I create the RESOURCE schema "$A" for resource type "$RT" from "syntax-error.ttl"
    Then the response status is 400
    And no provider e-mail mentions schema "$A"

  @SIMPL-14712 @SIMPL-33694 @D40
  Scenario: A failed new version causes no new e-mail
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I add a new version to schema "$A" from "missing-defaults.ttl"
    Then the response status is 400
    And exactly 1 provider e-mail mentions schema "$A"

  @SIMPL-14712 @D5 @D40 @rbac
  Scenario: A new version refused with 403 causes no e-mail
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And I am "ga-viewer"
    When I add a new version to schema "$A"
    Then the response status is 403
    And no provider e-mail with the subject "Schema new version published: $A (version 2)" arrives

  @SIMPL-14791 @SIMPL-33694 @D40
  Scenario: A failed revocation causes no e-mail
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And schema "$A" has been revoked
    When I revoke schema "$A"
    Then the response status is 409
    And exactly 2 provider e-mail mentions schema "$A"

  @SIMPL-20529 @D18 @D40
  Scenario: A publish refused with 409 causes no e-mail
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And the RESOURCE schema "$B" exists for resource type "$RT"
    When I publish schema "$B"
    Then the response status is 409
    And no provider e-mail mentions schema "$B"

  @SIMPL-33694 @D40 @D47 @search-serial
  Scenario: SEARCH schema changes never notify the Providers
    Given no SEARCH schema is published
    And I am "ga-admin"
    When I create the SEARCH schema "$S"
    Then the response status is 201
    When I add a new version to schema "$S"
    Then the response status is 201
    When I revoke schema "$S"
    Then the response status is 200
    When I publish schema "$S"
    Then the response status is 200
    And no provider e-mail mentions schema "$S"
